package com.hoangluongtran0309.infrastructure.resourcepack;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.logging.Logger;

import com.hoangluongtran0309.domain.model.ItemDefinition;

public class TextureFileCopier {

    private final Logger logger;

    public TextureFileCopier(Logger logger) {
        this.logger = logger;
    }

    public boolean copyIfExists(ItemDefinition item, Path sourceTextureDir, String namespace, Path outputDir)
            throws IOException {
        Path source = sourceTextureDir.resolve(item.id() + ".png");
        if (Files.notExists(source)) {
            // Logged here because this is the only place that knows exactly why the item was
            // skipped.
            logger.warning("No texture found for item '" + item.id() + "' (" + source
                    + "); skipping this item in the resource pack.");
            return false;
        }

        Path target = outputDir.resolve("assets/" + namespace + "/textures/item/" + item.id() + ".png");
        Files.createDirectories(target.getParent());
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        return true;
    }
}
