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

class LegacyModelStrategyTest {

    private final LegacyModelStrategy strategy = new LegacyModelStrategy(new ModelJsonGenerator(),
            new TextureFileCopier(Logger.getAnonymousLogger()));

    @Test
    void itemsSharingMaterialShareOneOverridesFile(@TempDir Path sourceDir, @TempDir Path outputDir)
            throws IOException {
        ItemDefinition voidSword = new ItemDefinition("void_sword", "NETHERITE_SWORD", 1000, "name", List.of(),
                List.of());
        ItemDefinition iceSword = new ItemDefinition("ice_sword", "NETHERITE_SWORD", 2000, "name", List.of(),
                List.of());

        strategy.generateResourcePackFiles(List.of(voidSword, iceSword), sourceDir, "itemforge", outputDir);

        Path overridesFile = outputDir.resolve("assets/minecraft/models/item/netherite_sword.json");
        assertTrue(Files.exists(overridesFile));
        String json = Files.readString(overridesFile);
        assertTrue(json.contains("void_sword"));
        assertTrue(json.contains("ice_sword"));
    }

    @Test
    void itemsWithoutCustomModelDataAreExcluded(@TempDir Path sourceDir, @TempDir Path outputDir) throws IOException {
        ItemDefinition vanillaLike = new ItemDefinition("plain_sword", "NETHERITE_SWORD", 0, "name", List.of(),
                List.of());

        strategy.generateResourcePackFiles(List.of(vanillaLike), sourceDir, "itemforge", outputDir);

        assertFalse(Files.exists(outputDir.resolve("assets/minecraft/models/item/netherite_sword.json")));
        assertFalse(Files.exists(outputDir.resolve("assets/itemforge/models/custom/plain_sword.json")));
    }

    @Test
    void eachItemGetsItsOwnLeafModel(@TempDir Path sourceDir, @TempDir Path outputDir) throws IOException {
        ItemDefinition voidSword = new ItemDefinition("void_sword", "NETHERITE_SWORD", 1000, "name", List.of(),
                List.of());

        strategy.generateResourcePackFiles(List.of(voidSword), sourceDir, "itemforge", outputDir);

        assertTrue(Files.exists(outputDir.resolve("assets/itemforge/models/custom/void_sword.json")));
    }

    @Test
    void overridesFileKeepsVanillaTextureForUnrelatedItemsOfSameMaterial(@TempDir Path sourceDir,
            @TempDir Path outputDir) throws IOException {
        ItemDefinition voidSword = new ItemDefinition("void_sword", "NETHERITE_SWORD", 1000, "name", List.of(),
                List.of());

        strategy.generateResourcePackFiles(List.of(voidSword), sourceDir, "itemforge", outputDir);

        String json = Files.readString(outputDir.resolve("assets/minecraft/models/item/netherite_sword.json"));
        assertTrue(json.contains("minecraft:item/netherite_sword"),
                "base textures must be preserved so unrelated vanilla items do not lose their icon: " + json);
    }

    @Test
    void itemMissingTextureStillGetsOwnOverrideAndLeafModelPointingToMissingTexture(@TempDir Path sourceDir,
            @TempDir Path outputDir) throws IOException {
        ItemDefinition voidSword = new ItemDefinition("void_sword", "NETHERITE_SWORD", 1000, "name", List.of(),
                List.of());

        strategy.generateResourcePackFiles(List.of(voidSword), sourceDir, "itemforge", outputDir);

        String json = Files.readString(outputDir.resolve("assets/minecraft/models/item/netherite_sword.json"));
        assertTrue(json.contains("void_sword"), "an item without a texture must still get its own override: " + json);
        // The leaf model is still written (not skipped), but its layer0 texture was never
        // copied -> the client shows the missing-texture block when resolving it.
        assertTrue(Files.exists(outputDir.resolve("assets/itemforge/models/custom/void_sword.json")));
        assertFalse(Files.exists(outputDir.resolve("assets/itemforge/textures/item/void_sword.png")));
    }
}
