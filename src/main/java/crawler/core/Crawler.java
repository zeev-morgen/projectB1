package crawler.core;

import crawler.analysis.AnalysisStrategy;
import crawler.analysis.AnalysisStrategyFactory;
import crawler.cli.CliArgs;
import crawler.fetch.FetchResult;
import crawler.fetch.Fetcher;
import crawler.model.PageResult;
import crawler.observer.CrawlEventPublisher;
import crawler.observer.CrawlObserver;
import crawler.observer.NoOpObserver;
import crawler.output.OutputFormatter;
import crawler.output.OutputFormatterFactory;
import crawler.output.ReportPayload;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

/**
 * Orchestrates the concurrent crawl:
 *   - ConcurrentHashMap.putIfAbsent provides the atomic visited check (no
 *     global synchronized blocks, no busy-waiting).
 *   - Workers schedule child tasks and exit immediately; the main thread
 *     waits on a CountDownLatch that fires when the in-flight task counter
 *     reaches zero.
 *   - A ConcurrentLinkedQueue preserves first-discovery order; results are
 *     emitted in that order regardless of completion order.
 */
public final class Crawler {

    private static final Pattern WORD_PATTERN = Pattern.compile("\\S+");

    private final CliArgs args;
    private final ExecutorService executor;
    private final CrawlEventPublisher events;

    /** Visited set - putIfAbsent is the atomic dedup gate. */
    private final ConcurrentHashMap<String, Boolean> visited = new ConcurrentHashMap<>();

    /** First-discovery-ordered list of URLs. */
    private final ConcurrentLinkedQueue<String> discoveryOrder = new ConcurrentLinkedQueue<>();

    /** url -> PageResult, populated once the page is fetched. */
    private final ConcurrentHashMap<String, PageResult> results = new ConcurrentHashMap<>();

    private final AtomicInteger inFlight = new AtomicInteger(0);
    private final CountDownLatch done = new CountDownLatch(1);

    public Crawler(CliArgs args) {
        this.args = args;
        this.executor = Executors.newFixedThreadPool(args.getPoolSize(),
                r -> {
                    Thread t = new Thread(r, "crawler-worker");
                    t.setDaemon(true);
                    return t;
                });
        this.events = new CrawlEventPublisher();
        this.events.register(new NoOpObserver()); // demonstrates Observer plug-in point
    }

    /** Visible for tests. */
    public void registerObserver(CrawlObserver o) {
        events.register(o);
    }

    public void run() {
        List<String> seeds = readSeeds();
        if (seeds == null) return; // error already reported + exit

        // Bootstrap guard: treat the submission phase itself as one in-flight
        // unit so early worker completion cannot prematurely fire the latch.
        inFlight.incrementAndGet();
        for (String seed : seeds) {
            trySchedule(seed, 0);
        }
        if (inFlight.decrementAndGet() == 0) {
            done.countDown();
        }

        try {
            done.await();
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }

        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            executor.shutdownNow();
        }

        List<PageResult> orderedPages = collectOrderedResults();
        events.fireCrawlFinished(orderedPages.size());

