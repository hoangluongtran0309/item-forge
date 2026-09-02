package com.hoangluongtran0309.domain.balance;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The result of one analysis: a one-line summary plus every finding, most severe first.
 */
public record BalanceReport(String summary, List<BalanceFinding> findings) {

    private static final Comparator<BalanceFinding> MOST_SEVERE_FIRST = Comparator
            .comparing(BalanceFinding::severity, Comparator.reverseOrder())
            .thenComparing(BalanceFinding::targetId)
            .thenComparing(BalanceFinding::rule);

    public BalanceReport {
        summary = summary == null ? "" : summary;
        findings = findings == null ? List.of() : List.copyOf(findings);
    }

    public static BalanceReport of(String summary, List<BalanceFinding> findings) {
        List<BalanceFinding> sorted = new ArrayList<>(findings);
        sorted.sort(MOST_SEVERE_FIRST);
        return new BalanceReport(summary, sorted);
    }

    public BalanceReport withFindingsAdded(String summary, List<BalanceFinding> extra) {
        List<BalanceFinding> combined = new ArrayList<>(findings);
        combined.addAll(extra);
        return of(summary, combined);
    }

    public long countOf(BalanceSeverity severity) {
        return findings.stream().filter(finding -> finding.severity() == severity).count();
    }
}
