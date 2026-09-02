package com.hoangluongtran0309.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.hoangluongtran0309.domain.exception.InvalidItemDefinitionException;

class CustomBlockDefinitionTest {

    @Test
    void blankIdThrows() {
        assertThrows(InvalidItemDefinitionException.class,
                () -> new CustomBlockDefinition("", "BASS_GUITAR", 12, "texture", "GLOWSTONE", "name", 1, List.of()));
    }

    @Test
    void blankInstrumentThrows() {
        assertThrows(InvalidItemDefinitionException.class,
                () -> new CustomBlockDefinition("id", " ", 12, "texture", "GLOWSTONE", "name", 1, List.of()));
    }

    @Test
    void blankTextureIdThrows() {
        assertThrows(InvalidItemDefinitionException.class,
                () -> new CustomBlockDefinition("id", "BASS_GUITAR", 12, "", "GLOWSTONE", "name", 1, List.of()));
    }

    @Test
    void blankDropItemIdThrows() {
        assertThrows(InvalidItemDefinitionException.class,
                () -> new CustomBlockDefinition("id", "BASS_GUITAR", 12, "texture", " ", "name", 1, List.of()));
    }

    @Test
    void negativeNoteThrows() {
        assertThrows(InvalidItemDefinitionException.class,
                () -> new CustomBlockDefinition("id", "BASS_GUITAR", -1, "texture", "GLOWSTONE", "name", 1,
                        List.of()));
    }

    @Test
    void noteAboveTwentyFourThrows() {
        assertThrows(InvalidItemDefinitionException.class,
                () -> new CustomBlockDefinition("id", "BASS_GUITAR", 25, "texture", "GLOWSTONE", "name", 1,
                        List.of()));
    }

    @Test
    void noteBoundariesAreAccepted() {
        new CustomBlockDefinition("id", "BASS_GUITAR", 0, "texture", "GLOWSTONE", "name", 1, List.of());
        new CustomBlockDefinition("id", "BASS_GUITAR", 24, "texture", "GLOWSTONE", "name", 1, List.of());
    }

    @Test
    void loreIsDefensivelyCopied() {
        List<String> mutableLore = new ArrayList<>(List.of("line"));
        CustomBlockDefinition definition = new CustomBlockDefinition("id", "BASS_GUITAR", 12, "texture",
                "GLOWSTONE", "name", 1, mutableLore);
        mutableLore.add("mutated");
        assertEquals(1, definition.lore().size());
    }
}
