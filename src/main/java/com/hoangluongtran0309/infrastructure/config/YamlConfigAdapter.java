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

import com.hoangluongtran0309.application.port.ConfigSourcePort;
import com.hoangluongtran0309.domain.model.ItemDefinition;

public class YamlConfigAdapter implements ConfigSourcePort {

    private static final String SECTION = "items";

    private final Path itemsFile;
    private final Logger logger;

    public YamlConfigAdapter(Path itemsFile, Logger logger) {
        this.itemsFile = itemsFile;
        this.logger = logger;
    }

    @Override
    public List<ItemDefinition> loadAll() {
        if (Files.notExists(itemsFile)) {
            logger.warning("Items file not found, skipping: " + itemsFile);
            return List.of();
        }

        Map<String, Object> root;
        try (InputStream in = Files.newInputStream(itemsFile)) {
            root = new Yaml().load(in);
        } catch (IOException | YAMLException e) {
            logger.warning("Failed to read items file '" + itemsFile + "': " + e.getMessage());
            return List.of();
        }

        if (root == null || !(root.get(SECTION) instanceof Map<?, ?> itemsSection)) {
            return List.of();
        }

        List<ItemDefinition> result = new ArrayList<>();
        for (Map.Entry<?, ?> entry : itemsSection.entrySet()) {
            String id = String.valueOf(entry.getKey());
            try {
                result.add(ItemDefinitionMapper.fromMap(id, ItemDefinitionMapper.asMap(entry.getValue())));
            } catch (RuntimeException e) {
                logger.warning("Skipping item '" + id + "' due to an invalid configuration: " + e.getMessage());
            }
        }

        return result;
    }

    @Override
    public void save(ItemDefinition definition) {
        Map<String, Object> root = YamlSectionFile.loadRootOrEmpty(itemsFile);
        YamlSectionFile.section(root, SECTION).put(definition.id(), ItemDefinitionMapper.toMap(definition));
        YamlSectionFile.dump(itemsFile, root);
    }

    @Override
    public void delete(String id) {
        Map<String, Object> root = YamlSectionFile.loadRootOrEmpty(itemsFile);
        YamlSectionFile.section(root, SECTION).remove(id);
        YamlSectionFile.dump(itemsFile, root);
    }
}
