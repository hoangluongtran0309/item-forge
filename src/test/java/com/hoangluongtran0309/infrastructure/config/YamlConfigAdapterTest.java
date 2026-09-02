package com.hoangluongtran0309.infrastructure.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.hoangluongtran0309.domain.model.AbilityDefinition;
import com.hoangluongtran0309.domain.model.DamageBonusAbilityDefinition;
import com.hoangluongtran0309.domain.model.ItemDefinition;
import com.hoangluongtran0309.domain.model.PotionEffectAbilityDefinition;
import com.hoangluongtran0309.domain.model.TriggerType;

class YamlConfigAdapterTest {

    @Test
    void missingFileReturnsEmptyList(@TempDir Path dir) {
        YamlConfigAdapter adapter = new YamlConfigAdapter(dir.resolve("items.yml"), Logger.getAnonymousLogger());
        assertTrue(adapter.loadAll().isEmpty());
    }

    @Test
    void potionEffectAbilityParsesCorrectly(@TempDir Path dir) throws IOException {
        Path itemsFile = dir.resolve("items.yml");
        Files.writeString(itemsFile, """
                items:
                  void_sword:
                    material: NETHERITE_SWORD
                    custom-model-data: 1001
                    display-name: "Fire Sword"
                    lore:
                      - "A blazing blade."
                    abilities:
                      - type: POTION_EFFECT
                        trigger: RIGHT_CLICK
                        effect: FIRE_RESISTANCE
                        duration-seconds: 30
                        cooldown-seconds: 60
                """);

        YamlConfigAdapter adapter = new YamlConfigAdapter(itemsFile, Logger.getAnonymousLogger());
        List<ItemDefinition> result = adapter.loadAll();

        assertEquals(1, result.size());
        ItemDefinition parsed = result.get(0);
        assertEquals("void_sword", parsed.id());
        assertEquals("NETHERITE_SWORD", parsed.material());
        assertEquals(1001, parsed.customModelData());
        assertEquals(List.of("A blazing blade."), parsed.lore());
        assertEquals(1, parsed.abilities().size());

        AbilityDefinition ability = parsed.abilities().get(0);
        PotionEffectAbilityDefinition potionAbility = assertInstanceOf(PotionEffectAbilityDefinition.class, ability);
        assertEquals(TriggerType.RIGHT_CLICK, potionAbility.trigger());
        assertEquals(30, potionAbility.durationSeconds());
        assertEquals(60, potionAbility.cooldownSeconds());
    }

    @Test
    void damageBonusAbilityParsesCorrectly(@TempDir Path dir) throws IOException {
        Path itemsFile = dir.resolve("items.yml");
        Files.writeString(itemsFile, """
                items:
                  void_pickaxe:
                    material: NETHERITE_PICKAXE
                    custom-model-data: 2001
                    display-name: "Power Axe"
                    abilities:
                      - type: DAMAGE_BONUS
                        trigger: ON_HIT
                        cooldown-seconds: 10
                        bonus-percent: 15.5
                """);

        YamlConfigAdapter adapter = new YamlConfigAdapter(itemsFile, Logger.getAnonymousLogger());
        List<ItemDefinition> result = adapter.loadAll();

        assertEquals(1, result.size());
        DamageBonusAbilityDefinition ability = assertInstanceOf(DamageBonusAbilityDefinition.class,
                result.get(0).abilities().get(0));
        assertEquals(15.5, ability.bonusPercent());
    }

    @Test
    void missingMaterialSkipsEntryWithoutCrashing(@TempDir Path dir) throws IOException {
        Path itemsFile = dir.resolve("items.yml");
        Files.writeString(itemsFile, """
                items:
                  broken_item:
                    display-name: "Broken"
                """);

        YamlConfigAdapter adapter = new YamlConfigAdapter(itemsFile, Logger.getAnonymousLogger());
        assertTrue(adapter.loadAll().isEmpty());
    }

