package com.hoangluongtran0309.infrastructure.resourcepack;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.hoangluongtran0309.domain.model.ArmorDefinition;
import com.hoangluongtran0309.domain.model.ArmorSlot;

class ArmorTextureFileCopierTest {

    private final ArmorTextureFileCopier copier = new ArmorTextureFileCopier(Logger.getAnonymousLogger());
    private final ArmorDefinition armor = new ArmorDefinition("void_chestplate", "NETHERITE_CHESTPLATE",
            ArmorSlot.CHESTPLATE, null, 0, "name", List.of());

    @Test
    void layer1MissingReturnsFalse(@TempDir Path sourceDir, @TempDir Path outputDir) throws IOException {
        boolean copied = copier.copyLayer1("void_chestplate", sourceDir, "itemforge", outputDir);

        assertFalse(copied);
        assertTrue(Files.notExists(outputDir.resolve(
                "assets/itemforge/textures/entity/equipment/humanoid/void_chestplate.png")));
    }

    @Test
    void layer1PresentIsCopiedToExpectedPath(@TempDir Path sourceDir, @TempDir Path outputDir) throws IOException {
        Files.writeString(sourceDir.resolve("void_chestplate_layer_1.png"), "fake-png-bytes");

        boolean copied = copier.copyLayer1("void_chestplate", sourceDir, "itemforge", outputDir);

        assertTrue(copied);
        assertTrue(Files.exists(outputDir.resolve(
                "assets/itemforge/textures/entity/equipment/humanoid/void_chestplate.png")));
    }

    @Test
    void layer2MissingReturnsFalseSilently(@TempDir Path sourceDir, @TempDir Path outputDir) throws IOException {
        boolean copied = copier.copyLayer2IfExists("void_chestplate", sourceDir, "itemforge", outputDir);
        assertFalse(copied);
    }

    @Test
    void layer2PresentIsCopiedToLeggingsPath(@TempDir Path sourceDir, @TempDir Path outputDir) throws IOException {
        Files.writeString(sourceDir.resolve("void_chestplate_layer_2.png"), "fake-png-bytes");

        boolean copied = copier.copyLayer2IfExists("void_chestplate", sourceDir, "itemforge", outputDir);

        assertTrue(copied);
        assertTrue(Files.exists(outputDir.resolve(
                "assets/itemforge/textures/entity/equipment/humanoid_leggings/void_chestplate.png")));
    }

    @Test
    void iconMissingReturnsFalse(@TempDir Path sourceDir, @TempDir Path outputDir) throws IOException {
        boolean copied = copier.copyIcon(armor, sourceDir, "itemforge", outputDir);

        assertFalse(copied);
        assertTrue(Files.notExists(outputDir.resolve("assets/itemforge/textures/item/void_chestplate.png")));
    }

    @Test
    void iconPresentIsCopiedToItemTexturePath(@TempDir Path sourceDir, @TempDir Path outputDir) throws IOException {
        Files.writeString(sourceDir.resolve("void_chestplate.png"), "fake-icon-bytes");

        boolean copied = copier.copyIcon(armor, sourceDir, "itemforge", outputDir);

        assertTrue(copied);
        assertTrue(Files.exists(outputDir.resolve("assets/itemforge/textures/item/void_chestplate.png")));
    }

    @Test
    void iconDoesNotMatchLayerSuffixedFiles(@TempDir Path sourceDir, @TempDir Path outputDir) throws IOException {
        Files.writeString(sourceDir.resolve("void_chestplate_layer_1.png"), "fake-layer-bytes");

        boolean copied = copier.copyIcon(armor, sourceDir, "itemforge", outputDir);

        assertFalse(copied);
    }
}
