package com.hoangluongtran0309.infrastructure.bukkit.model;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.util.logging.Logger;

import org.junit.jupiter.api.Test;

import com.hoangluongtran0309.application.ServerVersion;
import com.hoangluongtran0309.infrastructure.resourcepack.ArmorTextureFileCopier;
import com.hoangluongtran0309.infrastructure.resourcepack.EquipmentAssetGenerator;
import com.hoangluongtran0309.infrastructure.resourcepack.ModelJsonGenerator;

class ArmorModelStrategyFactoryTest {

    private final ArmorModelStrategyFactory factory = new ArmorModelStrategyFactory();
    private final EquipmentAssetGenerator equipmentAssetGenerator = new EquipmentAssetGenerator();
    private final ArmorTextureFileCopier armorTextureFileCopier = new ArmorTextureFileCopier(
            Logger.getAnonymousLogger());
    private final ModelJsonGenerator modelJsonGenerator = new ModelJsonGenerator();

    @Test
    void selectsModernStrategyAtCutoffVersion() {
        ArmorModelStrategy strategy = factory.create(new ServerVersion(1, 21, 4), equipmentAssetGenerator,
                armorTextureFileCopier, modelJsonGenerator, Logger.getAnonymousLogger());
        assertInstanceOf(ModernArmorModelStrategy.class, strategy);
    }

    @Test
    void selectsLegacyStrategyBelowCutoffVersion() {
        ArmorModelStrategy strategy = factory.create(new ServerVersion(1, 20, 6), equipmentAssetGenerator,
                armorTextureFileCopier, modelJsonGenerator, Logger.getAnonymousLogger());
        assertInstanceOf(LegacyArmorModelStrategy.class, strategy);
    }
}
