package com.hoangluongtran0309.domain.balance;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.Set;

import com.hoangluongtran0309.domain.model.AbilityDefinition;
import com.hoangluongtran0309.domain.model.ArmorDefinition;
import com.hoangluongtran0309.domain.model.DamageBonusAbilityDefinition;
import com.hoangluongtran0309.domain.model.ItemDefinition;
import com.hoangluongtran0309.domain.model.PotionEffectAbilityDefinition;
import com.hoangluongtran0309.domain.model.TriggerType;

/**
 * The deterministic half of the balance analysis: every finding here is reproducible, costs
 * nothing to produce, and holds whether or not an AI provider is configured.
 */
public class BalanceRuleSet {

    // Above this, an effect is available often enough that players will plan around having it.
    private static final double HIGH_UPTIME_RATIO = 0.5;

    private static final double DAMAGE_BONUS_WARNING_PERCENT = 50.0;
    private static final double DAMAGE_BONUS_CRITICAL_PERCENT = 100.0;

    // An item is called cheap when its power is more than twice what it costs to craft. Both
    // sides are on the same 1-5-per-tier scale, so the comparison is meaningful.
    private static final double CHEAP_POWER_TO_COST_RATIO = 2.0;

    // Below this there is not enough power to be worth complaining about however cheap it is.
    private static final double CHEAP_MINIMUM_POWER = 3.0;

    private static final double OUTLIER_MULTIPLE_OF_MEDIAN = 2.0;

    // Comparing against a median needs enough peers for that median to mean anything.
    private static final int MIN_PEERS_FOR_OUTLIER = 3;

    // Documented v1.0.0 gaps: only RIGHT_CLICK and LEFT_CLICK are dispatched at runtime.
    private static final Set<TriggerType> UNDISPATCHED_TRIGGERS = EnumSet.of(
            TriggerType.ON_HIT, TriggerType.ON_KILL, TriggerType.ON_CONSUME);

    private final ItemPowerScorer scorer;

    public BalanceRuleSet() {
        this(new ItemPowerScorer());
    }

    public BalanceRuleSet(ItemPowerScorer scorer) {
        this.scorer = scorer;
    }

    public List<BalanceFinding> evaluate(BalanceAnalysisRequest request) {
        List<BalanceFinding> findings = new ArrayList<>();
        RecipeCostTable costs = new RecipeCostTable(request.recipes(), request.items());

        // Scored across every item, not only the ones being reported on, so that narrowing the
        // report to one id does not change what counts as an outlier.
        Map<String, PowerScore> scores = new HashMap<>();
        request.items().forEach(item -> scores.put(item.id(), scorer.score(item)));
        Map<MaterialCategory, Double> medians = mediansByCategory(request.items(), scores);

        for (ItemDefinition item : request.items()) {
            if (!request.covers(item.id())) {
                continue;
            }
            checkAbilities(item, findings);
            checkObtainability(item.id(), "Item", costs, findings);
            checkPriceAgainstPower(item, scores.get(item.id()), costs, findings);
            checkOutlier(item, scores.get(item.id()), medians, findings);
        }

        for (ArmorDefinition piece : request.armor()) {
            if (request.covers(piece.id())) {
                checkObtainability(piece.id(), "Armor piece", costs, findings);
            }
        }

        checkArmorSets(request, findings);

        return findings;
    }

    private void checkAbilities(ItemDefinition item, List<BalanceFinding> findings) {
        for (AbilityDefinition ability : item.abilities()) {
            checkCooldown(item, ability, findings);
            checkInertAbility(item, ability, findings);

            switch (ability) {
                case PotionEffectAbilityDefinition potion -> checkUptime(item, potion, findings);
                case DamageBonusAbilityDefinition damage -> checkDamageBonus(item, damage, findings);
            }
        }
    }

