package com.hoangluongtran0309.infrastructure.resourcepack;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.logging.Logger;

import com.hoangluongtran0309.domain.model.ArmorDefinition;

public class ArmorTextureFileCopier {

    private final Logger logger;

    public ArmorTextureFileCopier(Logger logger) {
        this.logger = logger;
    }

    // layer_1 is the mandatory layer (helmet/chestplate/boots all share one humanoid
    // layer). It is keyed by armorAssetId rather than by each piece's id, because several
    // pieces -- a full set of four, say -- can share a single asset.
    public boolean copyLayer1(String armorAssetId, Path sourceTextureDir, String namespace, Path outputDir)
            throws IOException {
        Path source = sourceTextureDir.resolve(armorAssetId + "_layer_1.png");
        if (Files.notExists(source)) {
            logger.warning("No layer_1 texture found for armor asset '" + armorAssetId + "' (" + source
                    + "); skipping this asset in the resource pack.");
            return false;
        }

        Path target = outputDir.resolve("assets/" + namespace + "/textures/entity/equipment/humanoid/"
                + armorAssetId + ".png");
        Files.createDirectories(target.getParent());
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        return true;
    }

    // layer_2 is the leggings-specific layer -- optional, since many sets use a single
    // texture and need no separate overlay for the legs.
    public boolean copyLayer2IfExists(String armorAssetId, Path sourceTextureDir, String namespace, Path outputDir)
            throws IOException {
        Path source = sourceTextureDir.resolve(armorAssetId + "_layer_2.png");
        if (Files.notExists(source)) {
            return false;
        }

        Path target = outputDir.resolve("assets/" + namespace + "/textures/entity/equipment/humanoid_leggings/"
                + armorAssetId + ".png");
        Files.createDirectories(target.getParent());
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        return true;
    }

    // The flat icon shown in the inventory, in hand and on the ground. It is a different
    // thing from layer_1/layer_2 (textures UV-mapped onto the humanoid model when worn),
    // so it lives in its own <id>.png with no "_layer_" suffix.
    public boolean copyIcon(ArmorDefinition armor, Path sourceTextureDir, String namespace, Path outputDir)
            throws IOException {
        Path source = sourceTextureDir.resolve(armor.id() + ".png");
        if (Files.notExists(source)) {
            return false;
        }

        Path target = outputDir.resolve("assets/" + namespace + "/textures/item/" + armor.id() + ".png");
        Files.createDirectories(target.getParent());
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        return true;
    }
}
