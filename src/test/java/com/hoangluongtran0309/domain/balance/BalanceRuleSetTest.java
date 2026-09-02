package com.hoangluongtran0309.domain.balance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.hoangluongtran0309.domain.model.AbilityDefinition;
import com.hoangluongtran0309.domain.model.ArmorDefinition;
import com.hoangluongtran0309.domain.model.ArmorSlot;
import com.hoangluongtran0309.domain.model.DamageBonusAbilityDefinition;
import com.hoangluongtran0309.domain.model.EffectCommand;
import com.hoangluongtran0309.domain.model.ItemDefinition;
import com.hoangluongtran0309.domain.model.PotionEffectAbilityDefinition;
import com.hoangluongtran0309.domain.model.RecipeDefinition;
import com.hoangluongtran0309.domain.model.ShapedRecipeDefinition;
import com.hoangluongtran0309.domain.model.ShapelessRecipeDefinition;
import com.hoangluongtran0309.domain.model.TriggerType;

class BalanceRuleSetTest {

    private final BalanceRuleSet rules = new BalanceRuleSet();

    @Test
    void anEmptyConfigProducesNoFindings() {
        assertTrue(rules.evaluate(BalanceAnalysisRequest.wholeConfig(List.of(), List.of(), List.of())).isEmpty());
    }

    @Test
    void anEffectLastingAsLongAsItsCooldownIsCritical() {
        List<BalanceFinding> findings = evaluate(item("void_sword", "NETHERITE_SWORD", speed(60, 30)));

        BalanceFinding finding = findingFor(findings, "PERMANENT_EFFECT");
        assertEquals(BalanceSeverity.CRITICAL, finding.severity());
        assertEquals("void_sword", finding.targetId());
        assertTrue(finding.issue().contains("60s"));
        assertTrue(finding.issue().contains("30s"));
    }

    @Test
    void anEffectActiveMoreThanHalfTheTimeIsAWarningRatherThanCritical() {
        List<BalanceFinding> findings = evaluate(item("void_sword", "NETHERITE_SWORD", speed(20, 30)));

        assertEquals(BalanceSeverity.WARNING, findingFor(findings, "HIGH_UPTIME").severity());
        assertFalse(hasRule(findings, "PERMANENT_EFFECT"));
    }

    @Test
    void anEffectActiveLessThanHalfTheTimeIsNotReported() {
        List<BalanceFinding> findings = evaluate(item("void_sword", "NETHERITE_SWORD", speed(10, 60)));

        assertFalse(hasRule(findings, "HIGH_UPTIME"));
        assertFalse(hasRule(findings, "PERMANENT_EFFECT"));
    }

    @Test
    void aMissingCooldownIsReportedOnceRatherThanAlsoAsAPermanentEffect() {
        List<BalanceFinding> findings = evaluate(item("void_sword", "NETHERITE_SWORD", speed(10, 0)));

        assertEquals(BalanceSeverity.WARNING, findingFor(findings, "NO_COOLDOWN").severity());
        assertFalse(hasRule(findings, "PERMANENT_EFFECT"));
    }

    @Test
    void aDamageBonusOverAHundredPercentIsCriticalAndOverFiftyIsAWarning() {
        assertEquals(BalanceSeverity.CRITICAL,
                findingFor(evaluate(item("a", "NETHERITE_SWORD", damageBonus(150))), "EXCESSIVE_DAMAGE_BONUS")
                        .severity());
        assertEquals(BalanceSeverity.WARNING,
                findingFor(evaluate(item("b", "NETHERITE_SWORD", damageBonus(75))), "EXCESSIVE_DAMAGE_BONUS")
                        .severity());
        assertFalse(hasRule(evaluate(item("c", "NETHERITE_SWORD", damageBonus(20))), "EXCESSIVE_DAMAGE_BONUS"));
    }

    @Test
    void anAbilityOnAnUndispatchedTriggerIsReportedAsInert() {
        List<BalanceFinding> findings = evaluate(item("void_sword", "NETHERITE_SWORD",
                new PotionEffectAbilityDefinition(TriggerType.ON_KILL, EffectCommand.EffectType.SPEED, 5, 30)));

        BalanceFinding finding = findingFor(findings, "INERT_ABILITY");
        assertEquals(BalanceSeverity.INFO, finding.severity());
        assertTrue(finding.issue().contains("ON_KILL"));
    }

