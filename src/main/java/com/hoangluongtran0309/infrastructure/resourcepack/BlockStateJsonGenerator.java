package com.hoangluongtran0309.infrastructure.resourcepack;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.bukkit.Instrument;

import com.hoangluongtran0309.domain.model.CustomBlockDefinition;

// Generates assets/minecraft/blockstates/note_block.json, COMPLETELY REPLACING the
// vanilla original (which has a single empty "" variant covering EVERY state --
// confirmed by pulling the real file from the official asset set). "variants" cannot
// mix a wildcard with specific keys: if SOME combinations need their own texture, ALL
// remaining combinations MUST be declared explicitly (mapping back to the default
// vanilla model), with none left out. Otherwise the client shows the missing-texture
// block for Note Blocks that have nothing to do with ItemForge.
public class BlockStateJsonGenerator {

    private static final String VANILLA_DEFAULT_MODEL = "minecraft:block/note_block";
    private static final int NOTE_COUNT = 25;

    // Minecraft's real blockstate property names for each Instrument value -- these do
    // NOT match the Bukkit enum names (e.g. BASS_GUITAR -> "bass", STICKS -> "hat").
    private static final Map<Instrument, String> BLOCKSTATE_NAMES = buildBlockstateNames();

    public void write(List<CustomBlockDefinition> blocks, String namespace, Path outputDir) throws IOException {
        Map<String, CustomBlockDefinition> comboIndex = indexByCombo(blocks);

        StringBuilder variants = new StringBuilder();
        boolean first = true;
        for (Instrument instrument : Instrument.values()) {
            String instrumentName = BLOCKSTATE_NAMES.get(instrument);
            for (int note = 0; note < NOTE_COUNT; note++) {
                String model = comboIndex.containsKey(comboKey(instrument, note))
                        ? namespace + ":block/" + comboIndex.get(comboKey(instrument, note)).textureId()
                        : VANILLA_DEFAULT_MODEL;

                for (boolean powered : new boolean[] { false, true }) {
                    if (!first) {
                        variants.append(",\n");
                    }
                    first = false;
                    variants.append("    \"instrument=%s,note=%d,powered=%b\": { \"model\": \"%s\" }"
                            .formatted(instrumentName, note, powered, model));
                }
            }
        }

        Path target = outputDir.resolve("assets/minecraft/blockstates/note_block.json");
        Files.createDirectories(target.getParent());
        Files.writeString(target, """
                {
                  "variants": {
                %s
                  }
                }
                """.formatted(variants));
    }

    private Map<String, CustomBlockDefinition> indexByCombo(List<CustomBlockDefinition> blocks) {
        Map<String, CustomBlockDefinition> index = new HashMap<>();
        for (CustomBlockDefinition block : blocks) {
            index.put(block.instrument().toUpperCase(Locale.ROOT) + ":" + block.note(), block);
        }
        return index;
    }

    private String comboKey(Instrument instrument, int note) {
        return instrument.name() + ":" + note;
    }

    private static Map<Instrument, String> buildBlockstateNames() {
        Map<Instrument, String> names = new EnumMap<>(Instrument.class);
        names.put(Instrument.PIANO, "harp");
        names.put(Instrument.BASS_DRUM, "basedrum");
        names.put(Instrument.SNARE_DRUM, "snare");
        names.put(Instrument.STICKS, "hat");
        names.put(Instrument.BASS_GUITAR, "bass");
        names.put(Instrument.FLUTE, "flute");
        names.put(Instrument.BELL, "bell");
        names.put(Instrument.GUITAR, "guitar");
        names.put(Instrument.CHIME, "chime");
        names.put(Instrument.XYLOPHONE, "xylophone");
        names.put(Instrument.IRON_XYLOPHONE, "iron_xylophone");
        names.put(Instrument.COW_BELL, "cow_bell");
        names.put(Instrument.DIDGERIDOO, "didgeridoo");
        names.put(Instrument.BIT, "bit");
        names.put(Instrument.BANJO, "banjo");
        names.put(Instrument.PLING, "pling");
        names.put(Instrument.ZOMBIE, "zombie");
        names.put(Instrument.SKELETON, "skeleton");
        names.put(Instrument.CREEPER, "creeper");
        names.put(Instrument.DRAGON, "dragon");
        names.put(Instrument.WITHER_SKELETON, "wither_skeleton");
        names.put(Instrument.PIGLIN, "piglin");
        names.put(Instrument.CUSTOM_HEAD, "custom_head");
        return names;
    }
}
