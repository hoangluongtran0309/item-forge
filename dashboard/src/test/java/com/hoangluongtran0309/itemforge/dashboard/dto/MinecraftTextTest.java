package com.hoangluongtran0309.itemforge.dashboard.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

class MinecraftTextTest {

    @Test
    void colourAndFormatCodesAreRemoved() {
        assertEquals("Void Netherite Sword", MinecraftText.stripColorCodes("&5Void Netherite Sword"));
        assertEquals("Bold Reset", MinecraftText.stripColorCodes("&l&nBold &rReset"));
    }

    @Test
    void codesAreMatchedRegardlessOfCase() {
        assertEquals("Storm Blade", MinecraftText.stripColorCodes("&BStorm &KBlade"));
    }

    @Test
    void anAmpersandThatIsNotACodeIsKept() {
        assertEquals("Salt & Pepper", MinecraftText.stripColorCodes("Salt & Pepper"));
        assertEquals("Tom &Jerry", MinecraftText.stripColorCodes("Tom &Jerry"));
    }

    @Test
    void anAmpersandFollowedByACodeLetterIsACodeJustAsItIsInGame() {
        assertEquals("R Wand", MinecraftText.stripColorCodes("R&D Wand"));
    }

    @Test
    void aMissingNameBecomesAnEmptyString() {
        assertEquals("", MinecraftText.stripColorCodes(null));
    }

    @Test
    void everyDtoExposesItsNameWithoutCodes() {
        assertEquals("Void Sword",
                new ItemJson("void_sword", "NETHERITE_SWORD", 1, "&5Void Sword", List.of(), List.of())
                        .plainDisplayName());
        assertEquals("Void Helmet",
                new ArmorJson("void_helmet", "NETHERITE_HELMET", "HELMET", "void_armor", "&5Void Helmet", List.of())
                        .plainDisplayName());
        assertEquals("Void Block",
                new BlockJson("void_block", "BASS_GUITAR", 12, "void_block", "GLOWSTONE", "&5Void Block", 2001,
                        List.of()).plainDisplayName());
    }
}
