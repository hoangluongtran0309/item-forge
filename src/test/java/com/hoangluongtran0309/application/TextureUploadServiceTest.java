package com.hoangluongtran0309.application;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

import com.hoangluongtran0309.application.port.ResourcePackInfo;
import com.hoangluongtran0309.application.port.ResourcePackPort;
import com.hoangluongtran0309.application.port.TextureStoragePort;
import com.hoangluongtran0309.domain.exception.InvalidTextureException;

class TextureUploadServiceTest {

    @Test
    void uploadItemTextureStoresAndRebuildsAndReturnsSha1() {
        FakeTextureStorage storage = new FakeTextureStorage();
        FakeResourcePackPort resourcePackPort = new FakeResourcePackPort("abc123");
        TextureUploadService service = new TextureUploadService(storage, resourcePackPort);

        String sha1 = service.uploadItemTexture("void_sword", png(32, 32));

        assertEquals("abc123", sha1);
        assertEquals(1, resourcePackPort.rebuildCount);
        assertTrue(storage.itemTextureExists("void_sword"));
    }

    @Test
    void uploadItemTextureRejectsInvalidPngWithoutStoringOrRebuilding() {
        FakeTextureStorage storage = new FakeTextureStorage();
        FakeResourcePackPort resourcePackPort = new FakeResourcePackPort("abc123");
        TextureUploadService service = new TextureUploadService(storage, resourcePackPort);

        assertThrows(InvalidTextureException.class, () -> service.uploadItemTexture("void_sword", png(20, 20)));

        assertEquals(0, resourcePackPort.rebuildCount);
        assertFalse(storage.itemTextureExists("void_sword"));
    }

    @Test
    void uploadArmorLayer1AndLayer2AreKeyedByAssetIdAndReturnNullWhenPackNotHosted() {
        FakeTextureStorage storage = new FakeTextureStorage();
        FakeResourcePackPort resourcePackPort = new FakeResourcePackPort(null);
        TextureUploadService service = new TextureUploadService(storage, resourcePackPort);

        String sha1 = service.uploadArmorLayer1("void_armor", png(64, 32));
        service.uploadArmorLayer2("void_armor", png(64, 32));

        assertEquals(null, sha1);
        assertTrue(storage.armorLayer1Exists("void_armor"));
        assertTrue(storage.armorLayer2Exists("void_armor"));
        assertEquals(2, resourcePackPort.rebuildCount);
    }

    @Test
    void uploadArmorIconStoresUnderArmorId() {
        FakeTextureStorage storage = new FakeTextureStorage();
        TextureUploadService service = new TextureUploadService(storage, new FakeResourcePackPort("hash"));

        service.uploadArmorIcon("void_helmet", png(16, 16));

        assertTrue(storage.armorIconExists("void_helmet"));
        assertFalse(storage.armorIconExists("void_boots"));
    }

    @Test
    void uploadBlockTextureStoresAndRebuildsAndReturnsSha1() {
        FakeTextureStorage storage = new FakeTextureStorage();
        FakeResourcePackPort resourcePackPort = new FakeResourcePackPort("def456");
        TextureUploadService service = new TextureUploadService(storage, resourcePackPort);

        String sha1 = service.uploadBlockTexture("void_netherite_block", png(16, 16));

        assertEquals("def456", sha1);
        assertEquals(1, resourcePackPort.rebuildCount);
        assertTrue(storage.blockTextureExists("void_netherite_block"));
    }

    @Test
    void uploadBlockTextureRejectsInvalidPngWithoutStoringOrRebuilding() {
        FakeTextureStorage storage = new FakeTextureStorage();
        FakeResourcePackPort resourcePackPort = new FakeResourcePackPort("def456");
        TextureUploadService service = new TextureUploadService(storage, resourcePackPort);

        assertThrows(InvalidTextureException.class,
                () -> service.uploadBlockTexture("void_netherite_block", png(20, 20)));

        assertEquals(0, resourcePackPort.rebuildCount);
        assertFalse(storage.blockTextureExists("void_netherite_block"));
    }

    @Test
    void itemTextureReturnsEmptyWhenMissingAndBytesAfterUpload() {
        FakeTextureStorage storage = new FakeTextureStorage();
        TextureUploadService service = new TextureUploadService(storage, new FakeResourcePackPort("abc123"));

        assertTrue(service.itemTexture("void_sword").isEmpty());

        byte[] png = png(32, 32);
        service.uploadItemTexture("void_sword", png);

        assertEquals(true, service.itemTexture("void_sword").isPresent());
    }

