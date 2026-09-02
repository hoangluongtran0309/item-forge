package com.hoangluongtran0309.infrastructure.resourcepack;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;
import java.util.logging.Logger;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

public class PackHttpServer {

    public static final String CONTEXT_PATH = "/resource-pack.zip";

    private final int port;
    private final Supplier<Path> zipFileSupplier;
    private final Logger logger;
    private HttpServer server;
    private ExecutorService executor;

    public PackHttpServer(int port, Supplier<Path> zipFileSupplier, Logger logger) {
        this.port = port;
        this.zipFileSupplier = zipFileSupplier;
        this.logger = logger;
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        executor = Executors.newFixedThreadPool(4); // avoids a bottleneck when many players join at once
        server.setExecutor(executor);
        server.createContext(CONTEXT_PATH, this::handleRequest);
        server.start();
        logger.info("Resource pack HTTP server started on port " + port);
    }

    private void handleRequest(HttpExchange exchange) throws IOException {
        // Re-read the file on every request rather than caching a byte[] when the route is
        // registered, because rebuild() can run after the server has started and overwrite
        // the zip.
        Path zipFile = zipFileSupplier.get();
        if (zipFile == null || Files.notExists(zipFile)) {
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
            return;
        }

        byte[] bytes = Files.readAllBytes(zipFile);
        exchange.getResponseHeaders().add("Content-Type", "application/zip");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
        if (executor != null) {
            executor.shutdownNow();
        }
    }
}
