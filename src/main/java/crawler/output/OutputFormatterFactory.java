package crawler.output;

/** Factory design pattern for {@link OutputFormatter} implementations. */
public final class OutputFormatterFactory {

    private OutputFormatterFactory() {}

    public static OutputFormatter create(String format) {
        if (format == null) return new JsonFormatter();
        return switch (format.toLowerCase()) {
            case "csv" -> new CsvFormatter();
            case "json" -> new JsonFormatter();
            default -> new JsonFormatter();
        };
    }
}
