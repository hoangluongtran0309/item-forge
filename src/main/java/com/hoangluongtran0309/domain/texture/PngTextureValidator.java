package com.hoangluongtran0309.domain.texture;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

import javax.imageio.ImageIO;

import com.hoangluongtran0309.domain.exception.InvalidTextureException;

// Validates a PNG byte[] in pure Java (javax.imageio only, no Bukkit) so it can be unit
// tested directly without MockBukkit. The magic bytes are checked first because
// ImageIO.read() "successfully" decodes a JPEG renamed to .png -- which happens in practice,
// and Minecraft then rejects it silently with a missing texture and no obvious cause.
public final class PngTextureValidator {

    private static final byte[] PNG_SIGNATURE = { (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A };
    private static final int MULTIPLE_OF = 16;
    private static final int ARMOR_LAYER_WIDTH = 64;
    private static final int ARMOR_LAYER_HEIGHT = 32;

    private PngTextureValidator() {
    }

    /** Item texture / armor icon: square, with sides a multiple of 16. */
    public static void validateIcon(byte[] pngBytes) {
        BufferedImage image = decode(pngBytes);
        int width = image.getWidth();
        int height = image.getHeight();

        if (width != height) {
            throw new InvalidTextureException(
                    "Texture must be square (width must equal height), got " + width + "x" + height);
        }
        if (width % MULTIPLE_OF != 0) {
            throw new InvalidTextureException(
                    "Texture dimensions must be a multiple of " + MULTIPLE_OF + ", got " + width + "x" + height);
        }
    }

    /** Armor layers worn on the body (humanoid / humanoid_leggings): exactly 64x32. */
    public static void validateArmorLayer(byte[] pngBytes) {
        BufferedImage image = decode(pngBytes);
        int width = image.getWidth();
        int height = image.getHeight();

        if (width != ARMOR_LAYER_WIDTH || height != ARMOR_LAYER_HEIGHT) {
            throw new InvalidTextureException("Armor layer texture must be exactly " + ARMOR_LAYER_WIDTH + "x"
                    + ARMOR_LAYER_HEIGHT + ", got " + width + "x" + height);
        }
    }

    private static BufferedImage decode(byte[] bytes) {
        requirePngSignature(bytes);

        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
            if (image == null) {
                throw new InvalidTextureException("File is not a readable PNG image");
            }
            return image;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void requirePngSignature(byte[] bytes) {
        if (bytes == null || bytes.length < PNG_SIGNATURE.length) {
            throw new InvalidTextureException("File is not a valid PNG image (file too small)");
        }
        for (int i = 0; i < PNG_SIGNATURE.length; i++) {
            if (bytes[i] != PNG_SIGNATURE[i]) {
                throw new InvalidTextureException("File is not a valid PNG image (invalid PNG signature)");
            }
        }
    }
}
