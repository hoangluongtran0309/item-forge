package com.hoangluongtran0309.infrastructure.bukkit.model;

import com.hoangluongtran0309.application.ServerVersion;
import com.hoangluongtran0309.infrastructure.resourcepack.ModelJsonGenerator;
import com.hoangluongtran0309.infrastructure.resourcepack.TextureFileCopier;

public class ItemModelStrategyFactory {

    public ItemModelStrategy create(ServerVersion version, ModelJsonGenerator modelJsonGenerator,
            TextureFileCopier textureFileCopier) {
        return version.isAtLeast(1, 21, 4)
                ? new ModernModelStrategy(modelJsonGenerator, textureFileCopier)
                : new LegacyModelStrategy(modelJsonGenerator, textureFileCopier);
    }
}
