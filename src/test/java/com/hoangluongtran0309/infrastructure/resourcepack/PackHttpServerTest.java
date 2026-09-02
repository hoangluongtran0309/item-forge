package com.hoangluongtran0309.infrastructure.resourcepack;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PackHttpServerTest {

    // A high port range, unlikely to be in use, with one port per test so two tests never
    // overlap on the same one.
    private PackHttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop();
        }
    }

    @Test
    void returns404WhenZipFileDoesNotExist(@TempDir Path dir) throws IOException, InterruptedException {
        Path missingZip = dir.resolve("resource-pack.zip");
        server = new PackHttpServer(18471, () -> missingZip, Logger.getAnonymousLogger());
        server.start();

        HttpResponse<byte[]> response = get(18471);

        assertEquals(404, response.statusCode());
    }

    @Test
    void returns200WithFileBytesWhenZipExists(@TempDir Path dir) throws IOException, InterruptedException {
        Path zipFile = dir.resolve("resource-pack.zip");
        byte[] content = "fake-zip-bytes".getBytes();
        Files.write(zipFile, content);

        server = new PackHttpServer(18472, () -> zipFile, Logger.getAnonymousLogger());
        server.start();

        HttpResponse<byte[]> response = get(18472);

        assertEquals(200, response.statusCode());
        assertArrayEquals(content, response.body());
    }

    @Test
    void readsFileFreshOnEveryRequest(@TempDir Path dir) throws IOException, InterruptedException {
        Path zipFile = dir.resolve("resource-pack.zip");
        Files.write(zipFile, "first-version".getBytes());

        server = new PackHttpServer(18473, () -> zipFile, Logger.getAnonymousLogger());
        server.start();

        HttpResponse<byte[]> first = get(18473);
        Files.write(zipFile, "second-version".getBytes());
        HttpResponse<byte[]> second = get(18473);

        assertArrayEquals("first-version".getBytes(), first.body());
        assertArrayEquals("second-version".getBytes(), second.body());
    }

    private HttpResponse<byte[]> get(int port) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + PackHttpServer.CONTEXT_PATH))
                .GET()
                .build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofByteArray());
    }
}
