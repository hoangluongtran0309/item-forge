package com.hoangluongtran0309.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.hoangluongtran0309.domain.exception.InvalidItemDefinitionException;

class DamageBonusAbilityDefinitionTest {

    @Test
    void zeroBonusPercentThrows() {
        assertThrows(InvalidItemDefinitionException.class,
                () -> new DamageBonusAbilityDefinition(TriggerType.ON_HIT, 30, 0));
    }

    @Test
    void negativeBonusPercentThrows() {
        assertThrows(InvalidItemDefinitionException.class,
                () -> new DamageBonusAbilityDefinition(TriggerType.ON_HIT, 30, -5));
    }

    @Test
    void positiveBonusPercentIsAccepted() {
        DamageBonusAbilityDefinition definition = new DamageBonusAbilityDefinition(TriggerType.ON_HIT, 30, 15.5);
        assertEquals(15.5, definition.bonusPercent());
    }
}
