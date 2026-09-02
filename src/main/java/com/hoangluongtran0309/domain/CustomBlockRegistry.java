package com.hoangluongtran0309.domain;

import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import com.hoangluongtran0309.domain.model.CustomBlockDefinition;

public class CustomBlockRegistry {

    private final Map<String, CustomBlockDefinition> blocks = new ConcurrentHashMap<>();

    // The set of (instrument, note) pairs already taken. A real Note Block offers only
    // ~1150 usable blockstate combinations, and two blocks with different ids must not
    // share one, because the client could not tell them apart when drawing textures.
    private final Map<String, String> comboToId = new ConcurrentHashMap<>();

    // true on a successful registration; false when it fails (the instrument+note pair
    // already belongs to a DIFFERENT id). It does not throw, so one bad config entry cannot
    // bring down the whole load (see CustomBlockLoaderService).
    public boolean register(CustomBlockDefinition definition) {
        String combo = comboKey(definition.instrument(), definition.note());
        String existingOwner = comboToId.get(combo);
        if (existingOwner != null && !existingOwner.equals(definition.id())) {
            return false;
        }

        // Re-registering the same id with a new combination -- release the old one first.
        CustomBlockDefinition previous = blocks.get(definition.id());
        if (previous != null) {
            comboToId.remove(comboKey(previous.instrument(), previous.note()));
        }

        blocks.put(definition.id(), definition);
        comboToId.put(combo, definition.id());
        return true;
    }

    public Optional<CustomBlockDefinition> get(String id) {
        return Optional.ofNullable(blocks.get(id));
    }

    public Collection<CustomBlockDefinition> getAll() {
        return blocks.values();
    }

    public void remove(String id) {
        CustomBlockDefinition removed = blocks.remove(id);
        if (removed != null) {
            comboToId.remove(comboKey(removed.instrument(), removed.note()));
        }
    }

    public void clear() {
        blocks.clear();
        comboToId.clear();
    }

    public int size() {
        return blocks.size();
    }

    // A collision-free id range, used by the dashboard API when an admin leaves
    // custom-model-data blank -- the same model as
    // ItemRegistry.nextAvailableCustomModelData().
    public int nextAvailableCustomModelData() {
        return blocks.values().stream()
                .mapToInt(CustomBlockDefinition::customModelData)
                .max()
                .orElse(0) + 1;
    }

    private static String comboKey(String instrument, int note) {
        return instrument.toUpperCase(Locale.ROOT) + ":" + note;
    }
}
