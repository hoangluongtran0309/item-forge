package com.hoangluongtran0309.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.hoangluongtran0309.application.port.RecipeConfigSourcePort;
import com.hoangluongtran0309.domain.RecipeRegistry;
import com.hoangluongtran0309.domain.model.RecipeDefinition;
import com.hoangluongtran0309.domain.model.ShapelessRecipeDefinition;

class RecipeConfigLoaderServiceTest {

    @Test
    void loadAllRegistersEverythingFromSource() {
        RecipeDefinition torchBundle = new ShapelessRecipeDefinition("void_sword_salvage", "TORCH", 4,
                List.of("COAL", "STICK"));
        FakeRecipeConfigSource source = new FakeRecipeConfigSource(List.of(torchBundle));
        RecipeRegistry registry = new RecipeRegistry();
        RecipeConfigLoaderService service = new RecipeConfigLoaderService(source, registry);

        service.loadAll();

        assertEquals(1, registry.size());
        assertEquals(torchBundle, registry.get("void_sword_salvage").orElseThrow());
    }

    @Test
    void secondLoadClearsPreviousState() {
        RecipeDefinition torchBundle = new ShapelessRecipeDefinition("void_sword_salvage", "TORCH", 4,
                List.of("COAL", "STICK"));
        FakeRecipeConfigSource source = new FakeRecipeConfigSource(List.of(torchBundle));
        RecipeRegistry registry = new RecipeRegistry();
        RecipeConfigLoaderService service = new RecipeConfigLoaderService(source, registry);

        service.loadAll();
        source.setDefinitions(List.of());
        service.loadAll();

        assertEquals(0, registry.size());
    }

    @Test
    void saveWritesToSourceAndRegistersInPlace() {
        FakeRecipeConfigSource source = new FakeRecipeConfigSource(List.of());
        RecipeRegistry registry = new RecipeRegistry();
        RecipeConfigLoaderService service = new RecipeConfigLoaderService(source, registry);
        RecipeDefinition torchBundle = new ShapelessRecipeDefinition("void_sword_salvage", "TORCH", 4,
                List.of("COAL", "STICK"));

        service.save(torchBundle);

        assertTrue(source.saved.contains(torchBundle));
        assertEquals(torchBundle, registry.get("void_sword_salvage").orElseThrow());
    }

    @Test
    void deleteRemovesFromSourceAndRegistry() {
        FakeRecipeConfigSource source = new FakeRecipeConfigSource(List.of());
        RecipeRegistry registry = new RecipeRegistry();
        registry.register(new ShapelessRecipeDefinition("void_sword_salvage", "TORCH", 4, List.of("COAL", "STICK")));
        RecipeConfigLoaderService service = new RecipeConfigLoaderService(source, registry);

        service.delete("void_sword_salvage");

        assertTrue(source.deleted.contains("void_sword_salvage"));
        assertEquals(0, registry.size());
    }

    private static final class FakeRecipeConfigSource implements RecipeConfigSourcePort {
        private List<RecipeDefinition> definitions;
        private final List<RecipeDefinition> saved = new ArrayList<>();
        private final List<String> deleted = new ArrayList<>();

        FakeRecipeConfigSource(List<RecipeDefinition> definitions) {
            this.definitions = definitions;
        }

        void setDefinitions(List<RecipeDefinition> definitions) {
            this.definitions = definitions;
        }

        @Override
        public List<RecipeDefinition> loadAll() {
            return definitions;
        }

        @Override
        public void save(RecipeDefinition definition) {
            saved.add(definition);
        }

        @Override
        public void delete(String id) {
            deleted.add(id);
        }
    }
}
