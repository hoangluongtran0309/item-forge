package com.hoangluongtran0309.infrastructure.bukkit.model;

import com.hoangluongtran0309.application.ServerVersion;
import com.hoangluongtran0309.infrastructure.resourcepack.BlockModelJsonGenerator;
import com.hoangluongtran0309.infrastructure.resourcepack.BlockStateJsonGenerator;
import com.hoangluongtran0309.infrastructure.resourcepack.BlockTextureFileCopier;

public class BlockModelStrategyFactory {

    public BlockModelStrategy create(ServerVersion version, BlockModelJsonGenerator modelJsonGenerator,
            BlockTextureFileCopier textureFileCopier, BlockStateJsonGenerator blockStateJsonGenerator) {
        return version.isAtLeast(1, 21, 4)
                ? new ModernBlockModelStrategy(modelJsonGenerator, textureFileCopier, blockStateJsonGenerator)
                : new LegacyBlockModelStrategy(modelJsonGenerator, textureFileCopier, blockStateJsonGenerator);
    }
}
