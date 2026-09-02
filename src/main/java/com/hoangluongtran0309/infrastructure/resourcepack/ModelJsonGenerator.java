package com.hoangluongtran0309.infrastructure.resourcepack;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import com.hoangluongtran0309.domain.model.ArmorDefinition;
import com.hoangluongtran0309.domain.model.ItemDefinition;

public class ModelJsonGenerator {

    private static final String PARENT_GENERATED = "item/generated";
    private static final String PARENT_HANDHELD = "item/handheld";

    // Vanilla weapons and tools are named "<material>_<kind>" (e.g. DIAMOND_SWORD,
    // WOODEN_HOE), so a suffix match identifies them. A few other handheld items do not
    // follow that pattern and are listed by full name separately.
    private static final List<String> HANDHELD_SUFFIXES = List.of(
            "_SWORD", "_PICKAXE", "_AXE", "_SHOVEL", "_HOE");
    private static final Set<String> HANDHELD_EXACT_MATERIALS = Set.of(
            "BOW", "CROSSBOW", "TRIDENT", "FISHING_ROD", "SHEARS", "FLINT_AND_STEEL",
            "SHIELD", "CARROT_ON_A_STICK", "WARPED_FUNGUS_ON_A_STICK", "MACE");

    public void writeMaterialOverrides(String material, List<ItemDefinition> itemsForMaterial,
            String namespace, Path outputDir) throws IOException {
        Path target = outputDir.resolve("assets/minecraft/models/item/" + material + ".json");
        Files.createDirectories(target.getParent());
        Files.writeString(target, buildOverridesJson(material, itemsForMaterial, namespace));
    }

    public void writeItemModel(ItemDefinition item, String namespace, Path outputDir) throws IOException {
        writeLeafModel(item.id(), parentFor(item.material()), namespace, outputDir);
    }

    // A dedicated overload for legacy armor icons: unlike writeMaterialOverrides for
    // regular items, armor never uses the handheld parent, so there is no parent to infer
    // from the material -- it is always item/generated (like writeItemModel(String)).
    public void writeArmorMaterialOverrides(String material, List<ArmorDefinition> armorsForMaterial,
            String namespace, Path outputDir) throws IOException {
        Path target = outputDir.resolve("assets/minecraft/models/item/" + material + ".json");
        Files.createDirectories(target.getParent());
        Files.writeString(target, buildArmorOverridesJson(material, armorsForMaterial, namespace));
    }

    // A shared overload for armor (ArmorDefinition) -- it only needs the id, not a whole
    // ItemDefinition, which keeps ModelJsonGenerator decoupled from the armor domain
    // model. Armor is never "held", so it always uses the generated parent.
    public void writeItemModel(String id, String namespace, Path outputDir) throws IOException {
        writeLeafModel(id, PARENT_GENERATED, namespace, outputDir);
    }

    private void writeLeafModel(String id, String parent, String namespace, Path outputDir) throws IOException {
        Path target = outputDir.resolve("assets/" + namespace + "/models/custom/" + id + ".json");
        Files.createDirectories(target.getParent());
        Files.writeString(target, buildLeafModelJson(id, namespace, parent));
    }

    // For the modern format (1.21.4+, Client Items). Points at the same leaf model
    // writeItemModel() already wrote under "models/custom/" -- reused, not duplicated.
    public void writeClientItem(ItemDefinition item, String namespace, Path outputDir) throws IOException {
        writeClientItem(item.id(), namespace, outputDir);
    }

    public void writeClientItem(String id, String namespace, Path outputDir) throws IOException {
        Path target = outputDir.resolve("assets/" + namespace + "/items/" + id + ".json");
        Files.createDirectories(target.getParent());
        Files.writeString(target, buildClientItemJson(id, namespace));
    }

    // Vanilla weapons and tools use the "item/handheld" parent so they animate correctly
    // in hand (an angled sword blade, a held fishing rod...); everything else (food,
    // blocks, materials...) uses "item/generated" -- flat, like an icon facing the camera.
    // The material name (a String) is the only thing available to tell them apart,
    // because ItemDefinition is not allowed to depend on org.bukkit.Material (domain
    // layer).
    private static String parentFor(String material) {
        String upper = material.toUpperCase(Locale.ROOT);
        boolean handheld = HANDHELD_EXACT_MATERIALS.contains(upper)
                || HANDHELD_SUFFIXES.stream().anyMatch(upper::endsWith);
        return handheld ? PARENT_HANDHELD : PARENT_GENERATED;
    }

    private String buildOverridesJson(String material, List<ItemDefinition> items, String namespace) {
        // Sort by customModelData so the JSON file is stable across rebuilds and its diffs
        // stay readable.
        String overrides = items.stream()
                .sorted(Comparator.comparingInt(ItemDefinition::customModelData))
                .map(item -> """
                        { "predicate": { "custom_model_data": %d }, "model": "%s:custom/%s" }"""
                        .formatted(item.customModelData(), namespace, item.id()))
                .collect(Collectors.joining(",\n    "));

        // This file completely replaces the material's vanilla model (diamond_sword.json,
        // for example), so it has to re-declare the original "textures" itself. Otherwise
        // UNRELATED vanilla items -- ones matching none of the predicates above -- lose
        // their texture too and show the missing-texture block for no reason.
        return """
                {
                  "parent": "%s",
                  "textures": {
                    "layer0": "minecraft:item/%s"
                  },
                  "overrides": [
                    %s
                  ]
                }
                """.formatted(parentFor(material), material, overrides);
    }

    private String buildArmorOverridesJson(String material, List<ArmorDefinition> armors, String namespace) {
        String overrides = armors.stream()
                .sorted(Comparator.comparingInt(ArmorDefinition::customModelData))
                .map(armor -> """
                        { "predicate": { "custom_model_data": %d }, "model": "%s:custom/%s" }"""
                        .formatted(armor.customModelData(), namespace, armor.id()))
                .collect(Collectors.joining(",\n    "));

        // Armor is never "held" the way a weapon is -- always item/generated, unlike
        // buildOverridesJson() for regular items which has to infer it from the material.
        return """
                {
                  "parent": "%s",
                  "textures": {
                    "layer0": "minecraft:item/%s"
                  },
                  "overrides": [
                    %s
                  ]
                }
                """.formatted(PARENT_GENERATED, material, overrides);
    }

    private String buildLeafModelJson(String id, String namespace, String parent) {
        return """
                {
                  "parent": "%s",
                  "textures": {
                    "layer0": "%s:item/%s"
                  }
                }
                """.formatted(parent, namespace, id);
    }

    private String buildClientItemJson(String id, String namespace) {
        return """
                {
                  "model": {
                    "type": "minecraft:model",
                    "model": "%s:custom/%s"
                  }
                }
                """.formatted(namespace, id);
    }
}