    @Test
    void aDamageBonusAbilityIsReportedAsInertEvenOnADispatchedTrigger() {
        List<BalanceFinding> findings = evaluate(item("void_sword", "NETHERITE_SWORD",
                new DamageBonusAbilityDefinition(TriggerType.RIGHT_CLICK, 30, 10)));

        assertTrue(findingFor(findings, "INERT_ABILITY").issue().contains("DAMAGE_BONUS"));
    }

    @Test
    void anAbilityOnADispatchedTriggerIsNotReportedAsInert() {
        assertFalse(hasRule(evaluate(item("void_sword", "NETHERITE_SWORD", speed(5, 30))), "INERT_ABILITY"));
    }

    @Test
    void anItemWithNoRecipeIsReportedAsUnobtainable() {
        List<BalanceFinding> findings = evaluate(item("void_sword", "NETHERITE_SWORD"));

        assertEquals(BalanceSeverity.INFO, findingFor(findings, "UNOBTAINABLE").severity());
    }

    @Test
    void anItemWithARecipeIsNotReportedAsUnobtainable() {
        BalanceAnalysisRequest request = new BalanceAnalysisRequest(
                List.of(item("void_sword", "NETHERITE_SWORD")), List.of(),
                List.of(recipe("void_sword", "NETHERITE_INGOT", "NETHERITE_INGOT")), "");

        assertFalse(hasRule(rules.evaluate(request), "UNOBTAINABLE"));
    }

    @Test
    void aStrongItemCraftedFromNothingIsReportedAsTooCheap() {
        BalanceAnalysisRequest request = new BalanceAnalysisRequest(
                List.of(item("void_sword", "NETHERITE_SWORD")), List.of(),
                List.of(recipe("void_sword", "STICK")), "");

        BalanceFinding finding = findingFor(rules.evaluate(request), "CHEAP_FOR_POWER");
        assertEquals(BalanceSeverity.WARNING, finding.severity());
    }

    @Test
    void aFairlyPricedItemIsNotReportedAsTooCheap() {
        BalanceAnalysisRequest request = new BalanceAnalysisRequest(
                List.of(item("void_sword", "NETHERITE_SWORD")), List.of(),
                List.of(recipe("void_sword", "NETHERITE_INGOT", "NETHERITE_INGOT")), "");

        assertFalse(hasRule(rules.evaluate(request), "CHEAP_FOR_POWER"));
    }

    @Test
    void anItemMoreThanTwiceAsStrongAsItsPeersIsReportedAsAnOutlier() {
        BalanceAnalysisRequest request = BalanceAnalysisRequest.wholeConfig(
                List.of(item("weak_a", "WOODEN_SWORD"),
                        item("weak_b", "WOODEN_SWORD"),
                        item("weak_c", "STONE_SWORD"),
                        item("monster", "NETHERITE_SWORD", speed(60, 30))),
                List.of(), List.of());

        BalanceFinding finding = findingFor(rules.evaluate(request), "POWER_OUTLIER");
        assertEquals("monster", finding.targetId());
    }

    @Test
    void outliersAreNotReportedWithoutEnoughPeersToCompareAgainst() {
        BalanceAnalysisRequest request = BalanceAnalysisRequest.wholeConfig(
                List.of(item("weak", "WOODEN_SWORD"), item("monster", "NETHERITE_SWORD", speed(60, 30))),
                List.of(), List.of());

        assertFalse(hasRule(rules.evaluate(request), "POWER_OUTLIER"));
    }

    @Test
    void anArmorSetBuiltFromMixedTiersIsReported() {
        BalanceAnalysisRequest request = BalanceAnalysisRequest.wholeConfig(List.of(),
                List.of(armor("void_helmet", "NETHERITE_HELMET", ArmorSlot.HELMET, "void_armor"),
                        armor("void_boots", "LEATHER_BOOTS", ArmorSlot.BOOTS, "void_armor")),
                List.of());

        BalanceFinding finding = findingFor(rules.evaluate(request), "INCONSISTENT_ARMOR_SET");
        assertEquals("void_armor", finding.targetId());
    }

