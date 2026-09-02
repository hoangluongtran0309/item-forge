package com.hoangluongtran0309.domain.model;

public record PotionEffectAbilityDefinition(
        TriggerType trigger,
        EffectCommand.EffectType effectType,
        int durationSeconds,
        int cooldownSeconds) implements AbilityDefinition {

    public EffectCommand toEffectCommand() {
        return EffectCommand.of(effectType, durationSeconds);
    }
}
