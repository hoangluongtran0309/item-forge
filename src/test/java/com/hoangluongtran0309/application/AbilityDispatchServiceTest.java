package com.hoangluongtran0309.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.hoangluongtran0309.application.port.EffectApplierPort;
import com.hoangluongtran0309.domain.ItemRegistry;
import com.hoangluongtran0309.domain.model.AbilityEffect;
import com.hoangluongtran0309.domain.model.EffectCommand;
import com.hoangluongtran0309.domain.model.ItemDefinition;
import com.hoangluongtran0309.domain.model.PotionEffectAbilityDefinition;
import com.hoangluongtran0309.domain.model.TriggerType;
import com.hoangluongtran0309.domain.model.TriggeredAbility;

class AbilityDispatchServiceTest {

    @Test
    void dispatchReturnsTriggeredAbilityPairingDefinitionAndEffect() {
        PotionEffectAbilityDefinition definition = new PotionEffectAbilityDefinition(TriggerType.RIGHT_CLICK,
                EffectCommand.EffectType.FIRE_RESISTANCE, 30, 60);
        ItemRegistry itemRegistry = new ItemRegistry();
        itemRegistry.register(new ItemDefinition("void_sword", "NETHERITE_SWORD", 1001, "Fire Sword", List.of(),
                List.of(definition)));

        RecordingEffectApplier effectApplier = new RecordingEffectApplier();
        AbilityDispatchService service = new AbilityDispatchService(itemRegistry, new CooldownService(),
                effectApplier);

        List<TriggeredAbility> triggered = service.dispatch(UUID.randomUUID(), "void_sword", TriggerType.RIGHT_CLICK);

        assertEquals(1, triggered.size());
        assertSame(definition, triggered.get(0).definition());
        assertEquals(EffectCommand.of(EffectCommand.EffectType.FIRE_RESISTANCE, 30), triggered.get(0).effect());
        assertEquals(1, effectApplier.applied.size());
    }

    @Test
    void wrongTriggerIsIgnored() {
        PotionEffectAbilityDefinition definition = new PotionEffectAbilityDefinition(TriggerType.RIGHT_CLICK,
                EffectCommand.EffectType.SPEED, 10, 5);
        ItemRegistry itemRegistry = new ItemRegistry();
        itemRegistry.register(new ItemDefinition("boots", "LEATHER_BOOTS", 1, "Boots", List.of(),
                List.of(definition)));

        AbilityDispatchService service = new AbilityDispatchService(itemRegistry, new CooldownService(),
                new RecordingEffectApplier());

        List<TriggeredAbility> triggered = service.dispatch(UUID.randomUUID(), "boots", TriggerType.LEFT_CLICK);

        assertTrue(triggered.isEmpty());
    }

    @Test
    void secondDispatchWithinCooldownIsSkipped() {
        PotionEffectAbilityDefinition definition = new PotionEffectAbilityDefinition(TriggerType.RIGHT_CLICK,
                EffectCommand.EffectType.SPEED, 10, 60);
        ItemRegistry itemRegistry = new ItemRegistry();
        itemRegistry.register(new ItemDefinition("boots", "LEATHER_BOOTS", 1, "Boots", List.of(),
                List.of(definition)));

        AbilityDispatchService service = new AbilityDispatchService(itemRegistry, new CooldownService(),
                new RecordingEffectApplier());
        UUID playerId = UUID.randomUUID();

        List<TriggeredAbility> first = service.dispatch(playerId, "boots", TriggerType.RIGHT_CLICK);
        List<TriggeredAbility> second = service.dispatch(playerId, "boots", TriggerType.RIGHT_CLICK);

        assertEquals(1, first.size());
        assertTrue(second.isEmpty());
    }

    private static final class RecordingEffectApplier implements EffectApplierPort {
        final List<AbilityEffect> applied = new ArrayList<>();

        @Override
        public void apply(UUID playerId, AbilityEffect effect) {
            applied.add(effect);
        }
    }
}
