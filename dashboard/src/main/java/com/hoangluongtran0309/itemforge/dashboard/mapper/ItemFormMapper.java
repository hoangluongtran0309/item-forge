package com.hoangluongtran0309.itemforge.dashboard.mapper;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.hoangluongtran0309.itemforge.dashboard.dto.AbilityJson;
import com.hoangluongtran0309.itemforge.dashboard.dto.DamageBonusAbilityJson;
import com.hoangluongtran0309.itemforge.dashboard.dto.ItemJson;
import com.hoangluongtran0309.itemforge.dashboard.dto.PotionEffectAbilityJson;
import com.hoangluongtran0309.itemforge.dashboard.form.AbilityFormRow;
import com.hoangluongtran0309.itemforge.dashboard.form.ItemForm;

@Component
public class ItemFormMapper {

    public ItemJson toJson(ItemForm form) {
        List<AbilityJson> abilities = new ArrayList<>();
        for (AbilityFormRow row : form.getAbilities()) {
            AbilityJson ability = toAbilityJson(row);
            if (ability != null) {
                abilities.add(ability);
            }
        }

        int customModelData = form.getCustomModelData() == null ? 0 : form.getCustomModelData();
        List<String> lore = splitLore(form.getLoreText());

        return new ItemJson(form.getId(), form.getMaterial(), customModelData, form.getDisplayName(), lore,
                abilities);
    }

    public ItemForm fromJson(ItemJson json) {
        ItemForm form = new ItemForm();
        form.setId(json.id());
        form.setMaterial(json.material());
        form.setCustomModelData(json.customModelData());
        form.setDisplayName(json.displayName());
        form.setLoreText(String.join("\n", json.lore()));

        List<AbilityFormRow> abilities = new ArrayList<>();
        for (AbilityJson ability : json.abilities()) {
            abilities.add(toFormRow(ability));
        }
        form.setAbilities(abilities);

        return form;
    }

    // Skip a row missing required data rather than sending an invalid ability to the API
    // -- consistent with the plugin's "skip invalid entries" philosophy (see
    // YamlConfigAdapter.loadAll).
    private AbilityJson toAbilityJson(AbilityFormRow row) {
        if (isBlank(row.getType()) || isBlank(row.getTrigger()) || row.getCooldownSeconds() == null) {
            return null;
        }

        return switch (row.getType()) {
            case "POTION_EFFECT" -> {
                if (isBlank(row.getEffect()) || row.getDurationSeconds() == null) {
                    yield null;
                }
                yield new PotionEffectAbilityJson(row.getType(), row.getTrigger(), row.getCooldownSeconds(),
                        row.getEffect(), row.getDurationSeconds());
            }
            case "DAMAGE_BONUS" -> {
                if (row.getBonusPercent() == null) {
                    yield null;
                }
                yield new DamageBonusAbilityJson(row.getType(), row.getTrigger(), row.getCooldownSeconds(),
                        row.getBonusPercent());
            }
            default -> null;
        };
    }

    private AbilityFormRow toFormRow(AbilityJson ability) {
        AbilityFormRow row = new AbilityFormRow();
        row.setType(ability.type());
        row.setTrigger(ability.trigger());

        switch (ability) {
            case PotionEffectAbilityJson potion -> {
                row.setCooldownSeconds(potion.cooldownSeconds());
                row.setEffect(potion.effect());
                row.setDurationSeconds(potion.durationSeconds());
            }
            case DamageBonusAbilityJson damage -> {
                row.setCooldownSeconds(damage.cooldownSeconds());
                row.setBonusPercent(damage.bonusPercent());
            }
        }

        return row;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static List<String> splitLore(String loreText) {
        if (loreText == null || loreText.isBlank()) {
            return List.of();
        }
        return loreText.lines().map(String::trim).filter(line -> !line.isEmpty()).toList();
    }
}