        Map<String, Object> analysisOut = runAnalyses(orderedPages);
        writeReport(orderedPages, analysisOut);
    }

    // ---------------------------------------------------------------- internals

    private List<String> readSeeds() {
        Path p = Paths.get(args.getInputFile());
        if (!Files.isRegularFile(p)) {
            System.err.println("invalid input file");
            System.exit(1);
            return null;
        }
        try {
            List<String> raw = Files.readAllLines(p);
            List<String> seeds = new ArrayList<>();
            for (String line : raw) {
                String s = line.trim();
                if (!s.isEmpty()) seeds.add(s);
            }
            return seeds;
        } catch (IOException e) {
            System.err.println("invalid input file");
            System.exit(1);
            return null;
        }
    }

    /**
     * Atomically claims the URL and, if we won the race, schedules a worker.
     * Returns true if this call enqueued a task.
     */
    private boolean trySchedule(String rawUrl, int depth) {
        if (rawUrl == null || rawUrl.isBlank()) return false;
        if (!UrlUtils.isValidHttpUrl(rawUrl)) {
            System.err.println(rawUrl + " malformed");
            return false;
        }
        String normalized = UrlUtils.normalize(rawUrl);

        if (!domainAllowed(normalized)) return false;

        // atomic, lock-free visit check
        if (visited.putIfAbsent(normalized, Boolean.TRUE) != null) {
            return false;
        }
        discoveryOrder.add(normalized);
        events.fireDiscovered(normalized, depth);

        inFlight.incrementAndGet();
        try {
            executor.execute(() -> {
                try {
                    process(normalized, depth);
                } finally {
                    if (inFlight.decrementAndGet() == 0) {
                        done.countDown();
                    }
                }
            });
        } catch (RuntimeException ex) {
            // e.g. RejectedExecutionException on shutdown
            if (inFlight.decrementAndGet() == 0) {
                done.countDown();
            }
        }
        return true;
    }

    private boolean domainAllowed(String url) {
        Set<String> whitelist = args.getDomainWhitelist();
        if (whitelist == null) return true;
        String host = UrlUtils.host(url);
        if (host.isEmpty()) return false;
        for (String allow : whitelist) {
            if (host.equals(allow) || host.endsWith("." + allow)) return true;
        }
        return false;
    }

    private void process(String url, int depth) {
        FetchResult fr;
        try {
            fr = Fetcher.fetch(url);
        } catch (IOException ex) {
            System.err.println(url + " failed");
            events.firePageFailed(url, ex.getMessage());
            // Spec: "continue" - the URL was discovered but never got a 200.
            // We still record it so BROKEN_LINKS can include it.
            PageResult p = PageResult.builder()
                    .url(url)
                    .title("")
                    .wordCount(0)
                    .outgoingLinks(List.of())
                    .domain(UrlUtils.host(url))
                    .status(0)
                    .depth(depth)
                    .build();
            results.put(url, p);
            return;
        }

        int status = fr.getStatus();
        Document doc = fr.getDocument();

        String title = "";
        int wordCount = 0;
        List<String> links = List.of();
        String bodyText = "";

        if (status == 200 && doc != null) {
            title = doc.title();
            bodyText = doc.text();
            wordCount = countWords(bodyText);
            links = extractLinks(doc);
        }

        PageResult page = PageResult.builder()
                .url(url)
                .title(title)
                .wordCount(wordCount)
                .outgoingLinks(links)
                .domain(UrlUtils.host(url))
                .status(status)
                .depth(depth)
                .bodyText(bodyText)
                .build();
        results.put(url, page);
        events.firePageCompleted(page);

        if (status == 200 && depth < args.getDepth()) {
            int childDepth = depth + 1;
            for (String child : links) {
                trySchedule(child, childDepth);
            }
        }
    }

    private static int countWords(String text) {
        if (text == null || text.isEmpty()) return 0;
        int count = 0;
        var m = WORD_PATTERN.matcher(text);
        while (m.find()) count++;
        return count;
    }

    private static List<String> extractLinks(Document doc) {
        Elements anchors = doc.select("a[href]");
        List<String> out = new ArrayList<>(anchors.size());
        for (Element a : anchors) {
            String abs = a.absUrl("href");
            if (abs == null || abs.isEmpty()) continue;
            int hash = abs.indexOf('#');
            if (hash >= 0) abs = abs.substring(0, hash);
            if (abs.isEmpty()) continue;
            if (UrlUtils.isValidHttpUrl(abs)) {
                out.add(abs);
            }
        }
        return Collections.unmodifiableList(out);
    }

    private List<PageResult> collectOrderedResults() {
        List<PageResult> ordered = new ArrayList<>(discoveryOrder.size());
        for (String url : discoveryOrder) {
            PageResult r = results.get(url);
            if (r != null) ordered.add(r);
        }
        return ordered;
    }

    private Map<String, Object> runAnalyses(List<PageResult> pages) {
        Map<String, Object> out = new LinkedHashMap<>();
        for (String name : args.getAnalyses()) {
            AnalysisStrategy strategy = AnalysisStrategyFactory.create(name);
            out.put(strategy.name(), strategy.analyze(pages));
        }
        return out;
    }

    private void writeReport(List<PageResult> pages, Map<String, Object> analysis) {
        try {
            OutputFormatter formatter = OutputFormatterFactory.create(args.getFormat());
            formatter.write(new ReportPayload(pages, analysis),
                    Paths.get(args.getOutputFile()));
        } catch (IOException | RuntimeException e) {
            System.err.println("error saving report");
        }
    }

    // ---------- test-facing hooks ----------

    /** Visible for tests: all URLs that were discovered, in discovery order. */
    public List<String> discoveredUrls() {
        return new ArrayList<>(discoveryOrder);
    }

    /** Visible for tests: all page results in discovery order. */
    public List<PageResult> orderedResults() {
        return collectOrderedResults();
    }
}
