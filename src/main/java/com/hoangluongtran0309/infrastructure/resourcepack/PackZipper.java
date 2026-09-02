package com.hoangluongtran0309.infrastructure.resourcepack;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class PackZipper {

    public Path zip(Path sourceDir, Path zipOutputFile) throws IOException {
        Files.createDirectories(zipOutputFile.getParent());

        // Write to a temp file and then move it into place atomically. Otherwise
        // PackHttpServer can read a half-written zip when rebuild() runs while a player is
        // downloading the pack -- the hash no longer matches and the client reports
        // "failed to download".
        Path tmpFile = zipOutputFile.resolveSibling(zipOutputFile.getFileName().toString() + ".tmp");
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(tmpFile))) {
            try (Stream<Path> walk = Files.walk(sourceDir)) {
                walk.filter(Files::isRegularFile).forEach(path -> {
                    // Relative to sourceDir, so pack.mcmeta ends up at the zip root rather than
                    // nested inside an extra parent directory.
                    String entryName = sourceDir.relativize(path).toString().replace(File.separatorChar, '/');
                    try {
                        zos.putNextEntry(new ZipEntry(entryName));
                        Files.copy(path, zos);
                        zos.closeEntry();
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
            }
        }
        Files.move(tmpFile, zipOutputFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        return zipOutputFile;
    }
}
