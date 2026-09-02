package com.hoangluongtran0309.infrastructure.bukkit.model;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import org.junit.jupiter.api.Test;

import java.util.logging.Logger;

import com.hoangluongtran0309.application.ServerVersion;
import com.hoangluongtran0309.infrastructure.resourcepack.ModelJsonGenerator;
import com.hoangluongtran0309.infrastructure.resourcepack.TextureFileCopier;

class ItemModelStrategyFactoryTest {

    private final ItemModelStrategyFactory factory = new ItemModelStrategyFactory();
    private final ModelJsonGenerator modelJsonGenerator = new ModelJsonGenerator();
    private final TextureFileCopier textureFileCopier = new TextureFileCopier(Logger.getAnonymousLogger());

    @Test
    void selectsModernStrategyAtCutoffVersion() {
        ItemModelStrategy strategy = factory.create(new ServerVersion(1, 21, 4), modelJsonGenerator,
                textureFileCopier);
        assertInstanceOf(ModernModelStrategy.class, strategy);
    }

    @Test
    void selectsModernStrategyAboveCutoffVersion() {
        ItemModelStrategy strategy = factory.create(new ServerVersion(1, 21, 11), modelJsonGenerator,
                textureFileCopier);
        assertInstanceOf(ModernModelStrategy.class, strategy);
    }

    @Test
    void selectsLegacyStrategyBelowCutoffVersion() {
        ItemModelStrategy strategy = factory.create(new ServerVersion(1, 20, 6), modelJsonGenerator,
                textureFileCopier);
        assertInstanceOf(LegacyModelStrategy.class, strategy);
    }

    @Test
    void selectsLegacyStrategyJustBelowCutoffPatch() {
        ItemModelStrategy strategy = factory.create(new ServerVersion(1, 21, 3), modelJsonGenerator,
                textureFileCopier);
        assertInstanceOf(LegacyModelStrategy.class, strategy);
    }
}
