package com.hoangluongtran0309.itemforge.dashboard.client;

import java.io.IOException;
import java.util.Map;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpResponse;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Parses the plugin API's {"error": "message"} error body into a PluginApiException. When
 * the body is not valid JSON -- a raw 502 from a proxy sitting in front of the plugin, say
 * -- a generic message is used instead.
 */
public final class PluginApiErrorHandler {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private PluginApiErrorHandler() {
    }

    public static void handle(HttpRequest request, ClientHttpResponse response) throws IOException {
        int status = response.getStatusCode().value();
        String message = "Plugin API request failed with status " + status;

        try {
            byte[] body = response.getBody().readAllBytes();
            if (body.length > 0) {
                Map<?, ?> parsed = MAPPER.readValue(body, Map.class);
                Object error = parsed.get("error");
                if (error != null) {
                    message = error.toString();
                }
            }
        } catch (IOException | RuntimeException e) {
            // The body is not valid JSON -- keep the generic message from above.
        }

        throw new PluginApiException(status, message);
    }
}
