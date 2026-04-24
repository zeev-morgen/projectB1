package crawler.observer;

import crawler.model.PageResult;

/**
 * Observer design pattern: pluggable listener notified by the crawler
 * as events happen. Implementations must be thread-safe - they are
 * invoked from worker threads.
 */
public interface CrawlObserver {

    /** Called after a URL has been scheduled (visit-check won) for processing. */
    default void onDiscovered(String url, int depth) {}

    /** Called when a page has been fetched (any status code, incl. 200). */
    default void onPageCompleted(PageResult page) {}

    /** Called when a URL fails to be fetched (I/O / timeout / malformed). */
    default void onPageFailed(String url, String reason) {}

    /** Called exactly once when the crawl finishes (all tasks drained). */
    default void onCrawlFinished(int totalPages) {}
}
