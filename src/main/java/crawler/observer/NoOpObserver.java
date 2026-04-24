package crawler.observer;

/**
 * Sample {@link CrawlObserver} implementation that is intentionally silent.
 * Exists so the pattern is exercised at runtime and can be swapped out
 * for a logging / metrics observer without touching the core crawler.
 */
public final class NoOpObserver implements CrawlObserver {
    // All interface methods use default no-op implementations.
}
