import crawler.analysis.AnalysisStrategyFactory;
import crawler.analysis.BrokenLinksStrategy;
import crawler.analysis.KeywordFrequencyStrategy;
import crawler.analysis.MostLinkedDomainStrategy;
import crawler.analysis.WordCountStrategy;
import crawler.model.PageResult;

import java.util.List;
import java.util.Map;

/** Unit-style tests for the analysis strategies (no network). */
public final class AnalysisTests extends TestHarness {

    public AnalysisTests() {
        test("WORD_COUNT sums 200s only", this::wordCountOnly200);
        test("MOST_LINKED_DOMAIN lexicographic tie-break", this::mostLinkedTie);
        test("BROKEN_LINKS preserves discovery order", this::brokenLinksOrder);
        test("KEYWORD_FREQUENCY case-insensitive counts", this::keywordFrequency);
        test("factory rejects unknown analyses", this::unknownAnalysis);
    }

    public static void main(String[] args) {
        boolean ok = new AnalysisTests().runAll();
        if (!ok) System.exit(1);
    }

    void wordCountOnly200() {
        PageResult a = PageResult.builder().url("http://h/a").status(200).wordCount(10).build();
        PageResult b = PageResult.builder().url("http://h/b").status(404).wordCount(5).build();
        PageResult c = PageResult.builder().url("http://h/c").status(200).wordCount(3).build();
        Object result = new WordCountStrategy().analyze(List.of(a, b, c));
        assertEquals(13L, result, "200-only word count");
    }

    void mostLinkedTie() {
        PageResult p = PageResult.builder().url("http://s/p").status(200)
                .outgoingLinks(List.of(
                        "http://zeta.test/1",
                        "http://alpha.test/1",
                        "http://zeta.test/2",
                        "http://alpha.test/2"))
                .build();
        Object result = new MostLinkedDomainStrategy().analyze(List.of(p));
        assertEquals("alpha.test", result, "alpha wins lexicographic tie");
    }

    void brokenLinksOrder() {
        PageResult a = PageResult.builder().url("http://h/a").status(200).build();
        PageResult b = PageResult.builder().url("http://h/b").status(500).build();
        PageResult c = PageResult.builder().url("http://h/c").status(200).build();
        PageResult d = PageResult.builder().url("http://h/d").status(0).build();
        Object result = new BrokenLinksStrategy().analyze(List.of(a, b, c, d));
        assertEquals(List.of("http://h/b", "http://h/d"), result,
                "discovery-ordered broken list");
    }

    void keywordFrequency() {
        PageResult a = PageResult.builder().url("http://h/a").status(200)
                .bodyText("Java java THREAD pattern pattern pattern unrelated")
                .build();
        PageResult b = PageResult.builder().url("http://h/b").status(500)
                .bodyText("java thread").build(); // ignored (non-200)
        Object result = new KeywordFrequencyStrategy().analyze(List.of(a, b));
        @SuppressWarnings("unchecked")
        Map<String, Long> freq = (Map<String, Long>) result;
        assertEquals(2L, freq.get("java"), "java count");
        assertEquals(1L, freq.get("thread"), "thread count");
        assertEquals(3L, freq.get("pattern"), "pattern count");
    }

    void unknownAnalysis() {
        assertTrue(!AnalysisStrategyFactory.isKnown("FOO"), "unknown=false");
        assertTrue(AnalysisStrategyFactory.isKnown("WORD_COUNT"), "WORD_COUNT known");
        try {
            AnalysisStrategyFactory.create("FOO");
            throw new AssertionError("should have thrown");
        } catch (IllegalArgumentException expected) {
            // ok
        }
    }
}
