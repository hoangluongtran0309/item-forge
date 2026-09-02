package com.hoangluongtran0309.infrastructure.bukkit.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.util.logging.Logger;

import com.hoangluongtran0309.domain.model.ItemDefinition;
import com.hoangluongtran0309.infrastructure.resourcepack.ModelJsonGenerator;
import com.hoangluongtran0309.infrastructure.resourcepack.TextureFileCopier;

class ModernModelStrategyTest {

    private final ModernModelStrategy strategy = new ModernModelStrategy(new ModelJsonGenerator(),
            new TextureFileCopier(Logger.getAnonymousLogger()));

    @Test
    void itemWithCustomModelDataGetsBothFiles(@TempDir Path sourceDir, @TempDir Path outputDir) throws IOException {
        ItemDefinition voidSword = new ItemDefinition("void_sword", "NETHERITE_SWORD", 1000, "name", List.of(),
                List.of());

        strategy.generateResourcePackFiles(List.of(voidSword), sourceDir, "itemforge", outputDir);

        assertTrue(Files.exists(outputDir.resolve("assets/itemforge/models/custom/void_sword.json")));
        assertTrue(Files.exists(outputDir.resolve("assets/itemforge/items/void_sword.json")));
    }

    @Test
    void itemsWithoutCustomModelDataAreExcluded(@TempDir Path sourceDir, @TempDir Path outputDir) throws IOException {
        ItemDefinition vanillaLike = new ItemDefinition("plain_sword", "NETHERITE_SWORD", 0, "name", List.of(),
                List.of());

        strategy.generateResourcePackFiles(List.of(vanillaLike), sourceDir, "itemforge", outputDir);

        assertFalse(Files.exists(outputDir.resolve("assets/itemforge/models/custom/plain_sword.json")));
        assertFalse(Files.exists(outputDir.resolve("assets/itemforge/items/plain_sword.json")));
    }

    @Test
    void missingTextureStillWritesModelFilesSoClientRendersMissingInsteadOfVanilla(@TempDir Path sourceDir,
            @TempDir Path outputDir) throws IOException {
        ItemDefinition voidSword = new ItemDefinition("void_sword", "NETHERITE_SWORD", 1000, "name", List.of(),
                List.of());

        strategy.generateResourcePackFiles(List.of(voidSword), sourceDir, "itemforge", outputDir);

        assertTrue(Files.exists(outputDir.resolve("assets/itemforge/models/custom/void_sword.json")));
        assertTrue(Files.exists(outputDir.resolve("assets/itemforge/items/void_sword.json")));
        assertFalse(Files.exists(outputDir.resolve("assets/itemforge/textures/item/void_sword.png")));
    }
}
