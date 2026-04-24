package crawler.analysis;

import crawler.model.PageResult;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Case-insensitive frequency counter for the fixed keywords: java, thread, pattern. */
public final class KeywordFrequencyStrategy implements AnalysisStrategy {
    public static final String NAME = "KEYWORD_FREQUENCY";

    private static final String[] KEYWORDS = {"java", "thread", "pattern"};
    private static final Pattern WORD_SPLIT = Pattern.compile("[A-Za-z]+");

    @Override
    public String name() { return NAME; }

    @Override
    public Object analyze(List<PageResult> pages) {
        Map<String, Long> freq = new LinkedHashMap<>();
        for (String k : KEYWORDS) freq.put(k, 0L);

        for (PageResult p : pages) {
            if (p.getStatus() != 200) continue;
            String text = p.getBodyText();
            if (text == null || text.isEmpty()) continue;
            Matcher m = WORD_SPLIT.matcher(text);
            while (m.find()) {
                String w = m.group().toLowerCase();
                if (freq.containsKey(w)) {
                    freq.merge(w, 1L, Long::sum);
                }
            }
        }
        return freq;
    }
}
