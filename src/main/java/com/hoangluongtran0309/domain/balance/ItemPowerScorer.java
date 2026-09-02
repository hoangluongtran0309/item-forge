package com.hoangluongtran0309.domain.balance;

import java.util.Map;

import com.hoangluongtran0309.domain.model.AbilityDefinition;
import com.hoangluongtran0309.domain.model.DamageBonusAbilityDefinition;
import com.hoangluongtran0309.domain.model.EffectCommand;
import com.hoangluongtran0309.domain.model.ItemDefinition;
import com.hoangluongtran0309.domain.model.PotionEffectAbilityDefinition;

/**
 * Puts a single number on an item so items can be compared with each other.
 *
 * <p>The number is not an absolute measure of anything -- it exists to make outliers visible.
 * An effect worth twice as much as another is weighted twice as heavily, an effect that is
 * available half the time counts half, and the base material contributes its progression tier.
 */
public class ItemPowerScorer {

    // Weights are relative to each other, not to any vanilla value. Fire resistance and
    // regeneration remove whole categories of threat, speed is strong but situational, night
    // vision is convenience, and slowness on the holder is a drawback rather than a gain.
    private static final Map<EffectCommand.EffectType, Double> EFFECT_WEIGHTS = Map.of(
            EffectCommand.EffectType.FIRE_RESISTANCE, 3.0,
            EffectCommand.EffectType.REGENERATION, 3.0,
            EffectCommand.EffectType.SPEED, 2.0,
            EffectCommand.EffectType.NIGHT_VISION, 1.0,
            EffectCommand.EffectType.SLOWNESS, 0.5);

    // An always-on effect is worth more than a half-time one, but not without limit: past
    // permanent uptime there is nothing further to gain.
    private static final double MAX_UPTIME_MULTIPLIER = 1.5;

    // 50% extra damage is worth about one material tier.
    private static final double DAMAGE_BONUS_DIVISOR = 50.0;

    public PowerScore score(ItemDefinition definition) {
        int materialTier = MaterialTier.gearTier(definition.material());

        double abilityScore = 0;
        double maxUptimeRatio = 0;

        for (AbilityDefinition ability : definition.abilities()) {
            switch (ability) {
                case PotionEffectAbilityDefinition potion -> {
                    double uptimeRatio = uptimeRatio(potion);
                    maxUptimeRatio = Math.max(maxUptimeRatio, uptimeRatio);
                    abilityScore += EFFECT_WEIGHTS.getOrDefault(potion.effectType(), 1.0)
                            * Math.min(uptimeRatio, MAX_UPTIME_MULTIPLIER);
                }
                case DamageBonusAbilityDefinition damage ->
                    abilityScore += damage.bonusPercent() / DAMAGE_BONUS_DIVISOR;
            }
        }

        return new PowerScore(materialTier + abilityScore, materialTier, abilityScore, maxUptimeRatio);
    }

    /**
     * How much of the time the effect is available. A cooldown of zero means it never stops, so
     * it is treated as permanent rather than as a division by zero.
     */
    public static double uptimeRatio(PotionEffectAbilityDefinition ability) {
        if (ability.cooldownSeconds() <= 0) {
            return MAX_UPTIME_MULTIPLIER;
        }
        return (double) ability.durationSeconds() / ability.cooldownSeconds();
    }
}
