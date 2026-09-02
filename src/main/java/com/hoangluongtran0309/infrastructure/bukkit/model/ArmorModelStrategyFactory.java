package com.hoangluongtran0309.infrastructure.bukkit.model;

import java.util.logging.Logger;

import com.hoangluongtran0309.application.ServerVersion;
import com.hoangluongtran0309.infrastructure.resourcepack.ArmorTextureFileCopier;
import com.hoangluongtran0309.infrastructure.resourcepack.EquipmentAssetGenerator;
import com.hoangluongtran0309.infrastructure.resourcepack.ModelJsonGenerator;

public class ArmorModelStrategyFactory {

    public ArmorModelStrategy create(ServerVersion version, EquipmentAssetGenerator equipmentAssetGenerator,
            ArmorTextureFileCopier armorTextureFileCopier, ModelJsonGenerator modelJsonGenerator, Logger logger) {
        return version.isAtLeast(1, 21, 4)
                ? new ModernArmorModelStrategy(equipmentAssetGenerator, armorTextureFileCopier, modelJsonGenerator,
                        logger)
                : new LegacyArmorModelStrategy(armorTextureFileCopier, modelJsonGenerator, logger);
    }
}
