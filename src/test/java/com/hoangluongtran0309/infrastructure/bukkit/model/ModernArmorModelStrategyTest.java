package com.hoangluongtran0309.infrastructure.bukkit.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Logger;

import org.bukkit.inventory.EquipmentSlot;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.hoangluongtran0309.domain.model.ArmorDefinition;
import com.hoangluongtran0309.domain.model.ArmorSlot;
import com.hoangluongtran0309.infrastructure.resourcepack.ArmorTextureFileCopier;
import com.hoangluongtran0309.infrastructure.resourcepack.EquipmentAssetGenerator;
import com.hoangluongtran0309.infrastructure.resourcepack.ModelJsonGenerator;

class ModernArmorModelStrategyTest {

    private final ModernArmorModelStrategy strategy = new ModernArmorModelStrategy(new EquipmentAssetGenerator(),
            new ArmorTextureFileCopier(Logger.getAnonymousLogger()), new ModelJsonGenerator(),
            Logger.getAnonymousLogger());

    private final ArmorDefinition armor = new ArmorDefinition("void_chestplate", "NETHERITE_CHESTPLATE",
            ArmorSlot.CHESTPLATE, null, 0, "name", List.of());

    @Test
    void iconPresentGeneratesItemModelAndClientItem(@TempDir Path sourceDir, @TempDir Path outputDir)
            throws IOException {
        Files.writeString(sourceDir.resolve("void_chestplate.png"), "fake-icon-bytes");

        strategy.generateResourcePackFiles(List.of(armor), sourceDir, "itemforge", outputDir);

        assertTrue(Files.exists(outputDir.resolve("assets/itemforge/models/custom/void_chestplate.json")));
        assertTrue(Files.exists(outputDir.resolve("assets/itemforge/items/void_chestplate.json")));
    }

    @Test
    void iconMissingStillWritesModelFilesSoClientRendersMissingInsteadOfVanilla(@TempDir Path sourceDir,
            @TempDir Path outputDir) throws IOException {
        strategy.generateResourcePackFiles(List.of(armor), sourceDir, "itemforge", outputDir);

        // The model/client item is still written (exactly the mechanism regular items use);
        // only the texture is absent, so the client shows the familiar missing-texture block
        // rather than some other, less controllable "missing model" behaviour.
        assertTrue(Files.exists(outputDir.resolve("assets/itemforge/models/custom/void_chestplate.json")));
        assertTrue(Files.exists(outputDir.resolve("assets/itemforge/items/void_chestplate.json")));
        assertFalse(Files.exists(outputDir.resolve("assets/itemforge/textures/item/void_chestplate.png")));
    }

    @Test
    void iconMissingStillProcessesEquipLayerWhenPresent(@TempDir Path sourceDir, @TempDir Path outputDir)
            throws IOException {
        Files.writeString(sourceDir.resolve("void_chestplate_layer_1.png"), "fake-layer-bytes");

        strategy.generateResourcePackFiles(List.of(armor), sourceDir, "itemforge", outputDir);

        assertTrue(Files.exists(
                outputDir.resolve("assets/itemforge/textures/entity/equipment/humanoid/void_chestplate.png")));
        assertTrue(Files.exists(outputDir.resolve("assets/itemforge/equipment/void_chestplate.json")));
    }

    @Test
    void layer1MissingStillWritesEquipmentAssetSoClientRendersMissingInsteadOfVanilla(@TempDir Path sourceDir,
            @TempDir Path outputDir) throws IOException {
        strategy.generateResourcePackFiles(List.of(armor), sourceDir, "itemforge", outputDir);

        // The equipment asset is still written without layer_1 -- its texture entry points
        // at a path that was never copied, the same missing-texture mechanism as icons.
        assertTrue(Files.exists(outputDir.resolve("assets/itemforge/equipment/void_chestplate.json")));
        assertFalse(Files.exists(
                outputDir.resolve("assets/itemforge/textures/entity/equipment/humanoid/void_chestplate.png")));
    }

    @Test
    void sharedArmorAssetIdGeneratesExactlyOneEquipmentAsset(@TempDir Path sourceDir, @TempDir Path outputDir)
            throws IOException {
        Files.writeString(sourceDir.resolve("void_armor_layer_1.png"), "fake-layer-bytes");

        ArmorDefinition helmet = new ArmorDefinition("void_helmet", "NETHERITE_HELMET", ArmorSlot.HELMET,
                "void_armor", 0, "helm", List.of());
        ArmorDefinition chestplate = new ArmorDefinition("void_chestplate", "NETHERITE_CHESTPLATE",
                ArmorSlot.CHESTPLATE, "void_armor", 0, "chest", List.of());

        strategy.generateResourcePackFiles(List.of(helmet, chestplate), sourceDir, "itemforge", outputDir);

        assertTrue(Files.exists(outputDir.resolve("assets/itemforge/equipment/void_armor.json")));
        assertFalse(Files.exists(outputDir.resolve("assets/itemforge/equipment/void_helmet.json")));
        assertFalse(Files.exists(outputDir.resolve("assets/itemforge/equipment/void_chestplate.json")));
    }

    @Test
    void toEquipmentSlotMapsAllFourArmorSlots() {
        assertEquals(EquipmentSlot.HEAD, strategy.toEquipmentSlot(ArmorSlot.HELMET));
        assertEquals(EquipmentSlot.CHEST, strategy.toEquipmentSlot(ArmorSlot.CHESTPLATE));
        assertEquals(EquipmentSlot.LEGS, strategy.toEquipmentSlot(ArmorSlot.LEGGINGS));
        assertEquals(EquipmentSlot.FEET, strategy.toEquipmentSlot(ArmorSlot.BOOTS));
    }
}
