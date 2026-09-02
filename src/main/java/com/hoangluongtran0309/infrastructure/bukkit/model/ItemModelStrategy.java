package com.hoangluongtran0309.infrastructure.bukkit.model;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.bukkit.inventory.meta.ItemMeta;

import com.hoangluongtran0309.domain.model.ItemDefinition;

public interface ItemModelStrategy {

    void applyModel(ItemMeta meta, ItemDefinition definition);

    // Takes a list of items (not a single item) because the legacy CustomModelData
    // format allows each base material EXACTLY ONE overrides file. Taking one item at a
    // time and rewriting the file would drop the overrides of every other item sharing
    // that material.
    //
    // Always processes EVERY item with customModelData > 0, even when its texture is
    // missing: the model/override is still written and only the texture copy is
    // best-effort. An item without a texture points at a path that does not exist in the
    // pack, so the client shows the missing-texture block instead of falling back to the
    // vanilla icon.
    void generateResourcePackFiles(List<ItemDefinition> items, Path textureSourceDir, String namespace,
            Path outputDir) throws IOException;
}
