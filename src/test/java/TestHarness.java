import java.util.ArrayList;
import java.util.List;

/**
 * Zero-dependency test harness. Subclasses register named checks and
 * the runner prints results + exits non-zero on failure. Avoids pulling
 * JUnit so the project sticks to the two declared dependencies.
 */
public abstract class TestHarness {

    public interface Check {
        void run() throws Throwable;
    }

    private record Case(String name, Check check) {}

    private final List<Case> cases = new ArrayList<>();

    protected final void test(String name, Check check) {
        cases.add(new Case(name, check));
    }

    protected static void assertTrue(boolean cond, String message) {
        if (!cond) throw new AssertionError("expected true: " + message);
    }

    protected static void assertEquals(Object expected, Object actual, String message) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError(message
                    + " expected=<" + expected + "> actual=<" + actual + ">");
        }
    }

    /** Run all registered cases. @return true if all pass. */
    public final boolean runAll() {
        int passed = 0;
        int failed = 0;
        for (Case c : cases) {
            try {
                c.check.run();
                System.out.println("[PASS] " + c.name);
                passed++;
            } catch (Throwable t) {
                System.out.println("[FAIL] " + c.name + " -> " + t);
                t.printStackTrace(System.out);
                failed++;
            }
        }
        System.out.println();
        System.out.println("results: " + passed + " passed, " + failed + " failed");
        return failed == 0;
    }
}
