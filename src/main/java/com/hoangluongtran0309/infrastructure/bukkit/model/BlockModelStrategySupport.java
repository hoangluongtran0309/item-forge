package com.hoangluongtran0309.infrastructure.bukkit.model;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import com.hoangluongtran0309.domain.model.CustomBlockDefinition;
import com.hoangluongtran0309.infrastructure.resourcepack.BlockModelJsonGenerator;
import com.hoangluongtran0309.infrastructure.resourcepack.BlockStateJsonGenerator;
import com.hoangluongtran0309.infrastructure.resourcepack.BlockTextureFileCopier;

// The steps shared by Legacy and Modern: blockstates/note_block.json and
// models/block/<textureId>.json have the SAME format across Minecraft versions. Only
// the way the held item points AT the block/custom model differs (CustomModelData vs
// the item_model component), and each strategy handles that itself (see
// LegacyBlockModelStrategy/ModernBlockModelStrategy).
final class BlockModelStrategySupport {

    private BlockModelStrategySupport() {
    }

    static void writeBlockAssets(List<CustomBlockDefinition> blocks, Path textureSourceDir, String namespace,
            Path outputDir, BlockTextureFileCopier textureFileCopier, BlockModelJsonGenerator modelJsonGenerator,
            BlockStateJsonGenerator blockStateJsonGenerator) throws IOException {
        for (CustomBlockDefinition block : blocks) {
            // The copy is best-effort; the model is written whether or not the texture
            // exists -- the same convention ModelJsonGenerator/TextureFileCopier use for
            // items.
            textureFileCopier.copyIfExists(block.textureId(), textureSourceDir, namespace, outputDir);
            modelJsonGenerator.writeBlockModel(block.textureId(), namespace, outputDir);
        }

        blockStateJsonGenerator.write(blocks, namespace, outputDir);
    }
}
