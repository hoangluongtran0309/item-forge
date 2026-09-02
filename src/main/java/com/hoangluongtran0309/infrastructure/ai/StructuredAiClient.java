package com.hoangluongtran0309.infrastructure.ai;

import java.util.Map;

/**
 * A provider that can be asked for one JSON object matching a schema. Everything
 * provider-specific -- endpoint, auth, how structured output is requested, how the response is
 * unwrapped -- lives behind this interface, so a new AI feature is a new prompt and schema
 * rather than a new adapter per provider.
 */
public interface StructuredAiClient {

    /**
     * @return the response object, already unwrapped from whatever envelope the provider uses
     * @throws com.hoangluongtran0309.application.exception.AiRequestException if the call fails,
     *         the provider declines, or the response cannot be parsed
     */
    Map<String, Object> requestJsonObject(StructuredAiRequest request);
}
