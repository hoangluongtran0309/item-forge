package com.hoangluongtran0309.infrastructure.resourcepack;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.hoangluongtran0309.domain.model.ItemDefinition;

class TextureFileCopierTest {

    private final TextureFileCopier copier = new TextureFileCopier(Logger.getAnonymousLogger());
    private final ItemDefinition item = new ItemDefinition("void_sword", "NETHERITE_SWORD", 1001, "name", List.of(),
            List.of());

    @Test
    void missingTextureReturnsFalse(@TempDir Path sourceDir, @TempDir Path outputDir) throws IOException {
        boolean copied = copier.copyIfExists(item, sourceDir, "itemforge", outputDir);

        assertFalse(copied);
        assertTrue(Files.notExists(outputDir.resolve("assets/itemforge/textures/item/void_sword.png")));
    }

    @Test
    void presentTextureIsCopiedToExpectedPath(@TempDir Path sourceDir, @TempDir Path outputDir) throws IOException {
        Files.writeString(sourceDir.resolve("void_sword.png"), "fake-png-bytes");

        boolean copied = copier.copyIfExists(item, sourceDir, "itemforge", outputDir);

        assertTrue(copied);
        assertTrue(Files.exists(outputDir.resolve("assets/itemforge/textures/item/void_sword.png")));
    }
}
