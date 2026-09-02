package com.hoangluongtran0309.application.port;

import java.util.List;

import com.hoangluongtran0309.domain.model.RecipeDefinition;

public interface RecipeConfigSourcePort {

    List<RecipeDefinition> loadAll();

    void save(RecipeDefinition definition);

    void delete(String id);
}
