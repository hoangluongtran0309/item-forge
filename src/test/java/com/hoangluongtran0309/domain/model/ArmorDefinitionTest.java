package com.hoangluongtran0309.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.hoangluongtran0309.domain.exception.InvalidItemDefinitionException;

class ArmorDefinitionTest {

    @Test
    void blankIdThrows() {
        assertThrows(InvalidItemDefinitionException.class,
                () -> new ArmorDefinition("", "NETHERITE_HELMET", ArmorSlot.HELMET, null, 0, "name", List.of()));
    }

    @Test
    void blankMaterialThrows() {
        assertThrows(InvalidItemDefinitionException.class,
                () -> new ArmorDefinition("id", " ", ArmorSlot.HELMET, null, 0, "name", List.of()));
    }

    @Test
    void nullSlotThrows() {
        assertThrows(InvalidItemDefinitionException.class,
                () -> new ArmorDefinition("id", "NETHERITE_HELMET", null, null, 0, "name", List.of()));
    }

    @Test
    void nullLoreDefaultsToEmptyList() {
        ArmorDefinition definition = new ArmorDefinition("id", "NETHERITE_HELMET", ArmorSlot.HELMET, null, 0, "name",
                null);
        assertTrue(definition.lore().isEmpty());
    }

    @Test
    void loreIsDefensivelyCopied() {
        List<String> mutableLore = new java.util.ArrayList<>(List.of("line"));
        ArmorDefinition definition = new ArmorDefinition("id", "NETHERITE_HELMET", ArmorSlot.HELMET, null, 0, "name",
                mutableLore);
        mutableLore.add("mutated");
        assertEquals(1, definition.lore().size());
    }

    @Test
    void nullArmorAssetIdDefaultsToId() {
        ArmorDefinition definition = new ArmorDefinition("void_helmet", "NETHERITE_HELMET", ArmorSlot.HELMET, null, 0,
                "name", List.of());
        assertEquals("void_helmet", definition.armorAssetId());
    }

    @Test
    void blankArmorAssetIdDefaultsToId() {
        ArmorDefinition definition = new ArmorDefinition("void_helmet", "NETHERITE_HELMET", ArmorSlot.HELMET, " ", 0,
                "name", List.of());
        assertEquals("void_helmet", definition.armorAssetId());
    }

    @Test
    void explicitArmorAssetIdIsPreserved() {
        ArmorDefinition definition = new ArmorDefinition("void_helmet", "NETHERITE_HELMET", ArmorSlot.HELMET,
                "void_armor", 0, "name", List.of());
        assertEquals("void_armor", definition.armorAssetId());
    }

    @Test
    void customModelDataIsPreserved() {
        ArmorDefinition definition = new ArmorDefinition("void_helmet", "NETHERITE_HELMET", ArmorSlot.HELMET, null,
                4002, "name", List.of());
        assertEquals(4002, definition.customModelData());
    }
}
