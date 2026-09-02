package com.hoangluongtran0309.itemforge.dashboard.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.hoangluongtran0309.itemforge.dashboard.dto.BlockJson;
import com.hoangluongtran0309.itemforge.dashboard.form.BlockForm;

class BlockFormMapperTest {

    private final BlockFormMapper mapper = new BlockFormMapper();

    @Test
    void toJsonSplitsLoreTextIntoLines() {
        BlockForm form = new BlockForm();
        form.setId("void_netherite_block");
        form.setInstrument("BASS_GUITAR");
        form.setNote(12);
        form.setTextureId("void_netherite_block");
        form.setDropItemId("GLOWSTONE");
        form.setDisplayName("Glowing Lantern");
        form.setCustomModelData(3001);
        form.setLoreText("A decorative lantern.\nGlows softly.");

        BlockJson json = mapper.toJson(form);

        assertEquals(List.of("A decorative lantern.", "Glows softly."), json.lore());
    }

    @Test
    void toJsonDefaultsNullNoteAndCustomModelDataToZero() {
        BlockForm form = new BlockForm();
        form.setId("void_netherite_block");
        form.setInstrument("BASS_GUITAR");
        form.setTextureId("void_netherite_block");
        form.setDropItemId("GLOWSTONE");
        form.setDisplayName("Glowing Lantern");
        form.setLoreText("");

        BlockJson json = mapper.toJson(form);

        assertEquals(0, json.note());
        assertEquals(0, json.customModelData());
    }

    @Test
    void fromJsonThenToJsonRoundTrips() {
        BlockJson original = new BlockJson("void_netherite_block", "BASS_GUITAR", 12, "void_netherite_block", "GLOWSTONE",
                "Glowing Lantern", 3001, List.of("A decorative lantern."));

        BlockForm form = mapper.fromJson(original);
        BlockJson roundTripped = mapper.toJson(form);

        assertEquals(original, roundTripped);
    }
}
