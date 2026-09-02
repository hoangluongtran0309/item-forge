package com.hoangluongtran0309.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.hoangluongtran0309.application.port.ArmorConfigSourcePort;
import com.hoangluongtran0309.domain.ArmorRegistry;
import com.hoangluongtran0309.domain.model.ArmorDefinition;
import com.hoangluongtran0309.domain.model.ArmorSlot;

class ArmorConfigLoaderServiceTest {

    @Test
    void loadAllRegistersEverythingFromSource() {
        ArmorDefinition helmet = new ArmorDefinition("void_helmet", "NETHERITE_HELMET", ArmorSlot.HELMET, null, 0,
                "name", List.of());
        FakeArmorConfigSource source = new FakeArmorConfigSource(List.of(helmet));
        ArmorRegistry registry = new ArmorRegistry();
        ArmorConfigLoaderService service = new ArmorConfigLoaderService(source, registry);

        service.loadAll();

        assertEquals(1, registry.size());
        assertEquals(helmet, registry.get("void_helmet").orElseThrow());
    }

    @Test
    void secondLoadClearsPreviousState() {
        ArmorDefinition helmet = new ArmorDefinition("void_helmet", "NETHERITE_HELMET", ArmorSlot.HELMET, null, 0,
                "name", List.of());
        FakeArmorConfigSource source = new FakeArmorConfigSource(List.of(helmet));
        ArmorRegistry registry = new ArmorRegistry();
        ArmorConfigLoaderService service = new ArmorConfigLoaderService(source, registry);

        service.loadAll();
        source.setDefinitions(List.of());
        service.loadAll();

        assertEquals(0, registry.size());
    }

    @Test
    void saveWritesToSourceAndRegistersInPlace() {
        FakeArmorConfigSource source = new FakeArmorConfigSource(List.of());
        ArmorRegistry registry = new ArmorRegistry();
        ArmorConfigLoaderService service = new ArmorConfigLoaderService(source, registry);
        ArmorDefinition helmet = new ArmorDefinition("void_helmet", "NETHERITE_HELMET", ArmorSlot.HELMET, null, 0,
                "name", List.of());

        service.save(helmet);

        assertTrue(source.saved.contains(helmet));
        assertEquals(helmet, registry.get("void_helmet").orElseThrow());
    }

    @Test
    void deleteRemovesFromSourceAndRegistry() {
        FakeArmorConfigSource source = new FakeArmorConfigSource(List.of());
        ArmorRegistry registry = new ArmorRegistry();
        registry.register(new ArmorDefinition("void_helmet", "NETHERITE_HELMET", ArmorSlot.HELMET, null, 0, "name",
                List.of()));
        ArmorConfigLoaderService service = new ArmorConfigLoaderService(source, registry);

        service.delete("void_helmet");

        assertTrue(source.deleted.contains("void_helmet"));
        assertEquals(0, registry.size());
    }

    private static final class FakeArmorConfigSource implements ArmorConfigSourcePort {
        private List<ArmorDefinition> definitions;
        private final List<ArmorDefinition> saved = new ArrayList<>();
        private final List<String> deleted = new ArrayList<>();

        FakeArmorConfigSource(List<ArmorDefinition> definitions) {
            this.definitions = definitions;
        }

        void setDefinitions(List<ArmorDefinition> definitions) {
            this.definitions = definitions;
        }

        @Override
        public List<ArmorDefinition> loadAll() {
            return definitions;
        }

        @Override
        public void save(ArmorDefinition definition) {
            saved.add(definition);
        }

        @Override
        public void delete(String id) {
            deleted.add(id);
        }
    }
}
