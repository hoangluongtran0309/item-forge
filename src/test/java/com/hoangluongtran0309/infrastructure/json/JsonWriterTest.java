package com.hoangluongtran0309.infrastructure.json;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

class JsonWriterTest {

    @Test
    void writesScalarsCorrectly() {
        assertEquals("null", JsonWriter.write(null));
        assertEquals("42", JsonWriter.write(42));
        assertEquals("15.5", JsonWriter.write(15.5));
        assertEquals("true", JsonWriter.write(true));
    }

    @Test
    void escapesSpecialCharactersInStrings() {
        String written = JsonWriter.write("line1\nline2 \"quoted\" back\\slash");
        assertEquals("\"line1\\nline2 \\\"quoted\\\" back\\\\slash\"", written);
    }

    @Test
    void writesNestedObjectsAndArrays() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", "void_sword");
        map.put("lore", List.of("A blazing blade.", "Handle with care."));
        map.put("nested", Map.of("a", 1));

        String written = JsonWriter.write(map);

        // Verified by parsing it back with SnakeYAML (JSON is a subset of YAML) rather
        // than comparing character by character, so the assertion does not depend on key
        // order.
        Map<String, Object> parsed = new Yaml().load(written);
        assertEquals("void_sword", parsed.get("id"));
        assertEquals(List.of("A blazing blade.", "Handle with care."), parsed.get("lore"));
    }

    @Test
    void producesStrictJsonThatAnyStandardParserAccepts() {
        Map<String, Object> map = Map.of("key with space", "value", "number", 7);
        String written = JsonWriter.write(map);

        assertEquals(Map.of("key with space", "value", "number", 7), new Yaml().load(written));
        // strict JSON requires every key/string to be double-quoted
        org.junit.jupiter.api.Assertions.assertTrue(written.contains("\"key with space\""));
    }
}
