package com.hoangluongtran0309.domain.model;

import java.util.List;

import com.hoangluongtran0309.domain.exception.InvalidItemDefinitionException;

public record ShapelessRecipeDefinition(
        String id,
        String resultId,
        int resultCount,
        List<String> ingredientIds) implements RecipeDefinition {

    public ShapelessRecipeDefinition {
        if (id == null || id.isBlank()) {
            throw new InvalidItemDefinitionException("Recipe ID cannot be blank");
        }

        if (resultId == null || resultId.isBlank()) {
            throw new InvalidItemDefinitionException("Recipe result ID cannot be blank");
        }

        if (resultCount <= 0) {
            throw new InvalidItemDefinitionException("Recipe result count must be positive");
        }

        if (ingredientIds == null || ingredientIds.isEmpty()) {
            throw new InvalidItemDefinitionException("Shapeless recipe must declare at least one ingredient");
        }

        // The vanilla crafting table's 9-slot limit (a 3x3 grid).
        if (ingredientIds.size() > 9) {
            throw new InvalidItemDefinitionException("Shapeless recipe cannot declare more than 9 ingredients");
        }

        for (String ingredientId : ingredientIds) {
            if (ingredientId == null || ingredientId.isBlank()) {
                throw new InvalidItemDefinitionException("Ingredient ID cannot be blank");
            }
        }

        ingredientIds = List.copyOf(ingredientIds);
    }
}
