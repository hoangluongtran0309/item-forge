package com.hoangluongtran0309.infrastructure.ai;

/**
 * One request shared by the four adapter tests, so each test file is about its provider's wire
 * format rather than about restating a schema.
 */
final class TestAiRequests {

    static final String ESCAPING_TORTURE_TEST = "A sword with \"fire\" damage\nand a backslash \\ in it";

    private TestAiRequests() {
    }

    static StructuredAiRequest sample() {
        return sample(ESCAPING_TORTURE_TEST);
    }

    static StructuredAiRequest sample(String userMessage) {
        return new StructuredAiRequest(
                "You are an expert Minecraft item designer.",
                userMessage,
                "submit_item_definition",
                "Submit the generated item.",
                """
                {
                  "type": "object",
                  "properties": {
                    "material": { "type": "string" },
                    "lore": { "type": "array", "items": { "type": "string" } }
                  },
                  "required": ["material"]
                }
                """,
                "Respond with ONLY a single JSON object with the keys \"material\" and \"lore\".");
    }
}
