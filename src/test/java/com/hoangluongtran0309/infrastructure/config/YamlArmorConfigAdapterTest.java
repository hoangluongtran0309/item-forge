package com.hoangluongtran0309.infrastructure.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.hoangluongtran0309.domain.model.ArmorDefinition;
import com.hoangluongtran0309.domain.model.ArmorSlot;

class YamlArmorConfigAdapterTest {

    @Test
    void missingFileReturnsEmptyList(@TempDir Path dir) {
        YamlArmorConfigAdapter adapter = new YamlArmorConfigAdapter(dir.resolve("armor.yml"),
                Logger.getAnonymousLogger());

        assertTrue(adapter.loadAll().isEmpty());
    }

    @Test
    void validEntryParsesCorrectly(@TempDir Path dir) throws IOException {
        Path armorFile = dir.resolve("armor.yml");
        Files.writeString(armorFile, """
                armor:
                  void_chestplate:
                    material: NETHERITE_CHESTPLATE
                    slot: CHESTPLATE
                    display-name: "Dragon Scale Chestplate"
                    lore:
                      - "Forged from dragon scales."
                """);

        YamlArmorConfigAdapter adapter = new YamlArmorConfigAdapter(armorFile, Logger.getAnonymousLogger());
        List<ArmorDefinition> result = adapter.loadAll();

        assertEquals(1, result.size());
        ArmorDefinition parsed = result.get(0);
        assertEquals("void_chestplate", parsed.id());
        assertEquals("NETHERITE_CHESTPLATE", parsed.material());
        assertEquals(ArmorSlot.CHESTPLATE, parsed.slot());
        assertEquals("void_chestplate", parsed.armorAssetId());
        assertEquals("Dragon Scale Chestplate", parsed.displayName());
        assertEquals(List.of("Forged from dragon scales."), parsed.lore());
    }

    @Test
    void armorAssetIdIsParsedWhenPresent(@TempDir Path dir) throws IOException {
        Path armorFile = dir.resolve("armor.yml");
        Files.writeString(armorFile, """
                armor:
                  void_helmet:
                    material: NETHERITE_HELMET
                    slot: HELMET
                    armor-asset-id: void_armor
                """);

        YamlArmorConfigAdapter adapter = new YamlArmorConfigAdapter(armorFile, Logger.getAnonymousLogger());
        List<ArmorDefinition> result = adapter.loadAll();

        assertEquals("void_armor", result.get(0).armorAssetId());
    }

    @Test
    void missingMaterialSkipsEntryWithoutCrashing(@TempDir Path dir) throws IOException {
        Path armorFile = dir.resolve("armor.yml");
        Files.writeString(armorFile, """
                armor:
                  broken_entry:
                    slot: HELMET
                """);

        YamlArmorConfigAdapter adapter = new YamlArmorConfigAdapter(armorFile, Logger.getAnonymousLogger());

        assertTrue(adapter.loadAll().isEmpty());
    }

    @Test
    void malformedYamlSyntaxReturnsEmptyListWithoutCrashing(@TempDir Path dir) throws IOException {
        Path armorFile = dir.resolve("armor.yml");
        Files.writeString(armorFile, """
                armor:
                  void_helmet: [unclosed
                """);

        YamlArmorConfigAdapter adapter = new YamlArmorConfigAdapter(armorFile, Logger.getAnonymousLogger());

        assertTrue(adapter.loadAll().isEmpty());
    }

    @Test
    void missingSlotSkipsEntryWithoutCrashing(@TempDir Path dir) throws IOException {
        Path armorFile = dir.resolve("armor.yml");
        Files.writeString(armorFile, """
                armor:
                  broken_entry:
                    material: NETHERITE_HELMET
                """);

        YamlArmorConfigAdapter adapter = new YamlArmorConfigAdapter(armorFile, Logger.getAnonymousLogger());

        assertTrue(adapter.loadAll().isEmpty());
    }

    @Test
    void saveThenLoadAllRoundTripsANewArmor(@TempDir Path dir) {
        Path armorFile = dir.resolve("armor.yml");
        YamlArmorConfigAdapter adapter = new YamlArmorConfigAdapter(armorFile, Logger.getAnonymousLogger());

        ArmorDefinition definition = new ArmorDefinition("void_helmet", "NETHERITE_HELMET", ArmorSlot.HELMET,
                "void_set", 4002, "Dragon Helmet", List.of("Forged in fire."));

        adapter.save(definition);
        List<ArmorDefinition> result = adapter.loadAll();

        assertEquals(1, result.size());
        assertEquals(definition, result.get(0));
    }

    @Test
    void saveAddsAnArmorWithoutRemovingExistingOnes(@TempDir Path dir) throws IOException {
        Path armorFile = dir.resolve("armor.yml");
        Files.writeString(armorFile, """
                armor:
                  plain_helmet:
                    material: IRON_HELMET
                    slot: HELMET
                """);

        YamlArmorConfigAdapter adapter = new YamlArmorConfigAdapter(armorFile, Logger.getAnonymousLogger());
        adapter.save(new ArmorDefinition("void_helmet", "NETHERITE_HELMET", ArmorSlot.HELMET, null, 0,
                "Dragon Helmet", List.of()));

        List<ArmorDefinition> result = adapter.loadAll();
        assertEquals(2, result.size());
    }

    @Test
    void saveOverwritesAnExistingEntryWithTheSameId(@TempDir Path dir) {
        Path armorFile = dir.resolve("armor.yml");
        YamlArmorConfigAdapter adapter = new YamlArmorConfigAdapter(armorFile, Logger.getAnonymousLogger());

        adapter.save(new ArmorDefinition("void_helmet", "NETHERITE_HELMET", ArmorSlot.HELMET, null, 0, "Old Name",
                List.of()));
        adapter.save(new ArmorDefinition("void_helmet", "NETHERITE_HELMET", ArmorSlot.HELMET, null, 0, "New Name",
                List.of()));

        List<ArmorDefinition> result = adapter.loadAll();
        assertEquals(1, result.size());
        assertEquals("New Name", result.get(0).displayName());
    }

    @Test
    void deleteRemovesOnlyTheMatchingEntry(@TempDir Path dir) {
        Path armorFile = dir.resolve("armor.yml");
        YamlArmorConfigAdapter adapter = new YamlArmorConfigAdapter(armorFile, Logger.getAnonymousLogger());

        adapter.save(new ArmorDefinition("void_helmet", "NETHERITE_HELMET", ArmorSlot.HELMET, null, 0, "name",
                List.of()));
        adapter.save(new ArmorDefinition("void_chestplate", "NETHERITE_CHESTPLATE", ArmorSlot.CHESTPLATE, null, 0,
                "name", List.of()));

        adapter.delete("void_helmet");

        List<ArmorDefinition> result = adapter.loadAll();
        assertEquals(1, result.size());
        assertEquals("void_chestplate", result.get(0).id());
    }
}
