package com.hoangluongtran0309.domain.model;

import java.util.List;

import com.hoangluongtran0309.domain.exception.InvalidItemDefinitionException;

public record CustomBlockDefinition(
        String id,
        String instrument,
        int note,
        String textureId,
        String dropItemId,
        String displayName,
        int customModelData,
        List<String> lore) {

    public CustomBlockDefinition {
        if (id == null || id.isBlank()) {
            throw new InvalidItemDefinitionException("Custom Block ID cannot be blank");
        }

        if (instrument == null || instrument.isBlank()) {
            throw new InvalidItemDefinitionException("Custom Block Instrument cannot be blank");
        }

        if (note < 0 || note > 24) {
            throw new InvalidItemDefinitionException("Custom Block Note must be between 0 and 24");
        }

        if (textureId == null || textureId.isBlank()) {
            throw new InvalidItemDefinitionException("Custom Block Texture ID cannot be blank");
        }

        if (dropItemId == null || dropItemId.isBlank()) {
            throw new InvalidItemDefinitionException("Custom Block Drop Item ID cannot be blank");
        }

        lore = lore == null ? List.of() : List.copyOf(lore);
    }
}
