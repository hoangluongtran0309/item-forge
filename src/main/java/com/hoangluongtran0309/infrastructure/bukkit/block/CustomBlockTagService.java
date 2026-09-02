package com.hoangluongtran0309.infrastructure.bukkit.block;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.bukkit.block.Block;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

// A Note Block has NO TileState (confirmed against the Paper API: only TileState
// extends PersistentDataHolder, while org.bukkit.block.data.type.NoteBlock is plain
// BlockData), so a PersistentDataContainer cannot be attached to the block in the world.
// This file is therefore the ONLY source of truth for "is this block managed by
// ItemForge" -- a separate file on disk, read at startup and rewritten immediately after
// EVERY change (the same write-through spirit as YamlSectionFile.dump), needing no
// complex cache or in-memory bookkeeping.
public class CustomBlockTagService {

    private static final String SECTION = "tags";

    private final Path dataFile;
    private final Logger logger;
    private final Map<String, String> tags = new ConcurrentHashMap<>();

    public CustomBlockTagService(Path dataFile, Logger logger) {
        this.dataFile = dataFile;
        this.logger = logger;
    }

    public void load() {
        tags.clear();
        if (Files.notExists(dataFile)) {
            return;
        }

        Map<String, Object> root;
        try (InputStream in = Files.newInputStream(dataFile)) {
            root = new Yaml().load(in);
        } catch (IOException | YAMLException e) {
            logger.warning("Failed to read custom block tag file '" + dataFile + "': " + e.getMessage());
            return;
        }

        if (root == null || !(root.get(SECTION) instanceof Map<?, ?> section)) {
            return;
        }

        for (Map.Entry<?, ?> entry : section.entrySet()) {
            tags.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
        }
    }

    public void tag(Block block, String blockId) {
        tags.put(keyOf(block), blockId);
        persist();
    }

    public void untag(Block block) {
        if (tags.remove(keyOf(block)) != null) {
            persist();
        }
    }

    public Optional<String> lookup(Block block) {
        return Optional.ofNullable(tags.get(keyOf(block)));
    }

    // Used by CustomBlockPistonListener: vanilla has already moved the physical block
    // (BlockData travels with it), so all that is left is rekeying the registry entry.
    public void move(Block from, Block to) {
        String blockId = tags.remove(keyOf(from));
        if (blockId == null) {
            return;
        }
        tags.put(keyOf(to), blockId);
        persist();
    }

    private String keyOf(Block block) {
        return block.getWorld().getName() + ";" + block.getX() + ";" + block.getY() + ";" + block.getZ();
    }

    private void persist() {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put(SECTION, new LinkedHashMap<>(tags));

        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        Yaml yaml = new Yaml(options);
        try {
            Files.createDirectories(dataFile.getParent());
            try (Writer writer = Files.newBufferedWriter(dataFile)) {
                yaml.dump(root, writer);
            }
        } catch (IOException e) {
            logger.severe("Failed to write custom block tag file '" + dataFile + "': " + e.getMessage());
        }
    }
}