    @Test
    void unknownAbilityTypeSkipsOnlyThatItem(@TempDir Path dir) throws IOException {
        Path itemsFile = dir.resolve("items.yml");
        Files.writeString(itemsFile, """
                items:
                  broken_item:
                    material: NETHERITE_SWORD
                    abilities:
                      - type: NOT_A_REAL_TYPE
                        trigger: RIGHT_CLICK
                  fine_item:
                    material: IRON_SWORD
                """);

        YamlConfigAdapter adapter = new YamlConfigAdapter(itemsFile, Logger.getAnonymousLogger());
        List<ItemDefinition> result = adapter.loadAll();

        assertEquals(1, result.size());
        assertEquals("fine_item", result.get(0).id());
    }

    @Test
    void malformedYamlSyntaxReturnsEmptyListWithoutCrashing(@TempDir Path dir) throws IOException {
        Path itemsFile = dir.resolve("items.yml");
        Files.writeString(itemsFile, """
                items:
                  void_sword: [unclosed
                """);

        YamlConfigAdapter adapter = new YamlConfigAdapter(itemsFile, Logger.getAnonymousLogger());
        assertTrue(adapter.loadAll().isEmpty());
    }

    @Test
    void saveThenLoadAllRoundTripsANewItem(@TempDir Path dir) {
        Path itemsFile = dir.resolve("items.yml");
        YamlConfigAdapter adapter = new YamlConfigAdapter(itemsFile, Logger.getAnonymousLogger());

        ItemDefinition definition = new ItemDefinition("void_sword", "NETHERITE_SWORD", 1001, "Fire Sword",
                List.of("A blazing blade."),
                List.of(new PotionEffectAbilityDefinition(TriggerType.RIGHT_CLICK,
                        com.hoangluongtran0309.domain.model.EffectCommand.EffectType.FIRE_RESISTANCE, 30, 60)));

        adapter.save(definition);
        List<ItemDefinition> result = adapter.loadAll();

        assertEquals(1, result.size());
        assertEquals(definition, result.get(0));
    }

    @Test
    void saveAddsAnItemWithoutRemovingExistingOnes(@TempDir Path dir) throws IOException {
        Path itemsFile = dir.resolve("items.yml");
        Files.writeString(itemsFile, """
                items:
                  plain_item:
                    material: STICK
                """);

        YamlConfigAdapter adapter = new YamlConfigAdapter(itemsFile, Logger.getAnonymousLogger());
        adapter.save(new ItemDefinition("void_sword", "NETHERITE_SWORD", 1001, "Fire Sword", List.of(), List.of()));

        List<ItemDefinition> result = adapter.loadAll();
        assertEquals(2, result.size());
    }

    @Test
    void saveOverwritesAnExistingEntryWithTheSameId(@TempDir Path dir) {
        Path itemsFile = dir.resolve("items.yml");
        YamlConfigAdapter adapter = new YamlConfigAdapter(itemsFile, Logger.getAnonymousLogger());

        adapter.save(new ItemDefinition("void_sword", "NETHERITE_SWORD", 1001, "Old Name", List.of(), List.of()));
        adapter.save(new ItemDefinition("void_sword", "NETHERITE_SWORD", 1001, "New Name", List.of(), List.of()));

        List<ItemDefinition> result = adapter.loadAll();
        assertEquals(1, result.size());
        assertEquals("New Name", result.get(0).displayName());
    }

    @Test
    void itemWithoutAbilitiesKeyDefaultsToEmptyList(@TempDir Path dir) throws IOException {
        Path itemsFile = dir.resolve("items.yml");
        Files.writeString(itemsFile, """
                items:
                  plain_item:
                    material: STICK
                """);

        YamlConfigAdapter adapter = new YamlConfigAdapter(itemsFile, Logger.getAnonymousLogger());
        List<ItemDefinition> result = adapter.loadAll();

        assertEquals(1, result.size());
        assertTrue(result.get(0).abilities().isEmpty());
    }
}
