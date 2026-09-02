package com.hoangluongtran0309.itemforge.dashboard.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record DamageBonusAbilityJson(
        String type,
        String trigger,
        @JsonProperty("cooldown-seconds") int cooldownSeconds,
        @JsonProperty("bonus-percent") double bonusPercent) implements AbilityJson {
}
