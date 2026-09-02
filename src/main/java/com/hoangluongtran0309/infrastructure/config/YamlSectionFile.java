package com.hoangluongtran0309.infrastructure.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

import com.hoangluongtran0309.domain.exception.InvalidItemDefinitionException;

/**
 * Reads and rewrites an entire "section"-style YAML file (items.yml, armor.yml, recipes.yml)
 * on behalf of all three Yaml*ConfigAdapters, so the load-modify-dump logic is not repeated
 * in each of them.
 *
 * Note: rewriting the whole file through SnakeYAML does NOT preserve hand-written comments.
 * That is an accepted trade-off for being able to write the file programmatically.
 */
final class YamlSectionFile {

    private YamlSectionFile() {
    }

    static Map<String, Object> loadRootOrEmpty(Path file) {
        if (Files.notExists(file)) {
            return new LinkedHashMap<>();
        }

        try (InputStream in = Files.newInputStream(file)) {
            Map<String, Object> root = new Yaml().load(in);
            return root == null ? new LinkedHashMap<>() : new LinkedHashMap<>(root);
        } catch (IOException | YAMLException e) {
            throw new InvalidItemDefinitionException("Failed to read file '" + file + "': " + e.getMessage());
        }
    }

    static void dump(Path file, Map<String, Object> root) {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        Yaml yaml = new Yaml(options);
        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file)) {
                yaml.dump(root, writer);
            }
        } catch (IOException e) {
            throw new InvalidItemDefinitionException("Failed to write file '" + file + "': " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> section(Map<String, Object> root, String sectionName) {
        return (Map<String, Object>) root.computeIfAbsent(sectionName, key -> new LinkedHashMap<String, Object>());
    }
}
