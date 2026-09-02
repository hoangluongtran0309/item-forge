package com.hoangluongtran0309.infrastructure.bukkit.model;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.bukkit.inventory.meta.ItemMeta;

import com.hoangluongtran0309.domain.model.CustomBlockDefinition;

// A sibling of ItemModelStrategy/ArmorModelStrategy rather than a direct reuse of
// them: blockstates and block models are a different kind of asset entirely (e.g.
// assets/minecraft/blockstates/note_block.json), and forcing CustomBlockDefinition
// through ItemModelStrategy would drag in its parentFor() heuristic, which is
// item-only (item/generated vs item/handheld) and means nothing for a block. See
// docs/standards/version-compatibility.md -- every appearance-related feature has to
// go through its own strategy interface like this one, never hardcoded.
public interface BlockModelStrategy {

    // Applies the identity of the held ITEM (before it is placed) -- CustomModelData
    // on legacy, the item_model component on modern. The placed block in the world
    // needs nothing from here: the note_block.json blockstate picks its texture from
    // the instrument/note pair, independent of CustomModelData/item_model.
    void applyModel(ItemMeta meta, CustomBlockDefinition definition);

    void generateResourcePackFiles(List<CustomBlockDefinition> blocks, Path textureSourceDir, String namespace,
            Path outputDir) throws IOException;
}
