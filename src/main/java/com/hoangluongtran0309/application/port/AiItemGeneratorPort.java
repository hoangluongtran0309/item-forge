package com.hoangluongtran0309.application.port;

import com.hoangluongtran0309.domain.model.ItemDefinition;

/**
 * The port to an AI provider (Claude, OpenAI, ...) for generating an ItemDefinition from a
 * natural-language description. It depends on no specific provider, in the same way
 * ItemModelStrategy does not depend on a specific render mechanism.
 */
public interface AiItemGeneratorPort {

    /**
     * Generates a draft ItemDefinition from the user's description. The draft's
     * customModelData is a placeholder with no meaning yet, because the AI cannot know
     * which values are free in the current registry -- assigning the real value is
     * AiItemGenerationService's job.
     */
    ItemDefinition generateDraft(String itemId, String description);
}
