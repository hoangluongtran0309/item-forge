package com.hoangluongtran0309.infrastructure.resourcepack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.hoangluongtran0309.domain.model.ItemDefinition;

class ModelJsonGeneratorTest {

    private final ModelJsonGenerator generator = new ModelJsonGenerator();

    @Test
    void writeMaterialOverridesSortsByCustomModelData(@TempDir Path outputDir) throws IOException {
        ItemDefinition high = new ItemDefinition("ice_sword", "NETHERITE_SWORD", 2000, "name", List.of(), List.of());
        ItemDefinition low = new ItemDefinition("void_sword", "NETHERITE_SWORD", 1000, "name", List.of(), List.of());

        generator.writeMaterialOverrides("netherite_sword", List.of(high, low), "itemforge", outputDir);

        String json = Files.readString(outputDir.resolve("assets/minecraft/models/item/netherite_sword.json"));

        assertTrue(json.contains("\"parent\""));
        int lowIndex = json.indexOf("void_sword");
        int highIndex = json.indexOf("ice_sword");
        assertTrue(lowIndex >= 0 && highIndex >= 0 && lowIndex < highIndex,
                "override with smaller custom-model-data should appear first: " + json);
    }

    @Test
    void writeMaterialOverridesKeepsBaseVanillaTexture(@TempDir Path outputDir) throws IOException {
        ItemDefinition voidSword = new ItemDefinition("void_sword", "NETHERITE_SWORD", 1000, "name", List.of(),
                List.of());

        generator.writeMaterialOverrides("netherite_sword", List.of(voidSword), "itemforge", outputDir);

        String json = Files.readString(outputDir.resolve("assets/minecraft/models/item/netherite_sword.json"));
        assertTrue(json.contains("minecraft:item/netherite_sword"),
                "base textures must be preserved so vanilla items matching no predicate still render correctly: " + json);
    }

    @Test
    void writeItemModelReferencesCorrectTexture(@TempDir Path outputDir) throws IOException {
        ItemDefinition item = new ItemDefinition("void_sword", "NETHERITE_SWORD", 1000, "name", List.of(), List.of());

        generator.writeItemModel(item, "itemforge", outputDir);

        String json = Files.readString(outputDir.resolve("assets/itemforge/models/custom/void_sword.json"));
        assertTrue(json.contains("itemforge:item/void_sword"));
    }

    @Test
    void writeClientItemReferencesCustomModel(@TempDir Path outputDir) throws IOException {
        ItemDefinition item = new ItemDefinition("void_sword", "NETHERITE_SWORD", 1000, "name", List.of(), List.of());

        generator.writeClientItem(item, "itemforge", outputDir);

        String json = Files.readString(outputDir.resolve("assets/itemforge/items/void_sword.json"));
        assertTrue(json.contains("minecraft:model"));
        assertTrue(json.contains("itemforge:custom/void_sword"));
    }

    @Test
    void writeItemModelCreatesExpectedFileCount(@TempDir Path outputDir) throws IOException {
        ItemDefinition item = new ItemDefinition("void_sword", "NETHERITE_SWORD", 1000, "name", List.of(), List.of());
        generator.writeItemModel(item, "itemforge", outputDir);

        try (var files = Files.list(outputDir.resolve("assets/itemforge/models/custom"))) {
            assertEquals(1, files.count());
        }
    }

    @Test
    void writeItemModelUsesHandheldParentForWeaponsAndTools(@TempDir Path outputDir) throws IOException {
        ItemDefinition item = new ItemDefinition("void_sword", "NETHERITE_SWORD", 1000, "name", List.of(), List.of());

        generator.writeItemModel(item, "itemforge", outputDir);

        String json = Files.readString(outputDir.resolve("assets/itemforge/models/custom/void_sword.json"));
        assertTrue(json.contains("\"parent\": \"item/handheld\""),
                "weapons and tools must use the handheld parent so they are not held like a flat icon: " + json);
    }

    @Test
    void writeItemModelUsesGeneratedParentForNonHandheldMaterials(@TempDir Path outputDir) throws IOException {
        ItemDefinition item = new ItemDefinition("magic_apple", "APPLE", 1000, "name", List.of(), List.of());

        generator.writeItemModel(item, "itemforge", outputDir);

        String json = Files.readString(outputDir.resolve("assets/itemforge/models/custom/magic_apple.json"));
        assertTrue(json.contains("\"parent\": \"item/generated\""), json);
    }

    @Test
    void writeItemModelWithoutMaterialAlwaysUsesGeneratedParent(@TempDir Path outputDir) throws IOException {
        // The overload used by armor -- there is no material, so handheld cannot be
        // inferred.
        generator.writeItemModel("chest_iron", "itemforge", outputDir);

        String json = Files.readString(outputDir.resolve("assets/itemforge/models/custom/chest_iron.json"));
        assertTrue(json.contains("\"parent\": \"item/generated\""), json);
    }

    @Test
    void writeMaterialOverridesUsesHandheldParentForSwordMaterial(@TempDir Path outputDir) throws IOException {
        ItemDefinition voidSword = new ItemDefinition("void_sword", "NETHERITE_SWORD", 1000, "name", List.of(),
                List.of());

        generator.writeMaterialOverrides("netherite_sword", List.of(voidSword), "itemforge", outputDir);

        String json = Files.readString(outputDir.resolve("assets/minecraft/models/item/netherite_sword.json"));
        assertTrue(json.contains("\"parent\": \"item/handheld\""), json);
    }
}
