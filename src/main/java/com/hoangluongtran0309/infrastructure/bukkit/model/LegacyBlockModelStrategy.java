package com.hoangluongtran0309.infrastructure.bukkit.model;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.bukkit.inventory.meta.ItemMeta;

import com.hoangluongtran0309.domain.model.CustomBlockDefinition;
import com.hoangluongtran0309.infrastructure.resourcepack.BlockModelJsonGenerator;
import com.hoangluongtran0309.infrastructure.resourcepack.BlockStateJsonGenerator;
import com.hoangluongtran0309.infrastructure.resourcepack.BlockTextureFileCopier;

public class LegacyBlockModelStrategy implements BlockModelStrategy {

    private final BlockModelJsonGenerator modelJsonGenerator;
    private final BlockTextureFileCopier textureFileCopier;
    private final BlockStateJsonGenerator blockStateJsonGenerator;

    public LegacyBlockModelStrategy(BlockModelJsonGenerator modelJsonGenerator,
            BlockTextureFileCopier textureFileCopier, BlockStateJsonGenerator blockStateJsonGenerator) {
        this.modelJsonGenerator = modelJsonGenerator;
        this.textureFileCopier = textureFileCopier;
        this.blockStateJsonGenerator = blockStateJsonGenerator;
    }

    @Override
    public void applyModel(ItemMeta meta, CustomBlockDefinition definition) {
        if (definition.customModelData() > 0) {
            meta.setCustomModelData(definition.customModelData());
        }
    }

    @Override
    public void generateResourcePackFiles(List<CustomBlockDefinition> blocks, Path textureSourceDir,
            String namespace, Path outputDir) throws IOException {
        BlockModelStrategySupport.writeBlockAssets(blocks, textureSourceDir, namespace, outputDir, textureFileCopier,
                modelJsonGenerator, blockStateJsonGenerator);

        // Every custom block uses Material.NOTE_BLOCK, so there is exactly one overrides
        // file to write and no need to group by material the way items do. Nothing is
        // written at all when no block is configured -- the same "no group, no file"
        // convention LegacyModelStrategy follows for items.
        List<CustomBlockDefinition> configured = blocks.stream()
                .filter(block -> block.customModelData() > 0)
                .toList();
        if (configured.isEmpty()) {
            return;
        }

        modelJsonGenerator.writeMaterialOverrides(configured, namespace, outputDir);

        for (CustomBlockDefinition block : configured) {
            modelJsonGenerator.writeItemModel(block.id(), block.textureId(), namespace, outputDir);
        }
    }
}
