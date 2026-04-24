package crawler.model;

import java.util.Collections;
import java.util.List;

/**
 * Immutable record of a crawled page. Constructed via {@link Builder}
 * (Builder design pattern).
 */
public final class PageResult {
    private final String url;
    private final String title;
    private final int wordCount;
    private final List<String> outgoingLinks;
    private final String domain;
    private final int status;
    private final int depth;
    private final String bodyText;

    private PageResult(Builder b) {
        this.url = b.url;
        this.title = b.title == null ? "" : b.title;
        this.wordCount = b.wordCount;
        this.outgoingLinks = b.outgoingLinks == null
                ? List.of()
                : Collections.unmodifiableList(b.outgoingLinks);
        this.domain = b.domain == null ? "" : b.domain;
        this.status = b.status;
        this.depth = b.depth;
        this.bodyText = b.bodyText == null ? "" : b.bodyText;
    }

    public String getUrl() { return url; }
    public String getTitle() { return title; }
    public int getWordCount() { return wordCount; }
    public List<String> getOutgoingLinks() { return outgoingLinks; }
    public String getDomain() { return domain; }
    public int getStatus() { return status; }
    public int getDepth() { return depth; }
    public String getBodyText() { return bodyText; }

    public static Builder builder() { return new Builder(); }

    /** Builder for {@link PageResult}. */
    public static final class Builder {
        private String url;
        private String title;
        private int wordCount;
        private List<String> outgoingLinks;
        private String domain;
        private int status;
        private int depth;
        private String bodyText;

        public Builder url(String url) { this.url = url; return this; }
        public Builder title(String title) { this.title = title; return this; }
        public Builder wordCount(int wc) { this.wordCount = wc; return this; }
        public Builder outgoingLinks(List<String> l) { this.outgoingLinks = l; return this; }
        public Builder domain(String d) { this.domain = d; return this; }
        public Builder status(int s) { this.status = s; return this; }
        public Builder depth(int d) { this.depth = d; return this; }
        public Builder bodyText(String t) { this.bodyText = t; return this; }
        public PageResult build() { return new PageResult(this); }
    }
}
