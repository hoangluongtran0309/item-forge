package com.hoangluongtran0309.infrastructure.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.hoangluongtran0309.domain.exception.InvalidItemDefinitionException;
import com.hoangluongtran0309.domain.model.RecipeDefinition;
import com.hoangluongtran0309.domain.model.ShapedRecipeDefinition;
import com.hoangluongtran0309.domain.model.ShapelessRecipeDefinition;

class RecipeDefinitionMapperTest {

    @Test
    void toMapThenFromMapRoundTripsAShapedRecipe() {
        RecipeDefinition original = new ShapedRecipeDefinition("void_sword_recipe", "void_sword", 1,
                List.of("A", "B"), Map.of('A', "STICK", 'B', "NETHERITE_SWORD"));

        Map<String, Object> map = RecipeDefinitionMapper.toMap(original);
        RecipeDefinition roundTripped = RecipeDefinitionMapper.fromMap("void_sword_recipe", map);

        assertEquals(original, roundTripped);
    }

    @Test
    void toMapThenFromMapRoundTripsAShapelessRecipe() {
        RecipeDefinition original = new ShapelessRecipeDefinition("void_sword_salvage", "TORCH", 4,
                List.of("COAL", "STICK"));

        Map<String, Object> map = RecipeDefinitionMapper.toMap(original);
        RecipeDefinition roundTripped = RecipeDefinitionMapper.fromMap("void_sword_salvage", map);

        assertEquals(original, roundTripped);
    }

    @Test
    void fromMapThrowsOnUnknownType() {
        Map<String, Object> map = Map.of("type", "NOT_A_REAL_TYPE", "result", "void_sword");
        assertThrows(InvalidItemDefinitionException.class, () -> RecipeDefinitionMapper.fromMap("broken", map));
    }

    @Test
    void fromMapThrowsOnMissingResult() {
        Map<String, Object> map = Map.of("type", "SHAPELESS");
        assertThrows(InvalidItemDefinitionException.class, () -> RecipeDefinitionMapper.fromMap("broken", map));
    }
}
