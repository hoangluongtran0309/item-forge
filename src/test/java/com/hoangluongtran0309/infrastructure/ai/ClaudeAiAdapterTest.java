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

class ClaudeAiAdapterTest {

    private final ClaudeAiAdapter adapter = new ClaudeAiAdapter("test-api-key", "claude-haiku-4-5", 2048, 30,
            Logger.getAnonymousLogger());

    @Test
    void parseResponseExtractsTheToolUseInput() {
        String responseBody = """
                {
                  "id": "msg_01",
                  "stop_reason": "tool_use",
                  "content": [
                    {
                      "type": "tool_use",
                      "id": "toolu_01",
                      "name": "submit_item_definition",
                      "input": {
                        "material": "NETHERITE_PICKAXE",
                        "lore": ["A heavy axe."]
                      }
                    }
                  ]
                }
                """;

        Map<String, Object> result = adapter.parseResponse(TestAiRequests.sample(), responseBody);

        assertEquals("NETHERITE_PICKAXE", result.get("material"));
        assertEquals(List.of("A heavy axe."), result.get("lore"));
    }

    @Test
    void parseResponseIgnoresAToolUseBlockForAnotherTool() {
        String responseBody = """
                {
                  "stop_reason": "tool_use",
                  "content": [
                    { "type": "tool_use", "name": "some_other_tool", "input": { "material": "STICK" } }
                  ]
                }
                """;

        assertThrows(AiRequestException.class, () -> adapter.parseResponse(TestAiRequests.sample(), responseBody));
    }

    @Test
    void parseResponseThrowsOnRefusal() {
        String responseBody = """
                { "stop_reason": "refusal", "content": [] }
                """;

        assertThrows(AiRequestException.class, () -> adapter.parseResponse(TestAiRequests.sample(), responseBody));
    }

    @Test
    void parseResponseThrowsWhenThereIsNoToolUseBlock() {
        String responseBody = """
                {
                  "stop_reason": "end_turn",
                  "content": [ { "type": "text", "text": "Sorry, I can't help with that." } ]
                }
                """;

        assertThrows(AiRequestException.class, () -> adapter.parseResponse(TestAiRequests.sample(), responseBody));
    }

    @Test
    void buildRequestBodyEscapesTheUserMessageAndStaysValidJson() {
        String requestBody = adapter.buildRequestBody(TestAiRequests.sample());

        Map<String, Object> root = new Yaml().load(requestBody);
        assertEquals("claude-haiku-4-5", root.get("model"));
        assertEquals(2048, root.get("max_tokens"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> messages = (List<Map<String, Object>>) root.get("messages");
        assertEquals(TestAiRequests.ESCAPING_TORTURE_TEST, messages.get(0).get("content"));
    }

    @Test
    void buildRequestBodyForcesTheRequestedToolAndPassesItsSchemaThrough() {
        String requestBody = adapter.buildRequestBody(TestAiRequests.sample());

        Map<String, Object> root = new Yaml().load(requestBody);

        @SuppressWarnings("unchecked")
        Map<String, Object> toolChoice = (Map<String, Object>) root.get("tool_choice");
        assertEquals("tool", toolChoice.get("type"));
        assertEquals("submit_item_definition", toolChoice.get("name"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> tools = (List<Map<String, Object>>) root.get("tools");
        assertEquals("submit_item_definition", tools.get(0).get("name"));

        @SuppressWarnings("unchecked")
        Map<String, Object> inputSchema = (Map<String, Object>) tools.get(0).get("input_schema");
        assertEquals("object", inputSchema.get("type"));
        assertTrue(inputSchema.containsKey("properties"));
    }
}
