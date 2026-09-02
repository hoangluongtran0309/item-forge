package com.hoangluongtran0309.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.hoangluongtran0309.application.exception.AiItemGenerationException;
import com.hoangluongtran0309.application.port.AiItemGeneratorPort;
import com.hoangluongtran0309.application.port.ConfigSourcePort;
import com.hoangluongtran0309.domain.ItemRegistry;
import com.hoangluongtran0309.domain.model.ItemDefinition;

class AiItemGenerationServiceTest {

    @Test
    void generateDraftRejectsAnIdThatIsAlreadyRegistered() {
        ItemRegistry registry = new ItemRegistry();
        registry.register(new ItemDefinition("void_sword", "NETHERITE_SWORD", 1, "Fire Sword", List.of(), List.of()));
        AiItemGenerationService service = newService(registry, new FakeConfigSourcePort());

        assertThrows(AiItemGenerationException.class, () -> service.generateDraft("void_sword", "a flaming sword"));
    }

    @Test
    void generateDraftAssignsTheNextFreeCustomModelData() {
        ItemRegistry registry = new ItemRegistry();
        registry.register(new ItemDefinition("existing_a", "STICK", 5, "A", List.of(), List.of()));
        registry.register(new ItemDefinition("existing_b", "STICK", 12, "B", List.of(), List.of()));
        AiItemGenerationService service = newService(registry, new FakeConfigSourcePort());

        ItemDefinition draft = service.generateDraft("new_item", "a shiny new item");

        assertEquals(13, draft.customModelData());
    }

    @Test
    void generateDraftAssignsOneWhenRegistryIsEmpty() {
        ItemRegistry registry = new ItemRegistry();
        AiItemGenerationService service = newService(registry, new FakeConfigSourcePort());

        ItemDefinition draft = service.generateDraft("new_item", "a shiny new item");

        assertEquals(1, draft.customModelData());
    }

    @Test
    void persistSavesAndRegistersTheDefinition() {
        ItemRegistry registry = new ItemRegistry();
        FakeConfigSourcePort configSource = new FakeConfigSourcePort();
        AiItemGenerationService service = newService(registry, configSource);
        ItemDefinition definition = new ItemDefinition("new_item", "STICK", 1, "New Item", List.of(), List.of());

        service.persist(definition);

        assertTrue(configSource.saved.contains(definition));
        assertEquals(definition, registry.get("new_item").orElseThrow());
    }

    private static AiItemGenerationService newService(ItemRegistry registry, ConfigSourcePort configSource) {
        ItemConfigLoaderService loaderService = new ItemConfigLoaderService(configSource, registry);
        return new AiItemGenerationService(new FakeAiItemGeneratorPort(), registry, loaderService);
    }

    private static final class FakeAiItemGeneratorPort implements AiItemGeneratorPort {
        @Override
        public ItemDefinition generateDraft(String itemId, String description) {
            return new ItemDefinition(itemId, "STICK", 0, "Generated " + itemId, List.of(), List.of());
        }
    }

    private static final class FakeConfigSourcePort implements ConfigSourcePort {
        private final List<ItemDefinition> saved = new ArrayList<>();

        @Override
        public List<ItemDefinition> loadAll() {
            return List.of();
        }

        @Override
        public void save(ItemDefinition definition) {
            saved.add(definition);
        }

        @Override
        public void delete(String id) {
        }
    }
}
