package crawler.fetch;

import org.jsoup.nodes.Document;

/**
 * Raw outcome of fetching a URL: final status, final URL after redirects,
 * and (when status == 200) a parsed Jsoup document.
 */
public final class FetchResult {
    private final int status;
    private final String finalUrl;
    private final Document document;

    public FetchResult(int status, String finalUrl, Document document) {
        this.status = status;
        this.finalUrl = finalUrl;
        this.document = document;
    }

    public int getStatus() { return status; }
    public String getFinalUrl() { return finalUrl; }
    public Document getDocument() { return document; }
}
