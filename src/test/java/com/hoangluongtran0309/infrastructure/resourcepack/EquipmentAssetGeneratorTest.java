package com.hoangluongtran0309.infrastructure.resourcepack;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class EquipmentAssetGeneratorTest {

    private final EquipmentAssetGenerator generator = new EquipmentAssetGenerator();

    @Test
    void withoutLeggingsLayerOmitsHumanoidLeggingsKey(@TempDir Path outputDir) throws IOException {
        generator.writeEquipmentAsset("void_chestplate", "itemforge", outputDir, false);

        String json = Files.readString(outputDir.resolve("assets/itemforge/equipment/void_chestplate.json"));

        assertTrue(json.contains("\"humanoid\""));
        assertTrue(json.contains("itemforge:void_chestplate"));
        assertFalse(json.contains("humanoid_leggings"));
    }

    @Test
    void withLeggingsLayerIncludesHumanoidLeggingsKey(@TempDir Path outputDir) throws IOException {
        generator.writeEquipmentAsset("void_chestplate", "itemforge", outputDir, true);

        String json = Files.readString(outputDir.resolve("assets/itemforge/equipment/void_chestplate.json"));

        assertTrue(json.contains("\"humanoid\""));
        assertTrue(json.contains("\"humanoid_leggings\""));
    }
}
