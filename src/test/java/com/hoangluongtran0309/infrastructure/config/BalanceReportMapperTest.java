package com.hoangluongtran0309.infrastructure.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.hoangluongtran0309.domain.balance.BalanceFinding;
import com.hoangluongtran0309.domain.balance.BalanceReport;
import com.hoangluongtran0309.domain.balance.BalanceSeverity;
import com.hoangluongtran0309.domain.balance.FindingSource;

class BalanceReportMapperTest {

    private final List<String> warnings = new ArrayList<>();

    @Test
    void fromMapReadsSummaryAndFindings() {
        BalanceReport report = BalanceReportMapper.fromMap(Map.of(
                "summary", "Two problems.",
                "findings", List.of(Map.of(
                        "target-id", "void_sword",
                        "severity", "CRITICAL",
                        "rule", "THEME",
                        "issue", "Too strong.",
                        "suggestion", "Weaken it."))),
                FindingSource.AI, warnings::add);

        assertEquals("Two problems.", report.summary());
        BalanceFinding finding = report.findings().get(0);
        assertEquals("void_sword", finding.targetId());
        assertEquals(BalanceSeverity.CRITICAL, finding.severity());
        assertEquals("THEME", finding.rule());
        assertEquals("Weaken it.", finding.suggestion());
        assertEquals(FindingSource.AI, finding.source());
        assertTrue(warnings.isEmpty());
    }

    @Test
    void anUnrecognizedSeverityBecomesInfoRatherThanLosingTheFinding() {
        BalanceReport report = BalanceReportMapper.fromMap(Map.of("findings", List.of(
                Map.of("target-id", "void_sword", "severity", "CATASTROPHIC", "issue", "Too strong."))),
                FindingSource.AI, warnings::add);

        assertEquals(BalanceSeverity.INFO, report.findings().get(0).severity());
    }

    @Test
    void aFindingWithoutATargetIsSkippedAndLogged() {
        BalanceReport report = BalanceReportMapper.fromMap(Map.of("findings", List.of(
                Map.of("severity", "WARNING", "issue", "Something."),
                Map.of("target-id", "void_sword", "severity", "WARNING", "issue", "Something else."))),
                FindingSource.AI, warnings::add);

        assertEquals(1, report.findings().size());
        assertEquals(1, warnings.size());
    }

    @Test
    void aFindingThatIsNotAnObjectIsSkippedAndLogged() {
        BalanceReport report = BalanceReportMapper.fromMap(Map.of("findings", List.of("just a string")),
                FindingSource.AI, warnings::add);

        assertTrue(report.findings().isEmpty());
        assertEquals(1, warnings.size());
    }

    @Test
    void aResponseWithNoFindingsKeyIsAnEmptyReportRatherThanAnError() {
        BalanceReport report = BalanceReportMapper.fromMap(Map.of("summary", "All good."), FindingSource.AI,
                warnings::add);

        assertEquals("All good.", report.summary());
        assertTrue(report.findings().isEmpty());
        assertTrue(warnings.isEmpty());
    }

    @Test
    void toMapWritesEveryFieldTheDashboardReads() {
        BalanceReport report = BalanceReport.of("One problem.", List.of(BalanceFinding.rule("void_sword",
                BalanceSeverity.CRITICAL, "PERMANENT_EFFECT", "SPEED never expires.", "Raise the cooldown.")));

        Map<String, Object> map = BalanceReportMapper.toMap(report, false);

        assertEquals("One problem.", map.get("summary"));
        assertEquals(false, map.get("ai-enabled"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> findings = (List<Map<String, Object>>) map.get("findings");
        assertEquals("void_sword", findings.get(0).get("target-id"));
        assertEquals("CRITICAL", findings.get(0).get("severity"));
        assertEquals("PERMANENT_EFFECT", findings.get(0).get("rule"));
        assertEquals("SPEED never expires.", findings.get(0).get("issue"));
        assertEquals("Raise the cooldown.", findings.get(0).get("suggestion"));
        assertEquals("RULE", findings.get(0).get("source"));
    }

    @Test
    void aReportSurvivesARoundTripThroughTheMapShape() {
        BalanceReport original = BalanceReport.of("Summary.", List.of(BalanceFinding.rule("void_sword",
                BalanceSeverity.WARNING, "CHEAP_FOR_POWER", "Too cheap.", "Ask for more.")));

        @SuppressWarnings("unchecked")
        Map<String, Object> asMap = (Map<String, Object>) (Map<?, ?>) BalanceReportMapper.toMap(original, true);
        BalanceReport roundTripped = BalanceReportMapper.fromMap(asMap, FindingSource.RULE, warnings::add);

        assertEquals(original.summary(), roundTripped.summary());
        assertEquals(original.findings(), roundTripped.findings());
    }
}
