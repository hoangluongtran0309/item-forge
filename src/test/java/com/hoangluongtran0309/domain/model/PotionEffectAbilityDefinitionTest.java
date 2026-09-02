package com.hoangluongtran0309.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PotionEffectAbilityDefinitionTest {

    @Test
    void toEffectCommandMapsFieldsCorrectly() {
        PotionEffectAbilityDefinition definition = new PotionEffectAbilityDefinition(TriggerType.RIGHT_CLICK,
                EffectCommand.EffectType.SPEED, 20, 60);

        EffectCommand command = definition.toEffectCommand();

        assertEquals(EffectCommand.EffectType.SPEED, command.type());
        assertEquals(20, command.durationSeconds());
        assertEquals(0, command.amplifier());
    }
}
