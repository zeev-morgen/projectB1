import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * Tiny in-process HTTP server used only by tests. JDK-provided, no
 * external dependency added.
 */
public final class LocalHttpServer implements AutoCloseable {

    public static final class Page {
        public final int status;
        public final String body;
        public Page(int status, String body) {
            this.status = status;
            this.body = body == null ? "" : body;
        }
    }

    private final HttpServer server;
    private final Map<String, Page> routes = new HashMap<>();
    private final int port;

    public LocalHttpServer() throws IOException {
        this.server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        this.port = server.getAddress().getPort();
        server.setExecutor(Executors.newFixedThreadPool(4));
        server.createContext("/", new Router());
        server.start();
    }

    public int port() { return port; }
    public String base() { return "http://127.0.0.1:" + port; }
    public String url(String path) { return base() + path; }

    public void add(String path, Page page) {
        routes.put(path, page);
    }

    public void addHtml(String path, String htmlBody) {
        routes.put(path, new Page(200, htmlBody));
    }

    @Override
    public void close() {
        server.stop(0);
    }

    private final class Router implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            String path = ex.getRequestURI().getPath();
            Page p = routes.get(path);
            if (p == null) {
                byte[] body = ("not found: " + path).getBytes(StandardCharsets.UTF_8);
                ex.sendResponseHeaders(404, body.length);
                try (OutputStream os = ex.getResponseBody()) { os.write(body); }
                return;
            }
            byte[] body = p.body.getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
            ex.sendResponseHeaders(p.status, body.length == 0 ? -1 : body.length);
            if (body.length > 0) {
                try (OutputStream os = ex.getResponseBody()) { os.write(body); }
            } else {
                ex.close();
            }
        }
    }
}
