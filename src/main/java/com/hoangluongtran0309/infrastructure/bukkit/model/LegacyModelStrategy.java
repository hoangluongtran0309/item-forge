package com.hoangluongtran0309.infrastructure.bukkit.model;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.bukkit.inventory.meta.ItemMeta;

import com.hoangluongtran0309.domain.model.ItemDefinition;
import com.hoangluongtran0309.infrastructure.resourcepack.ModelJsonGenerator;
import com.hoangluongtran0309.infrastructure.resourcepack.TextureFileCopier;

public class LegacyModelStrategy implements ItemModelStrategy {

    // Fixed resource pack namespace; making it configurable would be YAGNI.
    private static final String NAMESPACE = "itemforge";

    private final ModelJsonGenerator modelJsonGenerator;
    private final TextureFileCopier textureFileCopier;

    public LegacyModelStrategy(ModelJsonGenerator modelJsonGenerator, TextureFileCopier textureFileCopier) {
        this.modelJsonGenerator = modelJsonGenerator;
        this.textureFileCopier = textureFileCopier;
    }

    @Override
    public void applyModel(ItemMeta meta, ItemDefinition definition) {
        if (definition.customModelData() > 0) {
            meta.setCustomModelData(definition.customModelData());
        }
    }

    @Override
    public void generateResourcePackFiles(List<ItemDefinition> items, Path textureSourceDir, String namespace,
            Path outputDir) throws IOException {
        // Put EVERY item with customModelData > 0 into the overrides file, even when its
        // texture is missing: the item's predicate is still declared, but points at a leaf
        // model that will never be written (there is no texture). The client then shows
        // the missing-texture block for that item alone, while every other item on the
        // same material -- unrelated vanilla items included -- keeps using the original
        // "textures" entry above.
        Map<String, List<ItemDefinition>> byMaterial = items.stream()
                .filter(item -> item.customModelData() > 0)
                // The material in ItemDefinition is the Bukkit enum name (e.g.
                // "DIAMOND_SWORD"), but a Minecraft resource id must be lowercase (e.g.
                // "diamond_sword").
                .collect(Collectors.groupingBy(item -> item.material().toLowerCase(Locale.ROOT)));

        for (Map.Entry<String, List<ItemDefinition>> entry : byMaterial.entrySet()) {
            modelJsonGenerator.writeMaterialOverrides(entry.getKey(), entry.getValue(), namespace, outputDir);
        }

        for (ItemDefinition item : items) {
            if (item.customModelData() <= 0) {
                continue;
            }
            // The copy is best-effort -- like ModernModelStrategy the leaf model is written
            // whether or not the texture exists, so there is exactly one "missing" point to
            // point at (either the texture or the model) rather than a silent skip.
            textureFileCopier.copyIfExists(item, textureSourceDir, namespace, outputDir);
            modelJsonGenerator.writeItemModel(item, namespace, outputDir);
        }
    }
}
