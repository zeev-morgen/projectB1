import crawler.cli.CliArgs;
import crawler.core.Crawler;
import crawler.model.PageResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Test suite covering the concurrency + ordering guarantees required by
 * the spec: poolSize = 1, poolSize > 1, depth = 0, depth > 1, duplicate
 * and circular links, and a 100+ page graph.
 *
 * Each test brings up {@link LocalHttpServer}, configures the crawler
 * via {@link CliArgs}, and asserts on the outcome.
 */
public final class CrawlerTests extends TestHarness {

    public CrawlerTests() {
        test("poolSize=1 single seed depth=0", this::testPool1Depth0);
        test("poolSize>1 concurrent with depth=1", this::testConcurrentDepth1);
        test("depth=0 only visits seeds", this::testDepth0);
        test("depth>1 traverses transitively", this::testDepthGreaterThan1);
        test("duplicate URLs deduped (each processed once)", this::testDuplicates);
        test("circular links terminate and dedupe", this::testCircular);
        test("100+ pages crawled concurrently", this::test100Pages);
        test("discovery order preserved", this::testDiscoveryOrder);
        test("broken-link / non-200 recorded but not traversed", this::testBrokenLinks);
        test("domain whitelist filters out foreign hosts", this::testDomainWhitelist);
    }

    public static void main(String[] args) {
        boolean ok = new CrawlerTests().runAll();
        if (!ok) System.exit(1);
    }

    // ------------------------------------------------------------------ helpers

    private static Path writeSeeds(List<String> seeds) throws IOException {
        Path tmp = Files.createTempFile("seeds-", ".txt");
        Files.write(tmp, seeds);
        tmp.toFile().deleteOnExit();
        return tmp;
    }

    private static Path tempOut() throws IOException {
        Path tmp = Files.createTempFile("out-", ".json");
        tmp.toFile().deleteOnExit();
        return tmp;
    }

    private static CliArgs cfg(int pool, int depth, Path input, Path output) {
        return new CliArgs(List.of("WORD_COUNT", "BROKEN_LINKS"),
                pool, depth, input.toString(), output.toString(),
                "json", null);
    }

    private static String html(String title, String... links) {
        StringBuilder sb = new StringBuilder("<!doctype html><html><head><title>")
                .append(title).append("</title></head><body><p>")
                .append(title).append(" body ").append(title).append("</p>");
        for (String l : links) {
            sb.append("<a href=\"").append(l).append("\">x</a>");
        }
        sb.append("</body></html>");
        return sb.toString();
    }

    // --------------------------------------------------------------------- tests

    void testPool1Depth0() throws Exception {
        try (LocalHttpServer s = new LocalHttpServer()) {
            s.addHtml("/a", html("A"));
            Path seeds = writeSeeds(List.of(s.url("/a")));
            Path out = tempOut();
            Crawler c = new Crawler(cfg(1, 0, seeds, out));
            c.run();
            List<PageResult> pages = c.orderedResults();
            assertEquals(1, pages.size(), "single seed only");
            assertEquals(200, pages.get(0).getStatus(), "200 OK");
            assertTrue(pages.get(0).getWordCount() > 0, "word count > 0");
        }
    }

    void testConcurrentDepth1() throws Exception {
        try (LocalHttpServer s = new LocalHttpServer()) {
            s.addHtml("/home", html("home",
                    s.url("/a"), s.url("/b"), s.url("/c"), s.url("/d")));
            s.addHtml("/a", html("A"));
            s.addHtml("/b", html("B"));
            s.addHtml("/c", html("C"));
            s.addHtml("/d", html("D"));
            Path seeds = writeSeeds(List.of(s.url("/home")));
            Path out = tempOut();
            Crawler c = new Crawler(cfg(4, 1, seeds, out));
            c.run();
            assertEquals(5, c.orderedResults().size(), "home + 4 children");
        }
    }

    void testDepth0() throws Exception {
        try (LocalHttpServer s = new LocalHttpServer()) {
            s.addHtml("/root", html("root", s.url("/child")));
            s.addHtml("/child", html("child"));
            Path seeds = writeSeeds(List.of(s.url("/root")));
            Path out = tempOut();
            Crawler c = new Crawler(cfg(2, 0, seeds, out));
            c.run();
            List<PageResult> pages = c.orderedResults();
            assertEquals(1, pages.size(), "depth=0 means seed only");
            assertEquals(s.url("/root"), pages.get(0).getUrl(), "seed url");
        }
    }

    void testDepthGreaterThan1() throws Exception {
        try (LocalHttpServer s = new LocalHttpServer()) {
            s.addHtml("/l0", html("l0", s.url("/l1")));
            s.addHtml("/l1", html("l1", s.url("/l2")));
            s.addHtml("/l2", html("l2", s.url("/l3")));
            s.addHtml("/l3", html("l3"));
            Path seeds = writeSeeds(List.of(s.url("/l0")));
            Path out = tempOut();
            Crawler c = new Crawler(cfg(2, 2, seeds, out));
            c.run();
            List<PageResult> pages = c.orderedResults();
            // depth 2 => l0 (d0), l1 (d1), l2 (d2). l3 not traversed.
            assertEquals(3, pages.size(), "depth=2 => 3 pages");
            assertEquals(0, pages.get(0).getDepth(), "seed at depth 0");
            assertEquals(1, pages.get(1).getDepth(), "child at depth 1");
            assertEquals(2, pages.get(2).getDepth(), "grandchild at depth 2");
        }
    }

