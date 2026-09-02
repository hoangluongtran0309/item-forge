package com.hoangluongtran0309.itemforge.dashboard.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import com.hoangluongtran0309.itemforge.dashboard.dto.BlockJson;
import com.hoangluongtran0309.itemforge.dashboard.form.BlockForm;

@Component
public class BlockFormMapper {

    public BlockJson toJson(BlockForm form) {
        int note = form.getNote() == null ? 0 : form.getNote();
        int customModelData = form.getCustomModelData() == null ? 0 : form.getCustomModelData();
        List<String> lore = splitLore(form.getLoreText());

        return new BlockJson(form.getId(), form.getInstrument(), note, form.getTextureId(), form.getDropItemId(),
                form.getDisplayName(), customModelData, lore);
    }

    public BlockForm fromJson(BlockJson json) {
        BlockForm form = new BlockForm();
        form.setId(json.id());
        form.setInstrument(json.instrument());
        form.setNote(json.note());
        form.setTextureId(json.textureId());
        form.setDropItemId(json.dropItemId());
        form.setDisplayName(json.displayName());
        form.setCustomModelData(json.customModelData());
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
