package com.hoangluongtran0309.itemforge.dashboard.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.hoangluongtran0309.itemforge.dashboard.config.ReferenceLibraryProperties;

class ReferenceLibraryServiceTest {

    private static final byte[] PNG_HEADER = { (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A };

    @TempDir
    Path libraryDir;

    private ReferenceLibraryService service;

    @BeforeEach
    void setUp() {
        ReferenceLibraryProperties properties = new ReferenceLibraryProperties();
        properties.setDir(libraryDir);
        service = new ReferenceLibraryService(properties);
    }

    @Test
    void importsOnlyPngTexturesUnderTheAssetsTree() throws IOException {
        byte[] zip = zip(
                entry("assets/minecraft/textures/item/netherite_sword.png", png(16)),
                entry("assets/minecraft/textures/block/stone.png", png(8)),
                // Skipped: not under textures/
                entry("assets/minecraft/lang/en_us.json", "{}".getBytes()),
                // Skipped: right path, but not a real PNG
                entry("assets/minecraft/textures/item/fake.png", "not a png".getBytes()),
                entry("pack.mcmeta", "{}".getBytes()));

        var result = service.importPack("MyPack.zip", new ByteArrayInputStream(zip));

        assertEquals("MyPack", result.pack());
        assertEquals(2, result.imported());
        assertEquals(List.of("MyPack"), service.packs());
        assertTrue(Files.exists(libraryDir.resolve("MyPack/assets/minecraft/textures/item/netherite_sword.png")));
        assertFalse(Files.exists(libraryDir.resolve("MyPack/assets/minecraft/lang/en_us.json")));
        assertFalse(Files.exists(libraryDir.resolve("MyPack/assets/minecraft/textures/item/fake.png")));
    }

    /**
     * Zip-slip: the entry name is entirely chosen by the zip file, so an entry containing
     * "../" must never be able to write outside the pack's directory.
     */
    @Test
    void zipSlipEntriesCannotEscapeThePackDirectory() throws IOException {
        Path outside = libraryDir.getParent().resolve("pwned.png");
        byte[] zip = zip(entry("assets/minecraft/textures/../../../../pwned.png", png(16)));

        assertThrows(IllegalArgumentException.class,
                () -> service.importPack("evil", new ByteArrayInputStream(zip)));
        assertFalse(Files.exists(outside), "must not write a file outside the library directory");
    }

    @Test
    void oversizedEntriesAreRejected() throws IOException {
        ReferenceLibraryProperties properties = new ReferenceLibraryProperties();
        properties.setDir(libraryDir);
        properties.setMaxEntryBytes(64);
        ReferenceLibraryService tightService = new ReferenceLibraryService(properties);

        byte[] zip = zip(entry("assets/minecraft/textures/item/big.png", png(4096)));

        assertThrows(IllegalArgumentException.class,
                () -> tightService.importPack("big", new ByteArrayInputStream(zip)));
    }

    @Test
    void aPackWithNoTexturesIsRejectedAndLeavesNothingBehind() throws IOException {
        byte[] zip = zip(entry("pack.mcmeta", "{}".getBytes()));

        assertThrows(IllegalArgumentException.class,
                () -> service.importPack("empty", new ByteArrayInputStream(zip)));
        assertEquals(List.of(), service.packs());
    }

    @Test
    void searchFiltersByPathAndReadReturnsTheBytes() throws IOException {
        byte[] expected = png(16);
        byte[] zip = zip(
                entry("assets/minecraft/textures/item/netherite_sword.png", expected),
                entry("assets/minecraft/textures/block/stone.png", png(8)));
        service.importPack("pack", new ByteArrayInputStream(zip));

        assertEquals(2, service.search("", 100).size());
        List<ReferenceLibraryService.ReferenceTexture> swords = service.search("netherite", 100);
        assertEquals(1, swords.size());
        assertEquals("netherite_sword.png", swords.get(0).name());

        assertArrayEquals(expected, service.read("pack", swords.get(0).path()).orElseThrow());
        assertTrue(service.read("pack", "assets/minecraft/textures/item/missing.png").isEmpty());
    }

    @Test
    void readRefusesToEscapeThePackDirectory() throws IOException {
        byte[] zip = zip(entry("assets/minecraft/textures/item/a.png", png(16)));
        service.importPack("pack", new ByteArrayInputStream(zip));
        Files.writeString(libraryDir.resolve("secret.txt"), "nope");

        assertThrows(IllegalArgumentException.class, () -> service.read("pack", "../secret.txt"));
    }

    @Test
    void deleteRemovesTheWholePack() throws IOException {
        byte[] zip = zip(entry("assets/minecraft/textures/item/a.png", png(16)));
        service.importPack("pack", new ByteArrayInputStream(zip));

        service.deletePack("pack");

        assertEquals(List.of(), service.packs());
        assertFalse(Files.exists(libraryDir.resolve("pack")));
    }

    @Test
    void packNamesAreSanitisedAndNonsenseIsRejected() {
        assertEquals("My-Pack", ReferenceLibraryService.sanitisePackName("My Pack.zip"));
        assertEquals("faithful32", ReferenceLibraryService.sanitisePackName("faithful32.mcpack"));
        assertEquals("evil", ReferenceLibraryService.sanitisePackName("../evil"));
        assertThrows(IllegalArgumentException.class, () -> ReferenceLibraryService.sanitisePackName("../.."));
        assertThrows(IllegalArgumentException.class, () -> ReferenceLibraryService.sanitisePackName(""));
    }

    private static byte[] png(int payloadBytes) {
        byte[] bytes = new byte[PNG_HEADER.length + payloadBytes];
        System.arraycopy(PNG_HEADER, 0, bytes, 0, PNG_HEADER.length);
        return bytes;
    }

    private record Entry(String name, byte[] content) {
    }

    private static Entry entry(String name, byte[] content) {
        return new Entry(name, content);
    }

    private static byte[] zip(Entry... entries) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            for (Entry entry : entries) {
                zip.putNextEntry(new ZipEntry(entry.name()));
                zip.write(entry.content());
                zip.closeEntry();
            }
        }
        return out.toByteArray();
    }
}
