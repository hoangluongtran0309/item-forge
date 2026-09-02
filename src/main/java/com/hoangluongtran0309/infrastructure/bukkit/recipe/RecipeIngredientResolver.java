package com.hoangluongtran0309.infrastructure.bukkit.recipe;

import java.util.Optional;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;

import com.hoangluongtran0309.domain.ArmorRegistry;
import com.hoangluongtran0309.domain.ItemRegistry;
import com.hoangluongtran0309.infrastructure.bukkit.ArmorStackFactory;
import com.hoangluongtran0309.infrastructure.bukkit.ItemStackFactory;

public class RecipeIngredientResolver {

    private final ItemRegistry itemRegistry;
    private final ArmorRegistry armorRegistry;
    private final ItemStackFactory itemStackFactory;
    private final ArmorStackFactory armorStackFactory;

    public RecipeIngredientResolver(ItemRegistry itemRegistry, ArmorRegistry armorRegistry,
            ItemStackFactory itemStackFactory, ArmorStackFactory armorStackFactory) {
        this.itemRegistry = itemRegistry;
        this.armorRegistry = armorRegistry;
        this.itemStackFactory = itemStackFactory;
        this.armorStackFactory = armorStackFactory;
    }

    public Optional<ItemStack> resolveItemStack(String id, int count) {
        var itemDefinition = itemRegistry.get(id);
        if (itemDefinition.isPresent()) {
            ItemStack stack = itemStackFactory.create(itemDefinition.get());
            stack.setAmount(count);
            return Optional.of(stack);
        }

        var armorDefinition = armorRegistry.get(id);
        if (armorDefinition.isPresent()) {
            ItemStack stack = armorStackFactory.create(armorDefinition.get());
            stack.setAmount(count);
            return Optional.of(stack);
        }

        Material material = Material.matchMaterial(id);
        if (material != null) {
            return Optional.of(new ItemStack(material, count));
        }

        return Optional.empty();
    }

    // Custom item/armor ids take precedence over vanilla materials, because a custom id is
    // the more specific match. ExactChoice preserves the exact custom-model-data/item-id
    // when the ingredient is shown in the crafting grid.
    public Optional<RecipeChoice> resolveChoice(String id) {
        var itemDefinition = itemRegistry.get(id);
        if (itemDefinition.isPresent()) {
            return Optional.of(new RecipeChoice.ExactChoice(itemStackFactory.create(itemDefinition.get())));
        }

        var armorDefinition = armorRegistry.get(id);
        if (armorDefinition.isPresent()) {
            return Optional.of(new RecipeChoice.ExactChoice(armorStackFactory.create(armorDefinition.get())));
        }

        Material material = Material.matchMaterial(id);
        if (material != null) {
            return Optional.of(new RecipeChoice.MaterialChoice(material));
        }

        return Optional.empty();
    }
}
