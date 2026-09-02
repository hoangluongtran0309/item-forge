package com.hoangluongtran0309.domain.balance;

import java.util.Locale;
import java.util.Map;

/**
 * Reads a vanilla material name as a progression tier and a gear category.
 *
 * <p>Definitions store the material as a plain String precisely so the domain stays free of
 * Bukkit, which means the vanilla stats behind a name are not available here. Minecraft's naming
 * is regular enough to work from instead: the prefix of {@code NETHERITE_SWORD} says where it
 * sits in the progression and the suffix says what it is. A name that fits neither pattern
 * scores {@link #UNKNOWN_TIER}, and the rules then decline to judge it rather than guessing.
 */
public final class MaterialTier {

    public static final int UNKNOWN_TIER = 0;
    public static final int MAX_TIER = 5;

    private static final Map<String, Integer> GEAR_PREFIX_TIERS = Map.of(
            "WOODEN", 1,
            "LEATHER", 1,
            "STONE", 2,
            "GOLDEN", 2,
            "CHAINMAIL", 2,
            "IRON", 3,
            "DIAMOND", 4,
            "NETHERITE", 5,
            "TURTLE", 3);

    // Crafting inputs an admin realistically uses, priced on the same 1-5 progression scale as
    // the gear tiers so a recipe's cost can be compared against the power of what it produces.
    private static final Map<String, Double> INGREDIENT_COSTS = Map.ofEntries(
            Map.entry("NETHERITE_INGOT", 5.0),
            Map.entry("NETHERITE_SCRAP", 4.0),
            Map.entry("ANCIENT_DEBRIS", 4.0),
            Map.entry("DIAMOND", 4.0),
            Map.entry("EMERALD", 3.5),
            Map.entry("IRON_INGOT", 3.0),
            Map.entry("GOLD_INGOT", 2.0),
            Map.entry("COPPER_INGOT", 1.5),
            Map.entry("LEATHER", 1.0),
            Map.entry("REDSTONE", 0.5),
            Map.entry("LAPIS_LAZULI", 0.5),
            Map.entry("STRING", 0.3),
            Map.entry("FLINT", 0.3),
            Map.entry("COBBLESTONE", 0.2),
            Map.entry("STONE", 0.2),
            Map.entry("STICK", 0.1));

    private static final double DEFAULT_INGREDIENT_COST = 1.0;

    // A block of a resource is nine of it, so pricing one as such keeps a "craft it from blocks"
    // recipe from reading as cheap.
    private static final double BLOCK_MULTIPLIER = 9.0;

    private MaterialTier() {
    }

    /**
     * @return 1-5 for a recognized tool or armor material, {@link #UNKNOWN_TIER} otherwise
     */
    public static int gearTier(String material) {
        if (material == null || material.isBlank()) {
            return UNKNOWN_TIER;
        }

        String name = material.toUpperCase(Locale.ROOT);
        if (categoryOf(name) == MaterialCategory.OTHER) {
            return UNKNOWN_TIER;
        }

        int underscore = name.indexOf('_');
        if (underscore < 0) {
            return UNKNOWN_TIER;
        }

        return GEAR_PREFIX_TIERS.getOrDefault(name.substring(0, underscore), UNKNOWN_TIER);
    }

    public static MaterialCategory categoryOf(String material) {
        if (material == null) {
            return MaterialCategory.OTHER;
        }

        String name = material.toUpperCase(Locale.ROOT);
        if (name.endsWith("_SWORD")) {
            return MaterialCategory.SWORD;
        }
        if (name.endsWith("_PICKAXE") || name.endsWith("_AXE") || name.endsWith("_SHOVEL")
                || name.endsWith("_HOE")) {
            return MaterialCategory.TOOL;
        }
        if (name.endsWith("_HELMET") || name.endsWith("_CHESTPLATE") || name.endsWith("_LEGGINGS")
                || name.endsWith("_BOOTS")) {
            return MaterialCategory.ARMOR;
        }
        return MaterialCategory.OTHER;
    }

    /**
     * What one unit of this material is worth as a crafting ingredient. Unknown names are priced
     * at {@link #DEFAULT_INGREDIENT_COST} rather than zero, so an unrecognized ingredient never
     * makes a recipe look free.
     */
    public static double ingredientCost(String material) {
        if (material == null || material.isBlank()) {
            return DEFAULT_INGREDIENT_COST;
        }

        String name = material.toUpperCase(Locale.ROOT);
        Double known = INGREDIENT_COSTS.get(name);
        if (known != null) {
            return known;
        }

        int gearTier = gearTier(name);
        if (gearTier != UNKNOWN_TIER) {
            return gearTier;
        }

        if (name.endsWith("_BLOCK")) {
            String resource = name.substring(0, name.length() - "_BLOCK".length());
            Double resourceCost = INGREDIENT_COSTS.get(resource);
            if (resourceCost == null) {
                resourceCost = INGREDIENT_COSTS.get(resource + "_INGOT");
            }
            if (resourceCost != null) {
                return resourceCost * BLOCK_MULTIPLIER;
            }
        }

        return DEFAULT_INGREDIENT_COST;
    }
}
