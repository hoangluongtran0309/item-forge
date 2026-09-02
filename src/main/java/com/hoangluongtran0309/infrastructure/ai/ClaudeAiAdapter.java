package com.hoangluongtran0309.infrastructure.ai;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.hoangluongtran0309.application.exception.AiRequestException;

/**
 * Adapter for the Anthropic Messages API, using java.net.http.HttpClient (built into the JDK)
 * and parsing the returned JSON with SnakeYAML. Structured output is obtained through forced
 * tool use, which is the strongest guarantee of the four providers: the response arrives as an
 * object rather than as a JSON string that has to be parsed twice.
 */
public class ClaudeAiAdapter implements StructuredAiClient {

    private static final String API_URL = "https://api.anthropic.com/v1/messages";
    private static final String ANTHROPIC_VERSION = "2023-06-01";

    private final String apiKey;
    private final String model;
    private final int maxTokens;
    private final Duration requestTimeout;
    private final Logger logger;
    private final HttpClient httpClient;

    public ClaudeAiAdapter(String apiKey, String model, int maxTokens, int timeoutSeconds, Logger logger) {
        this.apiKey = apiKey;
        this.model = model;
        this.maxTokens = maxTokens;
        this.requestTimeout = Duration.ofSeconds(timeoutSeconds);
        this.logger = logger;
        this.httpClient = AiHttpCall.newHttpClient(timeoutSeconds);
    }

    @Override
    public Map<String, Object> requestJsonObject(StructuredAiRequest request) {
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("x-api-key", apiKey)
                .header("anthropic-version", ANTHROPIC_VERSION)
                .header("content-type", "application/json")
                .timeout(requestTimeout)
                .POST(HttpRequest.BodyPublishers.ofString(buildRequestBody(request)))
                .build();

        return parseResponse(request, AiHttpCall.send(httpClient, httpRequest, "Claude", logger));
    }

    String buildRequestBody(StructuredAiRequest request) {
        return "{"
                + "\"model\": \"" + AiJsonSupport.escapeJson(model) + "\","
                + "\"max_tokens\": " + maxTokens + ","
                + "\"system\": \"" + AiJsonSupport.escapeJson(request.systemPrompt()) + "\","
                + "\"messages\": [{\"role\": \"user\", \"content\": \""
                + AiJsonSupport.escapeJson(request.userMessage()) + "\"}],"
                + "\"tools\": [" + toolDefinition(request) + "],"
                + "\"tool_choice\": {\"type\": \"tool\", \"name\": \""
                + AiJsonSupport.escapeJson(request.schemaName()) + "\"}"
                + "}";
    }

    private static String toolDefinition(StructuredAiRequest request) {
        return "{"
                + "\"name\": \"" + AiJsonSupport.escapeJson(request.schemaName()) + "\","
                + "\"description\": \"" + AiJsonSupport.escapeJson(request.schemaDescription()) + "\","
                + "\"input_schema\": " + request.jsonSchema()
                + "}";
    }

    @SuppressWarnings("unchecked")
    Map<String, Object> parseResponse(StructuredAiRequest request, String responseBody) {
        Map<String, Object> root = AiJsonSupport.parseJsonObject(responseBody,
                "Failed to parse Claude API response");

        if ("refusal".equals(root.get("stop_reason"))) {
            throw new AiRequestException("Claude declined to answer this request");
        }

        if (!(root.get("content") instanceof List<?> content)) {
            throw new AiRequestException("Unexpected Claude API response shape: " + responseBody);
        }

        for (Object rawBlock : content) {
            if (rawBlock instanceof Map<?, ?> block && "tool_use".equals(block.get("type"))
                    && request.schemaName().equals(block.get("name"))
                    && block.get("input") instanceof Map<?, ?> input) {
                return (Map<String, Object>) input;
            }
        }

        throw new AiRequestException("Claude response had no tool_use block: " + responseBody);
    }
}
