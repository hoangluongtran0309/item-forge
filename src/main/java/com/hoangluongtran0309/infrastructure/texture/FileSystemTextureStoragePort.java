package com.hoangluongtran0309.infrastructure.texture;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import com.hoangluongtran0309.application.port.TextureStoragePort;

// Writes uploaded texture files using exactly the path convention ResourcePackBuilder,
// TextureFileCopier and ArmorTextureFileCopier already read from (textures/<id>.png,
// textures/armor/<id>.png, textures/armor/<armorAssetId>_layer_{1,2}.png). The names and
// directories are deliberately left alone so the already-tested resource pack build layers
// need no changes.
public class FileSystemTextureStoragePort implements TextureStoragePort {

    // Not a plain "[A-Za-z0-9_.-]+": the string ".." matches that. This form requires the
    // first and last characters to be alphanumeric and allows a dot only singly between two
    // segments, so both ".." and "." are rejected.
    private static final Pattern SAFE_SEGMENT = Pattern.compile("[A-Za-z0-9_-]+(?:\\.[A-Za-z0-9_-]+)*");

    private final Path itemTextureDir;
    private final Path armorTextureDir;
    private final Path blockTextureDir;
    private final Logger logger;

    public FileSystemTextureStoragePort(Path dataFolder, Logger logger) {
        this.itemTextureDir = dataFolder.resolve("textures");
        this.armorTextureDir = dataFolder.resolve("textures").resolve("armor");
        this.blockTextureDir = dataFolder.resolve("textures").resolve("blocks");
        this.logger = logger;
    }

    @Override
    public void writeItemTexture(String itemId, byte[] pngBytes) {
        write(resolveSafe(itemTextureDir, itemId, ".png"), pngBytes);
    }

    @Override
    public boolean itemTextureExists(String itemId) {
        return Files.exists(resolveSafe(itemTextureDir, itemId, ".png"));
    }

    @Override
    public Optional<byte[]> readItemTexture(String itemId) {
        return read(resolveSafe(itemTextureDir, itemId, ".png"));
    }

    @Override
    public void writeArmorIcon(String armorId, byte[] pngBytes) {
        write(resolveSafe(armorTextureDir, armorId, ".png"), pngBytes);
    }

    @Override
    public boolean armorIconExists(String armorId) {
        return Files.exists(resolveSafe(armorTextureDir, armorId, ".png"));
    }

    @Override
    public Optional<byte[]> readArmorIcon(String armorId) {
        return read(resolveSafe(armorTextureDir, armorId, ".png"));
    }

    @Override
    public void writeArmorLayer1(String armorAssetId, byte[] pngBytes) {
        write(resolveSafe(armorTextureDir, armorAssetId, "_layer_1.png"), pngBytes);
    }

    @Override
    public void writeArmorLayer2(String armorAssetId, byte[] pngBytes) {
        write(resolveSafe(armorTextureDir, armorAssetId, "_layer_2.png"), pngBytes);
    }

    @Override
    public boolean armorLayer1Exists(String armorAssetId) {
        return Files.exists(resolveSafe(armorTextureDir, armorAssetId, "_layer_1.png"));
    }

    @Override
    public boolean armorLayer2Exists(String armorAssetId) {
        return Files.exists(resolveSafe(armorTextureDir, armorAssetId, "_layer_2.png"));
    }

    @Override
    public Optional<byte[]> readArmorLayer1(String armorAssetId) {
        return read(resolveSafe(armorTextureDir, armorAssetId, "_layer_1.png"));
    }

    @Override
    public Optional<byte[]> readArmorLayer2(String armorAssetId) {
        return read(resolveSafe(armorTextureDir, armorAssetId, "_layer_2.png"));
    }

    // textureId, NOT the block id -- several blocks could in principle share one texture,
    // and this is the path ResourcePackBuilder/BlockTextureFileCopier actually read from
    // (see blockTextureSourceDir in ResourcePackBuilder).
    @Override
    public void writeBlockTexture(String textureId, byte[] pngBytes) {
        write(resolveSafe(blockTextureDir, textureId, ".png"), pngBytes);
    }

    @Override
    public boolean blockTextureExists(String textureId) {
        return Files.exists(resolveSafe(blockTextureDir, textureId, ".png"));
    }

    @Override
    public Optional<byte[]> readBlockTexture(String textureId) {
        return read(resolveSafe(blockTextureDir, textureId, ".png"));
    }

    // Blocks path traversal at the single place where every texture path is built. It is
    // necessary because id/textureId/armorAssetId are never charset-checked anywhere:
    // CustomBlockDefinition only enforces non-blank, and textureId travels straight from
    // the JSON body of POST /api/blocks to here. A textureId like "../../../evil" would
    // write a file outside the plugin's data folder. A registry lookup in the handler layer
    // does NOT stop this -- the attacker's string is itself the registered value.
    private static Path resolveSafe(Path dir, String segment, String suffix) {
        if (segment == null || !SAFE_SEGMENT.matcher(segment).matches()) {
            throw new IllegalArgumentException("Unsafe texture id '" + segment
                    + "': only letters, digits, '_', '-' and single dots between them are allowed");
        }
        return dir.resolve(segment + suffix);
    }

    private void write(Path target, byte[] bytes) {
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, bytes);
            logger.info("Wrote texture file " + target);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write texture file " + target, e);
        }
    }

    private Optional<byte[]> read(Path target) {
        if (!Files.exists(target)) {
            return Optional.empty();
        }
        try {
            return Optional.of(Files.readAllBytes(target));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read texture file " + target, e);
        }
    }
}
