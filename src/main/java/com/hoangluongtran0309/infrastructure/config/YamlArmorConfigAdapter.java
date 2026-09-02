package com.hoangluongtran0309.infrastructure.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

import com.hoangluongtran0309.application.port.ArmorConfigSourcePort;
import com.hoangluongtran0309.domain.model.ArmorDefinition;

public class YamlArmorConfigAdapter implements ArmorConfigSourcePort {

    private static final String SECTION = "armor";

    private final Path armorFile;
    private final Logger logger;

    public YamlArmorConfigAdapter(Path armorFile, Logger logger) {
        this.armorFile = armorFile;
        this.logger = logger;
    }

    @Override
    public List<ArmorDefinition> loadAll() {
        if (Files.notExists(armorFile)) {
            logger.warning("Armor file not found, skipping: " + armorFile);
            return List.of();
        }

        Map<String, Object> root;
        try (InputStream in = Files.newInputStream(armorFile)) {
            root = new Yaml().load(in);
        } catch (IOException | YAMLException e) {
            logger.warning("Failed to read armor file '" + armorFile + "': " + e.getMessage());
            return List.of();
        }

        if (root == null || !(root.get(SECTION) instanceof Map<?, ?> armorSection)) {
            return List.of();
        }

        List<ArmorDefinition> result = new ArrayList<>();
        for (Map.Entry<?, ?> entry : armorSection.entrySet()) {
            String id = String.valueOf(entry.getKey());
            try {
                result.add(ArmorDefinitionMapper.fromMap(id, ArmorDefinitionMapper.asMap(entry.getValue())));
            } catch (RuntimeException e) {
                logger.warning("Skipping armor '" + id + "' due to an invalid configuration: " + e.getMessage());
            }
        }

        return result;
    }

    @Override
    public void save(ArmorDefinition definition) {
        Map<String, Object> root = YamlSectionFile.loadRootOrEmpty(armorFile);
        YamlSectionFile.section(root, SECTION).put(definition.id(), ArmorDefinitionMapper.toMap(definition));
        YamlSectionFile.dump(armorFile, root);
    }

    @Override
    public void delete(String id) {
        Map<String, Object> root = YamlSectionFile.loadRootOrEmpty(armorFile);
        YamlSectionFile.section(root, SECTION).remove(id);
        YamlSectionFile.dump(armorFile, root);
    }
}
