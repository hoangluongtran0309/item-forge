package com.hoangluongtran0309.infrastructure.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.hoangluongtran0309.domain.exception.InvalidItemDefinitionException;
import com.hoangluongtran0309.domain.model.DamageBonusAbilityDefinition;
import com.hoangluongtran0309.domain.model.EffectCommand;
import com.hoangluongtran0309.domain.model.ItemDefinition;
import com.hoangluongtran0309.domain.model.PotionEffectAbilityDefinition;
import com.hoangluongtran0309.domain.model.TriggerType;

class ItemDefinitionMapperTest {

    @Test
    void toMapThenFromMapRoundTripsAPotionEffectAbility() {
        ItemDefinition original = new ItemDefinition("void_sword", "NETHERITE_SWORD", 1001, "Fire Sword",
                List.of("A blazing blade."),
                List.of(new PotionEffectAbilityDefinition(TriggerType.RIGHT_CLICK,
                        EffectCommand.EffectType.FIRE_RESISTANCE, 30, 60)));

        Map<String, Object> map = ItemDefinitionMapper.toMap(original);
        ItemDefinition roundTripped = ItemDefinitionMapper.fromMap("void_sword", map);

        assertEquals(original, roundTripped);
    }

    @Test
    void toMapThenFromMapRoundTripsADamageBonusAbility() {
        ItemDefinition original = new ItemDefinition("void_pickaxe", "NETHERITE_PICKAXE", 2001, "Power Axe", List.of(),
                List.of(new DamageBonusAbilityDefinition(TriggerType.ON_HIT, 10, 15.5)));

        Map<String, Object> map = ItemDefinitionMapper.toMap(original);
        ItemDefinition roundTripped = ItemDefinitionMapper.fromMap("void_pickaxe", map);

        assertEquals(original, roundTripped);
    }

    @Test
    void fromMapThrowsOnMissingMaterial() {
        Map<String, Object> map = Map.of("display-name", "Broken");
        assertThrows(InvalidItemDefinitionException.class, () -> ItemDefinitionMapper.fromMap("broken", map));
    }

    @Test
    void fromMapThrowsOnUnknownAbilityType() {
        Map<String, Object> map = Map.of(
                "material", "NETHERITE_SWORD",
                "abilities", List.of(Map.of("type", "NOT_A_REAL_TYPE", "trigger", "RIGHT_CLICK")));

        assertThrows(InvalidItemDefinitionException.class, () -> ItemDefinitionMapper.fromMap("broken", map));
    }
}
