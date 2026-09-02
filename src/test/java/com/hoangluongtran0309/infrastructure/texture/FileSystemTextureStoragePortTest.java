package com.hoangluongtran0309.infrastructure.texture;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.logging.Logger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileSystemTextureStoragePortTest {

    @TempDir
    Path dataFolder;

    private FileSystemTextureStoragePort port;

    @BeforeEach
    void setUp() {
        port = new FileSystemTextureStoragePort(dataFolder, Logger.getAnonymousLogger());
    }

    @Test
    void writeItemTextureLandsAtExpectedPath() throws IOException {
        byte[] bytes = { 1, 2, 3 };
        assertFalse(port.itemTextureExists("void_sword"));

        port.writeItemTexture("void_sword", bytes);

        assertTrue(port.itemTextureExists("void_sword"));
        Path expected = dataFolder.resolve("textures").resolve("void_sword.png");
        assertTrue(Files.exists(expected));
        assertArrayEquals(bytes, Files.readAllBytes(expected));
    }

    @Test
    void writeArmorIconLandsUnderTexturesArmorKeyedByOwnId() {
        port.writeArmorIcon("void_helmet", new byte[] { 9 });

        assertTrue(Files.exists(dataFolder.resolve("textures").resolve("armor").resolve("void_helmet.png")));
        assertTrue(port.armorIconExists("void_helmet"));
        assertFalse(port.armorIconExists("void_boots"));
    }

    @Test
    void writeArmorLayer1And2UseSuffixConventionKeyedByAssetId() {
        port.writeArmorLayer1("void_armor", new byte[] { 1 });
        port.writeArmorLayer2("void_armor", new byte[] { 2 });

        Path armorDir = dataFolder.resolve("textures").resolve("armor");
        assertTrue(Files.exists(armorDir.resolve("void_armor_layer_1.png")));
        assertTrue(Files.exists(armorDir.resolve("void_armor_layer_2.png")));
        assertTrue(port.armorLayer1Exists("void_armor"));
        assertTrue(port.armorLayer2Exists("void_armor"));
    }

    @Test
    void writeBlockTextureLandsUnderTexturesBlocksKeyedByTextureId() {
        port.writeBlockTexture("void_netherite_block", new byte[] { 7 });

        assertTrue(Files.exists(dataFolder.resolve("textures").resolve("blocks").resolve("void_netherite_block.png")));
        assertTrue(port.blockTextureExists("void_netherite_block"));
        assertFalse(port.blockTextureExists("other_block"));
    }

    @Test
    void readBlockTextureReturnsBytesWrittenEarlierAndEmptyWhenMissing() {
        assertTrue(port.readBlockTexture("void_netherite_block").isEmpty());

        byte[] bytes = { 7, 7, 7 };
        port.writeBlockTexture("void_netherite_block", bytes);

        assertArrayEquals(bytes, port.readBlockTexture("void_netherite_block").orElseThrow());
        assertTrue(port.readBlockTexture("other_block").isEmpty());
    }

    // textureId travels straight from the JSON body of POST /api/blocks to here, and
    // CustomBlockDefinition only checks for non-blank, so this is the only barrier between
    // a malicious id and a file written outside the data folder.
    @Test
    void unsafeIdsAreRejectedBeforeTouchingTheFileSystem() {
        for (String unsafe : new String[] { "../evil", "..", ".", "a/b", "a\\b", "", "with space", "x/../../y" }) {
            assertThrows(IllegalArgumentException.class, () -> port.writeBlockTexture(unsafe, new byte[] { 1 }),
                    "expected block textureId '" + unsafe + "' to be rejected");
            assertThrows(IllegalArgumentException.class, () -> port.readBlockTexture(unsafe));
            assertThrows(IllegalArgumentException.class, () -> port.blockTextureExists(unsafe));
            assertThrows(IllegalArgumentException.class, () -> port.writeItemTexture(unsafe, new byte[] { 1 }));
            assertThrows(IllegalArgumentException.class, () -> port.writeArmorLayer1(unsafe, new byte[] { 1 }));
        }

        assertThrows(IllegalArgumentException.class, () -> port.writeItemTexture(null, new byte[] { 1 }));
    }

    @Test
    void ordinaryIdsWithDotsDashesAndDigitsAreStillAccepted() {
        port.writeItemTexture("void_sword", new byte[] { 1 });
        port.writeItemTexture("Fire-Sword2", new byte[] { 1 });
        port.writeItemTexture("pack.v2.sword", new byte[] { 1 });

        assertTrue(port.itemTextureExists("void_sword"));
        assertTrue(port.itemTextureExists("Fire-Sword2"));
        assertTrue(port.itemTextureExists("pack.v2.sword"));
    }

    @Test
    void reuploadOverwritesExistingFile() throws IOException {
        port.writeItemTexture("void_sword", new byte[] { 1 });
        port.writeItemTexture("void_sword", new byte[] { 2, 2 });

        Path expected = dataFolder.resolve("textures").resolve("void_sword.png");
        assertArrayEquals(new byte[] { 2, 2 }, Files.readAllBytes(expected));
    }

    @Test
    void readItemTextureReturnsEmptyWhenMissingAndBytesAfterWrite() {
        assertTrue(port.readItemTexture("void_sword").isEmpty());

        port.writeItemTexture("void_sword", new byte[] { 1, 2, 3 });

        Optional<byte[]> read = port.readItemTexture("void_sword");
        assertTrue(read.isPresent());
        assertArrayEquals(new byte[] { 1, 2, 3 }, read.get());
    }

    @Test
    void readArmorIconReturnsEmptyWhenMissingAndBytesAfterWrite() {
        assertTrue(port.readArmorIcon("void_helmet").isEmpty());

        port.writeArmorIcon("void_helmet", new byte[] { 9 });

        assertArrayEquals(new byte[] { 9 }, port.readArmorIcon("void_helmet").get());
    }

    @Test
    void readArmorLayer1And2ReturnEmptyWhenMissingAndBytesAfterWrite() {
        assertTrue(port.readArmorLayer1("void_armor").isEmpty());
        assertTrue(port.readArmorLayer2("void_armor").isEmpty());

        port.writeArmorLayer1("void_armor", new byte[] { 1 });
        port.writeArmorLayer2("void_armor", new byte[] { 2 });

        assertArrayEquals(new byte[] { 1 }, port.readArmorLayer1("void_armor").get());
        assertArrayEquals(new byte[] { 2 }, port.readArmorLayer2("void_armor").get());
    }
}
