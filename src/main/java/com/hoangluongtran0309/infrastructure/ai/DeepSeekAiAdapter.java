package com.hoangluongtran0309.infrastructure.ai;

import java.util.logging.Logger;

/**
 * Adapter for the DeepSeek Chat Completions API, whose shape is OpenAI-compatible -- see
 * {@link OpenAiCompatibleAdapter}. DeepSeek requires the word "json" to appear somewhere in the
 * prompt, otherwise the API can return a 400; every {@link StructuredAiRequest#schemaProse()}
 * satisfies that, and {@code StructuredAiRequest} rejects a blank one.
 *
 * <p>Unlike ChatGPT, the {@code refusal} field is not checked, because DeepSeek does not
 * document it.
 */
public class DeepSeekAiAdapter extends OpenAiCompatibleAdapter {

    private static final String API_URL = "https://api.deepseek.com/chat/completions";

    public DeepSeekAiAdapter(String apiKey, String model, int maxTokens, int timeoutSeconds, Logger logger) {
        super(API_URL, "DeepSeek", apiKey, model, maxTokens, timeoutSeconds, logger);
    }
}
