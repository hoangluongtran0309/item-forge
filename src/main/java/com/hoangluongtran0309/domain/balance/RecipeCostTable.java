package com.hoangluongtran0309.domain.balance;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.Set;

import com.hoangluongtran0309.domain.model.ItemDefinition;
import com.hoangluongtran0309.domain.model.RecipeDefinition;
import com.hoangluongtran0309.domain.model.ShapedRecipeDefinition;
import com.hoangluongtran0309.domain.model.ShapelessRecipeDefinition;

/**
 * What each craftable id costs to make, so an item's price can be weighed against its power.
 *
 * <p>An ingredient that is itself a custom item is priced by its own recipe, recursively, which
 * is what lets a "craft the strong sword from the weak sword" chain be costed correctly. A cycle
 * or an item with no recipe falls back to the material tier, so the table always terminates and
 * never reports a cost of zero it cannot justify.
 */
public class RecipeCostTable {

    private final Map<String, List<RecipeDefinition>> recipesByResult = new HashMap<>();
    private final Map<String, ItemDefinition> itemsById = new HashMap<>();

    public RecipeCostTable(Collection<RecipeDefinition> recipes, Collection<ItemDefinition> items) {
        for (RecipeDefinition recipe : recipes) {
            recipesByResult.computeIfAbsent(recipe.resultId(), key -> new ArrayList<>()).add(recipe);
        }
        for (ItemDefinition item : items) {
            itemsById.put(item.id(), item);
        }
    }

    public boolean isCraftable(String resultId) {
        return recipesByResult.containsKey(resultId);
    }

    /**
     * @return the cheapest way to craft this id, or empty when nothing produces it
     */
    public OptionalDouble costOf(String resultId) {
        return costOf(resultId, new HashSet<>());
    }

    private OptionalDouble costOf(String resultId, Set<String> beingPriced) {
        List<RecipeDefinition> recipes = recipesByResult.get(resultId);
        if (recipes == null || !beingPriced.add(resultId)) {
            return OptionalDouble.empty();
        }

        try {
            double cheapest = Double.MAX_VALUE;
            for (RecipeDefinition recipe : recipes) {
                cheapest = Math.min(cheapest, costOfRecipe(recipe, beingPriced));
            }
            return OptionalDouble.of(cheapest);
        } finally {
            beingPriced.remove(resultId);
        }
    }

    private double costOfRecipe(RecipeDefinition recipe, Set<String> beingPriced) {
        double total = switch (recipe) {
            case ShapedRecipeDefinition shaped -> shapedCost(shaped, beingPriced);
            case ShapelessRecipeDefinition shapeless -> shapeless.ingredientIds().stream()
                    .mapToDouble(ingredient -> ingredientCost(ingredient, beingPriced))
                    .sum();
        };

        return total / Math.max(recipe.resultCount(), 1);
    }

    private double shapedCost(ShapedRecipeDefinition shaped, Set<String> beingPriced) {
        double total = 0;
        for (String row : shaped.shape()) {
            for (char slot : row.toCharArray()) {
                if (slot == ' ') {
                    continue;
                }
                String ingredient = shaped.ingredients().get(slot);
                if (ingredient != null) {
                    total += ingredientCost(ingredient, beingPriced);
                }
            }
        }
        return total;
    }

    private double ingredientCost(String ingredientId, Set<String> beingPriced) {
        OptionalDouble craftedCost = costOf(ingredientId, beingPriced);
        if (craftedCost.isPresent()) {
            return craftedCost.getAsDouble();
        }

        ItemDefinition customItem = itemsById.get(ingredientId);
        if (customItem != null) {
            return MaterialTier.ingredientCost(customItem.material());
        }

        return MaterialTier.ingredientCost(ingredientId);
    }
}
