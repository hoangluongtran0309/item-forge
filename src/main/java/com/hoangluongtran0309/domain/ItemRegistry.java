package com.hoangluongtran0309.domain;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import com.hoangluongtran0309.domain.model.ItemDefinition;

public class ItemRegistry {

    private final Map<String, ItemDefinition> items = new ConcurrentHashMap<>();

    public void register(ItemDefinition definition) {
        items.put(definition.id(), definition);
    }

    public Optional<ItemDefinition> get(String id) {
        return Optional.ofNullable(items.get(id));
    }

    public Collection<ItemDefinition> getAll() {
        return items.values();
    }

    public void remove(String id) {
        items.remove(id);
    }

    public void clear() {
        items.clear();
    }

    public int size() {
        return items.size();
    }

    // A collision-free id range, used both by the AI generation flow and by item creation
    // through the REST API when no customModelData is given.
    public int nextAvailableCustomModelData() {
        return items.values().stream()
                .mapToInt(ItemDefinition::customModelData)
                .max()
                .orElse(0) + 1;
    }
}
