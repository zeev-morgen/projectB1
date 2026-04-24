package crawler.cli;

import crawler.analysis.AnalysisStrategyFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Parses CLI arguments into a validated {@link CliArgs} object.
 * Returns null and exits on any fatal validation error per spec.
 */
public final class CliParser {

    private CliParser() {}

    public static CliArgs parse(String[] args) {
        String analysisRaw = null;
        Integer poolSize = null;
        Integer depth = null;
        String inputFile = null;
        String outputFile = null;
        String format = "json";
        String domainsRaw = null;

        for (int i = 0; i < args.length; i++) {
            String a = args[i];
            switch (a) {
                case "--analysis" -> analysisRaw = next(args, ++i);
                case "--poolsize" -> {
                    String v = next(args, ++i);
                    poolSize = safeParseInt(v);
                    if (poolSize == null) {
                        System.err.println("invalid pool size");
                        System.exit(1);
                    }
                }
                case "--depth" -> {
                    String v = next(args, ++i);
                    depth = safeParseInt(v);
                    if (depth == null) {
                        System.err.println("invalid depth");
                        System.exit(1);
                    }
                }
                case "--input" -> inputFile = next(args, ++i);
                case "--output" -> outputFile = next(args, ++i);
                case "--format" -> format = next(args, ++i);
                case "--domains" -> domainsRaw = next(args, ++i);
                default -> {
                    // unknown flag silently ignored to keep CLI forgiving
                }
            }
        }

        if (poolSize == null || poolSize <= 0) {
            System.err.println("invalid pool size");
            System.exit(1);
        }
        if (depth == null || depth < 0) {
            System.err.println("invalid depth");
            System.exit(1);
        }
        if (inputFile == null) {
            System.err.println("invalid input file");
            System.exit(1);
        }
        if (outputFile == null) {
            // no specific spec message; treat as invalid output path at write time
            outputFile = "output.json";
        }
        if (format == null) {
            format = "json";
        }
        format = format.toLowerCase();
        if (!format.equals("json") && !format.equals("csv")) {
            // unknown format is out of strict spec, fall back to json silently
            format = "json";
        }

        List<String> validAnalyses = parseAnalyses(analysisRaw);
        if (validAnalyses.isEmpty()) {
            System.err.println("no valid analysis");
            System.exit(1);
        }

        Set<String> domainWhitelist = parseDomains(domainsRaw);

        return new CliArgs(validAnalyses, poolSize, depth,
                inputFile, outputFile, format, domainWhitelist);
    }

    private static List<String> parseAnalyses(String raw) {
        List<String> out = new ArrayList<>();
        if (raw == null || raw.isBlank()) {
            return out;
        }
        Set<String> seen = new LinkedHashSet<>();
        for (String token : raw.split(",")) {
            String name = token.trim();
            if (name.isEmpty()) continue;
            if (AnalysisStrategyFactory.isKnown(name)) {
                if (seen.add(name)) {
                    out.add(name);
                }
            } else {
                System.err.println(name + " is unknown");
            }
        }
        return out;
    }

    private static Set<String> parseDomains(String raw) {
        if (raw == null || raw.isBlank()) return null;
        Set<String> set = new LinkedHashSet<>();
        for (String token : raw.split(",")) {
            String d = token.trim().toLowerCase();
            if (!d.isEmpty()) set.add(d);
        }
        return set.isEmpty() ? null : set;
    }

    private static String next(String[] args, int i) {
        return i < args.length ? args[i] : null;
    }

    private static Integer safeParseInt(String v) {
        if (v == null) return null;
        try {
            return Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
