package com.hoangluongtran0309.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.hoangluongtran0309.domain.exception.InvalidItemDefinitionException;

class ItemDefinitionTest {

    @Test
    void blankIdThrows() {
        assertThrows(InvalidItemDefinitionException.class,
                () -> new ItemDefinition("", "NETHERITE_SWORD", 1, "name", List.of(), List.of()));
    }

    @Test
    void blankMaterialThrows() {
        assertThrows(InvalidItemDefinitionException.class,
                () -> new ItemDefinition("id", " ", 1, "name", List.of(), List.of()));
    }

    @Test
    void nullLoreDefaultsToEmptyList() {
        ItemDefinition definition = new ItemDefinition("id", "NETHERITE_SWORD", 1, "name", null, List.of());
        assertTrue(definition.lore().isEmpty());
    }

    @Test
    void nullAbilitiesDefaultsToEmptyList() {
        ItemDefinition definition = new ItemDefinition("id", "NETHERITE_SWORD", 1, "name", List.of(), null);
        assertTrue(definition.abilities().isEmpty());
    }

    @Test
    void loreIsDefensivelyCopied() {
        List<String> mutableLore = new ArrayList<>(List.of("line"));
        ItemDefinition definition = new ItemDefinition("id", "NETHERITE_SWORD", 1, "name", mutableLore, List.of());
        mutableLore.add("mutated");
        assertEquals(1, definition.lore().size());
    }
}
