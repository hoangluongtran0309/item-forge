package com.hoangluongtran0309.domain.texture;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

import com.hoangluongtran0309.domain.exception.InvalidTextureException;

class PngTextureValidatorTest {

    @Test
    void validateIconAcceptsSquareMultipleOf16() {
        byte[] png = png(32, 32);
        assertDoesNotThrow(() -> PngTextureValidator.validateIcon(png));
    }

    @Test
    void validateIconRejectsNonSquare() {
        byte[] png = png(32, 48);
        assertThrows(InvalidTextureException.class, () -> PngTextureValidator.validateIcon(png));
    }

    @Test
    void validateIconRejectsNonMultipleOf16() {
        byte[] png = png(20, 20);
        assertThrows(InvalidTextureException.class, () -> PngTextureValidator.validateIcon(png));
    }

    @Test
    void validateArmorLayerAcceptsExactly64x32() {
        byte[] png = png(64, 32);
        assertDoesNotThrow(() -> PngTextureValidator.validateArmorLayer(png));
    }

    @Test
    void validateArmorLayerRejectsWrongDimensions() {
        byte[] png = png(32, 32);
        assertThrows(InvalidTextureException.class, () -> PngTextureValidator.validateArmorLayer(png));
    }

    @Test
    void validateArmorLayerRejects64x64() {
        byte[] png = png(64, 64);
        assertThrows(InvalidTextureException.class, () -> PngTextureValidator.validateArmorLayer(png));
    }

    @Test
    void validateIconRejectsRenamedJpeg() {
        byte[] jpeg = jpeg(32, 32);
        assertThrows(InvalidTextureException.class, () -> PngTextureValidator.validateIcon(jpeg));
    }

    @Test
    void validateArmorLayerRejectsRenamedJpeg() {
        byte[] jpeg = jpeg(64, 32);
        assertThrows(InvalidTextureException.class, () -> PngTextureValidator.validateArmorLayer(jpeg));
    }

    @Test
    void validateIconRejectsCorruptedMagicBytes() {
        byte[] png = png(32, 32);
        png[0] = 0x00;
        assertThrows(InvalidTextureException.class, () -> PngTextureValidator.validateIcon(png));
    }

    @Test
    void validateIconRejectsEmptyBytes() {
        assertThrows(InvalidTextureException.class, () -> PngTextureValidator.validateIcon(new byte[0]));
    }

    private static byte[] png(int width, int height) {
        return encode(width, height, "png");
    }

    private static byte[] jpeg(int width, int height) {
        return encode(width, height, "jpg");
    }

    private static byte[] encode(int width, int height, String format) {
        try {
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            ImageIO.write(image, format, buffer);
            return buffer.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
