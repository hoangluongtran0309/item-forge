package com.hoangluongtran0309.infrastructure.resourcepack;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.logging.Logger;

public class BlockTextureFileCopier {

    private final Logger logger;

    public BlockTextureFileCopier(Logger logger) {
        this.logger = logger;
    }

    // One texture per textureId -- the held item needs none of its own, because its
    // model parents straight to the block model (see BlockModelJsonGenerator).
    public boolean copyIfExists(String textureId, Path sourceTextureDir, String namespace, Path outputDir)
            throws IOException {
        Path source = sourceTextureDir.resolve(textureId + ".png");
        if (Files.notExists(source)) {
            logger.warning("No texture found for custom block '" + textureId + "' (" + source
                    + "); skipping this block in the resource pack.");
            return false;
        }

        Path target = outputDir.resolve("assets/" + namespace + "/textures/block/" + textureId + ".png");
        Files.createDirectories(target.getParent());
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        return true;
    }
}
