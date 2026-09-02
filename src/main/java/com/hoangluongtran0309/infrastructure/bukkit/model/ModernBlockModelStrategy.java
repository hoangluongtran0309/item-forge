package com.hoangluongtran0309.infrastructure.bukkit.model;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.meta.ItemMeta;

import com.hoangluongtran0309.domain.model.CustomBlockDefinition;
import com.hoangluongtran0309.infrastructure.resourcepack.BlockModelJsonGenerator;
import com.hoangluongtran0309.infrastructure.resourcepack.BlockStateJsonGenerator;
import com.hoangluongtran0309.infrastructure.resourcepack.BlockTextureFileCopier;

public class ModernBlockModelStrategy implements BlockModelStrategy {

    // Fixed resource pack namespace; making it configurable would be YAGNI -- same as
    // ModernModelStrategy/LegacyModelStrategy
    private static final String NAMESPACE = "itemforge";

    private final BlockModelJsonGenerator modelJsonGenerator;
    private final BlockTextureFileCopier textureFileCopier;
    private final BlockStateJsonGenerator blockStateJsonGenerator;

    public ModernBlockModelStrategy(BlockModelJsonGenerator modelJsonGenerator,
            BlockTextureFileCopier textureFileCopier, BlockStateJsonGenerator blockStateJsonGenerator) {
        this.modelJsonGenerator = modelJsonGenerator;
        this.textureFileCopier = textureFileCopier;
        this.blockStateJsonGenerator = blockStateJsonGenerator;
    }

    @Override
    public void applyModel(ItemMeta meta, CustomBlockDefinition definition) {
        if (definition.customModelData() > 0) {
            meta.setItemModel(new NamespacedKey(NAMESPACE, definition.id()));
        }
    }

    @Override
    public void generateResourcePackFiles(List<CustomBlockDefinition> blocks, Path textureSourceDir,
            String namespace, Path outputDir) throws IOException {
        BlockModelStrategySupport.writeBlockAssets(blocks, textureSourceDir, namespace, outputDir, textureFileCopier,
                modelJsonGenerator, blockStateJsonGenerator);

        // No grouping by material as on legacy -- each block is independent via its own
        // item_model.
        for (CustomBlockDefinition block : blocks) {
            if (block.customModelData() <= 0) {
                continue;
            }
            modelJsonGenerator.writeItemModel(block.id(), block.textureId(), namespace, outputDir);
            modelJsonGenerator.writeClientItem(block.id(), namespace, outputDir);
        }
    }
}
