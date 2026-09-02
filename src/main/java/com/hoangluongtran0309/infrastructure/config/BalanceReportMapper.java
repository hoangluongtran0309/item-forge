package com.hoangluongtran0309.infrastructure.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

import com.hoangluongtran0309.domain.balance.BalanceFinding;
import com.hoangluongtran0309.domain.balance.BalanceReport;
import com.hoangluongtran0309.domain.balance.BalanceSeverity;
import com.hoangluongtran0309.domain.balance.FindingSource;

/**
 * Converts between BalanceReport and the Map structure used on the wire, in both directions:
 * reading what an AI provider returned, and writing the report out over the dashboard API.
 *
 * <p>Reading follows the same convention as the YAML loaders -- one malformed finding is
 * skipped and reported to the caller's logger, never allowed to discard a whole report.
 */
public final class BalanceReportMapper {

    private BalanceReportMapper() {
    }

    /**
     * @param warningLogger told about each finding that had to be skipped
     */
    public static BalanceReport fromMap(Map<String, Object> reportMap, FindingSource source,
            Consumer<String> warningLogger) {
        String summary = reportMap.get("summary") == null ? "" : reportMap.get("summary").toString();

        List<BalanceFinding> findings = new ArrayList<>();
        if (reportMap.get("findings") instanceof List<?> rawFindings) {
            for (Object rawFinding : rawFindings) {
                if (!(rawFinding instanceof Map<?, ?> findingMap)) {
                    warningLogger.accept("Skipping a balance finding that was not an object: " + rawFinding);
                    continue;
                }
                try {
                    findings.add(findingFromMap(findingMap, source));
                } catch (RuntimeException e) {
                    warningLogger.accept("Skipping a malformed balance finding: " + e.getMessage());
                }
            }
        }

        return BalanceReport.of(summary, findings);
    }

    public static Map<String, Object> toMap(BalanceReport report, boolean aiEnabled) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("summary", report.summary());
        map.put("ai-enabled", aiEnabled);
        map.put("findings", report.findings().stream().map(BalanceReportMapper::toMap).toList());
        return map;
    }

    private static Map<String, Object> toMap(BalanceFinding finding) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("target-id", finding.targetId());
        map.put("severity", finding.severity().name());
        map.put("rule", finding.rule());
        map.put("issue", finding.issue());
        map.put("suggestion", finding.suggestion());
        map.put("source", finding.source().name());
        return map;
    }

    private static BalanceFinding findingFromMap(Map<?, ?> findingMap, FindingSource source) {
        Object targetId = findingMap.get("target-id");
        Object issue = findingMap.get("issue");
        if (targetId == null || targetId.toString().isBlank()) {
            throw new IllegalArgumentException("missing target-id");
        }
        if (issue == null || issue.toString().isBlank()) {
            throw new IllegalArgumentException("missing issue for '" + targetId + "'");
        }

        Object rule = findingMap.get("rule");
        Object suggestion = findingMap.get("suggestion");

        return new BalanceFinding(targetId.toString(), severityOf(findingMap.get("severity")),
                rule == null ? "" : rule.toString(),
                issue.toString(),
                suggestion == null ? "" : suggestion.toString(),
                source);
    }

    /**
     * An unrecognized or missing severity becomes INFO rather than failing the finding: the
     * text of a finding is worth keeping even when the model invents its own severity name.
     */
    private static BalanceSeverity severityOf(Object rawSeverity) {
        if (rawSeverity == null) {
            return BalanceSeverity.INFO;
        }

        try {
            return BalanceSeverity.valueOf(rawSeverity.toString().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return BalanceSeverity.INFO;
        }
    }
}
