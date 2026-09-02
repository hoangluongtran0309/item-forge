package com.hoangluongtran0309.itemforge.dashboard.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.hoangluongtran0309.itemforge.dashboard.dto.ArmorJson;
import com.hoangluongtran0309.itemforge.dashboard.form.ArmorForm;

class ArmorFormMapperTest {

    private final ArmorFormMapper mapper = new ArmorFormMapper();

    @Test
    void toJsonSplitsLoreTextIntoLines() {
        ArmorForm form = new ArmorForm();
        form.setId("void_helmet");
        form.setMaterial("NETHERITE_HELMET");
        form.setSlot("HELMET");
        form.setDisplayName("Dragon Helmet");
        form.setLoreText("Forged in fire.\nHandle with care.");

        ArmorJson json = mapper.toJson(form);

        assertEquals(List.of("Forged in fire.", "Handle with care."), json.lore());
    }

    @Test
    void toJsonTreatsBlankArmorAssetIdAsNull() {
        ArmorForm form = new ArmorForm();
        form.setId("void_helmet");
        form.setMaterial("NETHERITE_HELMET");
        form.setSlot("HELMET");
        form.setDisplayName("Dragon Helmet");
        form.setLoreText("");
        form.setArmorAssetId("   ");

        ArmorJson json = mapper.toJson(form);

        assertNull(json.armorAssetId());
    }

    @Test
    void fromJsonThenToJsonRoundTrips() {
        ArmorJson original = new ArmorJson("void_helmet", "NETHERITE_HELMET", "HELMET", "void_set",
                "Dragon Helmet", List.of("Forged in fire."));

        ArmorForm form = mapper.fromJson(original);
        ArmorJson roundTripped = mapper.toJson(form);

        assertEquals(original, roundTripped);
    }
}
