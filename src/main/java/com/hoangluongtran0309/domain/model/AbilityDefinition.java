package com.hoangluongtran0309.domain.model;

/**
 * AbilityDefinition
 */
public sealed interface AbilityDefinition
        permits PotionEffectAbilityDefinition, DamageBonusAbilityDefinition {

    TriggerType trigger();

    int cooldownSeconds();
}
