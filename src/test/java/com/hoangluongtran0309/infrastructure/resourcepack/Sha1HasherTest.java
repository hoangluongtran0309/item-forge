package com.hoangluongtran0309.infrastructure.resourcepack;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class Sha1HasherTest {

    private final Sha1Hasher hasher = new Sha1Hasher();

    @Test
    void hashHexMatchesKnownSha1(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("content.txt");
        Files.writeString(file, "hello");

        String hash = hasher.hashHex(file);

        assertEquals("aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d", hash);
    }

    @Test
    void differentContentProducesDifferentHash(@TempDir Path dir) throws IOException {
        Path fileA = dir.resolve("a.txt");
        Path fileB = dir.resolve("b.txt");
        Files.writeString(fileA, "hello");
        Files.writeString(fileB, "world");

        assertEquals(false, hasher.hashHex(fileA).equals(hasher.hashHex(fileB)));
    }
}
