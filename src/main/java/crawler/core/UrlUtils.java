package crawler.core;

import java.net.URI;
import java.net.URISyntaxException;

/** Normalization + validation helpers for URLs. */
public final class UrlUtils {

    private UrlUtils() {}

    /** Returns true iff the URL is syntactically valid and http/https. */
    public static boolean isValidHttpUrl(String raw) {
        if (raw == null || raw.isBlank()) return false;
        try {
            URI u = new URI(raw);
            if (!u.isAbsolute()) return false;
            String scheme = u.getScheme();
            if (scheme == null) return false;
            scheme = scheme.toLowerCase();
            if (!scheme.equals("http") && !scheme.equals("https")) return false;
            return u.getHost() != null && !u.getHost().isEmpty();
        } catch (URISyntaxException e) {
            return false;
        }
    }

    /** Extract lower-cased hostname; empty string on failure. */
    public static String host(String url) {
        try {
            String h = new URI(url).getHost();
            return h == null ? "" : h.toLowerCase();
        } catch (URISyntaxException e) {
            return "";
        }
    }

    /**
     * Normalize a URL for dedup purposes - strips fragment, lowercases
     * scheme + host, removes default ports. Returns the original string
     * unchanged if parsing fails.
     */
    public static String normalize(String url) {
        try {
            URI u = new URI(url);
            String scheme = u.getScheme() == null ? null : u.getScheme().toLowerCase();
            String host = u.getHost() == null ? null : u.getHost().toLowerCase();
            int port = u.getPort();
            if (("http".equals(scheme) && port == 80)
                    || ("https".equals(scheme) && port == 443)) {
                port = -1;
            }
            URI normalized = new URI(scheme, u.getUserInfo(), host, port,
                    u.getPath(), u.getQuery(), null);
            return normalized.toString();
        } catch (URISyntaxException e) {
            return url;
        }
    }
}
