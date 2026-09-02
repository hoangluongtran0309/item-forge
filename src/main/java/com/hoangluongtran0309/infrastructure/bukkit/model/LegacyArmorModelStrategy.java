package com.hoangluongtran0309.infrastructure.bukkit.model;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.bukkit.inventory.meta.ItemMeta;

import com.hoangluongtran0309.domain.model.ArmorDefinition;
import com.hoangluongtran0309.infrastructure.resourcepack.ArmorTextureFileCopier;
import com.hoangluongtran0309.infrastructure.resourcepack.MissingTexturePlaceholder;
import com.hoangluongtran0309.infrastructure.resourcepack.ModelJsonGenerator;

public class LegacyArmorModelStrategy implements ArmorModelStrategy {

    // Standard humanoid armor layer texture size, unchanged since 1.9.
    private static final int ARMOR_LAYER_WIDTH = 64;
    private static final int ARMOR_LAYER_HEIGHT = 32;

    private final ArmorTextureFileCopier armorTextureFileCopier;
    private final ModelJsonGenerator modelJsonGenerator;
    private final Logger logger;

    public LegacyArmorModelStrategy(ArmorTextureFileCopier armorTextureFileCopier,
            ModelJsonGenerator modelJsonGenerator, Logger logger) {
        this.armorTextureFileCopier = armorTextureFileCopier;
        this.modelJsonGenerator = modelJsonGenerator;
        this.logger = logger;
    }

    @Override
    public void applyModel(ItemMeta meta, ArmorDefinition definition) {
        // Before 1.21.4 there is no separate equippable component, but the 2D icon still
        // goes through exactly the same CustomModelData + overrides mechanism as a regular
        // item (see generateResourcePackFiles). The worn texture (layer_1/layer_2) can only
        // be overridden globally per material family, which is unrelated to this method.
        if (definition.customModelData() > 0) {
            meta.setCustomModelData(definition.customModelData());
        }
    }

    @Override
    public void generateResourcePackFiles(List<ArmorDefinition> armors, Path textureSourceDir, String namespace,
            Path outputDir) throws IOException {
        Map<String, List<ArmorDefinition>> byFamily = armors.stream()
                .collect(Collectors.groupingBy(this::familyOf));

        for (Map.Entry<String, List<ArmorDefinition>> entry : byFamily.entrySet()) {
            String family = entry.getKey();

            // Group again by armorAssetId within a family: several pieces of the SAME set
            // (e.g. all four sharing one armor-asset-id) is not a conflict. Only warn when a
            // family holds several DIFFERENT asset ids -- two distinct armor sets landing on
            // the same vanilla material.
            Map<String, List<ArmorDefinition>> byAssetId = entry.getValue().stream()
                    .collect(Collectors.groupingBy(ArmorDefinition::armorAssetId));

            String chosenAssetId = byAssetId.keySet().stream()
                    .min(Comparator.naturalOrder())
                    .orElseThrow();

            if (byAssetId.size() > 1) {
                String skipped = byAssetId.keySet().stream()
                        .filter(assetId -> !assetId.equals(chosenAssetId))
                        .collect(Collectors.joining(", "));
                logger.warning("Legacy servers support only one custom skin per vanilla material. Material '"
                        + family + "' is shared by several different armor sets: using '" + chosenAssetId
                        + "', skipping: " + skipped);
            }

            copyGlobalLayer(chosenAssetId, family, textureSourceDir, outputDir, 1);
            copyGlobalLayer(chosenAssetId, family, textureSourceDir, outputDir, 2);
        }

        generateIconOverrides(armors, textureSourceDir, namespace, outputDir);
    }

    // The 2D icon in the inventory, in hand and on the ground -- independent of
    // layer_1/layer_2 above, and sharing the CustomModelData + "overrides" mechanism
    // LegacyModelStrategy uses for regular items. Grouped by the EXACT material (e.g.
    // "diamond_helmet"), unlike familyOf() above which groups by family (e.g. "diamond")
    // for the worn layer textures, because every material has its own
    // models/item/*.json file.
    private void generateIconOverrides(List<ArmorDefinition> armors, Path textureSourceDir, String namespace,
            Path outputDir) throws IOException {
        Map<String, List<ArmorDefinition>> byMaterial = armors.stream()
                .filter(armor -> armor.customModelData() > 0)
                .collect(Collectors.groupingBy(armor -> armor.material().toLowerCase(Locale.ROOT)));

        for (Map.Entry<String, List<ArmorDefinition>> entry : byMaterial.entrySet()) {
            modelJsonGenerator.writeArmorMaterialOverrides(entry.getKey(), entry.getValue(), namespace, outputDir);
        }

        for (ArmorDefinition armor : armors) {
            if (armor.customModelData() <= 0) {
                continue;
            }
            // The copy is best-effort, like TextureFileCopier for regular items: the leaf
            // model is written even when the icon is missing, so there is a single
            // "missing" outcome instead of silently falling back to the material's vanilla
            // icon.
            boolean hasIcon = armorTextureFileCopier.copyIcon(armor, textureSourceDir, namespace, outputDir);
            if (!hasIcon) {
                logger.warning("No icon texture found for armor '" + armor.id()
                        + "'; the inventory will show the missing-texture block.");
            }
            modelJsonGenerator.writeItemModel(armor.id(), namespace, outputDir);
        }
    }

    private String familyOf(ArmorDefinition armor) {
        return armor.material().toLowerCase(Locale.ROOT)
                .replaceFirst("_(helmet|chestplate|leggings|boots)$", "");
    }

    private void copyGlobalLayer(String armorAssetId, String family, Path textureSourceDir, Path outputDir,
            int layer) throws IOException {
        Path source = textureSourceDir.resolve(armorAssetId + "_layer_" + layer + ".png");

        // Overwrite the global vanilla path directly (no custom namespace), because legacy
        // Minecraft picks the armor texture by material, not by item.
        Path target = outputDir.resolve("assets/minecraft/textures/models/armor/"
                + family + "_layer_" + layer + ".png");

        if (Files.notExists(source)) {
            if (layer == 1) {
                // layer_1 is mandatory. Unlike items -- which can "point at a model that does
                // not exist" to force the missing state -- a legacy armor texture is a fixed
                // PNG path, so we have to actively overwrite it with the missing-texture
                // placeholder. Skipping would leave the original vanilla texture in place,
                // exactly the silent fallback we are trying to avoid.
                logger.warning("No layer_1 texture found for armor asset '" + armorAssetId + "' (" + source
                        + "); using the missing-texture placeholder for material '" + family + "'.");
                Files.createDirectories(target.getParent());
                MissingTexturePlaceholder.writeTo(target, ARMOR_LAYER_WIDTH, ARMOR_LAYER_HEIGHT);
            }
            // layer_2 is an optional overlay (leggings use their own, for example). Its
            // absence is normal rather than a "missing" case, so no placeholder is forced.
            return;
        }

        Files.createDirectories(target.getParent());
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
    }
}
