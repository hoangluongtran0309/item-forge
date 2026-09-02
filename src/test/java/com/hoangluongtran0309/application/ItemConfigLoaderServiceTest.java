package com.hoangluongtran0309.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.hoangluongtran0309.application.port.ConfigSourcePort;
import com.hoangluongtran0309.domain.ItemRegistry;
import com.hoangluongtran0309.domain.model.ItemDefinition;

class ItemConfigLoaderServiceTest {

    @Test
    void loadAllRegistersEverythingFromSource() {
        ItemDefinition voidSword = new ItemDefinition("void_sword", "NETHERITE_SWORD", 1001, "name", List.of(),
                List.of());
        FakeConfigSource source = new FakeConfigSource(List.of(voidSword));
        ItemRegistry registry = new ItemRegistry();
        ItemConfigLoaderService service = new ItemConfigLoaderService(source, registry);

        service.loadAll();

        assertEquals(1, registry.size());
        assertEquals(voidSword, registry.get("void_sword").orElseThrow());
    }

    @Test
    void secondLoadClearsPreviousState() {
        ItemDefinition voidSword = new ItemDefinition("void_sword", "NETHERITE_SWORD", 1001, "name", List.of(),
                List.of());
        FakeConfigSource source = new FakeConfigSource(List.of(voidSword));
        ItemRegistry registry = new ItemRegistry();
        ItemConfigLoaderService service = new ItemConfigLoaderService(source, registry);

        service.loadAll();
        source.setDefinitions(List.of());
        service.loadAll();

        assertEquals(0, registry.size());
    }

    @Test
    void saveWritesToSourceAndRegistersInPlace() {
        FakeConfigSource source = new FakeConfigSource(List.of());
        ItemRegistry registry = new ItemRegistry();
        ItemConfigLoaderService service = new ItemConfigLoaderService(source, registry);
        ItemDefinition voidSword = new ItemDefinition("void_sword", "NETHERITE_SWORD", 1001, "name", List.of(),
                List.of());

        service.save(voidSword);

        assertTrue(source.saved.contains(voidSword));
        assertEquals(voidSword, registry.get("void_sword").orElseThrow());
    }

    @Test
    void deleteRemovesFromSourceAndRegistry() {
        FakeConfigSource source = new FakeConfigSource(List.of());
        ItemRegistry registry = new ItemRegistry();
        registry.register(new ItemDefinition("void_sword", "NETHERITE_SWORD", 1001, "name", List.of(), List.of()));
        ItemConfigLoaderService service = new ItemConfigLoaderService(source, registry);

        service.delete("void_sword");

        assertTrue(source.deleted.contains("void_sword"));
        assertEquals(0, registry.size());
    }

    private static final class FakeConfigSource implements ConfigSourcePort {
        private List<ItemDefinition> definitions;
        private final List<ItemDefinition> saved = new ArrayList<>();
        private final List<String> deleted = new ArrayList<>();

        FakeConfigSource(List<ItemDefinition> definitions) {
            this.definitions = definitions;
        }

        void setDefinitions(List<ItemDefinition> definitions) {
            this.definitions = definitions;
        }

        @Override
        public List<ItemDefinition> loadAll() {
            return definitions;
        }

        @Override
        public void save(ItemDefinition definition) {
            saved.add(definition);
        }

        @Override
        public void delete(String id) {
            deleted.add(id);
        }
    }
}
