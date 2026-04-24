package crawler.output;

import crawler.model.PageResult;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;

/**
 * CSV extension: emits a pages table followed by a blank line and then
 * a key/value analysis table. Collection analyses are joined with " | ".
 */
public final class CsvFormatter implements OutputFormatter {

    private static final String SEP = ",";

    @Override
    public void write(ReportPayload payload, Path destination) throws IOException {
        Path parent = destination.toAbsolutePath().getParent();
        if (parent != null) Files.createDirectories(parent);

        try (BufferedWriter w = Files.newBufferedWriter(destination, StandardCharsets.UTF_8)) {
            w.write("url,title,wordCount,outgoingLinks,domain,status,depth");
            w.newLine();
            for (PageResult p : payload.getPages()) {
                w.write(esc(p.getUrl())); w.write(SEP);
                w.write(esc(p.getTitle())); w.write(SEP);
                w.write(Integer.toString(p.getWordCount())); w.write(SEP);
                w.write(Integer.toString(p.getOutgoingLinks().size())); w.write(SEP);
                w.write(esc(p.getDomain())); w.write(SEP);
                w.write(Integer.toString(p.getStatus())); w.write(SEP);
                w.write(Integer.toString(p.getDepth()));
                w.newLine();
            }
            w.newLine();
            w.write("analysis,value");
            w.newLine();
            for (Map.Entry<String, Object> e : payload.getAnalysis().entrySet()) {
                w.write(esc(e.getKey()));
                w.write(SEP);
                w.write(esc(stringify(e.getValue())));
                w.newLine();
            }
        }
    }

    private static String stringify(Object v) {
        if (v == null) return "";
        if (v instanceof Collection<?> c) {
            StringBuilder sb = new StringBuilder();
            boolean first = true;
            for (Object o : c) {
                if (!first) sb.append(" | ");
                sb.append(o == null ? "" : o.toString());
                first = false;
            }
            return sb.toString();
        }
        if (v instanceof Map<?, ?> m) {
            StringBuilder sb = new StringBuilder();
            boolean first = true;
            for (Map.Entry<?, ?> e : m.entrySet()) {
                if (!first) sb.append(" | ");
                sb.append(e.getKey()).append('=').append(e.getValue());
                first = false;
            }
            return sb.toString();
        }
        return v.toString();
    }

    private static String esc(String s) {
        if (s == null) return "";
        boolean needsQuote = s.indexOf(',') >= 0 || s.indexOf('"') >= 0
                || s.indexOf('\n') >= 0 || s.indexOf('\r') >= 0;
        String out = s.replace("\"", "\"\"");
        return needsQuote ? "\"" + out + "\"" : out;
    }
}
