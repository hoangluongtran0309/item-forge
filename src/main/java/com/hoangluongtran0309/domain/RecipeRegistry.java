package com.hoangluongtran0309.domain;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import com.hoangluongtran0309.domain.model.RecipeDefinition;

public class RecipeRegistry {

    private final Map<String, RecipeDefinition> recipes = new ConcurrentHashMap<>();

    public void register(RecipeDefinition definition) {
        recipes.put(definition.id(), definition);
    }

    public Optional<RecipeDefinition> get(String id) {
        return Optional.ofNullable(recipes.get(id));
    }

    public Collection<RecipeDefinition> getAll() {
        return recipes.values();
    }

    public void remove(String id) {
        recipes.remove(id);
    }

    public void clear() {
        recipes.clear();
    }

    public int size() {
        return recipes.size();
    }
}
