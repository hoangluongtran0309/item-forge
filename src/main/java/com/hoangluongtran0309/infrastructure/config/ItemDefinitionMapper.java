package com.hoangluongtran0309.infrastructure.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.hoangluongtran0309.domain.exception.InvalidItemDefinitionException;
import com.hoangluongtran0309.domain.model.AbilityDefinition;
import com.hoangluongtran0309.domain.model.DamageBonusAbilityDefinition;
import com.hoangluongtran0309.domain.model.EffectCommand;
import com.hoangluongtran0309.domain.model.ItemDefinition;
import com.hoangluongtran0309.domain.model.PotionEffectAbilityDefinition;
import com.hoangluongtran0309.domain.model.TriggerType;

/**
 * Converts between ItemDefinition and the Map<String,Object> structure matching items.yml's
 * kebab-case schema. Shared by YamlConfigAdapter (reading and writing YAML) and
 * ClaudeAiAdapter (parsing the JSON returned by the API -- JSON is a subset of YAML, so it
 * yields the same Map/List shapes), which avoids duplicating the parse logic.
 */
public final class ItemDefinitionMapper {

    private ItemDefinitionMapper() {
    }

    public static ItemDefinition fromMap(String id, Map<String, Object> itemMap) {
        String material = requireString(itemMap, "material");
        int customModelData = intValue(itemMap.get("custom-model-data"), 0);
        String displayName = itemMap.getOrDefault("display-name", id).toString();
        List<String> lore = stringList(itemMap.get("lore"));
        List<AbilityDefinition> abilities = parseAbilities(itemMap.get("abilities"));

        return new ItemDefinition(id, material, customModelData, displayName, lore, abilities);
    }

    public static Map<String, Object> toMap(ItemDefinition definition) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("material", definition.material());
        map.put("custom-model-data", definition.customModelData());
        map.put("display-name", definition.displayName());
        map.put("lore", definition.lore());
        map.put("abilities", definition.abilities().stream().map(ItemDefinitionMapper::abilityToMap).toList());
        return map;
    }

    private static Map<String, Object> abilityToMap(AbilityDefinition ability) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("trigger", ability.trigger().name());
        map.put("cooldown-seconds", ability.cooldownSeconds());

        switch (ability) {
            case PotionEffectAbilityDefinition potion -> {
                map.put("type", "POTION_EFFECT");
                map.put("effect", potion.effectType().name());
                map.put("duration-seconds", potion.durationSeconds());
            }
            case DamageBonusAbilityDefinition damage -> {
                map.put("type", "DAMAGE_BONUS");
                map.put("bonus-percent", damage.bonusPercent());
            }
        }

        return map;
    }

    private static List<AbilityDefinition> parseAbilities(Object rawAbilities) {
        if (!(rawAbilities instanceof List<?> abilitiesList)) {
            return List.of();
        }

        List<AbilityDefinition> abilities = new ArrayList<>();
        for (Object rawAbility : abilitiesList) {
            abilities.add(parseAbility(asMap(rawAbility)));
        }
        return abilities;
    }

    private static AbilityDefinition parseAbility(Map<String, Object> abilityMap) {
        String type = requireString(abilityMap, "type");
        TriggerType trigger = TriggerType.valueOf(requireString(abilityMap, "trigger"));
        int cooldownSeconds = intValue(abilityMap.get("cooldown-seconds"), 0);

        return switch (type) {
            case "POTION_EFFECT" -> {
                EffectCommand.EffectType effectType = EffectCommand.EffectType
                        .valueOf(requireString(abilityMap, "effect"));
                int durationSeconds = intValue(abilityMap.get("duration-seconds"), 0);
                yield new PotionEffectAbilityDefinition(trigger, effectType, durationSeconds, cooldownSeconds);
            }
            case "DAMAGE_BONUS" -> {
                double bonusPercent = doubleValue(abilityMap.get("bonus-percent"), 0);
                yield new DamageBonusAbilityDefinition(trigger, cooldownSeconds, bonusPercent);
            }
            default -> throw new InvalidItemDefinitionException("Unknown ability type: " + type);
        };
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

    private static double doubleValue(Object value, double defaultValue) {
        return value == null ? defaultValue : ((Number) value).doubleValue();
    }
}
