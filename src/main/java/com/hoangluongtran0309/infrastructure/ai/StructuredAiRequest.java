package com.hoangluongtran0309.infrastructure.ai;

/**
 * One structured-output call, described in a way all four providers can satisfy.
 *
 * <p>The providers split into two families, which is why the same schema has to be supplied
 * twice. Claude and Gemini accept a machine-readable schema and constrain the model with it;
 * ChatGPT and DeepSeek only guarantee syntactically valid JSON via
 * {@code response_format: json_object}, so for them the shape has to be spelled out in prose
 * inside the system message instead. Keeping both on the request means a feature declares its
 * output shape once, in one place, rather than once per adapter.
 *
 * @param systemPrompt      task instructions, without the output shape
 * @param userMessage       the payload the model works on
 * @param schemaName        tool name for providers that model this as a tool call
 * @param schemaDescription one-line description of that tool
 * @param jsonSchema        JSON Schema for the response object, in the standard lowercase dialect
 * @param schemaProse       the same shape in prose; must mention JSON, as DeepSeek rejects a
 *                          request whose prompt never uses the word
 */
public record StructuredAiRequest(
        String systemPrompt,
        String userMessage,
        String schemaName,
        String schemaDescription,
        String jsonSchema,
        String schemaProse) {

    public StructuredAiRequest {
        if (systemPrompt == null || systemPrompt.isBlank()) {
            throw new IllegalArgumentException("systemPrompt cannot be blank");
        }
        if (userMessage == null || userMessage.isBlank()) {
            throw new IllegalArgumentException("userMessage cannot be blank");
        }
        if (jsonSchema == null || jsonSchema.isBlank()) {
            throw new IllegalArgumentException("jsonSchema cannot be blank");
        }
        if (schemaProse == null || schemaProse.isBlank()) {
            throw new IllegalArgumentException("schemaProse cannot be blank");
        }
    }

    /**
     * What the json_object providers send as their system message: the task plus the shape they
     * would otherwise have no way of knowing.
     */
    public String systemPromptWithSchemaProse() {
        return systemPrompt + " " + schemaProse;
    }
}
