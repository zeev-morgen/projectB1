package crawler.analysis;

import crawler.model.PageResult;

import java.util.ArrayList;
import java.util.List;

/** Collects URLs with a final status != 200, preserving first-discovery order. */
public final class BrokenLinksStrategy implements AnalysisStrategy {
    public static final String NAME = "BROKEN_LINKS";

    @Override
    public String name() { return NAME; }

    @Override
    public Object analyze(List<PageResult> pages) {
        List<String> broken = new ArrayList<>();
        for (PageResult p : pages) {
            if (p.getStatus() != 200) {
                broken.add(p.getUrl());
            }
        }
        return broken;
    }
}
