package com.hoangluongtran0309.domain.model;

import java.util.List;

import com.hoangluongtran0309.domain.exception.InvalidItemDefinitionException;

public record ArmorDefinition(
        String id,
        String material,
        ArmorSlot slot,
        String armorAssetId,
        int customModelData,
        String displayName,
        List<String> lore) {

    public ArmorDefinition {
        if (id == null || id.isBlank()) {
            throw new InvalidItemDefinitionException("Armor ID cannot be blank");
        }

        if (material == null || material.isBlank()) {
            throw new InvalidItemDefinitionException("Armor Material cannot be blank");
        }

        if (slot == null) {
            throw new InvalidItemDefinitionException("Armor Slot cannot be null");
        }

        // Optional -- defaults to this armor's own id. It only needs to be set explicitly
        // when several pieces share one equipment asset.
        armorAssetId = (armorAssetId == null || armorAssetId.isBlank()) ? id : armorAssetId;

        lore = lore == null ? List.of() : List.copyOf(lore);
    }
}
