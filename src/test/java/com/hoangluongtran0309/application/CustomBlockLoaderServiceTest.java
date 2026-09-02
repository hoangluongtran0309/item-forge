package com.hoangluongtran0309.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.hoangluongtran0309.application.port.CustomBlockConfigSourcePort;
import com.hoangluongtran0309.domain.ArmorRegistry;
import com.hoangluongtran0309.domain.CustomBlockRegistry;
import com.hoangluongtran0309.domain.ItemRegistry;
import com.hoangluongtran0309.domain.model.ArmorDefinition;
import com.hoangluongtran0309.domain.model.ArmorSlot;
import com.hoangluongtran0309.domain.model.CustomBlockDefinition;
import com.hoangluongtran0309.domain.model.ItemDefinition;

class CustomBlockLoaderServiceTest {

    @Test
    void reloadAllRegistersEverythingFromSource() {
        CustomBlockDefinition glowingLantern = block("void_netherite_block", "BASS_GUITAR", 12);
        FakeConfigSource source = new FakeConfigSource(List.of(glowingLantern));
        CustomBlockRegistry registry = new CustomBlockRegistry();
        CustomBlockLoaderService service = new CustomBlockLoaderService(source, registry, new ItemRegistry(),
                new ArmorRegistry());

        List<String> warnings = new ArrayList<>();
        int loaded = service.reloadAll(warnings::add);

        assertEquals(1, loaded);
        assertTrue(warnings.isEmpty());
        assertEquals(glowingLantern, registry.get("void_netherite_block").orElseThrow());
    }

    @Test
    void duplicateComboSkippedWithWarning() {
        CustomBlockDefinition first = block("void_netherite_block", "BASS_GUITAR", 12);
        CustomBlockDefinition second = block("other_void_block", "BASS_GUITAR", 12);
        FakeConfigSource source = new FakeConfigSource(List.of(first, second));
        CustomBlockRegistry registry = new CustomBlockRegistry();
        CustomBlockLoaderService service = new CustomBlockLoaderService(source, registry, new ItemRegistry(),
                new ArmorRegistry());

        List<String> warnings = new ArrayList<>();
        int loaded = service.reloadAll(warnings::add);

        assertEquals(1, loaded);
        assertEquals(1, registry.size());
        assertTrue(warnings.stream().anyMatch(warning -> warning.contains("other_void_block")));
    }

    @Test
    void crossRegistryIdCollisionWarns() {
        CustomBlockDefinition glowingLantern = block("void_sword", "BASS_GUITAR", 12);
        FakeConfigSource source = new FakeConfigSource(List.of(glowingLantern));
        CustomBlockRegistry registry = new CustomBlockRegistry();
        ItemRegistry itemRegistry = new ItemRegistry();
        itemRegistry.register(new ItemDefinition("void_sword", "NETHERITE_SWORD", 1001, "name", List.of(), List.of()));
        CustomBlockLoaderService service = new CustomBlockLoaderService(source, registry, itemRegistry,
                new ArmorRegistry());

        List<String> warnings = new ArrayList<>();
        service.reloadAll(warnings::add);

        assertTrue(warnings.stream().anyMatch(warning -> warning.contains("void_sword")));
    }

    @Test
    void noteBlockMaterialClashWarns() {
        CustomBlockDefinition glowingLantern = block("void_netherite_block", "BASS_GUITAR", 12);
        FakeConfigSource source = new FakeConfigSource(List.of(glowingLantern));
        CustomBlockRegistry registry = new CustomBlockRegistry();
        ItemRegistry itemRegistry = new ItemRegistry();
        itemRegistry.register(new ItemDefinition("weird_item", "NOTE_BLOCK", 5, "name", List.of(), List.of()));
        CustomBlockLoaderService service = new CustomBlockLoaderService(source, registry, itemRegistry,
                new ArmorRegistry());

        List<String> warnings = new ArrayList<>();
        service.reloadAll(warnings::add);

        assertTrue(warnings.stream().anyMatch(warning -> warning.contains("NOTE_BLOCK")));
    }

    @Test
    void armorRegistryUnaffectedWhenNoClash() {
        CustomBlockDefinition glowingLantern = block("void_netherite_block", "BASS_GUITAR", 12);
        FakeConfigSource source = new FakeConfigSource(List.of(glowingLantern));
        CustomBlockRegistry registry = new CustomBlockRegistry();
        ArmorRegistry armorRegistry = new ArmorRegistry();
        armorRegistry.register(new ArmorDefinition("void_helmet", "NETHERITE_HELMET", ArmorSlot.HELMET,
                "void_armor", 0, "name", List.of()));
        CustomBlockLoaderService service = new CustomBlockLoaderService(source, registry, new ItemRegistry(),
                armorRegistry);

        List<String> warnings = new ArrayList<>();
        service.reloadAll(warnings::add);

        assertTrue(warnings.isEmpty());
    }

    @Test
    void saveReturnsTrueAndPersistsOnSuccess() {
        FakeConfigSource source = new FakeConfigSource(List.of());
        CustomBlockRegistry registry = new CustomBlockRegistry();
        CustomBlockLoaderService service = new CustomBlockLoaderService(source, registry, new ItemRegistry(),
                new ArmorRegistry());
        CustomBlockDefinition glowingLantern = block("void_netherite_block", "BASS_GUITAR", 12);

        assertTrue(service.save(glowingLantern));

        assertTrue(source.saved.contains(glowingLantern));
        assertEquals(glowingLantern, registry.get("void_netherite_block").orElseThrow());
    }

    @Test
    void saveReturnsFalseAndDoesNotPersistOnComboCollision() {
        FakeConfigSource source = new FakeConfigSource(List.of());
        CustomBlockRegistry registry = new CustomBlockRegistry();
        registry.register(block("void_netherite_block", "BASS_GUITAR", 12));
        CustomBlockLoaderService service = new CustomBlockLoaderService(source, registry, new ItemRegistry(),
                new ArmorRegistry());

        boolean saved = service.save(block("other_void_block", "BASS_GUITAR", 12));

        assertFalse(saved);
        assertTrue(source.saved.isEmpty());
        assertTrue(registry.get("other_void_block").isEmpty());
    }

    private CustomBlockDefinition block(String id, String instrument, int note) {
        return new CustomBlockDefinition(id, instrument, note, "texture", "GLOWSTONE", "Display Name", 1, List.of());
    }

    private static final class FakeConfigSource implements CustomBlockConfigSourcePort {
        private List<CustomBlockDefinition> definitions;
        private final List<CustomBlockDefinition> saved = new ArrayList<>();
        private final List<String> deleted = new ArrayList<>();

        FakeConfigSource(List<CustomBlockDefinition> definitions) {
            this.definitions = definitions;
        }

        @Override
        public List<CustomBlockDefinition> loadAll() {
            return definitions;
        }

        @Override
        public void save(CustomBlockDefinition definition) {
            saved.add(definition);
        }

        @Override
        public void delete(String id) {
            deleted.add(id);
        }
    }
}
