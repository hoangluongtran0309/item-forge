package com.hoangluongtran0309.itemforge.dashboard.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record BalanceReportJson(
        String summary,
        @JsonProperty("ai-enabled") boolean aiEnabled,
        List<BalanceFindingJson> findings) {

    public BalanceReportJson {
        findings = findings == null ? List.of() : List.copyOf(findings);
    }

    public long countOf(String severity) {
        return findings.stream().filter(finding -> severity.equals(finding.severity())).count();
    }
}
