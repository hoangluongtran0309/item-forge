package com.hoangluongtran0309.domain.model;

import java.util.List;
import java.util.Map;

import com.hoangluongtran0309.domain.exception.InvalidItemDefinitionException;

public record ShapedRecipeDefinition(
        String id,
        String resultId,
        int resultCount,
        List<String> shape,
        Map<Character, String> ingredients) implements RecipeDefinition {

    public ShapedRecipeDefinition {
        if (id == null || id.isBlank()) {
            throw new InvalidItemDefinitionException("Recipe ID cannot be blank");
        }

        if (resultId == null || resultId.isBlank()) {
            throw new InvalidItemDefinitionException("Recipe result ID cannot be blank");
        }

        if (resultCount <= 0) {
            throw new InvalidItemDefinitionException("Recipe result count must be positive");
        }

        if (shape == null || shape.isEmpty()) {
            throw new InvalidItemDefinitionException("Recipe shape cannot be empty");
        }

        if (shape.size() > 3) {
            throw new InvalidItemDefinitionException("Recipe shape must not exceed 3 rows");
        }

        int width = shape.get(0) == null ? -1 : shape.get(0).length();
        for (String row : shape) {
            if (row == null || row.isEmpty() || row.length() > 3) {
                throw new InvalidItemDefinitionException("Recipe shape rows must be 1-3 characters");
            }
            // Bukkit's ShapedRecipe.shape() requires rows of equal length (a rectangle).
            if (row.length() != width) {
                throw new InvalidItemDefinitionException("Recipe shape rows must be rectangular");
            }
        }

        ingredients = ingredients == null ? Map.of() : Map.copyOf(ingredients);
        for (String row : shape) {
            for (char key : row.toCharArray()) {
                if (key == ' ') {
                    continue;
                }
                String value = ingredients.get(key);
                if (value == null) {
                    throw new InvalidItemDefinitionException("Missing ingredient mapping for key '" + key + "'");
                }
                if (value.isBlank()) {
                    throw new InvalidItemDefinitionException(
                            "Ingredient value for key '" + key + "' cannot be blank");
                }
            }
        }

        shape = List.copyOf(shape);
    }
}
