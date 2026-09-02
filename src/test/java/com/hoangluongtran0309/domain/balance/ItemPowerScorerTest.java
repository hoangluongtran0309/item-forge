package com.hoangluongtran0309.domain.balance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.hoangluongtran0309.domain.model.AbilityDefinition;
import com.hoangluongtran0309.domain.model.DamageBonusAbilityDefinition;
import com.hoangluongtran0309.domain.model.EffectCommand;
import com.hoangluongtran0309.domain.model.ItemDefinition;
import com.hoangluongtran0309.domain.model.PotionEffectAbilityDefinition;
import com.hoangluongtran0309.domain.model.TriggerType;

class ItemPowerScorerTest {

    private final ItemPowerScorer scorer = new ItemPowerScorer();

    @Test
    void anItemWithNoAbilitiesScoresItsMaterialTier() {
        PowerScore score = scorer.score(item("NETHERITE_SWORD"));

        assertEquals(5.0, score.materialTier());
        assertEquals(0.0, score.abilityScore());
        assertEquals(5.0, score.total());
    }

    @Test
    void anUnknownMaterialContributesNothingAndIsFlaggedAsSuch() {
        PowerScore score = scorer.score(item("FEATHER"));

        assertEquals(0.0, score.materialTier());
        assertFalse(score.hasKnownMaterial());
    }

    @Test
    void aLongerEffectOnTheSameCooldownScoresHigher() {
        PowerScore brief = scorer.score(item("IRON_SWORD", speed(5, 60)));
        PowerScore sustained = scorer.score(item("IRON_SWORD", speed(30, 60)));

        assertTrue(sustained.total() > brief.total());
    }

    @Test
    void aStrongerEffectScoresHigherThanAWeakerOneAtTheSameUptime() {
        PowerScore nightVision = scorer.score(
                item("IRON_SWORD", potion(EffectCommand.EffectType.NIGHT_VISION, 30, 60)));
        PowerScore fireResistance = scorer.score(
                item("IRON_SWORD", potion(EffectCommand.EffectType.FIRE_RESISTANCE, 30, 60)));

        assertTrue(fireResistance.total() > nightVision.total());
    }

    @Test
    void uptimeRatioIsDurationOverCooldown() {
        assertEquals(0.5, ItemPowerScorer.uptimeRatio(speed(30, 60)));
        assertEquals(2.0, ItemPowerScorer.uptimeRatio(speed(60, 30)));
    }

    @Test
    void aZeroCooldownCountsAsPermanentRatherThanDividingByZero() {
        double ratio = ItemPowerScorer.uptimeRatio(speed(10, 0));

        assertTrue(ratio >= 1.0);
        assertTrue(Double.isFinite(ratio));
    }

    @Test
    void maxUptimeRatioReportsTheStrongestAbilityNotTheLast() {
        PowerScore score = scorer.score(item("IRON_SWORD", speed(60, 30), speed(1, 60)));

        assertEquals(2.0, score.maxUptimeRatio());
    }

    @Test
    void aDamageBonusRaisesTheScoreInProportionToItsPercentage() {
        PowerScore small = scorer.score(item("IRON_SWORD", damageBonus(10)));
        PowerScore large = scorer.score(item("IRON_SWORD", damageBonus(100)));

        assertTrue(large.abilityScore() > small.abilityScore());
        assertEquals(2.0, large.abilityScore());
    }

    private static ItemDefinition item(String material, AbilityDefinition... abilities) {
        return new ItemDefinition("sample", material, 1, "Sample", List.of(), List.of(abilities));
    }

    private static PotionEffectAbilityDefinition speed(int durationSeconds, int cooldownSeconds) {
        return potion(EffectCommand.EffectType.SPEED, durationSeconds, cooldownSeconds);
    }

    private static PotionEffectAbilityDefinition potion(EffectCommand.EffectType effect, int durationSeconds,
            int cooldownSeconds) {
        return new PotionEffectAbilityDefinition(TriggerType.RIGHT_CLICK, effect, durationSeconds, cooldownSeconds);
    }

    private static DamageBonusAbilityDefinition damageBonus(double bonusPercent) {
        return new DamageBonusAbilityDefinition(TriggerType.ON_HIT, 30, bonusPercent);
    }
}
