package com.hoangluongtran0309.itemforge.dashboard.dto;

public record ArmorTextureStatusResponse(
        boolean hasIcon,
        String armorAssetId,
        boolean hasHumanoidLayer,
        boolean hasHumanoidLeggingsLayer) {
}
