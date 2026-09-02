package com.hoangluongtran0309.application;

import com.hoangluongtran0309.application.port.ArmorConfigSourcePort;
import com.hoangluongtran0309.domain.ArmorRegistry;
import com.hoangluongtran0309.domain.model.ArmorDefinition;

public class ArmorConfigLoaderService {

    private final ArmorConfigSourcePort configSource;
    private final ArmorRegistry registry;

    public ArmorConfigLoaderService(ArmorConfigSourcePort configSource, ArmorRegistry registry) {
        this.configSource = configSource;
        this.registry = registry;
    }

    public void loadAll() {
        registry.clear();
        configSource.loadAll().forEach(registry::register);
    }

    public void save(ArmorDefinition definition) {
        configSource.save(definition);
        registry.register(definition);
    }

    public void delete(String id) {
        configSource.delete(id);
        registry.remove(id);
    }
}
