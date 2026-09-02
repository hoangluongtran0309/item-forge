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

import com.hoangluongtran0309.application.port.CustomBlockConfigSourcePort;
import com.hoangluongtran0309.domain.model.CustomBlockDefinition;

public class YamlCustomBlockConfigAdapter implements CustomBlockConfigSourcePort {

    private static final String SECTION = "blocks";

    private final Path blocksFile;
    private final Logger logger;

    public YamlCustomBlockConfigAdapter(Path blocksFile, Logger logger) {
        this.blocksFile = blocksFile;
        this.logger = logger;
    }

    @Override
    public List<CustomBlockDefinition> loadAll() {
        if (Files.notExists(blocksFile)) {
            logger.warning("Blocks file not found, skipping: " + blocksFile);
            return List.of();
        }

        Map<String, Object> root;
        try (InputStream in = Files.newInputStream(blocksFile)) {
            root = new Yaml().load(in);
        } catch (IOException | YAMLException e) {
            logger.warning("Failed to read blocks file '" + blocksFile + "': " + e.getMessage());
            return List.of();
        }

        if (root == null || !(root.get(SECTION) instanceof Map<?, ?> blocksSection)) {
            return List.of();
        }

        List<CustomBlockDefinition> result = new ArrayList<>();
        for (Map.Entry<?, ?> entry : blocksSection.entrySet()) {
            String id = String.valueOf(entry.getKey());
            try {
                result.add(CustomBlockDefinitionMapper.fromMap(id, CustomBlockDefinitionMapper.asMap(entry.getValue())));
            } catch (RuntimeException e) {
                logger.warning("Skipping block '" + id + "' due to an invalid configuration: " + e.getMessage());
            }
        }

        return result;
    }

    @Override
    public void save(CustomBlockDefinition definition) {
        Map<String, Object> root = YamlSectionFile.loadRootOrEmpty(blocksFile);
        YamlSectionFile.section(root, SECTION).put(definition.id(), CustomBlockDefinitionMapper.toMap(definition));
        YamlSectionFile.dump(blocksFile, root);
    }

    @Override
    public void delete(String id) {
        Map<String, Object> root = YamlSectionFile.loadRootOrEmpty(blocksFile);
        YamlSectionFile.section(root, SECTION).remove(id);
        YamlSectionFile.dump(blocksFile, root);
    }
}
