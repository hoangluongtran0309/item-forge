package com.hoangluongtran0309.infrastructure.bukkit.model;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.meta.ItemMeta;

import com.hoangluongtran0309.domain.model.ItemDefinition;
import com.hoangluongtran0309.infrastructure.resourcepack.ModelJsonGenerator;
import com.hoangluongtran0309.infrastructure.resourcepack.TextureFileCopier;

public class ModernModelStrategy implements ItemModelStrategy {

    // Fixed resource pack namespace; making it configurable would be YAGNI -- same as LegacyModelStrategy.
    private static final String NAMESPACE = "itemforge";

    private final ModelJsonGenerator modelJsonGenerator;
    private final TextureFileCopier textureFileCopier;

    public ModernModelStrategy(ModelJsonGenerator modelJsonGenerator, TextureFileCopier textureFileCopier) {
        this.modelJsonGenerator = modelJsonGenerator;
        this.textureFileCopier = textureFileCopier;
    }

    @Override
    public void applyModel(ItemMeta meta, ItemDefinition definition) {
        if (definition.customModelData() > 0) {
            meta.setItemModel(new NamespacedKey(NAMESPACE, definition.id()));
        }
    }

    @Override
    public void generateResourcePackFiles(List<ItemDefinition> items, Path textureSourceDir, String namespace,
            Path outputDir) throws IOException {
        // No grouping by material as on legacy -- each item is independent via its own
        // item_model, so items sharing a material cannot overwrite each other.
        //
        // The model/client item is always written, texture present or not: without one,
        // layer0 points at a path that does not exist and the client shows the
        // missing-texture block instead of falling back to the material's vanilla icon.
        for (ItemDefinition item : items) {
            if (item.customModelData() <= 0) {
                continue;
            }
            textureFileCopier.copyIfExists(item, textureSourceDir, namespace, outputDir);
            modelJsonGenerator.writeItemModel(item, namespace, outputDir);
            modelJsonGenerator.writeClientItem(item, namespace, outputDir);
        }
    }
}
