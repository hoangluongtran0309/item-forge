package com.hoangluongtran0309.infrastructure.config;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.hoangluongtran0309.domain.exception.InvalidItemDefinitionException;
import com.hoangluongtran0309.domain.model.ArmorDefinition;
import com.hoangluongtran0309.domain.model.ArmorSlot;

/**
 * Converts between ArmorDefinition and the Map<String,Object> structure matching armor.yml's
 * kebab-case schema. Follows the same model as ItemDefinitionMapper.
 */
public final class ArmorDefinitionMapper {

    private ArmorDefinitionMapper() {
    }

    public static ArmorDefinition fromMap(String id, Map<String, Object> armorMap) {
        String material = requireString(armorMap, "material");
        ArmorSlot slot = ArmorSlot.valueOf(requireString(armorMap, "slot"));
        Object armorAssetIdValue = armorMap.get("armor-asset-id");
        String armorAssetId = armorAssetIdValue == null ? null : armorAssetIdValue.toString();
        int customModelData = intValue(armorMap.get("custom-model-data"), 0);
        String displayName = armorMap.getOrDefault("display-name", id).toString();
        List<String> lore = stringList(armorMap.get("lore"));

        return new ArmorDefinition(id, material, slot, armorAssetId, customModelData, displayName, lore);
    }

    public static Map<String, Object> toMap(ArmorDefinition definition) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("material", definition.material());
        map.put("slot", definition.slot().name());
        map.put("armor-asset-id", definition.armorAssetId());
        map.put("custom-model-data", definition.customModelData());
        map.put("display-name", definition.displayName());
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
