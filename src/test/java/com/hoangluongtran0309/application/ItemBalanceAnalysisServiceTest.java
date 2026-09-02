package com.hoangluongtran0309.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.hoangluongtran0309.application.exception.AiBalanceAnalysisException;
import com.hoangluongtran0309.application.exception.UnknownBalanceTargetException;
import com.hoangluongtran0309.application.port.AiBalanceAnalyzerPort;
import com.hoangluongtran0309.domain.ArmorRegistry;
import com.hoangluongtran0309.domain.ItemRegistry;
import com.hoangluongtran0309.domain.RecipeRegistry;
import com.hoangluongtran0309.domain.balance.BalanceAnalysisRequest;
import com.hoangluongtran0309.domain.balance.BalanceFinding;
import com.hoangluongtran0309.domain.balance.BalanceReport;
import com.hoangluongtran0309.domain.balance.BalanceRuleSet;
import com.hoangluongtran0309.domain.balance.BalanceSeverity;
import com.hoangluongtran0309.domain.balance.FindingSource;
import com.hoangluongtran0309.domain.model.ArmorDefinition;
import com.hoangluongtran0309.domain.model.ArmorSlot;
import com.hoangluongtran0309.domain.model.EffectCommand;
import com.hoangluongtran0309.domain.model.ItemDefinition;
import com.hoangluongtran0309.domain.model.PotionEffectAbilityDefinition;
import com.hoangluongtran0309.domain.model.ShapelessRecipeDefinition;
import com.hoangluongtran0309.domain.model.TriggerType;

class ItemBalanceAnalysisServiceTest {

    private ItemRegistry itemRegistry;
    private ArmorRegistry armorRegistry;
    private RecipeRegistry recipeRegistry;

    @BeforeEach
    void setUp() {
        itemRegistry = new ItemRegistry();
        armorRegistry = new ArmorRegistry();
        recipeRegistry = new RecipeRegistry();
    }

    @Test
    void ruleFindingsAreProducedWithoutAnAiProvider() {
        itemRegistry.register(permanentSpeedSword());

        ItemBalanceAnalysisService service = service(null);

        assertFalse(service.isAiEnabled());
        BalanceReport report = service.analyzeAll();
        assertTrue(report.findings().stream()
                .anyMatch(finding -> finding.rule().equals("PERMANENT_EFFECT")));
        assertTrue(report.findings().stream().allMatch(finding -> finding.source() == FindingSource.RULE));
    }

    @Test
    void theSummaryCountsFindingsBySeverity() {
        itemRegistry.register(permanentSpeedSword());

        BalanceReport report = service(null).analyzeAll();

        assertEquals("Analyzed 1 item(s) and 0 armor piece(s): 1 critical, 0 warning, 1 info.", report.summary());
    }

    @Test
    void findingsAreOrderedMostSevereFirst() {
        itemRegistry.register(permanentSpeedSword());

        BalanceReport report = service(null).analyzeAll();

        assertEquals(BalanceSeverity.CRITICAL, report.findings().get(0).severity());
    }

    @Test
    void aiFindingsAreAppendedToTheRuleFindings() {
        itemRegistry.register(permanentSpeedSword());
        FakeAiBalanceAnalyzerPort aiPort = new FakeAiBalanceAnalyzerPort(BalanceReport.of("Overall solid.",
                List.of(new BalanceFinding(BalanceFinding.WHOLE_CONFIG, BalanceSeverity.WARNING, "THEME",
                        "The set feels generic.", "Give it a hook.", FindingSource.AI))));

        BalanceReport report = service(aiPort).analyzeAll();

        assertTrue(report.findings().stream()
                .anyMatch(finding -> finding.rule().equals("PERMANENT_EFFECT")));
        assertTrue(report.findings().stream().anyMatch(finding -> finding.rule().equals("THEME")));
    }

    @Test
    void theAiNarrativeSummaryArrivesAsItsOwnFindingSoTheCountsStayMachineDerived() {
        itemRegistry.register(permanentSpeedSword());
        FakeAiBalanceAnalyzerPort aiPort = new FakeAiBalanceAnalyzerPort(
                BalanceReport.of("The set is coherent but the sword is too strong.", List.of()));

        BalanceReport report = service(aiPort).analyzeAll();

        BalanceFinding summary = report.findings().stream()
                .filter(finding -> finding.rule().equals(ItemBalanceAnalysisService.AI_SUMMARY_RULE))
                .findFirst()
                .orElseThrow();
        assertEquals(FindingSource.AI, summary.source());
        assertEquals(BalanceFinding.WHOLE_CONFIG, summary.targetId());
        assertTrue(report.summary().startsWith("Analyzed 1 item(s)"));
    }

    @Test
    void anAiFindingAboutATargetTheRulesAlreadyCoveredIsDiscarded() {
        itemRegistry.register(permanentSpeedSword());
        // What a model actually returns: the same problem, reworded, at its own severity.
        FakeAiBalanceAnalyzerPort aiPort = new FakeAiBalanceAnalyzerPort(BalanceReport.of("",
                List.of(new BalanceFinding("void_sword", BalanceSeverity.CRITICAL, "",
                        "Void Sword has a permanent effect because SPEED lasts 60s on a 30s cooldown.",
                        "Increase the cooldown.", FindingSource.AI))));

        BalanceReport report = service(aiPort).analyzeAll();

        assertTrue(report.findings().stream().noneMatch(finding -> finding.source() == FindingSource.AI
                && finding.targetId().equals("void_sword")));
        assertTrue(report.findings().stream()
                .anyMatch(finding -> finding.rule().equals("PERMANENT_EFFECT")));
    }

