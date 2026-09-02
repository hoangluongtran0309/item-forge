package com.hoangluongtran0309.infrastructure.ai;

import java.util.Map;
import java.util.logging.Logger;

import com.hoangluongtran0309.application.exception.AiRequestException;

/**
 * Adapter for the OpenAI Chat Completions API. Everything but the refusal check is shared with
 * DeepSeek -- see {@link OpenAiCompatibleAdapter}.
 */
public class ChatGptAiAdapter extends OpenAiCompatibleAdapter {

    private static final String API_URL = "https://api.openai.com/v1/chat/completions";

    public ChatGptAiAdapter(String apiKey, String model, int maxTokens, int timeoutSeconds, Logger logger) {
        super(API_URL, "ChatGPT", apiKey, model, maxTokens, timeoutSeconds, logger);
    }

    @Override
    void checkRefusal(Map<?, ?> message) {
        if (message.get("refusal") instanceof String refusal && !refusal.isBlank()) {
            throw new AiRequestException("ChatGPT declined to answer this request: " + refusal);
        }
    }
}
