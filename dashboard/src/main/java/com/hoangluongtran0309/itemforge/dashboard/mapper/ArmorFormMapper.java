package com.hoangluongtran0309.itemforge.dashboard.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import com.hoangluongtran0309.itemforge.dashboard.dto.ArmorJson;
import com.hoangluongtran0309.itemforge.dashboard.form.ArmorForm;

@Component
public class ArmorFormMapper {

    public ArmorJson toJson(ArmorForm form) {
        List<String> lore = splitLore(form.getLoreText());
        String armorAssetId = form.getArmorAssetId() == null || form.getArmorAssetId().isBlank()
                ? null
                : form.getArmorAssetId();

        return new ArmorJson(form.getId(), form.getMaterial(), form.getSlot(), armorAssetId, form.getDisplayName(),
                lore);
    }

    public ArmorForm fromJson(ArmorJson json) {
        ArmorForm form = new ArmorForm();
        form.setId(json.id());
        form.setMaterial(json.material());
        form.setSlot(json.slot());
        form.setArmorAssetId(json.armorAssetId());
        form.setDisplayName(json.displayName());
        form.setLoreText(String.join("\n", json.lore()));
        return form;
    }

    private static List<String> splitLore(String loreText) {
        if (loreText == null || loreText.isBlank()) {
            return List.of();
        }
        return loreText.lines().map(String::trim).filter(line -> !line.isEmpty()).toList();
    }
}
