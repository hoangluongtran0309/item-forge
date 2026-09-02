package com.hoangluongtran0309.infrastructure.config;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.hoangluongtran0309.domain.exception.InvalidItemDefinitionException;
import com.hoangluongtran0309.domain.model.RecipeDefinition;
import com.hoangluongtran0309.domain.model.ShapedRecipeDefinition;
import com.hoangluongtran0309.domain.model.ShapelessRecipeDefinition;

/**
 * Converts between RecipeDefinition and the Map<String,Object> structure matching
 * recipes.yml's kebab-case schema. Follows the same model as ItemDefinitionMapper.
 */
public final class RecipeDefinitionMapper {

    private RecipeDefinitionMapper() {
    }

    public static RecipeDefinition fromMap(String id, Map<String, Object> recipeMap) {
        String type = requireString(recipeMap, "type");
        String resultId = requireString(recipeMap, "result");
        int resultCount = intValue(recipeMap.get("result-count"), 1);

        return switch (type) {
            case "SHAPED" -> {
                List<String> shape = stringList(recipeMap.get("shape"));
                Map<Character, String> ingredients = parseIngredientKeyMap(recipeMap.get("ingredients"));
                yield new ShapedRecipeDefinition(id, resultId, resultCount, shape, ingredients);
            }
            case "SHAPELESS" -> {
                List<String> ingredientIds = stringList(recipeMap.get("ingredients"));
                yield new ShapelessRecipeDefinition(id, resultId, resultCount, ingredientIds);
            }
            default -> throw new InvalidItemDefinitionException("Unknown recipe type: " + type);
        };
    }

    public static Map<String, Object> toMap(RecipeDefinition definition) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("result", definition.resultId());
        map.put("result-count", definition.resultCount());

        switch (definition) {
            case ShapedRecipeDefinition shaped -> {
                map.put("type", "SHAPED");
                map.put("shape", shaped.shape());
                Map<String, Object> ingredients = new LinkedHashMap<>();
                shaped.ingredients().forEach((key, value) -> ingredients.put(String.valueOf(key), value));
                map.put("ingredients", ingredients);
            }
            case ShapelessRecipeDefinition shapeless -> {
                map.put("type", "SHAPELESS");
                map.put("ingredients", shapeless.ingredientIds());
            }
        }

        return map;
    }

    private static Map<Character, String> parseIngredientKeyMap(Object rawIngredients) {
        Map<String, Object> map = asMap(rawIngredients);
        Map<Character, String> ingredients = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            if (key == null || key.length() != 1) {
                throw new InvalidItemDefinitionException("Ingredient key must be a single character: " + key);
            }
            ingredients.put(key.charAt(0), String.valueOf(entry.getValue()));
        }
        return ingredients;
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> asMap(Object value) {
        if (!(value instanceof Map)) {
            throw new InvalidItemDefinitionException("Expected a mapping but found: " + value);
        }
        return (Map<String, Object>) value;
    }

    private static String requireString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            throw new InvalidItemDefinitionException("Missing required field: " + key);
        }
        return value.toString();
    }

    @SuppressWarnings("unchecked")
    private static List<String> stringList(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        return ((List<Object>) list).stream().map(Object::toString).toList();
    }

    private static int intValue(Object value, int defaultValue) {
        return value == null ? defaultValue : ((Number) value).intValue();
    }
}
