package com.hoangluongtran0309.domain.ability;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.hoangluongtran0309.domain.model.EffectCommand;
import com.hoangluongtran0309.domain.model.PotionEffectAbilityDefinition;
import com.hoangluongtran0309.domain.model.TriggerType;

class PotionEffectAbilityTest {

    @Test
    void resolveEffectDelegatesToDefinition() {
        PotionEffectAbilityDefinition definition = new PotionEffectAbilityDefinition(TriggerType.RIGHT_CLICK,
                EffectCommand.EffectType.NIGHT_VISION, 45, 90);

        EffectCommand effect = new PotionEffectAbility().resolveEffect(definition);

        assertEquals(definition.toEffectCommand(), effect);
    }
}
