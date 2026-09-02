package com.hoangluongtran0309.infrastructure.ai;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

import com.hoangluongtran0309.application.exception.AiRequestException;
import com.hoangluongtran0309.infrastructure.json.JsonWriter;

/**
 * Helpers shared by the four provider adapters: escaping strings while building a JSON request
 * by hand (no JSON library), parsing a chunk of text into a Map with SnakeYAML (JSON is a subset
 * of YAML, and the plugin already uses SnakeYAML for items.yml, so no new JSON library is needed
 * -- in line with keeping the plugin lightweight and free of maven-shade-plugin), and rewriting a
 * standard JSON Schema into the dialect Gemini expects.
 */
final class AiJsonSupport {

    private AiJsonSupport() {
    }

    static String escapeJson(String value) {
        StringBuilder sb = new StringBuilder(value.length() + 16);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> parseJsonObject(String text, String errorContext) {
        Object parsed;
        try {
            parsed = new Yaml().load(text);
        } catch (YAMLException e) {
            throw new AiRequestException(errorContext + ": " + e.getMessage());
        }

        if (!(parsed instanceof Map<?, ?> map)) {
            throw new AiRequestException(errorContext + ": empty or non-object response");
        }

        return (Map<String, Object>) map;
    }

    /**
     * Rewrites a standard JSON Schema into the trimmed-down OpenAPI 3.0 dialect Gemini's
     * responseSchema expects, whose only difference for the schemas used here is that type names
     * are uppercase (OBJECT, STRING, ARRAY, INTEGER, NUMBER, BOOLEAN).
     */
    static String toGeminiSchema(String jsonSchema) {
        Map<String, Object> schema = parseJsonObject(jsonSchema, "Failed to parse the response schema");
        return JsonWriter.write(uppercaseTypeNames(schema));
    }

    private static Object uppercaseTypeNames(Object node) {
        if (node instanceof Map<?, ?> map) {
            Map<String, Object> converted = new LinkedHashMap<>();
            map.forEach((key, value) -> {
                String name = String.valueOf(key);
                if ("type".equals(name) && value instanceof String type) {
                    converted.put(name, type.toUpperCase(Locale.ROOT));
                } else {
                    converted.put(name, uppercaseTypeNames(value));
                }
            });
            return converted;
        }

        if (node instanceof List<?> list) {
            return list.stream().map(AiJsonSupport::uppercaseTypeNames).toList();
        }

        return node;
    }
}
