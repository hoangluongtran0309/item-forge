package com.hoangluongtran0309.itemforge.dashboard.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.hoangluongtran0309.itemforge.dashboard.dto.RecipeJson;
import com.hoangluongtran0309.itemforge.dashboard.dto.ShapedRecipeJson;
import com.hoangluongtran0309.itemforge.dashboard.dto.ShapelessRecipeJson;
import com.hoangluongtran0309.itemforge.dashboard.form.IngredientRow;
import com.hoangluongtran0309.itemforge.dashboard.form.RecipeForm;

class RecipeFormMapperTest {

    private final RecipeFormMapper mapper = new RecipeFormMapper();

    @Test
    void toJsonBuildsAShapedRecipe() {
        RecipeForm form = new RecipeForm();
        form.setId("void_sword_recipe");
        form.setType("SHAPED");
        form.setResult("void_sword");
        form.setResultCount(1);
        form.setShapeLines(List.of("A", "B", ""));

        IngredientRow rowA = new IngredientRow();
        rowA.setKey("A");
        rowA.setItemId("STICK");
        IngredientRow rowB = new IngredientRow();
        rowB.setKey("B");
        rowB.setItemId("NETHERITE_SWORD");
        form.setIngredients(List.of(rowA, rowB));

        RecipeJson json = mapper.toJson(form);

        ShapedRecipeJson shaped = assertInstanceOf(ShapedRecipeJson.class, json);
        assertEquals(List.of("A", "B"), shaped.shape());
        assertEquals("STICK", shaped.ingredients().get("A"));
        assertEquals("NETHERITE_SWORD", shaped.ingredients().get("B"));
    }

    @Test
    void toJsonBuildsAShapelessRecipeIgnoringTheKeyColumn() {
        RecipeForm form = new RecipeForm();
        form.setId("void_sword_salvage");
        form.setType("SHAPELESS");
        form.setResult("TORCH");
        form.setResultCount(4);

        IngredientRow row1 = new IngredientRow();
        row1.setKey("ignored"); // the key must not leak into SHAPELESS output
        row1.setItemId("COAL");
        IngredientRow row2 = new IngredientRow();
        row2.setItemId("STICK");
        form.setIngredients(List.of(row1, row2));

        RecipeJson json = mapper.toJson(form);

        ShapelessRecipeJson shapeless = assertInstanceOf(ShapelessRecipeJson.class, json);
        assertEquals(List.of("COAL", "STICK"), shapeless.ingredients());
    }

    @Test
    void toJsonSkipsBlankIngredientRowsAndShapeLines() {
        RecipeForm form = new RecipeForm();
        form.setId("void_sword_recipe");
        form.setType("SHAPED");
        form.setResult("void_sword");
        form.setResultCount(1);
        form.setShapeLines(List.of("A", "", ""));

        IngredientRow complete = new IngredientRow();
        complete.setKey("A");
        complete.setItemId("STICK");
        IngredientRow blank = new IngredientRow();
        form.setIngredients(List.of(complete, blank));

        RecipeJson json = mapper.toJson(form);

        ShapedRecipeJson shaped = assertInstanceOf(ShapedRecipeJson.class, json);
        assertEquals(List.of("A"), shaped.shape());
        assertEquals(1, shaped.ingredients().size());
    }

    @Test
    void fromJsonThenToJsonRoundTripsAShapedRecipe() {
        RecipeJson original = new ShapedRecipeJson("void_sword_recipe", "SHAPED", "void_sword", 1,
                List.of("A", "B"), java.util.Map.of("A", "STICK", "B", "NETHERITE_SWORD"));

        RecipeForm form = mapper.fromJson(original);
        RecipeJson roundTripped = mapper.toJson(form);

        assertEquals(original.type(), roundTripped.type());
        assertEquals(original.result(), roundTripped.result());
        ShapedRecipeJson roundTrippedShaped = assertInstanceOf(ShapedRecipeJson.class, roundTripped);
        assertEquals(original.type(), "SHAPED");
        assertTrue(roundTrippedShaped.ingredients().entrySet()
                .containsAll(((ShapedRecipeJson) original).ingredients().entrySet()));
    }
}
