package com.hoangluongtran0309.infrastructure.bukkit.model;

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
import com.hoangluongtran0309.infrastructure.resourcepack.ArmorTextureFileCopier;
import com.hoangluongtran0309.infrastructure.resourcepack.ModelJsonGenerator;

class LegacyArmorModelStrategyTest {

    private final LegacyArmorModelStrategy strategy = new LegacyArmorModelStrategy(
            new ArmorTextureFileCopier(Logger.getAnonymousLogger()), new ModelJsonGenerator(),
            Logger.getAnonymousLogger());

    @Test
    void singleDefinitionPerFamilyIsProcessed(@TempDir Path sourceDir, @TempDir Path outputDir) throws IOException {
        Files.writeString(sourceDir.resolve("void_chestplate_layer_1.png"), "bytes");
        ArmorDefinition chestplate = new ArmorDefinition("void_chestplate", "NETHERITE_CHESTPLATE",
                ArmorSlot.CHESTPLATE, null, 0, "name", List.of());

        strategy.generateResourcePackFiles(List.of(chestplate), sourceDir, "itemforge", outputDir);

        assertTrue(Files.exists(outputDir.resolve("assets/minecraft/textures/models/armor/netherite_layer_1.png")));
    }

    @Test
    void collisionInSameFamilyKeepsOnlyAlphabeticallyFirst(@TempDir Path sourceDir, @TempDir Path outputDir)
            throws IOException {
        Files.writeString(sourceDir.resolve("aaa_helmet_layer_1.png"), "bytes-a");
        Files.writeString(sourceDir.resolve("zzz_helmet_layer_1.png"), "bytes-z");

        ArmorDefinition first = new ArmorDefinition("aaa_helmet", "NETHERITE_HELMET", ArmorSlot.HELMET, null, 0, "a",
                List.of());
        ArmorDefinition second = new ArmorDefinition("zzz_helmet", "NETHERITE_HELMET", ArmorSlot.HELMET, null, 0, "z",
                List.of());

        strategy.generateResourcePackFiles(List.of(second, first), sourceDir, "itemforge", outputDir);

        Path globalLayer = outputDir.resolve("assets/minecraft/textures/models/armor/netherite_layer_1.png");
        assertTrue(Files.exists(globalLayer));
        assertTrue(Files.readString(globalLayer).equals("bytes-a"));
    }

    @Test
    void missingLayer1WritesPlaceholderInsteadOfLeavingVanillaTexture(@TempDir Path sourceDir, @TempDir Path outputDir)
            throws IOException {
        ArmorDefinition chestplate = new ArmorDefinition("void_chestplate", "NETHERITE_CHESTPLATE",
                ArmorSlot.CHESTPLATE, null, 0, "name", List.of());

        strategy.generateResourcePackFiles(List.of(chestplate), sourceDir, "itemforge", outputDir);

        Path placeholder = outputDir.resolve("assets/minecraft/textures/models/armor/netherite_layer_1.png");
        assertTrue(Files.exists(placeholder));
        assertTrue(Files.size(placeholder) > 0);
    }

    @Test
    void sharedArmorAssetIdAcrossPiecesUsesSharedSourceTexture(@TempDir Path sourceDir, @TempDir Path outputDir)
            throws IOException {
        Files.writeString(sourceDir.resolve("void_armor_layer_1.png"), "shared-bytes");

        ArmorDefinition helmet = new ArmorDefinition("void_helmet", "NETHERITE_HELMET", ArmorSlot.HELMET,
                "void_armor", 0, "helm", List.of());
        ArmorDefinition chestplate = new ArmorDefinition("void_chestplate", "NETHERITE_CHESTPLATE",
                ArmorSlot.CHESTPLATE, "void_armor", 0, "chest", List.of());

        strategy.generateResourcePackFiles(List.of(helmet, chestplate), sourceDir, "itemforge", outputDir);

        Path globalLayer = outputDir.resolve("assets/minecraft/textures/models/armor/netherite_layer_1.png");
        assertTrue(Files.exists(globalLayer));
        assertTrue(Files.readString(globalLayer).equals("shared-bytes"));
    }

