package com.hoangluongtran0309.infrastructure.config;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.hoangluongtran0309.domain.exception.InvalidItemDefinitionException;
import com.hoangluongtran0309.domain.model.CustomBlockDefinition;

/**
 * Converts between CustomBlockDefinition and the Map<String,Object> structure matching
 * blocks.yml's kebab-case schema. Follows the same model as ItemDefinitionMapper
 * / ArmorDefinitionMapper.
 */
public final class CustomBlockDefinitionMapper {

    private CustomBlockDefinitionMapper() {
    }

    public static CustomBlockDefinition fromMap(String id, Map<String, Object> blockMap) {
        String instrument = requireString(blockMap, "instrument");
        int note = intValue(blockMap.get("note"), 0);
        String textureId = requireString(blockMap, "texture-id");
        String dropItemId = requireString(blockMap, "drop-item-id");
        String displayName = blockMap.getOrDefault("display-name", id).toString();
        int customModelData = intValue(blockMap.get("custom-model-data"), 0);
        List<String> lore = stringList(blockMap.get("lore"));

        return new CustomBlockDefinition(id, instrument, note, textureId, dropItemId, displayName, customModelData,
                lore);
    }

    public static Map<String, Object> toMap(CustomBlockDefinition definition) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("material", "NOTE_BLOCK");
        map.put("instrument", definition.instrument());
        map.put("note", definition.note());
        map.put("texture-id", definition.textureId());
        map.put("drop-item-id", definition.dropItemId());
        map.put("display-name", definition.displayName());
        map.put("custom-model-data", definition.customModelData());
        map.put("lore", definition.lore());
        return map;
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> asMap(Object value) {
        if (!(value instanceof Map)) {
            throw new InvalidItemDefinitionException("Expected a mapping but found: " + value);
        }
        return (Map<String, Object>) value;
    }

    private static String requireString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            throw new InvalidItemDefinitionException("Missing required field: " + key);
        }
        return value.toString();
    }

    @SuppressWarnings("unchecked")
    private static List<String> stringList(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        return ((List<Object>) list).stream().map(Object::toString).toList();
    }

    private static int intValue(Object value, int defaultValue) {
        return value == null ? defaultValue : ((Number) value).intValue();
    }
}
