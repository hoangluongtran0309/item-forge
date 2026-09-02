package com.hoangluongtran0309.infrastructure.resourcepack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PackZipperTest {

    private final PackZipper zipper = new PackZipper();

    @Test
    void zipContainsAllFilesWithRelativePaths(@TempDir Path sourceDir, @TempDir Path outputDir) throws IOException {
        Files.writeString(sourceDir.resolve("pack.mcmeta"), "{}");
        Path nested = sourceDir.resolve("assets/itemforge/models/custom");
        Files.createDirectories(nested);
        Files.writeString(nested.resolve("void_sword.json"), "{}");

        Path zipFile = zipper.zip(sourceDir, outputDir.resolve("resource-pack.zip"));

        Set<String> entries = new HashSet<>();
        try (ZipFile zf = new ZipFile(zipFile.toFile())) {
            zf.stream().map(ZipEntry::getName).forEach(entries::add);
        }

        assertEquals(Set.of("pack.mcmeta", "assets/itemforge/models/custom/void_sword.json"), entries);
    }

    @Test
    void rebuildingOverwritesPreviousZipContent(@TempDir Path sourceDir, @TempDir Path outputDir) throws IOException {
        Path zipFile = outputDir.resolve("resource-pack.zip");

        Files.writeString(sourceDir.resolve("a.json"), "{}");
        zipper.zip(sourceDir, zipFile);

        Files.delete(sourceDir.resolve("a.json"));
        Files.writeString(sourceDir.resolve("b.json"), "{}");
        zipper.zip(sourceDir, zipFile);

        Set<String> entries = new HashSet<>();
        try (ZipFile zf = new ZipFile(zipFile.toFile())) {
            zf.stream().map(ZipEntry::getName).forEach(entries::add);
        }

        assertEquals(Set.of("b.json"), entries);
    }

    @Test
    void noLeftoverTempFileAfterZipping(@TempDir Path sourceDir, @TempDir Path outputDir) throws IOException {
        Files.writeString(sourceDir.resolve("a.json"), "{}");
        Path zipFile = outputDir.resolve("resource-pack.zip");

        zipper.zip(sourceDir, zipFile);

        assertTrue(Files.notExists(outputDir.resolve("resource-pack.zip.tmp")));
    }
}
