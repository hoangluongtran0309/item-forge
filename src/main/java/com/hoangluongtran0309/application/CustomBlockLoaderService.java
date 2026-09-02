package com.hoangluongtran0309.application;

import java.util.function.Consumer;

import com.hoangluongtran0309.application.port.CustomBlockConfigSourcePort;
import com.hoangluongtran0309.domain.ArmorRegistry;
import com.hoangluongtran0309.domain.CustomBlockRegistry;
import com.hoangluongtran0309.domain.ItemRegistry;
import com.hoangluongtran0309.domain.model.CustomBlockDefinition;

public class CustomBlockLoaderService {

    private final CustomBlockConfigSourcePort configSource;
    private final CustomBlockRegistry registry;
    private final ItemRegistry itemRegistry;
    private final ArmorRegistry armorRegistry;

    public CustomBlockLoaderService(CustomBlockConfigSourcePort configSource, CustomBlockRegistry registry,
            ItemRegistry itemRegistry, ArmorRegistry armorRegistry) {
        this.configSource = configSource;
        this.registry = registry;
        this.itemRegistry = itemRegistry;
        this.armorRegistry = armorRegistry;
    }

    // Takes a Consumer<String> instead of logging directly, like the per-line warnings in
    // YamlConfigAdapter -- it lets the caller (ItemForgePlugin at startup, ItemForgeCommand
    // on /itemforge reload) decide where warnings go: the logger or the command sender.
    public int reloadAll(Consumer<String> warningLogger) {
        registry.clear();

        int loaded = 0;
        for (CustomBlockDefinition definition : configSource.loadAll()) {
            if (registry.register(definition)) {
                loaded++;
            } else {
                warningLogger.accept("Skipping custom block '" + definition.id() + "': instrument+note ("
                        + definition.instrument() + ", " + definition.note() + ") already used by another block.");
            }
        }

        warnOnCrossRegistryIdCollisions(warningLogger);
        warnOnNoteBlockMaterialClash(warningLogger);

        return loaded;
    }

    // Returns false when the instrument+note combination already belongs to a DIFFERENT
    // id. In that case nothing is written to blocks.yml, so no record is left on disk that
    // never makes it into the registry (see BlocksApiHandler.create/update). Checking with
    // register() BEFORE writing to disk is safe because a false return from register() is a
    // complete no-op -- it touches neither blocks nor comboToId (see CustomBlockRegistry).
    public boolean save(CustomBlockDefinition definition) {
        if (!registry.register(definition)) {
            return false;
        }
        configSource.save(definition);
        return true;
    }

    public void delete(String id) {
        configSource.delete(id);
        registry.remove(id);
    }

    // give/list share one id namespace across items, armor and blocks. A duplicate id does
    // not break anything (items win, then armor, then blocks -- see
    // ItemForgeCommand.handleGive), but the admin deserves a clear warning to avoid
    // confusion.
    private void warnOnCrossRegistryIdCollisions(Consumer<String> warningLogger) {
        for (CustomBlockDefinition definition : registry.getAll()) {
            String id = definition.id();
            if (itemRegistry.get(id).isPresent() || armorRegistry.get(id).isPresent()) {
                warningLogger.accept("Custom block id '" + id
                        + "' collides with an existing item/armor id. '/itemforge give " + id
                        + "' will not resolve to this block.");
            }
        }
    }

    // Both items.yml and blocks.yml can generate a legacy override for the same
    // assets/minecraft/models/item/note_block.json file. When both use the NOTE_BLOCK
    // material, whichever is written last wins (see ResourcePackBuilder). This is a known
    // limitation: warn about it rather than silently merging.
    private void warnOnNoteBlockMaterialClash(Consumer<String> warningLogger) {
        if (registry.size() == 0) {
            return;
        }

        boolean anyItemUsesNoteBlock = itemRegistry.getAll().stream()
                .anyMatch(item -> "NOTE_BLOCK".equalsIgnoreCase(item.material()));
        if (anyItemUsesNoteBlock) {
            warningLogger.accept("An item in items.yml uses material NOTE_BLOCK while custom blocks are configured. "
                    + "Both share the legacy override file for note_block and will conflict - "
                    + "material NOTE_BLOCK is reserved for custom blocks.");
        }
    }
}