    private void checkCooldown(ItemDefinition item, AbilityDefinition ability, List<BalanceFinding> findings) {
        if (ability.cooldownSeconds() > 0) {
            return;
        }

        findings.add(BalanceFinding.rule(item.id(), BalanceSeverity.WARNING, "NO_COOLDOWN",
                "The " + ability.trigger() + " ability has no cooldown, so it can be spammed every tick.",
                "Set cooldown-seconds to a positive value."));
    }

    private void checkUptime(ItemDefinition item, PotionEffectAbilityDefinition potion,
            List<BalanceFinding> findings) {
        if (potion.cooldownSeconds() <= 0) {
            // Already reported as NO_COOLDOWN; saying it twice adds nothing.
            return;
        }

        double ratio = ItemPowerScorer.uptimeRatio(potion);
        if (ratio >= 1.0) {
            findings.add(BalanceFinding.rule(item.id(), BalanceSeverity.CRITICAL, "PERMANENT_EFFECT",
                    potion.effectType() + " lasts " + potion.durationSeconds() + "s on a "
                            + potion.cooldownSeconds() + "s cooldown, so it is permanently active.",
                    "Raise cooldown-seconds above duration-seconds, or shorten the effect."));
            return;
        }

        if (ratio > HIGH_UPTIME_RATIO) {
            findings.add(BalanceFinding.rule(item.id(), BalanceSeverity.WARNING, "HIGH_UPTIME",
                    potion.effectType() + " is active " + asPercent(ratio) + " of the time ("
                            + potion.durationSeconds() + "s every " + potion.cooldownSeconds() + "s).",
                    "Players will treat this as always-on; consider a longer cooldown."));
        }
    }

    private void checkDamageBonus(ItemDefinition item, DamageBonusAbilityDefinition damage,
            List<BalanceFinding> findings) {
        if (damage.bonusPercent() > DAMAGE_BONUS_CRITICAL_PERCENT) {
            findings.add(BalanceFinding.rule(item.id(), BalanceSeverity.CRITICAL, "EXCESSIVE_DAMAGE_BONUS",
                    "A damage bonus of " + trimZero(damage.bonusPercent()) + "% more than doubles the base damage.",
                    "Bring bonus-percent under " + trimZero(DAMAGE_BONUS_CRITICAL_PERCENT) + "%."));
            return;
        }

        if (damage.bonusPercent() > DAMAGE_BONUS_WARNING_PERCENT) {
            findings.add(BalanceFinding.rule(item.id(), BalanceSeverity.WARNING, "EXCESSIVE_DAMAGE_BONUS",
                    "A damage bonus of " + trimZero(damage.bonusPercent())
                            + "% is worth more than a full material tier.",
                    "Compare this against the vanilla gap between tiers before keeping it."));
        }
    }

    private void checkInertAbility(ItemDefinition item, AbilityDefinition ability, List<BalanceFinding> findings) {
        boolean inertTrigger = UNDISPATCHED_TRIGGERS.contains(ability.trigger());
        boolean inertType = ability instanceof DamageBonusAbilityDefinition;

        if (!inertTrigger && !inertType) {
            return;
        }

        String reason = inertTrigger
                ? "the " + ability.trigger() + " trigger is not dispatched at runtime"
                : "DAMAGE_BONUS is not applied at runtime";

        findings.add(BalanceFinding.rule(item.id(), BalanceSeverity.INFO, "INERT_ABILITY",
                "This ability never fires in game: " + reason + ".",
                "It loads without error but has no effect; use RIGHT_CLICK or LEFT_CLICK with "
                        + "POTION_EFFECT until this is implemented."));
    }

    private void checkObtainability(String id, String label, RecipeCostTable costs,
            List<BalanceFinding> findings) {
        if (costs.isCraftable(id)) {
            return;
        }

        findings.add(BalanceFinding.rule(id, BalanceSeverity.INFO, "UNOBTAINABLE",
                label + " '" + id + "' has no recipe, so it can only be obtained with /itemforge give.",
                "Add a recipe in recipes.yml if players are meant to craft it."));
    }

