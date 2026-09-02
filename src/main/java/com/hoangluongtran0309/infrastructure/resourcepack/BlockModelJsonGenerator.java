package com.hoangluongtran0309.infrastructure.resourcepack;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import com.hoangluongtran0309.domain.model.CustomBlockDefinition;

public class BlockModelJsonGenerator {

    private static final String NOTE_BLOCK_MATERIAL = "note_block";

    public void writeBlockModel(String textureId, String namespace, Path outputDir) throws IOException {
        Path target = outputDir.resolve("assets/" + namespace + "/models/block/" + textureId + ".json");
        Files.createDirectories(target.getParent());
        Files.writeString(target, """
                {
                  "parent": "block/cube_all",
                  "textures": {
                    "all": "%s:block/%s"
                  }
                }
                """.formatted(namespace, textureId));
    }

    // The held item parents DIRECTLY to the block model (exactly what vanilla does for
    // every real block item) -- it declares no "textures" of its own and inherits them
    // all from the block model, so the same texture never has to be copied twice.
    public void writeItemModel(String id, String textureId, String namespace, Path outputDir) throws IOException {
        Path target = outputDir.resolve("assets/" + namespace + "/models/custom/" + id + ".json");
        Files.createDirectories(target.getParent());
        Files.writeString(target, """
                {
                  "parent": "%s:block/%s"
                }
                """.formatted(namespace, textureId));
    }

    // Modern format (1.21.4+, Client Items) -- points at the leaf model already
    // writeItemModel() wrote under "models/custom/", exactly as ModelJsonGenerator does for items
    public void writeClientItem(String id, String namespace, Path outputDir) throws IOException {
        Path target = outputDir.resolve("assets/" + namespace + "/items/" + id + ".json");
        Files.createDirectories(target.getParent());
        Files.writeString(target, """
                {
                  "model": {
                    "type": "minecraft:model",
                    "model": "%s:custom/%s"
                  }
                }
                """.formatted(namespace, id));
    }

    // Legacy format (<1.21.4) -- every custom block uses Material.NOTE_BLOCK, so there is
    // only ONE overrides file to write (unlike items, no grouping by material is needed
    // because it is always note_block).
    public void writeMaterialOverrides(List<CustomBlockDefinition> blocks, String namespace, Path outputDir)
            throws IOException {
        Path target = outputDir.resolve("assets/minecraft/models/item/" + NOTE_BLOCK_MATERIAL + ".json");
        Files.createDirectories(target.getParent());

        String overrides = blocks.stream()
                .sorted(Comparator.comparingInt(CustomBlockDefinition::customModelData))
                .map(block -> """
                        { "predicate": { "custom_model_data": %d }, "model": "%s:custom/%s" }"""
                        .formatted(block.customModelData(), namespace, block.id()))
                .collect(Collectors.joining(",\n    "));

        Files.writeString(target, """
                {
                  "parent": "item/generated",
                  "textures": {
                    "layer0": "minecraft:item/note_block"
                  },
                  "overrides": [
                    %s
                  ]
                }
                """.formatted(overrides));
    }
}