    void testDuplicates() throws Exception {
        try (LocalHttpServer s = new LocalHttpServer()) {
            String target = s.url("/t");
            s.addHtml("/root", html("root", target, target, target, target));
            s.addHtml("/t", html("T"));
            Path seeds = writeSeeds(List.of(s.url("/root"), s.url("/root")));
            Path out = tempOut();
            Crawler c = new Crawler(cfg(4, 1, seeds, out));
            c.run();
            List<PageResult> pages = c.orderedResults();
            assertEquals(2, pages.size(), "root + single t");
        }
    }

    void testCircular() throws Exception {
        try (LocalHttpServer s = new LocalHttpServer()) {
            s.addHtml("/a", html("A", s.url("/b")));
            s.addHtml("/b", html("B", s.url("/a"))); // cycle
            Path seeds = writeSeeds(List.of(s.url("/a")));
            Path out = tempOut();
            Crawler c = new Crawler(cfg(4, 10, seeds, out));
            c.run();
            assertEquals(2, c.orderedResults().size(), "cycle terminates at 2 nodes");
        }
    }

    void test100Pages() throws Exception {
        try (LocalHttpServer s = new LocalHttpServer()) {
            int n = 120;
            List<String> children = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                String path = "/p" + i;
                children.add(s.url(path));
                s.addHtml(path, html("P" + i));
            }
            s.addHtml("/hub", html("hub", children.toArray(new String[0])));
            Path seeds = writeSeeds(List.of(s.url("/hub")));
            Path out = tempOut();
            Crawler c = new Crawler(cfg(16, 1, seeds, out));
            long t0 = System.currentTimeMillis();
            c.run();
            long dt = System.currentTimeMillis() - t0;
            assertEquals(n + 1, c.orderedResults().size(), "hub + all children");
            assertTrue(dt < 30000, "completes in reasonable time: " + dt + "ms");
        }
    }

    void testDiscoveryOrder() throws Exception {
        try (LocalHttpServer s = new LocalHttpServer()) {
            s.addHtml("/root", html("root",
                    s.url("/alpha"), s.url("/beta"), s.url("/gamma")));
            s.addHtml("/alpha", html("alpha"));
            s.addHtml("/beta", html("beta"));
            s.addHtml("/gamma", html("gamma"));
            Path seeds = writeSeeds(List.of(s.url("/root")));
            Path out = tempOut();
            Crawler c = new Crawler(cfg(8, 1, seeds, out));
            c.run();
            List<String> urls = new ArrayList<>();
            for (PageResult p : c.orderedResults()) urls.add(p.getUrl());
            assertEquals(s.url("/root"), urls.get(0), "root first");
            // children must follow root and appear in discovery (link) order
            assertEquals(4, urls.size(), "4 urls total");
            assertEquals(s.url("/alpha"), urls.get(1), "alpha second");
            assertEquals(s.url("/beta"), urls.get(2), "beta third");
            assertEquals(s.url("/gamma"), urls.get(3), "gamma fourth");
        }
    }

    void testBrokenLinks() throws Exception {
        try (LocalHttpServer s = new LocalHttpServer()) {
            s.addHtml("/root", html("root", s.url("/missing"), s.url("/ok")));
            // /missing intentionally not registered -> 404
            s.addHtml("/ok", html("ok"));
            Path seeds = writeSeeds(List.of(s.url("/root")));
            Path out = tempOut();
            Crawler c = new Crawler(cfg(4, 1, seeds, out));
            c.run();
            List<PageResult> pages = c.orderedResults();
            boolean hasMissing = pages.stream().anyMatch(
                    p -> p.getUrl().equals(s.url("/missing")) && p.getStatus() == 404);
            boolean missingHasNoLinks = pages.stream()
                    .filter(p -> p.getUrl().equals(s.url("/missing")))
                    .findFirst().get().getOutgoingLinks().isEmpty();
            assertTrue(hasMissing, "missing page recorded as 404");
            assertTrue(missingHasNoLinks, "non-200 page has no outgoing links");
        }
    }

    void testDomainWhitelist() throws Exception {
        try (LocalHttpServer s = new LocalHttpServer()) {
            s.addHtml("/root", html("root",
                    s.url("/a"), "http://example.invalid/external"));
            s.addHtml("/a", html("a"));
            Path seeds = writeSeeds(List.of(s.url("/root")));
            Path out = tempOut();
            CliArgs args = new CliArgs(
                    List.of("WORD_COUNT"),
                    2, 1,
                    seeds.toString(), out.toString(),
                    "json",
                    new java.util.LinkedHashSet<>(Collections.singletonList("127.0.0.1")));
            Crawler c = new Crawler(args);
            c.run();
            for (PageResult p : c.orderedResults()) {
                assertTrue(p.getUrl().contains("127.0.0.1"),
                        "whitelist drops foreign host, got " + p.getUrl());
            }
        }
    }
}
