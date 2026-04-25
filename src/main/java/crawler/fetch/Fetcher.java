package crawler.fetch;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * HTTP fetcher: manually follows up to 20 redirects, enforces 3-second
 * connect + read timeouts. Uses Jsoup only as the HTTP client / parser.
 *
 * Stateless utility - thread-safe by construction.
 */
public final class Fetcher {

    public static final int MAX_REDIRECTS = 20;
    public static final int TIMEOUT_MS = 3000;

    private Fetcher() {}

    /**
     * Fetches the given URL, following up to {@link #MAX_REDIRECTS} redirects.
     *
     * @return FetchResult with the final HTTP status; document is populated
     *         only on status 200 with HTML-like content.
     * @throws IOException on network / timeout failure.
     */
    public static FetchResult fetch(String url) throws IOException {
        String current = url;
        for (int i = 0; i <= MAX_REDIRECTS; i++) {
            Connection.Response resp;
            try {
                resp = Jsoup.connect(current)
                        .timeout(TIMEOUT_MS)
                        .followRedirects(false)
                        .ignoreHttpErrors(true)
                        .ignoreContentType(true)
                        .validateTLSCertificates(false)
                        .userAgent("ConcurrentWebCrawler/1.0")
                        .execute();
            } catch (IOException ioe) {
                throw ioe;
            }

            int status = resp.statusCode();
            if (isRedirect(status) && resp.hasHeader("Location")) {
                if (i == MAX_REDIRECTS) {
                    throw new IOException("too many redirects");
                }
                String location = resp.header("Location");
                current = resolveLocation(current, location);
                continue;
            }

            Document doc = null;
            if (status == 200) {
                try {
                    doc = resp.parse();
                } catch (IOException parseErr) {
                    // non-HTML or malformed body; treat as empty document
                    doc = null;
                }
            }
            return new FetchResult(status, current, doc);
        }
        throw new IOException("too many redirects");
    }

    private static boolean isRedirect(int status) {
        return status == 301 || status == 302 || status == 303
                || status == 307 || status == 308;
    }

    private static String resolveLocation(String base, String location) throws IOException {
        try {
            URI baseUri = new URI(base);
            URI resolved = baseUri.resolve(location);
            return resolved.toString();
        } catch (URISyntaxException | IllegalArgumentException e) {
            throw new IOException("malformed redirect location");
        }
    }
}
