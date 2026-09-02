package com.hoangluongtran0309.infrastructure.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import com.hoangluongtran0309.application.exception.AiRequestException;

class GeminiAiAdapterTest {

    private final GeminiAiAdapter adapter = new GeminiAiAdapter("test-api-key", "gemini-2.0-flash", 2048, 30,
            Logger.getAnonymousLogger());

    @Test
    void parseResponseExtractsTheObjectFromDoubleEncodedText() {
        String innerJson = """
                {"material": "NETHERITE_PICKAXE", "lore": ["A heavy axe."]}""";
        String escapedInner = innerJson.replace("\\", "\\\\").replace("\"", "\\\"");
        String responseBody = """
                {
                  "candidates": [
                    {
                      "finishReason": "STOP",
                      "content": { "role": "model", "parts": [ { "text": "%s" } ] }
                    }
                  ]
                }
                """.formatted(escapedInner);

        Map<String, Object> result = adapter.parseResponse(responseBody);

        assertEquals("NETHERITE_PICKAXE", result.get("material"));
        assertEquals(List.of("A heavy axe."), result.get("lore"));
    }

    @Test
    void parseResponseThrowsWhenThePromptWasBlocked() {
        String responseBody = """
                { "promptFeedback": { "blockReason": "SAFETY" } }
                """;

        AiRequestException exception = assertThrows(AiRequestException.class,
                () -> adapter.parseResponse(responseBody));
        assertTrue(exception.getMessage().contains("SAFETY"));
    }

    @Test
    void parseResponseThrowsWhenTheModelStoppedEarly() {
        String responseBody = """
                {
                  "candidates": [
                    { "finishReason": "MAX_TOKENS", "content": { "parts": [ { "text": "{}" } ] } }
                  ]
                }
                """;

        assertThrows(AiRequestException.class, () -> adapter.parseResponse(responseBody));
    }

    @Test
    void parseResponseThrowsWhenTextIsNotValidJson() {
        String responseBody = """
                {
                  "candidates": [
                    { "finishReason": "STOP", "content": { "parts": [ { "text": "not json at all {{{" } ] } }
                  ]
                }
                """;

        assertThrows(AiRequestException.class, () -> adapter.parseResponse(responseBody));
    }

    @Test
    void parseResponseThrowsWhenCandidatesAreMissing() {
        String responseBody = """
                { "candidates": [] }
                """;

        assertThrows(AiRequestException.class, () -> adapter.parseResponse(responseBody));
    }

    @Test
    void buildRequestBodyUsesResponseSchemaAndEscapesTheUserMessage() {
        String requestBody = adapter.buildRequestBody(TestAiRequests.sample());

        Map<String, Object> root = new Yaml().load(requestBody);
        @SuppressWarnings("unchecked")
        Map<String, Object> generationConfig = (Map<String, Object>) root.get("generationConfig");
        assertEquals("application/json", generationConfig.get("responseMimeType"));
        assertEquals(2048, generationConfig.get("maxOutputTokens"));

        @SuppressWarnings("unchecked")
        Map<String, Object> responseSchema = (Map<String, Object>) generationConfig.get("responseSchema");
        assertEquals("OBJECT", responseSchema.get("type"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> contents = (List<Map<String, Object>>) root.get("contents");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> parts = (List<Map<String, Object>>) contents.get(0).get("parts");
        assertEquals(TestAiRequests.ESCAPING_TORTURE_TEST, parts.get(0).get("text"));
    }

    @Test
    void buildRequestBodyUppercasesNestedSchemaTypeNames() {
        String requestBody = adapter.buildRequestBody(TestAiRequests.sample());

        Map<String, Object> root = new Yaml().load(requestBody);
        @SuppressWarnings("unchecked")
        Map<String, Object> generationConfig = (Map<String, Object>) root.get("generationConfig");
        @SuppressWarnings("unchecked")
        Map<String, Object> responseSchema = (Map<String, Object>) generationConfig.get("responseSchema");
        @SuppressWarnings("unchecked")
        Map<String, Object> properties = (Map<String, Object>) responseSchema.get("properties");

        @SuppressWarnings("unchecked")
        Map<String, Object> lore = (Map<String, Object>) properties.get("lore");
        assertEquals("ARRAY", lore.get("type"));

        @SuppressWarnings("unchecked")
        Map<String, Object> loreItems = (Map<String, Object>) lore.get("items");
        assertEquals("STRING", loreItems.get("type"));

        assertEquals(List.of("material"), responseSchema.get("required"));
    }

    @Test
    void buildRequestUriPutsModelInPathAndApiKeyInQueryString() {
        String uri = adapter.buildRequestUri().toString();

        assertTrue(uri.contains("/models/gemini-2.0-flash:generateContent"));
        assertTrue(uri.contains("key=test-api-key"));
    }
}
