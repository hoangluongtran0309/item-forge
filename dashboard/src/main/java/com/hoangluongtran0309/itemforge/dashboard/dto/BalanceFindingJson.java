package com.hoangluongtran0309.itemforge.dashboard.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record BalanceFindingJson(
        @JsonProperty("target-id") String targetId,
        String severity,
        String rule,
        String issue,
        String suggestion,
        String source) {
}
