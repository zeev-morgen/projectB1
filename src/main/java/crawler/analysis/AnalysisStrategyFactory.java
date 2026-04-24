package crawler.analysis;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Factory design pattern: maps analysis names to strategy suppliers.
 * To add a new analysis type, register it here once - all consumers
 * (CLI parser + runner) pick it up automatically (OCP).
 */
public final class AnalysisStrategyFactory {

    private static final Map<String, Supplier<AnalysisStrategy>> REGISTRY = new LinkedHashMap<>();

    static {
        REGISTRY.put(WordCountStrategy.NAME, WordCountStrategy::new);
        REGISTRY.put(MostLinkedDomainStrategy.NAME, MostLinkedDomainStrategy::new);
        REGISTRY.put(BrokenLinksStrategy.NAME, BrokenLinksStrategy::new);
        REGISTRY.put(KeywordFrequencyStrategy.NAME, KeywordFrequencyStrategy::new);
    }

    private AnalysisStrategyFactory() {}

    public static boolean isKnown(String name) {
        return REGISTRY.containsKey(name);
    }

    public static AnalysisStrategy create(String name) {
        Supplier<AnalysisStrategy> s = REGISTRY.get(name);
        if (s == null) {
            throw new IllegalArgumentException(name + " is unknown");
        }
        return s.get();
    }
}
