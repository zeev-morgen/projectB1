package crawler.output;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import crawler.model.PageResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Writes the report as the spec's canonical JSON object. */
public final class JsonFormatter implements OutputFormatter {

    @Override
    public void write(ReportPayload payload, Path destination) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);

        Map<String, Object> root = new LinkedHashMap<>();
        List<Map<String, Object>> pages = new ArrayList<>(payload.getPages().size());
        for (PageResult p : payload.getPages()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("url", p.getUrl());
            m.put("title", p.getTitle());
            m.put("wordCount", p.getWordCount());
            m.put("outgoingLinks", p.getOutgoingLinks().size());
            m.put("domain", p.getDomain());
            m.put("status", p.getStatus());
            m.put("depth", p.getDepth());
            pages.add(m);
        }
        root.put("pages", pages);
        root.put("analysis", payload.getAnalysis());

        Path parent = destination.toAbsolutePath().getParent();
        if (parent != null) Files.createDirectories(parent);
        mapper.writeValue(destination.toFile(), root);
    }
}
