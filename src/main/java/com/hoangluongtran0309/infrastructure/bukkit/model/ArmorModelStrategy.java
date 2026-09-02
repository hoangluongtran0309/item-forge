package com.hoangluongtran0309.infrastructure.bukkit.model;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.bukkit.inventory.meta.ItemMeta;

import com.hoangluongtran0309.domain.model.ArmorDefinition;

// A separate interface rather than sharing ItemModelStrategy: the texture copy
// mechanism for legacy armor (a global overwrite per material family) is nothing
// like the "one texture per item" model ItemModelStrategy/TextureFileCopier assume.
public interface ArmorModelStrategy {

    void applyModel(ItemMeta meta, ArmorDefinition definition);

    void generateResourcePackFiles(List<ArmorDefinition> armors, Path textureSourceDir, String namespace,
            Path outputDir) throws IOException;
}
