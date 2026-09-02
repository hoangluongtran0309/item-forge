package com.hoangluongtran0309.application;

import com.hoangluongtran0309.application.exception.AiItemGenerationException;
import com.hoangluongtran0309.application.port.AiItemGeneratorPort;
import com.hoangluongtran0309.domain.ItemRegistry;
import com.hoangluongtran0309.domain.model.ItemDefinition;

public class AiItemGenerationService {

    private final AiItemGeneratorPort aiPort;
    private final ItemRegistry registry;
    private final ItemConfigLoaderService loaderService;

    public AiItemGenerationService(AiItemGeneratorPort aiPort, ItemRegistry registry,
            ItemConfigLoaderService loaderService) {
        this.aiPort = aiPort;
        this.registry = registry;
        this.loaderService = loaderService;
    }

    /**
     * Calls the AI to generate a draft and assigns an unused customModelData. It does NOT
     * save or register anything here -- call persist() once the Bukkit layer has confirmed
     * the material is valid.
     */
    public ItemDefinition generateDraft(String itemId, String description) {
        if (registry.get(itemId).isPresent()) {
            throw new AiItemGenerationException("Item id '" + itemId + "' already exists");
        }

        ItemDefinition draft = aiPort.generateDraft(itemId, description);
        int customModelData = registry.nextAvailableCustomModelData();

        return new ItemDefinition(draft.id(), draft.material(), customModelData, draft.displayName(),
                draft.lore(), draft.abilities());
    }

    public void persist(ItemDefinition definition) {
        loaderService.save(definition);
    }
}
