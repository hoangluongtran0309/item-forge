package com.hoangluongtran0309.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.hoangluongtran0309.domain.model.RecipeDefinition;
import com.hoangluongtran0309.domain.model.ShapelessRecipeDefinition;

class RecipeRegistryTest {

    private final RecipeRegistry registry = new RecipeRegistry();

    @Test
    void registerAndGet() {
        RecipeDefinition definition = recipe("void_sword_salvage");
        registry.register(definition);

        assertEquals(definition, registry.get("void_sword_salvage").orElseThrow());
        assertEquals(1, registry.size());
    }

    @Test
    void getMissingReturnsEmpty() {
        assertTrue(registry.get("nope").isEmpty());
    }

    @Test
    void duplicateIdOverwrites() {
        registry.register(recipe("void_sword_salvage"));
        RecipeDefinition replacement = new ShapelessRecipeDefinition("void_sword_salvage", "TORCH", 8, List.of("COAL"));
        registry.register(replacement);

        assertEquals(1, registry.size());
        assertEquals(8, registry.get("void_sword_salvage").orElseThrow().resultCount());
    }

    @Test
    void clearRemovesAll() {
        registry.register(recipe("void_sword_salvage"));
        registry.clear();
        assertEquals(0, registry.size());
    }

    private RecipeDefinition recipe(String id) {
        return new ShapelessRecipeDefinition(id, "TORCH", 4, List.of("COAL", "STICK"));
    }
}
