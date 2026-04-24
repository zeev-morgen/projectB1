/** Runs every suite in-process; exits non-zero if any fail. */
public final class RunAllTests {
    public static void main(String[] args) {
        boolean ok = true;
        System.out.println("=== AnalysisTests ===");
        ok &= new AnalysisTests().runAll();
        System.out.println();
        System.out.println("=== CrawlerTests ===");
        ok &= new CrawlerTests().runAll();
        if (!ok) System.exit(1);
    }
}
