package crawler.output;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Strategy for writing the final report. Concrete implementations:
 * {@link JsonFormatter} (default) and {@link CsvFormatter} (extension).
 */
public interface OutputFormatter {
    void write(ReportPayload payload, Path destination) throws IOException;
}
