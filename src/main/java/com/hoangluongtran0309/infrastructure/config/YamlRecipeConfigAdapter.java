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

import com.hoangluongtran0309.application.port.RecipeConfigSourcePort;
import com.hoangluongtran0309.domain.model.RecipeDefinition;

public class YamlRecipeConfigAdapter implements RecipeConfigSourcePort {

    private static final String SECTION = "recipes";

    private final Path recipesFile;
    private final Logger logger;

    public YamlRecipeConfigAdapter(Path recipesFile, Logger logger) {
        this.recipesFile = recipesFile;
        this.logger = logger;
    }

    @Override
    public List<RecipeDefinition> loadAll() {
        if (Files.notExists(recipesFile)) {
            logger.warning("Recipes file not found, skipping: " + recipesFile);
            return List.of();
        }

        Map<String, Object> root;
        try (InputStream in = Files.newInputStream(recipesFile)) {
            root = new Yaml().load(in);
        } catch (IOException | YAMLException e) {
            logger.warning("Failed to read recipes file '" + recipesFile + "': " + e.getMessage());
            return List.of();
        }

        if (root == null || !(root.get(SECTION) instanceof Map<?, ?> recipesSection)) {
            return List.of();
        }

        List<RecipeDefinition> result = new ArrayList<>();
        for (Map.Entry<?, ?> entry : recipesSection.entrySet()) {
            String id = String.valueOf(entry.getKey());
            try {
                result.add(RecipeDefinitionMapper.fromMap(id, RecipeDefinitionMapper.asMap(entry.getValue())));
            } catch (RuntimeException e) {
                logger.warning("Skipping recipe '" + id + "' due to an invalid configuration: " + e.getMessage());
            }
        }

        return result;
    }

    @Override
    public void save(RecipeDefinition definition) {
        Map<String, Object> root = YamlSectionFile.loadRootOrEmpty(recipesFile);
        YamlSectionFile.section(root, SECTION).put(definition.id(), RecipeDefinitionMapper.toMap(definition));
        YamlSectionFile.dump(recipesFile, root);
    }

    @Override
    public void delete(String id) {
        Map<String, Object> root = YamlSectionFile.loadRootOrEmpty(recipesFile);
        YamlSectionFile.section(root, SECTION).remove(id);
        YamlSectionFile.dump(recipesFile, root);
    }
}
