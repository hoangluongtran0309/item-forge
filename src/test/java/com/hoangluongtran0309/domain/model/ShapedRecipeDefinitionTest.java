package com.hoangluongtran0309.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.hoangluongtran0309.domain.exception.InvalidItemDefinitionException;

class ShapedRecipeDefinitionTest {

    @Test
    void blankIdThrows() {
        assertThrows(InvalidItemDefinitionException.class,
                () -> new ShapedRecipeDefinition(" ", "void_sword", 1, List.of("A"), Map.of('A', "DIAMOND")));
    }

    @Test
    void blankResultIdThrows() {
        assertThrows(InvalidItemDefinitionException.class,
                () -> new ShapedRecipeDefinition("recipe", " ", 1, List.of("A"), Map.of('A', "DIAMOND")));
    }

    @Test
    void nonPositiveResultCountThrows() {
        assertThrows(InvalidItemDefinitionException.class,
                () -> new ShapedRecipeDefinition("recipe", "void_sword", 0, List.of("A"), Map.of('A', "DIAMOND")));
    }

    @Test
    void nullShapeThrows() {
        assertThrows(InvalidItemDefinitionException.class,
                () -> new ShapedRecipeDefinition("recipe", "void_sword", 1, null, Map.of('A', "DIAMOND")));
    }

    @Test
    void emptyShapeThrows() {
        assertThrows(InvalidItemDefinitionException.class,
                () -> new ShapedRecipeDefinition("recipe", "void_sword", 1, List.of(), Map.of('A', "DIAMOND")));
    }

    @Test
    void tooManyRowsThrows() {
        assertThrows(InvalidItemDefinitionException.class,
                () -> new ShapedRecipeDefinition("recipe", "void_sword", 1, List.of("A", "A", "A", "A"),
                        Map.of('A', "DIAMOND")));
    }

    @Test
    void rowTooLongThrows() {
        assertThrows(InvalidItemDefinitionException.class,
                () -> new ShapedRecipeDefinition("recipe", "void_sword", 1, List.of("AAAA"), Map.of('A', "DIAMOND")));
    }

    @Test
    void nonRectangularRowsThrow() {
        assertThrows(InvalidItemDefinitionException.class,
                () -> new ShapedRecipeDefinition("recipe", "void_sword", 1, List.of("AA", "A"),
                        Map.of('A', "DIAMOND")));
    }

    @Test
    void missingIngredientMappingThrows() {
        assertThrows(InvalidItemDefinitionException.class,
                () -> new ShapedRecipeDefinition("recipe", "void_sword", 1, List.of("AB"), Map.of('A', "DIAMOND")));
    }

    @Test
    void blankIngredientValueThrows() {
        Map<Character, String> ingredients = new LinkedHashMap<>();
        ingredients.put('A', " ");
        assertThrows(InvalidItemDefinitionException.class,
                () -> new ShapedRecipeDefinition("recipe", "void_sword", 1, List.of("A"), ingredients));
    }

    @Test
    void spacesInShapeDoNotRequireIngredientMapping() {
        ShapedRecipeDefinition definition = new ShapedRecipeDefinition("recipe", "void_sword", 1,
                List.of("A "), Map.of('A', "DIAMOND"));

        assertEquals(List.of("A "), definition.shape());
    }

    @Test
    void validDefinitionDefensivelyCopiesShapeAndIngredients() {
        List<String> shape = new java.util.ArrayList<>(List.of("A"));
        Map<Character, String> ingredients = new LinkedHashMap<>();
        ingredients.put('A', "DIAMOND");

        ShapedRecipeDefinition definition = new ShapedRecipeDefinition("recipe", "void_sword", 1, shape, ingredients);
        shape.add("B");
        ingredients.put('B', "STICK");

        assertEquals(List.of("A"), definition.shape());
        assertEquals(Map.of('A', "DIAMOND"), definition.ingredients());
    }
}
