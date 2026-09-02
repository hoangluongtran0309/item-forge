package com.hoangluongtran0309.domain.balance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MaterialTierTest {

    @Test
    void gearTierFollowsTheVanillaProgression() {
        assertEquals(1, MaterialTier.gearTier("WOODEN_SWORD"));
        assertEquals(2, MaterialTier.gearTier("STONE_SWORD"));
        assertEquals(3, MaterialTier.gearTier("IRON_SWORD"));
        assertEquals(4, MaterialTier.gearTier("DIAMOND_SWORD"));
        assertEquals(5, MaterialTier.gearTier("NETHERITE_SWORD"));
    }

    @Test
    void gearTierCoversArmorPrefixesTooIncludingTheOnesThatDifferFromTools() {
        assertEquals(1, MaterialTier.gearTier("LEATHER_CHESTPLATE"));
        assertEquals(2, MaterialTier.gearTier("CHAINMAIL_CHESTPLATE"));
        assertEquals(5, MaterialTier.gearTier("NETHERITE_BOOTS"));
    }

    @Test
    void gearTierIsUnknownForSomethingThatIsNotGear() {
        assertEquals(MaterialTier.UNKNOWN_TIER, MaterialTier.gearTier("NETHERITE_INGOT"));
        assertEquals(MaterialTier.UNKNOWN_TIER, MaterialTier.gearTier("FEATHER"));
        assertEquals(MaterialTier.UNKNOWN_TIER, MaterialTier.gearTier("MADE_UP_SWORD"));
        assertEquals(MaterialTier.UNKNOWN_TIER, MaterialTier.gearTier(""));
        assertEquals(MaterialTier.UNKNOWN_TIER, MaterialTier.gearTier(null));
    }

    @Test
    void categoryComesFromTheSuffix() {
        assertEquals(MaterialCategory.SWORD, MaterialTier.categoryOf("NETHERITE_SWORD"));
        assertEquals(MaterialCategory.TOOL, MaterialTier.categoryOf("NETHERITE_PICKAXE"));
        assertEquals(MaterialCategory.TOOL, MaterialTier.categoryOf("IRON_SHOVEL"));
        assertEquals(MaterialCategory.ARMOR, MaterialTier.categoryOf("DIAMOND_LEGGINGS"));
        assertEquals(MaterialCategory.OTHER, MaterialTier.categoryOf("FEATHER"));
    }

    @Test
    void ingredientCostRanksCraftingMaterialsAgainstEachOther() {
        assertTrue(MaterialTier.ingredientCost("NETHERITE_INGOT") > MaterialTier.ingredientCost("DIAMOND"));
        assertTrue(MaterialTier.ingredientCost("DIAMOND") > MaterialTier.ingredientCost("IRON_INGOT"));
        assertTrue(MaterialTier.ingredientCost("IRON_INGOT") > MaterialTier.ingredientCost("STICK"));
    }

    @Test
    void ingredientCostOfAResourceBlockIsNineOfTheResource() {
        assertEquals(MaterialTier.ingredientCost("DIAMOND") * 9, MaterialTier.ingredientCost("DIAMOND_BLOCK"));
        assertEquals(MaterialTier.ingredientCost("IRON_INGOT") * 9, MaterialTier.ingredientCost("IRON_BLOCK"));
    }

    @Test
    void ingredientCostFallsBackToOneSoAnUnknownIngredientNeverLooksFree() {
        assertEquals(1.0, MaterialTier.ingredientCost("SOME_UNKNOWN_THING"));
        assertEquals(1.0, MaterialTier.ingredientCost(null));
    }

    @Test
    void ingredientCostOfGearIsItsOwnTier() {
        assertEquals(5.0, MaterialTier.ingredientCost("NETHERITE_SWORD"));
    }
}
