package com.hoangluongtran0309.infrastructure.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.hoangluongtran0309.domain.exception.InvalidItemDefinitionException;
import com.hoangluongtran0309.domain.model.ArmorDefinition;
import com.hoangluongtran0309.domain.model.ArmorSlot;

class ArmorDefinitionMapperTest {

    @Test
    void toMapThenFromMapRoundTrips() {
        ArmorDefinition original = new ArmorDefinition("void_helmet", "NETHERITE_HELMET", ArmorSlot.HELMET,
                "void_set", 4002, "Dragon Helmet", List.of("Forged in fire."));

        Map<String, Object> map = ArmorDefinitionMapper.toMap(original);
        ArmorDefinition roundTripped = ArmorDefinitionMapper.fromMap("void_helmet", map);

        assertEquals(original, roundTripped);
    }

    @Test
    void fromMapDefaultsCustomModelDataToZeroWhenMissing() {
        Map<String, Object> map = Map.of("material", "NETHERITE_HELMET", "slot", "HELMET");
        ArmorDefinition definition = ArmorDefinitionMapper.fromMap("void_helmet", map);

        assertEquals(0, definition.customModelData());
    }

    @Test
    void fromMapThrowsOnMissingMaterial() {
        Map<String, Object> map = Map.of("slot", "HELMET");
        assertThrows(InvalidItemDefinitionException.class, () -> ArmorDefinitionMapper.fromMap("broken", map));
    }

    @Test
    void fromMapThrowsOnMissingSlot() {
        Map<String, Object> map = Map.of("material", "NETHERITE_HELMET");
        assertThrows(InvalidItemDefinitionException.class, () -> ArmorDefinitionMapper.fromMap("broken", map));
    }
}
