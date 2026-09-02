package com.hoangluongtran0309.itemforge.dashboard.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ArmorJson(
        String id,
        String material,
        String slot,
        @JsonProperty("armor-asset-id") String armorAssetId,
        @JsonProperty("display-name") String displayName,
        List<String> lore) {
}
