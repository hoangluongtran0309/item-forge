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
 * Shared behaviour of the two Chat Completions providers, ChatGPT and DeepSeek. Their request
 * and response shapes are identical; only the endpoint, the name used in error messages, and
 * whether a {@code refusal} field exists differ.
 *
 * <p>JSON is forced via {@code response_format: json_object} rather than json_schema strict
 * mode, because DeepSeek does not support the latter and the two are kept symmetrical. That
 * means the model only guarantees syntactically valid JSON, not that it matches the schema, so
 * the shape has to be described in prose in the system message -- see
 * {@link StructuredAiRequest#schemaProse()}. The returned content is A STRING containing nested
 * JSON (unlike Claude's tool_use, which is already an object), so it has to be parsed twice.
 */
abstract class OpenAiCompatibleAdapter implements StructuredAiClient {

    private final String apiUrl;
    private final String providerName;
    private final String apiKey;
    private final String model;
    private final int maxTokens;
    private final Duration requestTimeout;
    private final Logger logger;
    private final HttpClient httpClient;

    protected OpenAiCompatibleAdapter(String apiUrl, String providerName, String apiKey, String model,
            int maxTokens, int timeoutSeconds, Logger logger) {
        this.apiUrl = apiUrl;
        this.providerName = providerName;
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
                .uri(URI.create(apiUrl))
                .header("Authorization", "Bearer " + apiKey)
                .header("content-type", "application/json")
                .timeout(requestTimeout)
                .POST(HttpRequest.BodyPublishers.ofString(buildRequestBody(request)))
                .build();

        return parseResponse(AiHttpCall.send(httpClient, httpRequest, providerName, logger));
    }

    String buildRequestBody(StructuredAiRequest request) {
        return "{"
                + "\"model\": \"" + AiJsonSupport.escapeJson(model) + "\","
                + "\"messages\": ["
                + "{\"role\": \"system\", \"content\": \""
                + AiJsonSupport.escapeJson(request.systemPromptWithSchemaProse()) + "\"},"
                + "{\"role\": \"user\", \"content\": \"" + AiJsonSupport.escapeJson(request.userMessage()) + "\"}"
                + "],"
                + "\"response_format\": {\"type\": \"json_object\"},"
                + "\"max_tokens\": " + maxTokens
                + "}";
    }

    Map<String, Object> parseResponse(String responseBody) {
        Map<String, Object> root = AiJsonSupport.parseJsonObject(responseBody,
                "Failed to parse " + providerName + " API response");

        if (root.get("error") instanceof Map<?, ?> error) {
            throw new AiRequestException(providerName + " API error: " + error.get("message"));
        }

        if (!(root.get("choices") instanceof List<?> choices) || choices.isEmpty()
                || !(choices.get(0) instanceof Map<?, ?> choice)) {
            throw new AiRequestException("Unexpected " + providerName + " API response shape: " + responseBody);
        }

        if (choice.get("finish_reason") instanceof String finishReason && "content_filter".equals(finishReason)) {
            throw new AiRequestException(providerName + " declined to answer this request (content filter)");
        }

        if (!(choice.get("message") instanceof Map<?, ?> message)) {
            throw new AiRequestException("Unexpected " + providerName + " API response shape: " + responseBody);
        }

        checkRefusal(message);

        if (!(message.get("content") instanceof String content)) {
            throw new AiRequestException(providerName + " response had no message content: " + responseBody);
        }

        return AiJsonSupport.parseJsonObject(content, "Failed to parse the JSON " + providerName + " returned");
    }

    /**
     * Only ChatGPT documents a {@code refusal} field, so the default is to ignore it.
     */
    void checkRefusal(Map<?, ?> message) {
        // Overridden where the provider documents a refusal field.
    }

    String providerName() {
        return providerName;
    }
}
