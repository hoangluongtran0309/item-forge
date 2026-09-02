package com.hoangluongtran0309.infrastructure.bukkit.http;

import java.io.IOException;
import java.io.OutputStream;

import com.sun.net.httpserver.HttpExchange;

// Writes binary responses (a PNG image, for example) for the ApiResourceHandlers. Kept
// separate from HttpJson, which deals only in JSON and is not meant for raw bytes.
final class HttpBinary {

    private HttpBinary() {
    }

    static void send(HttpExchange exchange, int status, String contentType, byte[] bytes) throws IOException {
        exchange.getResponseHeaders().add("Content-Type", contentType);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
