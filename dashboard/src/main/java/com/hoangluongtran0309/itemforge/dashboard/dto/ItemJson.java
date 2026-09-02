package com.hoangluongtran0309.itemforge.dashboard.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ItemJson(
        String id,
        String material,
        @JsonProperty("custom-model-data") int customModelData,
        @JsonProperty("display-name") String displayName,
        List<String> lore,
        List<AbilityJson> abilities) {
}
