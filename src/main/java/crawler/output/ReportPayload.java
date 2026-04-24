package crawler.output;

import crawler.model.PageResult;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Plain-data carrier for what the formatters render. */
public final class ReportPayload {
    private final List<PageResult> pages;
    private final Map<String, Object> analysis;

    public ReportPayload(List<PageResult> pages, Map<String, Object> analysis) {
        this.pages = pages;
        this.analysis = analysis == null ? new LinkedHashMap<>() : analysis;
    }

    public List<PageResult> getPages() { return pages; }
    public Map<String, Object> getAnalysis() { return analysis; }
}
