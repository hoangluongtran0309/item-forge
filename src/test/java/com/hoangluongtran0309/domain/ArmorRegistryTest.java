package com.hoangluongtran0309.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.hoangluongtran0309.domain.model.ArmorDefinition;
import com.hoangluongtran0309.domain.model.ArmorSlot;

class ArmorRegistryTest {

    private final ArmorRegistry registry = new ArmorRegistry();

    @Test
    void registerAndGet() {
        ArmorDefinition definition = armor("void_helmet");
        registry.register(definition);

        assertEquals(definition, registry.get("void_helmet").orElseThrow());
        assertEquals(1, registry.size());
    }

    @Test
    void getMissingReturnsEmpty() {
        assertTrue(registry.get("nope").isEmpty());
    }

    @Test
    void duplicateIdOverwrites() {
        registry.register(armor("void_helmet"));
        ArmorDefinition replacement = new ArmorDefinition("void_helmet", "IRON_HELMET", ArmorSlot.HELMET, null, 0,
                "new name", List.of());
        registry.register(replacement);

        assertEquals(1, registry.size());
        assertEquals("IRON_HELMET", registry.get("void_helmet").orElseThrow().material());
    }

    @Test
    void clearRemovesAll() {
        registry.register(armor("void_helmet"));
        registry.clear();
        assertEquals(0, registry.size());
    }

    private ArmorDefinition armor(String id) {
        return new ArmorDefinition(id, "NETHERITE_HELMET", ArmorSlot.HELMET, null, 0, "Dragon Helm", List.of());
    }
}
