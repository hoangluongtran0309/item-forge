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

class ChatGptAiAdapterTest {

    private final ChatGptAiAdapter adapter = new ChatGptAiAdapter("test-api-key", "gpt-4o-mini", 2048, 30,
            Logger.getAnonymousLogger());

    @Test
    void parseResponseExtractsTheObjectFromDoubleEncodedContent() {
        String innerJson = """
                {"material": "NETHERITE_PICKAXE", "lore": ["A heavy axe."]}""";
        String escapedInner = innerJson.replace("\\", "\\\\").replace("\"", "\\\"");
        String responseBody = """
                {
                  "choices": [
                    {
                      "finish_reason": "stop",
                      "message": { "role": "assistant", "content": "%s" }
                    }
                  ]
                }
                """.formatted(escapedInner);

        Map<String, Object> result = adapter.parseResponse(responseBody);

        assertEquals("NETHERITE_PICKAXE", result.get("material"));
        assertEquals(List.of("A heavy axe."), result.get("lore"));
    }

    @Test
    void parseResponseThrowsWhenFinishReasonIsContentFilter() {
        String responseBody = """
                {
                  "choices": [
                    { "finish_reason": "content_filter", "message": { "content": "{}" } }
                  ]
                }
                """;

        assertThrows(AiRequestException.class, () -> adapter.parseResponse(responseBody));
    }

    @Test
    void parseResponseThrowsWhenMessageHasRefusalField() {
        String responseBody = """
                {
                  "choices": [
                    { "finish_reason": "stop", "message": { "refusal": "Sorry, I can't help with that." } }
                  ]
                }
                """;

        assertThrows(AiRequestException.class, () -> adapter.parseResponse(responseBody));
    }

    @Test
    void parseResponseThrowsWhenContentIsNotValidJson() {
        String responseBody = """
                {
                  "choices": [
                    { "finish_reason": "stop", "message": { "content": "not json at all {{{" } }
                  ]
                }
                """;

        assertThrows(AiRequestException.class, () -> adapter.parseResponse(responseBody));
    }

    @Test
    void parseResponseThrowsWhenChoicesAreMissing() {
        String responseBody = """
                { "error": { "message": "Invalid API key" } }
                """;

        AiRequestException exception = assertThrows(AiRequestException.class,
                () -> adapter.parseResponse(responseBody));
        assertTrue(exception.getMessage().contains("Invalid API key"));
    }

    @Test
    void buildRequestBodyEscapesTheUserMessageAndSetsJsonMode() {
        String requestBody = adapter.buildRequestBody(TestAiRequests.sample());

        Map<String, Object> root = new Yaml().load(requestBody);
        assertEquals("gpt-4o-mini", root.get("model"));
        assertEquals(2048, root.get("max_tokens"));

        @SuppressWarnings("unchecked")
        Map<String, Object> responseFormat = (Map<String, Object>) root.get("response_format");
        assertEquals("json_object", responseFormat.get("type"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> messages = (List<Map<String, Object>>) root.get("messages");
        assertEquals("system", messages.get(0).get("role"));
        assertEquals("user", messages.get(1).get("role"));
        assertEquals(TestAiRequests.ESCAPING_TORTURE_TEST, messages.get(1).get("content"));
    }

    @Test
    void systemMessageCarriesTheSchemaProseSinceTheSchemaItselfIsNotSent() {
        String requestBody = adapter.buildRequestBody(TestAiRequests.sample());

        Map<String, Object> root = new Yaml().load(requestBody);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> messages = (List<Map<String, Object>>) root.get("messages");
        String systemContent = (String) messages.get(0).get("content");

        assertTrue(systemContent.contains("Minecraft item designer"));
        assertTrue(systemContent.contains("single JSON object"));
    }
}
