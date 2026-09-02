package com.hoangluongtran0309.infrastructure.resourcepack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MissingTexturePlaceholderTest {

    @Test
    void writesCheckerboardOfExpectedSize(@TempDir Path tempDir) throws IOException {
        Path target = tempDir.resolve("placeholder.png");

        MissingTexturePlaceholder.writeTo(target, 64, 32);

        BufferedImage image = ImageIO.read(target.toFile());
        assertEquals(64, image.getWidth());
        assertEquals(32, image.getHeight());

        int topLeft = image.getRGB(0, 0) & 0xFFFFFF;
        int adjacentTile = image.getRGB(8, 0) & 0xFFFFFF;
        assertNotEquals(topLeft, adjacentTile, "adjacent 8x8 tiles must alternate color like the vanilla missing texture");
    }
}
