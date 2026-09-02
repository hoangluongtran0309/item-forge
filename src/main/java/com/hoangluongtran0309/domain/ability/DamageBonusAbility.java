package com.hoangluongtran0309.domain.ability;

import com.hoangluongtran0309.domain.model.DamageBonusAbilityDefinition;
import com.hoangluongtran0309.domain.model.DamageModifierEffect;

public class DamageBonusAbility implements Ability<DamageBonusAbilityDefinition, DamageModifierEffect> {

    @Override
    public DamageModifierEffect resolveEffect(DamageBonusAbilityDefinition definition) {
        return new DamageModifierEffect(definition.bonusPercent());
    }
}
