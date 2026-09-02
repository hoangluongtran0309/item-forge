package com.hoangluongtran0309.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.hoangluongtran0309.domain.exception.InvalidItemDefinitionException;

class ShapelessRecipeDefinitionTest {

    @Test
    void blankIdThrows() {
        assertThrows(InvalidItemDefinitionException.class,
                () -> new ShapelessRecipeDefinition(" ", "void_sword_salvage", 1, List.of("STICK")));
    }

    @Test
    void blankResultIdThrows() {
        assertThrows(InvalidItemDefinitionException.class,
                () -> new ShapelessRecipeDefinition("recipe", " ", 1, List.of("STICK")));
    }

    @Test
    void nonPositiveResultCountThrows() {
        assertThrows(InvalidItemDefinitionException.class,
                () -> new ShapelessRecipeDefinition("recipe", "void_sword_salvage", 0, List.of("STICK")));
    }

    @Test
    void nullIngredientsThrows() {
        assertThrows(InvalidItemDefinitionException.class,
                () -> new ShapelessRecipeDefinition("recipe", "void_sword_salvage", 1, null));
    }

    @Test
    void emptyIngredientsThrows() {
        assertThrows(InvalidItemDefinitionException.class,
                () -> new ShapelessRecipeDefinition("recipe", "void_sword_salvage", 1, List.of()));
    }

    @Test
    void tooManyIngredientsThrows() {
        List<String> ingredients = List.of("A", "A", "A", "A", "A", "A", "A", "A", "A", "A");
        assertThrows(InvalidItemDefinitionException.class,
                () -> new ShapelessRecipeDefinition("recipe", "void_sword_salvage", 1, ingredients));
    }

    @Test
    void blankIngredientEntryThrows() {
        List<String> ingredients = new ArrayList<>(List.of("STICK", " "));
        assertThrows(InvalidItemDefinitionException.class,
                () -> new ShapelessRecipeDefinition("recipe", "void_sword_salvage", 1, ingredients));
    }

    @Test
    void validDefinitionDefensivelyCopiesIngredients() {
        List<String> ingredients = new ArrayList<>(List.of("STICK", "COAL"));

        ShapelessRecipeDefinition definition = new ShapelessRecipeDefinition("recipe", "void_sword_salvage", 4,
                ingredients);
        ingredients.add("COAL");

        assertEquals(List.of("STICK", "COAL"), definition.ingredientIds());
    }
}