    private void checkPriceAgainstPower(ItemDefinition item, PowerScore score, RecipeCostTable costs,
            List<BalanceFinding> findings) {
        OptionalDouble cost = costs.costOf(item.id());
        if (cost.isEmpty() || score.total() < CHEAP_MINIMUM_POWER) {
            return;
        }

        if (score.total() > cost.getAsDouble() * CHEAP_POWER_TO_COST_RATIO) {
            findings.add(BalanceFinding.rule(item.id(), BalanceSeverity.WARNING, "CHEAP_FOR_POWER",
                    "Its recipe costs about " + trimZero(cost.getAsDouble()) + " tiers of material for "
                            + trimZero(score.total()) + " tiers of power.",
                    "Ask for scarcer ingredients, or lower the item's power."));
        }
    }

    private void checkOutlier(ItemDefinition item, PowerScore score, Map<MaterialCategory, Double> medians,
            List<BalanceFinding> findings) {
        if (!score.hasKnownMaterial()) {
            return;
        }

        MaterialCategory category = MaterialTier.categoryOf(item.material());
        Double median = medians.get(category);
        if (median == null || median <= 0) {
            return;
        }

        if (score.total() > median * OUTLIER_MULTIPLE_OF_MEDIAN) {
            findings.add(BalanceFinding.rule(item.id(), BalanceSeverity.WARNING, "POWER_OUTLIER",
                    "At " + trimZero(score.total()) + " it is more than twice as strong as the typical "
                            + category + " on this server (" + trimZero(median) + ").",
                    "Either bring it in line, or make it correspondingly harder to obtain."));
        }
    }

    private void checkArmorSets(BalanceAnalysisRequest request, List<BalanceFinding> findings) {
        Map<String, List<ArmorDefinition>> sets = new HashMap<>();
        for (ArmorDefinition piece : request.armor()) {
            sets.computeIfAbsent(piece.armorAssetId(), key -> new ArrayList<>()).add(piece);
        }

        sets.forEach((assetId, pieces) -> {
            if (pieces.size() < 2 || pieces.stream().noneMatch(piece -> request.covers(piece.id()))) {
                return;
            }

            Set<Integer> tiers = new HashSet<>();
            pieces.forEach(piece -> tiers.add(MaterialTier.gearTier(piece.material())));
            if (tiers.size() < 2) {
                return;
            }

            findings.add(BalanceFinding.rule(assetId, BalanceSeverity.INFO, "INCONSISTENT_ARMOR_SET",
                    "The pieces sharing armor-asset-id '" + assetId
                            + "' are built from different material tiers, so the set protects unevenly "
                            + "while looking like one set.",
                    "Use one material tier across the set, or split it into separate sets."));
        });
    }

    private static Map<MaterialCategory, Double> mediansByCategory(List<ItemDefinition> items,
            Map<String, PowerScore> scores) {
        Map<MaterialCategory, List<Double>> byCategory = new HashMap<>();
        for (ItemDefinition item : items) {
            PowerScore score = scores.get(item.id());
            if (score.hasKnownMaterial()) {
                byCategory.computeIfAbsent(MaterialTier.categoryOf(item.material()), key -> new ArrayList<>())
                        .add(score.total());
            }
        }

        Map<MaterialCategory, Double> medians = new HashMap<>();
        byCategory.forEach((category, values) -> {
            if (values.size() >= MIN_PEERS_FOR_OUTLIER) {
                medians.put(category, median(values));
            }
        });
        return medians;
    }

    private static double median(List<Double> values) {
        List<Double> sorted = new ArrayList<>(values);
        sorted.sort(null);
        int middle = sorted.size() / 2;
        return sorted.size() % 2 == 1
                ? sorted.get(middle)
                : (sorted.get(middle - 1) + sorted.get(middle)) / 2;
    }

    private static String asPercent(double ratio) {
        return Math.round(ratio * 100) + "%";
    }

    private static String trimZero(double value) {
        return value == Math.rint(value) ? String.valueOf((long) value) : String.format("%.1f", value);
    }
}
