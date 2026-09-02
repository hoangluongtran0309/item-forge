package com.hoangluongtran0309.infrastructure.bukkit.block;

import java.util.Locale;

import org.bukkit.Instrument;
import org.bukkit.Material;
import org.bukkit.Note;
import org.bukkit.block.data.type.NoteBlock;

import com.hoangluongtran0309.domain.exception.InvalidItemDefinitionException;
import com.hoangluongtran0309.domain.model.CustomBlockDefinition;

// Converts a CustomBlockDefinition into real Note Block BlockData. This is the ONLY
// place in the whole feature that turns "instrument" (a String read from YAML) into a
// real org.bukkit.Instrument -- both CustomBlockPlaceListener and
// CustomBlockInstrumentGuardListener must go through this class so the BlockData written
// on placement and the BlockData restored on drift are IDENTICAL.
public class NoteBlockStateFactory {

    public NoteBlock createBlockData(CustomBlockDefinition definition) {
        NoteBlock data = (NoteBlock) Material.NOTE_BLOCK.createBlockData();
        data.setInstrument(resolveInstrument(definition.instrument()));
        data.setNote(new Note(definition.note()));
        return data;
    }

    // Split out from createBlockData(): this returns only the Instrument enum, touching
    // neither Material.NOTE_BLOCK.createBlockData() nor a real Bukkit world. It lets
    // BlocksApiHandler validate an instrument name when the dashboard creates or edits a
    // block, where building real BlockData is neither possible nor desirable.
    public Instrument resolveInstrument(String rawInstrument) {
        try {
            return Instrument.valueOf(rawInstrument.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new InvalidItemDefinitionException("Unknown instrument: " + rawInstrument);
        }
    }
}
