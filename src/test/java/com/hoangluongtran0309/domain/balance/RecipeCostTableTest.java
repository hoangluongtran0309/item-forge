package com.hoangluongtran0309.domain.balance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;

import org.junit.jupiter.api.Test;

import com.hoangluongtran0309.domain.model.ItemDefinition;
import com.hoangluongtran0309.domain.model.ShapedRecipeDefinition;
import com.hoangluongtran0309.domain.model.ShapelessRecipeDefinition;

class RecipeCostTableTest {

    @Test
    void anIdWithNoRecipeHasNoCost() {
        RecipeCostTable table = new RecipeCostTable(List.of(), List.of());

        assertFalse(table.isCraftable("void_sword"));
        assertTrue(table.costOf("void_sword").isEmpty());
    }

    @Test
    void aShapedRecipeCostsTheSumOfEverySlotItUses() {
        ShapedRecipeDefinition sword = new ShapedRecipeDefinition("void_sword", "void_sword", 1,
                List.of(" V ", " V ", " S "), Map.of('V', "NETHERITE_INGOT", 'S', "STICK"));

        RecipeCostTable table = new RecipeCostTable(List.of(sword), List.of());

        double expected = MaterialTier.ingredientCost("NETHERITE_INGOT") * 2
                + MaterialTier.ingredientCost("STICK");
        assertEquals(expected, table.costOf("void_sword").getAsDouble());
    }

    @Test
    void aShapelessRecipeCostsTheSumOfItsIngredients() {
        ShapelessRecipeDefinition recipe = new ShapelessRecipeDefinition("blend", "blend", 1,
                List.of("DIAMOND", "DIAMOND", "STICK"));

        RecipeCostTable table = new RecipeCostTable(List.of(recipe), List.of());

        double expected = MaterialTier.ingredientCost("DIAMOND") * 2 + MaterialTier.ingredientCost("STICK");
        assertEquals(expected, table.costOf("blend").getAsDouble());
    }

    @Test
    void producingSeveralAtOnceDividesTheCostPerItem() {
        ShapelessRecipeDefinition single = new ShapelessRecipeDefinition("one", "one", 1, List.of("DIAMOND"));
        ShapelessRecipeDefinition batch = new ShapelessRecipeDefinition("four", "four", 4, List.of("DIAMOND"));

        RecipeCostTable table = new RecipeCostTable(List.of(single, batch), List.of());

        assertEquals(table.costOf("one").getAsDouble() / 4, table.costOf("four").getAsDouble());
    }

    @Test
    void aCustomItemIngredientIsPricedByItsOwnRecipe() {
        ShapelessRecipeDefinition blade = new ShapelessRecipeDefinition("blade", "blade", 1,
                List.of("NETHERITE_INGOT", "NETHERITE_INGOT"));
        ShapelessRecipeDefinition upgraded = new ShapelessRecipeDefinition("upgraded", "upgraded", 1,
                List.of("blade", "STICK"));

        RecipeCostTable table = new RecipeCostTable(List.of(blade, upgraded),
                List.of(item("blade", "NETHERITE_SWORD"), item("upgraded", "NETHERITE_SWORD")));

        double expected = table.costOf("blade").getAsDouble() + MaterialTier.ingredientCost("STICK");
        assertEquals(expected, table.costOf("upgraded").getAsDouble());
    }

    @Test
    void aCustomItemIngredientWithNoRecipeFallsBackToItsMaterial() {
        ShapelessRecipeDefinition salvage = new ShapelessRecipeDefinition("salvage", "salvage", 1,
                List.of("void_sword"));

        RecipeCostTable table = new RecipeCostTable(List.of(salvage), List.of(item("void_sword", "NETHERITE_SWORD")));

        assertEquals(MaterialTier.ingredientCost("NETHERITE_SWORD"), table.costOf("salvage").getAsDouble());
    }

    @Test
    void twoRecipesForTheSameResultAreCostedAtTheCheaperOne() {
        ShapelessRecipeDefinition expensive = new ShapelessRecipeDefinition("a", "void_sword", 1,
                List.of("NETHERITE_INGOT", "NETHERITE_INGOT"));
        ShapelessRecipeDefinition cheap = new ShapelessRecipeDefinition("b", "void_sword", 1, List.of("STICK"));

        RecipeCostTable table = new RecipeCostTable(List.of(expensive, cheap), List.of());

        assertEquals(MaterialTier.ingredientCost("STICK"), table.costOf("void_sword").getAsDouble());
    }

    @Test
    void aRecipeCycleTerminatesInsteadOfRecursingForever() {
        ShapelessRecipeDefinition aFromB = new ShapelessRecipeDefinition("a", "a", 1, List.of("b"));
        ShapelessRecipeDefinition bFromA = new ShapelessRecipeDefinition("b", "b", 1, List.of("a"));

        RecipeCostTable table = new RecipeCostTable(List.of(aFromB, bFromA),
                List.of(item("a", "NETHERITE_SWORD"), item("b", "IRON_SWORD")));

        OptionalDouble cost = table.costOf("a");

        assertTrue(cost.isPresent());
        assertTrue(cost.getAsDouble() > 0);
    }

    private static ItemDefinition item(String id, String material) {
        return new ItemDefinition(id, material, 1, id, List.of(), List.of());
    }
}
