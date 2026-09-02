package com.hoangluongtran0309.itemforge.dashboard.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record BlockJson(
        String id,
        String instrument,
        int note,
        @JsonProperty("texture-id") String textureId,
        @JsonProperty("drop-item-id") String dropItemId,
        @JsonProperty("display-name") String displayName,
        @JsonProperty("custom-model-data") int customModelData,
        List<String> lore) {
}
