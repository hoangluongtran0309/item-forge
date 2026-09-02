package com.hoangluongtran0309.infrastructure.bukkit.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

import com.sun.net.httpserver.HttpExchange;

import com.hoangluongtran0309.infrastructure.json.JsonWriter;

/**
 * Reads and writes JSON for the ApiResourceHandlers. Reading goes through SnakeYAML (JSON
 * is a subset of YAML, so it parses fine and no extra JSON library is needed); writing goes
 * through JsonWriter, because the SnakeYAML dumper does not guarantee valid JSON.
 */
final class HttpJson {

    private HttpJson() {
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> readBody(HttpExchange exchange) throws IOException {
        byte[] bytes;
        try (InputStream in = exchange.getRequestBody()) {
            bytes = in.readAllBytes();
        }

        if (bytes.length == 0) {
            return Map.of();
        }

        Object parsed;
        try {
            parsed = new Yaml().load(new String(bytes, StandardCharsets.UTF_8));
        } catch (YAMLException e) {
            throw new ApiException(400, "Invalid JSON request body: " + e.getMessage());
        }

        if (!(parsed instanceof Map<?, ?> map)) {
            throw new ApiException(400, "Request body must be a JSON object");
        }
        return (Map<String, Object>) map;
    }

    static void send(HttpExchange exchange, int status, Object body) throws IOException {
        byte[] bytes = body == null ? new byte[0] : JsonWriter.write(body).getBytes(StandardCharsets.UTF_8);
        if (bytes.length > 0) {
            exchange.getResponseHeaders().add("Content-Type", "application/json");
        }
        exchange.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
        if (bytes.length > 0) {
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    static void sendError(HttpExchange exchange, int status, String message) throws IOException {
        send(exchange, status, Map.of("error", message));
    }
}
