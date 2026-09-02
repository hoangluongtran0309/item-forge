package com.hoangluongtran0309.domain.model;

/**
 * RecipeDefinition
 */
public sealed interface RecipeDefinition
        permits ShapedRecipeDefinition, ShapelessRecipeDefinition {

    String id();

    String resultId();

    int resultCount();
}
