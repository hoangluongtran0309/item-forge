package com.hoangluongtran0309.domain.model;

/**
 * EffectCommand
 */
public record EffectCommand(EffectType type, int durationSeconds, int amplifier) implements AbilityEffect {

    public enum EffectType {
        FIRE_RESISTANCE,
        SLOWNESS,
        SPEED,
        NIGHT_VISION,
        REGENERATION
    }

    public static EffectCommand of(EffectType type, int durationSeconds) {
        return new EffectCommand(type, durationSeconds, 0);
    }
}
