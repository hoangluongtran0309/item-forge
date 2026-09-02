package com.hoangluongtran0309.itemforge.dashboard.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PotionEffectAbilityJson(
        String type,
        String trigger,
        @JsonProperty("cooldown-seconds") int cooldownSeconds,
        String effect,
        @JsonProperty("duration-seconds") int durationSeconds) implements AbilityJson {
}