    // The tests below cover the 2D icon (CustomModelData + overrides), independently of
    // layer_1/layer_2 above. They follow the same shape as LegacyModelStrategyTest, since
    // the two mechanisms are now nearly identical apart from
    // ArmorDefinition vs ItemDefinition.

    @Test
    void armorsSharingMaterialShareOneOverridesFile(@TempDir Path sourceDir, @TempDir Path outputDir)
            throws IOException {
        ArmorDefinition helmet = new ArmorDefinition("void_helmet", "NETHERITE_HELMET", ArmorSlot.HELMET, null, 4001,
                "name", List.of());
        ArmorDefinition otherHelmet = new ArmorDefinition("frost_helmet", "NETHERITE_HELMET", ArmorSlot.HELMET, null,
                4002, "name", List.of());

        strategy.generateResourcePackFiles(List.of(helmet, otherHelmet), sourceDir, "itemforge", outputDir);

        Path overridesFile = outputDir.resolve("assets/minecraft/models/item/netherite_helmet.json");
        assertTrue(Files.exists(overridesFile));
        String json = Files.readString(overridesFile);
        assertTrue(json.contains("void_helmet"));
        assertTrue(json.contains("frost_helmet"));
    }

    @Test
    void armorsWithoutCustomModelDataAreExcludedFromIconOverrides(@TempDir Path sourceDir, @TempDir Path outputDir)
            throws IOException {
        ArmorDefinition vanillaLike = new ArmorDefinition("plain_helmet", "NETHERITE_HELMET", ArmorSlot.HELMET, null, 0,
                "name", List.of());

        strategy.generateResourcePackFiles(List.of(vanillaLike), sourceDir, "itemforge", outputDir);

        assertFalse(Files.exists(outputDir.resolve("assets/minecraft/models/item/netherite_helmet.json")));
        assertFalse(Files.exists(outputDir.resolve("assets/itemforge/models/custom/plain_helmet.json")));
    }

    @Test
    void eachArmorGetsItsOwnLeafModel(@TempDir Path sourceDir, @TempDir Path outputDir) throws IOException {
        ArmorDefinition helmet = new ArmorDefinition("void_helmet", "NETHERITE_HELMET", ArmorSlot.HELMET, null, 4001,
                "name", List.of());

        strategy.generateResourcePackFiles(List.of(helmet), sourceDir, "itemforge", outputDir);

        assertTrue(Files.exists(outputDir.resolve("assets/itemforge/models/custom/void_helmet.json")));
    }

    @Test
    void overridesFileKeepsVanillaTextureForUnrelatedArmorOfSameMaterial(@TempDir Path sourceDir,
            @TempDir Path outputDir) throws IOException {
        ArmorDefinition helmet = new ArmorDefinition("void_helmet", "NETHERITE_HELMET", ArmorSlot.HELMET, null, 4001,
                "name", List.of());

        strategy.generateResourcePackFiles(List.of(helmet), sourceDir, "itemforge", outputDir);

        String json = Files.readString(outputDir.resolve("assets/minecraft/models/item/netherite_helmet.json"));
        assertTrue(json.contains("minecraft:item/netherite_helmet"),
                "base textures must be preserved so unrelated vanilla armor does not lose its icon: " + json);
    }

    @Test
    void armorMissingIconTextureStillGetsOwnOverrideAndLeafModelPointingToMissingTexture(@TempDir Path sourceDir,
            @TempDir Path outputDir) throws IOException {
        ArmorDefinition helmet = new ArmorDefinition("void_helmet", "NETHERITE_HELMET", ArmorSlot.HELMET, null, 4001,
                "name", List.of());

        strategy.generateResourcePackFiles(List.of(helmet), sourceDir, "itemforge", outputDir);

        String json = Files.readString(outputDir.resolve("assets/minecraft/models/item/netherite_helmet.json"));
        assertTrue(json.contains("void_helmet"), "armor without an icon must still get its own override: " + json);
        // The leaf model is still written (not skipped), but its layer0 texture was never
        // copied -> the client shows the missing-texture block when resolving it.
        assertTrue(Files.exists(outputDir.resolve("assets/itemforge/models/custom/void_helmet.json")));
        assertFalse(Files.exists(outputDir.resolve("assets/itemforge/textures/item/void_helmet.png")));
    }
}
