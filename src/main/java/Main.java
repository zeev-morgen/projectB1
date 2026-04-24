import crawler.cli.CliArgs;
import crawler.cli.CliParser;
import crawler.core.Crawler;

/**
 * Entry point for the Concurrent Web Crawler & Content Analyzer.
 * Must be in the default package and named exactly "Main" per spec.
 */
public final class Main {

    public static void main(String[] args) {
        CliArgs parsed = CliParser.parse(args);
        if (parsed == null) {
            return;
        }
        Crawler crawler = new Crawler(parsed);
        crawler.run();
    }
}
