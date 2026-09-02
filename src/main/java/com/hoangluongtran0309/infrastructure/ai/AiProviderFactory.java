package com.hoangluongtran0309.infrastructure.ai;

import java.util.Locale;
import java.util.logging.Logger;

import com.hoangluongtran0309.application.exception.AiRequestException;

/**
 * Picks the AI adapter by provider name from config.yml (ai.provider), following the same
 * model as ItemModelStrategyFactory picking a render strategy by server version. Supports
 * claude, chatgpt, deepseek and gemini; adding a provider takes one switch branch and one
 * new adapter class in this package, touching nothing in application/domain/command.
 */
public class AiProviderFactory {

    public StructuredAiClient create(String provider, String apiKey, String model, int maxTokens,
            int timeoutSeconds, Logger logger) {
        return switch (provider.toLowerCase(Locale.ROOT)) {
            case "claude" -> new ClaudeAiAdapter(apiKey, model, maxTokens, timeoutSeconds, logger);
            case "chatgpt" -> new ChatGptAiAdapter(apiKey, model, maxTokens, timeoutSeconds, logger);
            case "deepseek" -> new DeepSeekAiAdapter(apiKey, model, maxTokens, timeoutSeconds, logger);
            case "gemini" -> new GeminiAiAdapter(apiKey, model, maxTokens, timeoutSeconds, logger);
            default -> throw new AiRequestException(
                    "Unknown AI provider: '" + provider + "'. Supported providers: claude, chatgpt, deepseek, gemini");
        };
    }
}
