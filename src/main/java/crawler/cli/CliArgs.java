package crawler.cli;

import java.util.List;
import java.util.Set;

/**
 * Immutable holder for parsed command-line arguments.
 */
public final class CliArgs {
    private final List<String> analyses;
    private final int poolSize;
    private final int depth;
    private final String inputFile;
    private final String outputFile;
    private final String format;
    private final Set<String> domainWhitelist;

    public CliArgs(List<String> analyses, int poolSize, int depth,
                   String inputFile, String outputFile,
                   String format, Set<String> domainWhitelist) {
        this.analyses = List.copyOf(analyses);
        this.poolSize = poolSize;
        this.depth = depth;
        this.inputFile = inputFile;
        this.outputFile = outputFile;
        this.format = format;
        this.domainWhitelist = domainWhitelist == null ? null : Set.copyOf(domainWhitelist);
    }

    public List<String> getAnalyses() { return analyses; }
    public int getPoolSize() { return poolSize; }
    public int getDepth() { return depth; }
    public String getInputFile() { return inputFile; }
    public String getOutputFile() { return outputFile; }
    public String getFormat() { return format; }
    public Set<String> getDomainWhitelist() { return domainWhitelist; }
}