    @Test
    void armorIconAndLayerReadsReturnEmptyWhenMissingAndBytesAfterUpload() {
        FakeTextureStorage storage = new FakeTextureStorage();
        TextureUploadService service = new TextureUploadService(storage, new FakeResourcePackPort("abc123"));

        assertTrue(service.armorIcon("void_helmet").isEmpty());
        assertTrue(service.armorLayer1("void_armor").isEmpty());
        assertTrue(service.armorLayer2("void_armor").isEmpty());

        service.uploadArmorIcon("void_helmet", png(16, 16));
        service.uploadArmorLayer1("void_armor", png(64, 32));
        service.uploadArmorLayer2("void_armor", png(64, 32));

        assertTrue(service.armorIcon("void_helmet").isPresent());
        assertTrue(service.armorLayer1("void_armor").isPresent());
        assertTrue(service.armorLayer2("void_armor").isPresent());
    }

    @Test
    void blockTextureReadsThroughToStorageWithoutRebuilding() {
        FakeTextureStorage storage = new FakeTextureStorage();
        FakeResourcePackPort resourcePackPort = new FakeResourcePackPort("abc123");
        TextureUploadService service = new TextureUploadService(storage, resourcePackPort);

        assertTrue(service.blockTexture("void_netherite_block").isEmpty());

        byte[] png = png(16, 16);
        service.uploadBlockTexture("void_netherite_block", png);
        int rebuildsAfterUpload = resourcePackPort.rebuildCount;

        assertArrayEquals(png, service.blockTexture("void_netherite_block").orElseThrow());
        // The read path must not trigger a resource pack rebuild -- the editor calls it
        // every time "Load existing" is opened, where a rebuild would be expensive and
        // pointless.
        assertEquals(rebuildsAfterUpload, resourcePackPort.rebuildCount);
    }

    private static byte[] png(int width, int height) {
        try {
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            ImageIO.write(image, "png", buffer);
            return buffer.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static final class FakeTextureStorage implements TextureStoragePort {
        private final Map<String, byte[]> itemTextures = new HashMap<>();
        private final Map<String, byte[]> armorIcons = new HashMap<>();
        private final Map<String, byte[]> armorLayer1 = new HashMap<>();
        private final Map<String, byte[]> armorLayer2 = new HashMap<>();
        private final Map<String, byte[]> blockTextures = new HashMap<>();

        @Override
        public void writeItemTexture(String itemId, byte[] pngBytes) {
            itemTextures.put(itemId, pngBytes);
        }

        @Override
        public boolean itemTextureExists(String itemId) {
            return itemTextures.containsKey(itemId);
        }

        @Override
        public Optional<byte[]> readItemTexture(String itemId) {
            return Optional.ofNullable(itemTextures.get(itemId));
        }

        @Override
        public void writeArmorIcon(String armorId, byte[] pngBytes) {
            armorIcons.put(armorId, pngBytes);
        }

        @Override
        public boolean armorIconExists(String armorId) {
            return armorIcons.containsKey(armorId);
        }

        @Override
        public Optional<byte[]> readArmorIcon(String armorId) {
            return Optional.ofNullable(armorIcons.get(armorId));
        }

        @Override
        public void writeArmorLayer1(String armorAssetId, byte[] pngBytes) {
            armorLayer1.put(armorAssetId, pngBytes);
        }

        @Override
        public void writeArmorLayer2(String armorAssetId, byte[] pngBytes) {
            armorLayer2.put(armorAssetId, pngBytes);
        }

        @Override
        public boolean armorLayer1Exists(String armorAssetId) {
            return armorLayer1.containsKey(armorAssetId);
        }

        @Override
        public boolean armorLayer2Exists(String armorAssetId) {
            return armorLayer2.containsKey(armorAssetId);
        }

        @Override
        public Optional<byte[]> readArmorLayer1(String armorAssetId) {
            return Optional.ofNullable(armorLayer1.get(armorAssetId));
        }

        @Override
        public Optional<byte[]> readArmorLayer2(String armorAssetId) {
            return Optional.ofNullable(armorLayer2.get(armorAssetId));
        }

        @Override
        public void writeBlockTexture(String textureId, byte[] pngBytes) {
            blockTextures.put(textureId, pngBytes);
        }

        @Override
        public boolean blockTextureExists(String textureId) {
            return blockTextures.containsKey(textureId);
        }

        @Override
        public Optional<byte[]> readBlockTexture(String textureId) {
            return Optional.ofNullable(blockTextures.get(textureId));
        }
    }

    private static final class FakeResourcePackPort implements ResourcePackPort {
        private final String sha1;
        private int rebuildCount = 0;

        private FakeResourcePackPort(String sha1) {
            this.sha1 = sha1;
        }

        @Override
        public void rebuild() {
            rebuildCount++;
        }

        @Override
        public Optional<ResourcePackInfo> currentPack() {
            return sha1 == null ? Optional.empty() : Optional.of(new ResourcePackInfo("http://host", sha1));
        }
    }
}
