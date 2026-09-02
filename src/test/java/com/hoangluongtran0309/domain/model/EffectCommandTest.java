package com.hoangluongtran0309.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class EffectCommandTest {

    @Test
    void ofFactoryDefaultsAmplifierToZero() {
        EffectCommand command = EffectCommand.of(EffectCommand.EffectType.FIRE_RESISTANCE, 30);

        assertEquals(EffectCommand.EffectType.FIRE_RESISTANCE, command.type());
        assertEquals(30, command.durationSeconds());
        assertEquals(0, command.amplifier());
    }
}
