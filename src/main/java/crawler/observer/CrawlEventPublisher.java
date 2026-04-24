package crawler.observer;

import crawler.model.PageResult;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Publisher (Subject) side of the Observer pattern. Safe to call from
 * any thread; listener list uses copy-on-write so iteration never
 * blocks writers.
 */
public final class CrawlEventPublisher {

    private final List<CrawlObserver> observers = new CopyOnWriteArrayList<>();

    public void register(CrawlObserver o) {
        if (o != null) observers.add(o);
    }

    public void fireDiscovered(String url, int depth) {
        for (CrawlObserver o : observers) o.onDiscovered(url, depth);
    }

    public void firePageCompleted(PageResult page) {
        for (CrawlObserver o : observers) o.onPageCompleted(page);
    }

    public void firePageFailed(String url, String reason) {
        for (CrawlObserver o : observers) o.onPageFailed(url, reason);
    }

    public void fireCrawlFinished(int totalPages) {
        for (CrawlObserver o : observers) o.onCrawlFinished(totalPages);
    }
}
