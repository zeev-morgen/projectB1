package crawler.analysis;

import crawler.model.PageResult;

import java.util.List;

/**
 * Strategy design pattern: analysis algorithm operating over the
 * first-discovery-ordered list of crawled pages.
 *
 * New analyses can be added by implementing this interface and
 * registering it with {@link AnalysisStrategyFactory} - the rest of
 * the system is closed for modification (OCP).
 */
public interface AnalysisStrategy {

    /** @return a stable name (used as the JSON key in the analysis output). */
    String name();

    /** Runs the analysis and returns a JSON-serializable value. */
    Object analyze(List<PageResult> pages);
}
