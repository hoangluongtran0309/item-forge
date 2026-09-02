package com.hoangluongtran0309.infrastructure.resourcepack;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Stream;

import com.hoangluongtran0309.application.ServerVersion;
import com.hoangluongtran0309.application.port.ResourcePackInfo;
import com.hoangluongtran0309.application.port.ResourcePackPort;
import com.hoangluongtran0309.domain.ArmorRegistry;
import com.hoangluongtran0309.domain.CustomBlockRegistry;
import com.hoangluongtran0309.domain.ItemRegistry;
import com.hoangluongtran0309.domain.model.ArmorDefinition;
import com.hoangluongtran0309.domain.model.CustomBlockDefinition;
import com.hoangluongtran0309.domain.model.ItemDefinition;
import com.hoangluongtran0309.infrastructure.bukkit.model.ArmorModelStrategy;
import com.hoangluongtran0309.infrastructure.bukkit.model.BlockModelStrategy;
import com.hoangluongtran0309.infrastructure.bukkit.model.ItemModelStrategy;

public class ResourcePackBuilder implements ResourcePackPort {

    private static final String NAMESPACE = "itemforge";
    private static final String PACK_DESCRIPTION = "ItemForge generated resource pack";

    private final ItemRegistry itemRegistry;
    private final ItemModelStrategy modelStrategy;
    private final ArmorRegistry armorRegistry;
    private final ArmorModelStrategy armorModelStrategy;
    private final CustomBlockRegistry customBlockRegistry;
    private final BlockModelStrategy blockModelStrategy;
    private final Path textureSourceDir;
    private final Path armorTextureSourceDir;
    private final Path blockTextureSourceDir;
    private final Path stagingDir;
    private final Path zipFile;
    private final String publicBaseUrl; // null = resource-pack.host not configured, build locally only
    private final int packFormat;
    private final Logger logger;

    private final PackZipper packZipper;
    private final Sha1Hasher sha1Hasher;

    private volatile ResourcePackInfo currentPackInfo;

    public ResourcePackBuilder(ItemRegistry itemRegistry, ItemModelStrategy modelStrategy,
            ArmorRegistry armorRegistry, ArmorModelStrategy armorModelStrategy,
            CustomBlockRegistry customBlockRegistry, BlockModelStrategy blockModelStrategy,
            Path dataFolder, String publicBaseUrl, ServerVersion serverVersion, Logger logger) {
        this.itemRegistry = itemRegistry;
        this.modelStrategy = modelStrategy;
        this.armorRegistry = armorRegistry;
        this.armorModelStrategy = armorModelStrategy;
        this.customBlockRegistry = customBlockRegistry;
        this.blockModelStrategy = blockModelStrategy;
        this.textureSourceDir = dataFolder.resolve("textures");
        this.armorTextureSourceDir = dataFolder.resolve("textures").resolve("armor");
        this.blockTextureSourceDir = dataFolder.resolve("textures").resolve("blocks");
        this.stagingDir = dataFolder.resolve("pack").resolve("staging");
        this.zipFile = dataFolder.resolve("pack").resolve("resource-pack.zip");
        this.publicBaseUrl = publicBaseUrl;
        this.packFormat = packFormatFor(serverVersion);
        this.logger = logger;
        this.packZipper = new PackZipper();
        this.sha1Hasher = new Sha1Hasher();
    }

    private static int packFormatFor(ServerVersion version) {
        // A best-effort estimate -- a mismatched pack_format only makes the client warn, it
        // does not block loading.
        if (version.isAtLeast(1, 21, 4)) {
            return 46; // Client Items system
        }
        if (version.isAtLeast(1, 20, 5)) {
            return 32;
        }
        return 22; // fallback for versions older than 1.20.5
    }

    @Override
    public void rebuild() {
        try {
            resetStagingDir();
            // Hand every item with customModelData > 0 to the strategy, missing texture or
            // not -- each strategy decides how to express "missing" itself (see
            // ItemModelStrategy#generateResourcePackFiles), so nothing is pre-filtered here
            // any more.
            List<ItemDefinition> configuredItems = itemRegistry.getAll().stream()
                    .filter(item -> item.customModelData() > 0)
                    .toList();
            modelStrategy.generateResourcePackFiles(configuredItems, textureSourceDir, NAMESPACE, stagingDir);

            // Every entry in armor.yml is treated as having its own texture, so there is no
            // pre-filter as there is for items (which filter on customModelData). A missing
            // texture is handled inside each ArmorModelStrategy, the same way
            // TextureFileCopier handles it for items.
            List<ArmorDefinition> armors = new ArrayList<>(armorRegistry.getAll());
            armorModelStrategy.generateResourcePackFiles(armors, armorTextureSourceDir, NAMESPACE, stagingDir);

            // Like armor: every entry in blocks.yml goes to the strategy with no
            // customModelData pre-filter here. BlockModelStrategy decides both when to write
            // the blockstate/block model (always needed, independent of the held icon) and
            // when to write the icon (only with customModelData > 0).
            List<CustomBlockDefinition> blocks = new ArrayList<>(customBlockRegistry.getAll());
            blockModelStrategy.generateResourcePackFiles(blocks, blockTextureSourceDir, NAMESPACE, stagingDir);

            writePackMcmeta();
            packZipper.zip(stagingDir, zipFile);
            String hash = sha1Hasher.hashHex(zipFile);

            if (publicBaseUrl != null) {
                currentPackInfo = new ResourcePackInfo(publicBaseUrl, hash);
                logger.info("Resource pack rebuilt (" + configuredItems.size() + " item(s), " + armors.size()
                        + " armor piece(s), " + blocks.size() + " custom block(s), sha1=" + hash + ")");
            } else {
                logger.info("Resource pack built locally (" + configuredItems.size() + " item(s), " + armors.size()
                        + " armor piece(s), " + blocks.size()
                        + " custom block(s)) but is not being served yet because resource-pack.host is not configured.");
            }
        } catch (IOException | UncheckedIOException e) {
            logger.severe("Failed to rebuild resource pack: " + e.getMessage());
            // Keep the previous currentPackInfo (if any) so players currently joining are not
            // disrupted.
        }
    }

    @Override
    public Optional<ResourcePackInfo> currentPack() {
        return Optional.ofNullable(currentPackInfo);
    }

    public Path zipFilePath() {
        return zipFile;
    }

    private void writePackMcmeta() throws IOException {
        String json = """
                {
                  "pack": {
                    "pack_format": %d,
                    "description": "%s"
                  }
                }
                """.formatted(packFormat, PACK_DESCRIPTION);
        Files.writeString(stagingDir.resolve("pack.mcmeta"), json);
    }

    private void resetStagingDir() throws IOException {
        // Wipe staging before every build, so model/texture files of items already removed
        // from items.yml do not linger in the new zip.
        if (Files.exists(stagingDir)) {
            try (Stream<Path> walk = Files.walk(stagingDir)) {
                walk.sorted(Comparator.reverseOrder()).forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
            }
        }
        Files.createDirectories(stagingDir);
    }
}