    @Test
    void anAiFindingAboutAnUntouchedTargetIsKept() {
        itemRegistry.register(permanentSpeedSword());
        itemRegistry.register(new ItemDefinition("void_axe", "NETHERITE_AXE", 2, "Axe", List.of(),
                List.of(new PotionEffectAbilityDefinition(TriggerType.RIGHT_CLICK,
                        EffectCommand.EffectType.SPEED, 1, 600))));
        recipeRegistry.register(new ShapelessRecipeDefinition("void_axe", "void_axe", 1,
                List.of("NETHERITE_INGOT", "NETHERITE_INGOT")));
        FakeAiBalanceAnalyzerPort aiPort = new FakeAiBalanceAnalyzerPort(BalanceReport.of("",
                List.of(new BalanceFinding("void_axe", BalanceSeverity.WARNING, "THEME",
                        "Nothing about the axe justifies its price.", "Give it an ability.",
                        FindingSource.AI))));

        BalanceReport report = service(aiPort).analyzeAll();

        assertTrue(report.findings().stream().anyMatch(finding -> finding.rule().equals("THEME")));
    }

    @Test
    void anAiFindingAboutTheWholeConfigIsAlwaysKept() {
        itemRegistry.register(permanentSpeedSword());
        FakeAiBalanceAnalyzerPort aiPort = new FakeAiBalanceAnalyzerPort(BalanceReport.of("",
                List.of(new BalanceFinding(BalanceFinding.WHOLE_CONFIG, BalanceSeverity.WARNING, "PROGRESSION",
                        "Every item sits at the top tier, so there is no progression.",
                        "Add lower-tier gear.", FindingSource.AI))));

        BalanceReport report = service(aiPort).analyzeAll();

        assertTrue(report.findings().stream().anyMatch(finding -> finding.rule().equals("PROGRESSION")));
    }

    @Test
    void theRuleFindingsSurviveAnAiFailure() {
        itemRegistry.register(permanentSpeedSword());
        AiBalanceAnalyzerPort failing = (request, ruleFindings) -> {
            throw new AiBalanceAnalysisException("Claude API returned HTTP 401");
        };

        BalanceReport report = service(failing).analyzeAll();

        assertTrue(report.findings().stream()
                .anyMatch(finding -> finding.rule().equals("PERMANENT_EFFECT")));
        BalanceFinding notice = report.findings().stream()
                .filter(finding -> finding.rule().equals(ItemBalanceAnalysisService.AI_UNAVAILABLE_RULE))
                .findFirst()
                .orElseThrow();
        assertTrue(notice.issue().contains("HTTP 401"));
    }

    @Test
    void theRuleFindingsArePassedToTheAiAsContext() {
        itemRegistry.register(permanentSpeedSword());
        FakeAiBalanceAnalyzerPort aiPort = new FakeAiBalanceAnalyzerPort(BalanceReport.of("", List.of()));

        service(aiPort).analyzeAll();

        assertTrue(aiPort.receivedRuleFindings.stream()
                .anyMatch(finding -> finding.rule().equals("PERMANENT_EFFECT")));
    }

    @Test
    void analyzingOneItemReportsOnlyThatItemButStillSeesTheWholeConfig() {
        itemRegistry.register(permanentSpeedSword());
        itemRegistry.register(new ItemDefinition("void_axe", "NETHERITE_AXE", 2, "Axe", List.of(), List.of()));
        FakeAiBalanceAnalyzerPort aiPort = new FakeAiBalanceAnalyzerPort(BalanceReport.of("", List.of()));

        BalanceReport report = service(aiPort).analyzeTarget("void_sword");

        assertTrue(report.findings().stream().allMatch(finding -> finding.targetId().equals("void_sword")));
        assertEquals(2, aiPort.receivedRequest.items().size());
        assertEquals("void_sword", aiPort.receivedRequest.targetId());
    }

    @Test
    void analyzingAnArmorPieceByIdIsAllowed() {
        armorRegistry.register(new ArmorDefinition("void_helmet", "NETHERITE_HELMET", ArmorSlot.HELMET,
                "void_armor", 1, "Helmet", List.of()));

        BalanceReport report = service(null).analyzeTarget("void_helmet");

        assertTrue(report.summary().contains("'void_helmet'"));
    }

    @Test
    void analyzingAnUnknownIdFails() {
        UnknownBalanceTargetException exception = assertThrows(UnknownBalanceTargetException.class,
                () -> service(null).analyzeTarget("nope"));

        assertTrue(exception.getMessage().contains("nope"));
    }

    private ItemBalanceAnalysisService service(AiBalanceAnalyzerPort aiPort) {
        return new ItemBalanceAnalysisService(itemRegistry, armorRegistry, recipeRegistry, new BalanceRuleSet(),
                aiPort);
    }

    private static ItemDefinition permanentSpeedSword() {
        return new ItemDefinition("void_sword", "NETHERITE_SWORD", 1, "Void Sword", List.of(),
                List.of(new PotionEffectAbilityDefinition(TriggerType.RIGHT_CLICK,
                        EffectCommand.EffectType.SPEED, 60, 30)));
    }

    private static final class FakeAiBalanceAnalyzerPort implements AiBalanceAnalyzerPort {

        private final BalanceReport response;
        private BalanceAnalysisRequest receivedRequest;
        private List<BalanceFinding> receivedRuleFindings = List.of();

        private FakeAiBalanceAnalyzerPort(BalanceReport response) {
            this.response = response;
        }

        @Override
        public BalanceReport analyze(BalanceAnalysisRequest request, List<BalanceFinding> ruleFindings) {
            this.receivedRequest = request;
            this.receivedRuleFindings = ruleFindings;
            return response;
        }
    }
}
