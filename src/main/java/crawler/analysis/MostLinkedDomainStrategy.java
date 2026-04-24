package crawler.analysis;

import crawler.model.PageResult;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Domain across all pages that appears most frequently as the target
 * of outgoing links. Lexicographical tie-break.
 */
public final class MostLinkedDomainStrategy implements AnalysisStrategy {
    public static final String NAME = "MOST_LINKED_DOMAIN";

    @Override
    public String name() { return NAME; }

    @Override
    public Object analyze(List<PageResult> pages) {
        Map<String, Long> counts = new HashMap<>();
        for (PageResult p : pages) {
            if (p.getStatus() != 200) continue;
            for (String link : p.getOutgoingLinks()) {
                String host = extractHost(link);
                if (host == null || host.isEmpty()) continue;
                counts.merge(host, 1L, Long::sum);
            }
        }
        if (counts.isEmpty()) return "";

        String best = null;
        long bestCount = -1L;
        for (Map.Entry<String, Long> e : counts.entrySet()) {
            long c = e.getValue();
            if (c > bestCount
                    || (c == bestCount && (best == null || e.getKey().compareTo(best) < 0))) {
                best = e.getKey();
                bestCount = c;
            }
        }
        return best == null ? "" : best;
    }

    private static String extractHost(String url) {
        try {
            String h = new URI(url).getHost();
            return h == null ? null : h.toLowerCase();
        } catch (Exception e) {
            return null;
        }
    }
}
