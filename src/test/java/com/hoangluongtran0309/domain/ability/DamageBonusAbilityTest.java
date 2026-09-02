package com.hoangluongtran0309.domain.ability;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.hoangluongtran0309.domain.model.DamageBonusAbilityDefinition;
import com.hoangluongtran0309.domain.model.DamageModifierEffect;
import com.hoangluongtran0309.domain.model.TriggerType;

class DamageBonusAbilityTest {

    @Test
    void resolveEffectWrapsBonusPercent() {
        DamageBonusAbilityDefinition definition = new DamageBonusAbilityDefinition(TriggerType.ON_HIT, 30, 25.0);

        DamageModifierEffect effect = new DamageBonusAbility().resolveEffect(definition);

        assertEquals(new DamageModifierEffect(25.0), effect);
    }
}
