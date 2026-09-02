package com.hoangluongtran0309.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.hoangluongtran0309.domain.model.CustomBlockDefinition;

class CustomBlockRegistryTest {

    private final CustomBlockRegistry registry = new CustomBlockRegistry();

    @Test
    void registerAndGet() {
        CustomBlockDefinition definition = block("void_netherite_block", "BASS_GUITAR", 12);
        assertTrue(registry.register(definition));

        assertEquals(definition, registry.get("void_netherite_block").orElseThrow());
        assertEquals(1, registry.size());
    }

    @Test
    void getMissingReturnsEmpty() {
        assertTrue(registry.get("nope").isEmpty());
    }

    @Test
    void duplicateComboRejected() {
        CustomBlockDefinition first = block("void_netherite_block", "BASS_GUITAR", 12);
        CustomBlockDefinition second = block("other_void_block", "BASS_GUITAR", 12);

        assertTrue(registry.register(first));
        assertFalse(registry.register(second));

        assertEquals(1, registry.size());
        assertEquals(first, registry.get("void_netherite_block").orElseThrow());
        assertTrue(registry.get("other_void_block").isEmpty());
    }

    @Test
    void sameIdReRegisterWithNewComboSucceeds() {
        registry.register(block("void_netherite_block", "BASS_GUITAR", 12));
        CustomBlockDefinition updated = block("void_netherite_block", "BASS_GUITAR", 20);

        assertTrue(registry.register(updated));
        assertEquals(20, registry.get("void_netherite_block").orElseThrow().note());

        // The old (BASS_GUITAR, 12) combination has to be released -- another id can claim
        // it as soon as void_netherite_block moves to a new one.
        assertTrue(registry.register(block("takes_old_combo", "BASS_GUITAR", 12)));
    }

    @Test
    void clearRemovesAll() {
        registry.register(block("void_netherite_block", "BASS_GUITAR", 12));
        registry.clear();
        assertEquals(0, registry.size());

        // After clear(), the old combination must be fully released.
        assertTrue(registry.register(block("void_netherite_block", "BASS_GUITAR", 12)));
    }

    @Test
    void nextAvailableCustomModelDataIsOneWhenRegistryIsEmpty() {
        assertEquals(1, registry.nextAvailableCustomModelData());
    }

    @Test
    void nextAvailableCustomModelDataIsOneMoreThanTheCurrentMax() {
        registry.register(block("void_netherite_block", "BASS_GUITAR", 12, 5));
        registry.register(block("other_void_block", "PIANO", 10, 2));

        assertEquals(6, registry.nextAvailableCustomModelData());
    }

    private CustomBlockDefinition block(String id, String instrument, int note, int customModelData) {
        return new CustomBlockDefinition(id, instrument, note, "texture", "GLOWSTONE", "Display Name",
                customModelData, List.of());
    }

    private CustomBlockDefinition block(String id, String instrument, int note) {
        return new CustomBlockDefinition(id, instrument, note, "texture", "GLOWSTONE", "Display Name", 1, List.of());
    }
}
