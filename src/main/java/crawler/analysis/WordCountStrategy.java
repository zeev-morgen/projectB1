package crawler.analysis;

import crawler.model.PageResult;

import java.util.List;

/** Sums visible word counts across all pages that returned HTTP 200. */
public final class WordCountStrategy implements AnalysisStrategy {
    public static final String NAME = "WORD_COUNT";

    @Override
    public String name() { return NAME; }

    @Override
    public Object analyze(List<PageResult> pages) {
        long total = 0;
        for (PageResult p : pages) {
            if (p.getStatus() == 200) {
                total += p.getWordCount();
            }
        }
        return total;
    }
}
