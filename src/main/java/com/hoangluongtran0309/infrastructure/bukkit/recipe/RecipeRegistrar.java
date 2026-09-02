package com.hoangluongtran0309.infrastructure.bukkit.recipe;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.plugin.Plugin;

import com.hoangluongtran0309.domain.model.RecipeDefinition;
import com.hoangluongtran0309.domain.model.ShapedRecipeDefinition;
import com.hoangluongtran0309.domain.model.ShapelessRecipeDefinition;

public class RecipeRegistrar {

    private final Plugin plugin;
    private final RecipeIngredientResolver resolver;
    private final Logger logger;
    private final List<NamespacedKey> registeredKeys = new ArrayList<>();

    public RecipeRegistrar(Plugin plugin, RecipeIngredientResolver resolver, Logger logger) {
        this.plugin = plugin;
        this.resolver = resolver;
        this.logger = logger;
    }

    public void registerAll(Collection<RecipeDefinition> definitions) {
        for (RecipeDefinition definition : definitions) {
            try {
                Recipe recipe = buildRecipe(definition);
                plugin.getServer().addRecipe(recipe);
                registeredKeys.add(((Keyed) recipe).getKey());
            } catch (RuntimeException e) {
                logger.warning("Skipping recipe '" + definition.id() + "' due to an invalid configuration: "
                        + e.getMessage());
            }
        }
    }

    public void unregisterAll() {
        for (NamespacedKey key : registeredKeys) {
            plugin.getServer().removeRecipe(key);
        }
        registeredKeys.clear();
    }

    // Bukkit's addRecipe() throws IllegalStateException when a key is registered twice
    // without a removeRecipe() in between, so everything is unregistered before being
    // registered again -- that makes /itemforge reload safe to run repeatedly.
    public void reload(Collection<RecipeDefinition> definitions) {
        unregisterAll();
        registerAll(definitions);
    }

    private Recipe buildRecipe(RecipeDefinition definition) {
        return switch (definition) {
            case ShapedRecipeDefinition shaped -> buildShapedRecipe(shaped);
            case ShapelessRecipeDefinition shapeless -> buildShapelessRecipe(shapeless);
        };
    }

    private ShapedRecipe buildShapedRecipe(ShapedRecipeDefinition definition) {
        NamespacedKey key = new NamespacedKey(plugin, definition.id());
        ItemStack result = resolver.resolveItemStack(definition.resultId(), definition.resultCount())
                .orElseThrow(() -> new IllegalStateException("Unknown recipe result id: " + definition.resultId()));

        ShapedRecipe recipe = new ShapedRecipe(key, result);
        recipe.shape(definition.shape().toArray(new String[0]));

        for (Map.Entry<Character, String> entry : definition.ingredients().entrySet()) {
            RecipeChoice choice = resolver.resolveChoice(entry.getValue())
                    .orElseThrow(() -> new IllegalStateException("Unknown ingredient id: " + entry.getValue()));
            recipe.setIngredient(entry.getKey(), choice);
        }

        return recipe;
    }

    private ShapelessRecipe buildShapelessRecipe(ShapelessRecipeDefinition definition) {
        NamespacedKey key = new NamespacedKey(plugin, definition.id());
        ItemStack result = resolver.resolveItemStack(definition.resultId(), definition.resultCount())
                .orElseThrow(() -> new IllegalStateException("Unknown recipe result id: " + definition.resultId()));

        ShapelessRecipe recipe = new ShapelessRecipe(key, result);
        for (String ingredientId : definition.ingredientIds()) {
            RecipeChoice choice = resolver.resolveChoice(ingredientId)
                    .orElseThrow(() -> new IllegalStateException("Unknown ingredient id: " + ingredientId));
            recipe.addIngredient(choice);
        }

        return recipe;
    }
}
