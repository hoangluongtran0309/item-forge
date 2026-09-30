package com.hoangluongtran0309.application;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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

/**
 * Runs a balance analysis over everything currently registered.
 *
 * <p>The deterministic rules always run. The AI is an addition on top and is allowed to fail:
 * a missing key, a rate limit or an outage costs the admin the narrative, never the rule
 * findings they would have had anyway.
 */
public class ItemBalanceAnalysisService {

    static final String AI_SUMMARY_RULE = "AI_SUMMARY";
    static final String AI_UNAVAILABLE_RULE = "AI_UNAVAILABLE";

    private final ItemRegistry itemRegistry;
    private final ArmorRegistry armorRegistry;
    private final RecipeRegistry recipeRegistry;
    private final BalanceRuleSet rules;
    // Read from the async thread that runs the analysis and replaced from the main thread on
    // /itemforge reload, hence volatile.
    private volatile AiBalanceAnalyzerPort aiPort;

    /**
     * @param aiPort null when ai.enabled is false in config.yml, matching how the item
     *        generation service is wired
     */
    public ItemBalanceAnalysisService(ItemRegistry itemRegistry, ArmorRegistry armorRegistry,
            RecipeRegistry recipeRegistry, BalanceRuleSet rules, AiBalanceAnalyzerPort aiPort) {
        this.itemRegistry = itemRegistry;
        this.armorRegistry = armorRegistry;
        this.recipeRegistry = recipeRegistry;
        this.rules = rules;
        this.aiPort = aiPort;
    }

    public boolean isAiEnabled() {
        return aiPort != null;
    }

    /**
     * Swaps the AI layer in place, so a changed ai section in config.yml takes effect on reload.
     *
     * @param aiPort null to fall back to the rules alone
     */
    public void useAiPort(AiBalanceAnalyzerPort aiPort) {
        this.aiPort = aiPort;
    }

    public BalanceReport analyzeAll() {
        return analyze(snapshot());
    }

    public BalanceReport analyzeTarget(String targetId) {
        if (itemRegistry.get(targetId).isEmpty() && armorRegistry.get(targetId).isEmpty()) {
            throw new UnknownBalanceTargetException(
                    "No item or armor piece is registered with the id '" + targetId + "'");
        }

        return analyze(snapshot().scopedTo(targetId));
    }

    private BalanceReport analyze(BalanceAnalysisRequest request) {
        List<BalanceFinding> ruleFindings = rules.evaluate(request);
        AiBalanceAnalyzerPort port = aiPort;
        if (port == null) {
            return BalanceReport.of(summaryOf(request, ruleFindings), ruleFindings);
        }

        List<BalanceFinding> combined = new ArrayList<>(ruleFindings);
        try {
            BalanceReport aiReport = port.analyze(request, ruleFindings);
            combined.addAll(withRuleDuplicatesRemoved(aiReport.findings(), ruleFindings));
            if (!aiReport.summary().isBlank()) {
                combined.add(new BalanceFinding(BalanceFinding.WHOLE_CONFIG, BalanceSeverity.INFO, AI_SUMMARY_RULE,
                        aiReport.summary(), "", FindingSource.AI));
            }
        } catch (RuntimeException e) {
            combined.add(BalanceFinding.rule(BalanceFinding.WHOLE_CONFIG, BalanceSeverity.INFO, AI_UNAVAILABLE_RULE,
                    "The AI analysis could not be completed: " + e.getMessage(),
                    "The rule findings above are unaffected. Check the ai section of config.yml."));
        }

        return BalanceReport.of(summaryOf(request, combined), combined);
    }

    /**
     * Drops AI findings about a target the rules already reported on.
     *
     * <p>Models restate a rule finding in their own words however firmly the prompt asks them
     * not to, and they pick their own severity when they do -- which is how "this item has no
     * recipe" ends up in the report twice, INFO from the rules and CRITICAL from the model. The
     * rules are the reproducible half, so they own every target they speak about, and the AI
     * keeps what it is actually for: findings about untouched targets and about the config as a
     * whole, which is where a cross-cutting observation belongs anyway.
     */
    private static List<BalanceFinding> withRuleDuplicatesRemoved(List<BalanceFinding> aiFindings,
            List<BalanceFinding> ruleFindings) {
        Set<String> coveredByRules = ruleFindings.stream()
                .map(BalanceFinding::targetId)
                .collect(Collectors.toSet());

        return aiFindings.stream()
                .filter(finding -> finding.targetId().equals(BalanceFinding.WHOLE_CONFIG)
                        || !coveredByRules.contains(finding.targetId()))
                .toList();
    }

    private BalanceAnalysisRequest snapshot() {
        // The registries hand back a live view of their backing map, so copy before analyzing.
        return BalanceAnalysisRequest.wholeConfig(
                List.copyOf(itemRegistry.getAll()),
                List.copyOf(armorRegistry.getAll()),
                List.copyOf(recipeRegistry.getAll()));
    }

    private static String summaryOf(BalanceAnalysisRequest request, List<BalanceFinding> findings) {
        BalanceReport report = new BalanceReport("", findings);
        String scope = request.isScoped()
                ? "'" + request.targetId() + "'"
                : request.items().size() + " item(s) and " + request.armor().size() + " armor piece(s)";

        return "Analyzed " + scope + ": " + report.countOf(BalanceSeverity.CRITICAL) + " critical, "
                + report.countOf(BalanceSeverity.WARNING) + " warning, "
                + report.countOf(BalanceSeverity.INFO) + " info.";
    }
}
