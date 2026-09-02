package com.hoangluongtran0309.infrastructure.bukkit.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import com.sun.net.httpserver.HttpExchange;

// A minimal hand-written multipart/form-data parser (no external library -- the project
// deliberately keeps its dependencies few). It supports only the case we need: a request
// uploading exactly one file. It finds the first multipart section with "filename=" in its
// Content-Disposition and returns its raw bytes.
final class MultipartParser {

    private MultipartParser() {
    }

    static byte[] extractSingleFilePart(HttpExchange exchange, long maxBytes) throws IOException {
        String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
        String boundary = extractBoundary(contentType);

        byte[] body = readAllBytesWithLimit(exchange.getRequestBody(), maxBytes);
        return findFilePartBytes(body, boundary);
    }

    private static String extractBoundary(String contentType) {
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("multipart/form-data")) {
            throw new ApiException(400, "Request must be multipart/form-data");
        }

        for (String part : contentType.split(";")) {
            String trimmed = part.trim();
            if (trimmed.toLowerCase(Locale.ROOT).startsWith("boundary=")) {
                String value = trimmed.substring("boundary=".length()).trim();
                if (value.startsWith("\"") && value.endsWith("\"") && value.length() >= 2) {
                    value = value.substring(1, value.length() - 1);
                }
                if (value.isEmpty()) {
                    throw new ApiException(400, "Missing multipart boundary");
                }
                return value;
            }
        }
        throw new ApiException(400, "Missing multipart boundary");
    }

    private static byte[] readAllBytesWithLimit(InputStream in, long maxBytes) throws IOException {
        byte[] buffer = new byte[8192];
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int read;
        long total = 0;
        while ((read = in.read(buffer)) != -1) {
            total += read;
            if (total > maxBytes) {
                throw new ApiException(413, "Uploaded file exceeds maximum size of " + maxBytes + " bytes");
            }
            out.write(buffer, 0, read);
        }
        return out.toByteArray();
    }

    private static byte[] findFilePartBytes(byte[] body, String boundary) {
        byte[] marker = ("--" + boundary).getBytes(StandardCharsets.ISO_8859_1);
        byte[] headerSeparator = "\r\n\r\n".getBytes(StandardCharsets.ISO_8859_1);

        int searchFrom = 0;
        while (true) {
            int markerIndex = indexOf(body, marker, searchFrom);
            if (markerIndex < 0) {
                throw new ApiException(400, "Request must contain exactly one file upload part");
            }

            int partStart = markerIndex + marker.length;
            // "--boundary--" danh dau ket thuc toan bo multipart body
            if (startsWith(body, partStart, "--".getBytes(StandardCharsets.ISO_8859_1))) {
                throw new ApiException(400, "Request must contain exactly one file upload part");
            }

            int headerEnd = indexOf(body, headerSeparator, partStart);
            if (headerEnd < 0) {
                throw new ApiException(400, "Malformed multipart body");
            }

            String headers = new String(body, partStart, headerEnd - partStart, StandardCharsets.ISO_8859_1);
            int contentStart = headerEnd + headerSeparator.length;
            int nextMarkerIndex = indexOf(body, marker, contentStart);
            int contentEnd = nextMarkerIndex < 0 ? body.length : nextMarkerIndex;
            // Strip the "\r\n" immediately before the next boundary.
            if (contentEnd >= 2 && body[contentEnd - 1] == '\n' && body[contentEnd - 2] == '\r') {
                contentEnd -= 2;
            }

            if (headers.toLowerCase(Locale.ROOT).contains("filename=")) {
                byte[] content = new byte[contentEnd - contentStart];
                System.arraycopy(body, contentStart, content, 0, content.length);
                return content;
            }

            if (nextMarkerIndex < 0) {
                throw new ApiException(400, "Request must contain exactly one file upload part");
            }
            searchFrom = nextMarkerIndex;
        }
    }

    private static boolean startsWith(byte[] haystack, int offset, byte[] prefix) {
        if (offset + prefix.length > haystack.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (haystack[offset + i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }

    private static int indexOf(byte[] haystack, byte[] needle, int from) {
        outer: for (int i = Math.max(from, 0); i <= haystack.length - needle.length; i++) {
            for (int j = 0; j < needle.length; j++) {
                if (haystack[i + j] != needle[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }
}
