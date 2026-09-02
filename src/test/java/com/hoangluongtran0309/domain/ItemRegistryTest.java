package com.hoangluongtran0309.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.hoangluongtran0309.domain.model.ItemDefinition;

class ItemRegistryTest {

    private final ItemRegistry registry = new ItemRegistry();

    @Test
    void registerAndGet() {
        ItemDefinition definition = item("void_sword");
        registry.register(definition);

        assertEquals(definition, registry.get("void_sword").orElseThrow());
        assertEquals(1, registry.size());
    }

    @Test
    void getMissingReturnsEmpty() {
        assertTrue(registry.get("nope").isEmpty());
    }

    @Test
    void duplicateIdOverwrites() {
        registry.register(item("void_sword"));
        ItemDefinition replacement = new ItemDefinition("void_sword", "IRON_SWORD", 2, "new name", List.of(),
                List.of());
        registry.register(replacement);

        assertEquals(1, registry.size());
        assertEquals("IRON_SWORD", registry.get("void_sword").orElseThrow().material());
    }

    @Test
    void clearRemovesAll() {
        registry.register(item("void_sword"));
        registry.clear();
        assertEquals(0, registry.size());
    }

    @Test
    void getAllReturnsEveryRegisteredItem() {
        registry.register(item("void_sword"));
        registry.register(item("ice_bow"));
        assertEquals(2, registry.getAll().size());
    }

    private ItemDefinition item(String id) {
        return new ItemDefinition(id, "NETHERITE_SWORD", 1001, "Fire Sword", List.of(), List.of());
    }
}
