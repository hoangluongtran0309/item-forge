package com.hoangluongtran0309.itemforge.dashboard.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.springframework.stereotype.Service;

import com.hoangluongtran0309.itemforge.dashboard.config.ReferenceLibraryProperties;

/**
 * Extracts a resource pack imported by an admin into the reference texture library.
 *
 * <p>No Mojang asset is bundled -- see {@link ReferenceLibraryProperties}.
 */
@Service
public class ReferenceLibraryService {

    /** Accepts only PNG textures inside a resource pack's assets tree. */
    private static final Pattern TEXTURE_ENTRY =
            Pattern.compile("^assets/[^/]+/textures/.+\\.png$", Pattern.CASE_INSENSITIVE);

    private static final Pattern SAFE_PACK_NAME = Pattern.compile("[A-Za-z0-9_-]+");
    private static final byte[] PNG_MAGIC = { (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A };

    private final ReferenceLibraryProperties properties;

    public ReferenceLibraryService(ReferenceLibraryProperties properties) {
        this.properties = properties;
    }

    public record ReferenceTexture(String pack, String path, String name) {
    }

    public record ImportResult(String pack, int imported, int skipped) {
    }

    /**
     * Extracts one resource pack. Returns how many entries were written and how many were skipped.
     *
     * @throws IllegalArgumentException when the zip breaches a limit or the pack name is invalid
     */
    public ImportResult importPack(String rawPackName, InputStream zipStream) throws IOException {
        String pack = sanitisePackName(rawPackName);
        Path root = properties.getDir().toAbsolutePath().normalize();
        Path packDir = root.resolve(pack).normalize();
        requireInside(root, packDir);

        Files.createDirectories(packDir);

        long totalBytes = 0;
        int imported = 0;
        int skipped = 0;

        try (ZipInputStream zip = new ZipInputStream(zipStream)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.isDirectory() || !TEXTURE_ENTRY.matcher(entry.getName()).matches()) {
                    skipped += 1;
                    continue;
                }
                if (imported >= properties.getMaxEntries()) {
                    throw new IllegalArgumentException(
                            "Pack contains more than " + properties.getMaxEntries() + " textures");
                }

                // Blocks zip-slip: the entry name is chosen by the zip file, so it has to be
                // normalized and the resulting path re-checked to be STILL inside packDir.
                // Looking for the substring "../" is not enough (".\\", "..%2f", ...).
                Path target = packDir.resolve(entry.getName()).normalize();
                requireInside(packDir, target);

                byte[] bytes = readLimited(zip, properties.getMaxEntryBytes(), entry.getName());
                totalBytes += bytes.length;
                if (totalBytes > properties.getMaxTotalBytes()) {
                    throw new IllegalArgumentException("Pack expands to more than "
                            + properties.getMaxTotalBytes() + " bytes");
                }
                if (!hasPngMagic(bytes)) {
                    skipped += 1;
                    continue;
                }

                Files.createDirectories(target.getParent());
                Files.write(target, bytes);
                imported += 1;
            }
        }

        if (imported == 0) {
            deletePack(pack);
            throw new IllegalArgumentException(
                    "No textures found - expected PNG files under assets/<namespace>/textures/");
        }
        return new ImportResult(pack, imported, skipped);
    }

    public List<String> packs() {
        Path root = properties.getDir();
        if (!Files.isDirectory(root)) {
            return List.of();
        }
        try (Stream<Path> children = Files.list(root)) {
            return children.filter(Files::isDirectory)
                    .map(path -> path.getFileName().toString())
                    .sorted()
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Finds textures by case-insensitive substring match on the path. */
    public List<ReferenceTexture> search(String query, int limit) {
        Path root = properties.getDir();
        if (!Files.isDirectory(root)) {
            return List.of();
        }
        String needle = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);

        List<ReferenceTexture> results = new ArrayList<>();
        for (String pack : packs()) {
            Path packDir = root.resolve(pack);
            try (Stream<Path> files = Files.walk(packDir)) {
                files.filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".png"))
                        .forEach(path -> {
                            String relative = packDir.relativize(path).toString().replace('\\', '/');
                            if (needle.isEmpty() || relative.toLowerCase(Locale.ROOT).contains(needle)) {
                                results.add(new ReferenceTexture(pack, relative, path.getFileName().toString()));
                            }
                        });
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        results.sort(Comparator.comparing(ReferenceTexture::path));
        return results.size() > limit ? results.subList(0, limit) : results;
    }

    public Optional<byte[]> read(String pack, String relativePath) {
        Path root = properties.getDir().toAbsolutePath().normalize();
        Path packDir = root.resolve(sanitisePackName(pack)).normalize();
        Path target = packDir.resolve(relativePath).normalize();
        // Same reason as during import: the path comes from the URL, so it has to be
        // re-checked to be inside the pack.
        requireInside(packDir, target);

        if (!Files.isRegularFile(target)) {
            return Optional.empty();
        }
        try {
            return Optional.of(Files.readAllBytes(target));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void deletePack(String rawPackName) {
        Path root = properties.getDir().toAbsolutePath().normalize();
        Path packDir = root.resolve(sanitisePackName(rawPackName)).normalize();
        requireInside(root, packDir);
        if (!Files.isDirectory(packDir)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(packDir)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.delete(path);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static String sanitisePackName(String raw) {
        String base = raw == null ? "" : raw.replaceAll("\\.(zip|mcpack)$", "");

        // Built one character at a time rather than through a chain of replaceAll calls:
        // trimming the separators with "^-+|-+$" backtracks quadratically on a name that
        // is nothing but dashes, and the name comes from an uploaded file.
        StringBuilder cleaned = new StringBuilder(base.length());
        for (int i = 0; i < base.length(); i += 1) {
            char c = base.charAt(i);
            boolean kept = (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z')
                    || (c >= '0' && c <= '9') || c == '_';
            if (kept) {
                cleaned.append(c);
            } else if (!cleaned.isEmpty() && cleaned.charAt(cleaned.length() - 1) != '-') {
                // Everything else collapses into a single separator; a leading run of them
                // never gets appended in the first place.
                cleaned.append('-');
            }
        }
        while (!cleaned.isEmpty() && cleaned.charAt(cleaned.length() - 1) == '-') {
            cleaned.deleteCharAt(cleaned.length() - 1);
        }

        String result = cleaned.toString();
        if (result.isEmpty() || !SAFE_PACK_NAME.matcher(result).matches()) {
            throw new IllegalArgumentException("Invalid pack name: " + raw);
        }
        return result;
    }

    private static void requireInside(Path parent, Path candidate) {
        if (!candidate.startsWith(parent)) {
            throw new IllegalArgumentException("Refusing to touch a path outside the reference library: " + candidate);
        }
    }

    private static byte[] readLimited(InputStream in, long limit, String entryName) throws IOException {
        // Not readAllBytes(): an entry that declares itself small but expands enormously
        // would eat all the memory before there was any chance to check it.
        byte[] buffer = new byte[8192];
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
            if (out.size() > limit) {
                throw new IllegalArgumentException(
                        "Entry '" + entryName + "' is larger than " + limit + " bytes");
            }
        }
        return out.toByteArray();
    }

    private static boolean hasPngMagic(byte[] bytes) {
        if (bytes.length < PNG_MAGIC.length) {
            return false;
        }
        for (int i = 0; i < PNG_MAGIC.length; i += 1) {
            if (bytes[i] != PNG_MAGIC[i]) {
                return false;
            }
        }
        return true;
    }
}