    @Test
    void anArmorSetBuiltFromOneTierIsNotReported() {
        BalanceAnalysisRequest request = BalanceAnalysisRequest.wholeConfig(List.of(),
                List.of(armor("void_helmet", "NETHERITE_HELMET", ArmorSlot.HELMET, "void_armor"),
                        armor("void_boots", "NETHERITE_BOOTS", ArmorSlot.BOOTS, "void_armor")),
                List.of());

        assertFalse(hasRule(rules.evaluate(request), "INCONSISTENT_ARMOR_SET"));
    }

    @Test
    void scopingToOneIdReportsOnlyThatId() {
        BalanceAnalysisRequest request = BalanceAnalysisRequest
                .wholeConfig(List.of(item("void_sword", "NETHERITE_SWORD", speed(60, 30)),
                        item("void_axe", "NETHERITE_AXE", speed(60, 30))), List.of(), List.of())
                .scopedTo("void_sword");

        List<BalanceFinding> findings = rules.evaluate(request);

        assertFalse(findings.isEmpty());
        assertTrue(findings.stream().allMatch(finding -> finding.targetId().equals("void_sword")));
    }

    private List<BalanceFinding> evaluate(ItemDefinition item) {
        return rules.evaluate(BalanceAnalysisRequest.wholeConfig(List.of(item), List.of(), List.of()));
    }

    private static boolean hasRule(List<BalanceFinding> findings, String rule) {
        return findings.stream().anyMatch(finding -> finding.rule().equals(rule));
    }

    private static BalanceFinding findingFor(List<BalanceFinding> findings, String rule) {
        return findings.stream()
                .filter(finding -> finding.rule().equals(rule))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No " + rule + " finding in " + findings));
    }

    private static ItemDefinition item(String id, String material, AbilityDefinition... abilities) {
        return new ItemDefinition(id, material, 1, id, List.of(), List.of(abilities));
    }

    private static ArmorDefinition armor(String id, String material, ArmorSlot slot, String assetId) {
        return new ArmorDefinition(id, material, slot, assetId, 1, id, List.of());
    }

    private static RecipeDefinition recipe(String resultId, String... ingredients) {
        return new ShapelessRecipeDefinition(resultId, resultId, 1, List.of(ingredients));
    }

    private static PotionEffectAbilityDefinition speed(int durationSeconds, int cooldownSeconds) {
        return new PotionEffectAbilityDefinition(TriggerType.RIGHT_CLICK, EffectCommand.EffectType.SPEED,
                durationSeconds, cooldownSeconds);
    }

    private static DamageBonusAbilityDefinition damageBonus(double bonusPercent) {
        return new DamageBonusAbilityDefinition(TriggerType.RIGHT_CLICK, 30, bonusPercent);
    }

    @Test
    void everyFindingCarriesItsRuleNameAndComesFromTheRuleSet() {
        List<BalanceFinding> findings = evaluate(item("void_sword", "NETHERITE_SWORD", speed(60, 30)));

        assertTrue(findings.stream().allMatch(finding -> !finding.rule().isBlank()));
        assertTrue(findings.stream().allMatch(finding -> finding.source() == FindingSource.RULE));
    }

    @Test
    void onlyTheSlotsAShapedRecipeActuallyUsesAreCharged() {
        // 'X' is declared but never appears in the shape, so its cost must not be counted.
        ShapedRecipeDefinition recipe = new ShapedRecipeDefinition("void_sword", "void_sword", 1,
                List.of("S"), Map.of('S', "STICK", 'X', "NETHERITE_BLOCK"));
        BalanceAnalysisRequest request = new BalanceAnalysisRequest(
                List.of(item("void_sword", "NETHERITE_SWORD")), List.of(), List.of(recipe), "");

        assertTrue(hasRule(rules.evaluate(request), "CHEAP_FOR_POWER"));
    }
}
