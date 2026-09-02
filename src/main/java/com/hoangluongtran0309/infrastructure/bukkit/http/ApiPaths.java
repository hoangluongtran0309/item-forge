package com.hoangluongtran0309.infrastructure.bukkit.http;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import com.sun.net.httpserver.HttpExchange;

final class ApiPaths {

    private ApiPaths() {
    }

    /** Parses a {@code key=value&key2=value2} query string into a map. */
    static Map<String, String> queryParams(HttpExchange exchange) {
        String query = exchange.getRequestURI().getQuery();
        if (query == null || query.isBlank()) {
            return Map.of();
        }

        Map<String, String> params = new HashMap<>();
        for (String pair : query.split("&")) {
            int eq = pair.indexOf('=');
            if (eq < 0) {
                continue;
            }
            String key = URLDecoder.decode(pair.substring(0, eq), StandardCharsets.UTF_8);
            String value = URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8);
            params.put(key, value);
        }
        return params;
    }

    /**
     * The remainder of the path after basePath. With basePath "/api/items":
     * "/api/items" -> "", "/api/items/" -> "", "/api/items/foo" -> "foo".
     * com.sun.net.httpserver itself matches a context by simple prefix, so the "/"
     * boundary is re-checked here to avoid false matches such as "/api/itemsxyz".
     */
    static String subPath(HttpExchange exchange, String basePath) {
        String path = exchange.getRequestURI().getPath();
        if (path.equals(basePath)) {
            return "";
        }
        if (path.startsWith(basePath + "/")) {
            return path.substring(basePath.length() + 1);
        }
        throw new ApiException(404, "Not found: " + path);
    }
}
