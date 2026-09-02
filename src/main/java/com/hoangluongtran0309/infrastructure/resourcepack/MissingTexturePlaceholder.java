package com.hoangluongtran0309.infrastructure.resourcepack;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.imageio.ImageIO;

// For the legacy cases (pre-1.21.4) that cannot "point at a reference that does not
// exist" the way item_model/equippable model can. A legacy armor texture is a fixed
// vanilla PNG path: overwrite nothing and the client silently falls back to the
// original vanilla texture, so it has to be actively overwritten with the same
// magenta-and-black pattern as Minecraft's own MissingTextureAtlasSprite.
public final class MissingTexturePlaceholder {

    private static final int TILE_SIZE = 8;
    private static final int BLACK = 0xFF000000;
    private static final int MAGENTA = 0xFFF800F8;

    private MissingTexturePlaceholder() {
    }

    public static void writeTo(Path target, int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                boolean evenTileX = (x / TILE_SIZE) % 2 == 0;
                boolean evenTileY = (y / TILE_SIZE) % 2 == 0;
                image.setRGB(x, y, (evenTileX == evenTileY) ? BLACK : MAGENTA);
            }
        }

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        ImageIO.write(image, "png", buffer);
        Files.write(target, buffer.toByteArray());
    }
}
