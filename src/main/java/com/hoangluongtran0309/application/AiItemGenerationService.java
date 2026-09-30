package com.hoangluongtran0309.application;

import com.hoangluongtran0309.application.exception.AiItemGenerationException;
import com.hoangluongtran0309.application.port.AiItemGeneratorPort;
import com.hoangluongtran0309.domain.ItemRegistry;
import com.hoangluongtran0309.domain.model.ItemDefinition;

public class AiItemGenerationService {

    // Read from the async thread that runs generateDraft and replaced from the main thread
    // on /itemforge reload, hence volatile.
    private volatile AiItemGeneratorPort aiPort;
    private final ItemRegistry registry;
    private final ItemConfigLoaderService loaderService;

    /**
     * @param aiPort null when ai.enabled is false in config.yml
     */
    public AiItemGenerationService(AiItemGeneratorPort aiPort, ItemRegistry registry,
            ItemConfigLoaderService loaderService) {
        this.aiPort = aiPort;
        this.registry = registry;
        this.loaderService = loaderService;
    }

    public boolean isEnabled() {
        return aiPort != null;
    }

    /**
     * Swaps the provider in place, so a changed ai section in config.yml takes effect on reload
     * without rebuilding everything that already holds this service.
     *
     * @param aiPort null to turn generation off
     */
    public void useProvider(AiItemGeneratorPort aiPort) {
        this.aiPort = aiPort;
    }

    /**
     * Calls the AI to generate a draft and assigns an unused customModelData. It does NOT
     * save or register anything here -- call persist() once the Bukkit layer has confirmed
     * the material is valid.
     */
    public ItemDefinition generateDraft(String itemId, String description) {
        AiItemGeneratorPort port = aiPort;
        if (port == null) {
            throw new AiItemGenerationException("AI item generation is disabled");
        }

        if (registry.get(itemId).isPresent()) {
            throw new AiItemGenerationException("Item id '" + itemId + "' already exists");
        }

        ItemDefinition draft = port.generateDraft(itemId, description);
        int customModelData = registry.nextAvailableCustomModelData();

        return new ItemDefinition(draft.id(), draft.material(), customModelData, draft.displayName(),
                draft.lore(), draft.abilities());
    }

    public void persist(ItemDefinition definition) {
        loaderService.save(definition);
    }
}
