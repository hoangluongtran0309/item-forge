package com.hoangluongtran0309.infrastructure.ai;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.hoangluongtran0309.application.exception.AiRequestException;

/**
 * Adapter for the Google Gemini generateContent API. Unlike the other three adapters, the
 * api-key travels in the URL query string rather than a header, so buildRequestUri()/the URI
 * must NEVER be written to a log or an exception -- only the response body or e.getMessage()
 * may be logged. JSON is forced via generationConfig.responseSchema (Google validates and
 * constrains it server-side, which is closer to Claude's tool use than to OpenAI/DeepSeek's
 * response_format), using the trimmed-down OpenAPI 3.0 schema dialect that
 * {@link AiJsonSupport#toGeminiSchema(String)} produces. candidates[0].content.parts[0].text is
 * A STRING containing nested JSON, so it still has to be parsed twice.
 */
public class GeminiAiAdapter implements StructuredAiClient {

    private static final String API_BASE = "https://generativelanguage.googleapis.com/v1beta/models/";

    private final String apiKey;
    private final String model;
    private final int maxTokens;
    private final Duration requestTimeout;
    private final Logger logger;
    private final HttpClient httpClient;

    public GeminiAiAdapter(String apiKey, String model, int maxTokens, int timeoutSeconds, Logger logger) {
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
                .uri(buildRequestUri())
                .header("content-type", "application/json")
                .timeout(requestTimeout)
                .POST(HttpRequest.BodyPublishers.ofString(buildRequestBody(request)))
                .build();

        return parseResponse(AiHttpCall.send(httpClient, httpRequest, "Gemini", logger));
    }

    URI buildRequestUri() {
        String encodedKey = URLEncoder.encode(apiKey, StandardCharsets.UTF_8);
        return URI.create(API_BASE + model + ":generateContent?key=" + encodedKey);
    }

    String buildRequestBody(StructuredAiRequest request) {
        return "{"
                + "\"system_instruction\": {\"parts\": [{\"text\": \""
                + AiJsonSupport.escapeJson(request.systemPrompt()) + "\"}]},"
                + "\"contents\": [{\"parts\": [{\"text\": \""
                + AiJsonSupport.escapeJson(request.userMessage()) + "\"}]}],"
                + "\"generationConfig\": {"
                + "\"responseMimeType\": \"application/json\","
                + "\"responseSchema\": " + AiJsonSupport.toGeminiSchema(request.jsonSchema()) + ","
                + "\"maxOutputTokens\": " + maxTokens
                + "}"
                + "}";
    }

    Map<String, Object> parseResponse(String responseBody) {
        Map<String, Object> root = AiJsonSupport.parseJsonObject(responseBody, "Failed to parse Gemini API response");

        if (root.get("promptFeedback") instanceof Map<?, ?> promptFeedback
                && promptFeedback.get("blockReason") instanceof String blockReason) {
            throw new AiRequestException("Gemini blocked this request: " + blockReason);
        }

        if (!(root.get("candidates") instanceof List<?> candidates) || candidates.isEmpty()) {
            throw new AiRequestException("Unexpected Gemini API response shape: " + responseBody);
        }

        if (!(candidates.get(0) instanceof Map<?, ?> candidate)) {
            throw new AiRequestException("Unexpected Gemini API response shape: " + responseBody);
        }

        if (candidate.get("finishReason") instanceof String finishReason && !"STOP".equals(finishReason)) {
            throw new AiRequestException("Gemini stopped early: " + finishReason);
        }

        if (!(candidate.get("content") instanceof Map<?, ?> content)
                || !(content.get("parts") instanceof List<?> parts) || parts.isEmpty()
                || !(parts.get(0) instanceof Map<?, ?> part) || !(part.get("text") instanceof String text)) {
            throw new AiRequestException("Unexpected Gemini API response shape: " + responseBody);
        }

        return AiJsonSupport.parseJsonObject(text, "Failed to parse the JSON Gemini returned");
    }
}
