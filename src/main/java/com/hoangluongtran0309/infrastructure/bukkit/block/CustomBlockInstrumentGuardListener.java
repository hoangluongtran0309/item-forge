package com.hoangluongtran0309.infrastructure.bukkit.block;

import java.util.Optional;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.NoteBlock;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPhysicsEvent;

import com.hoangluongtran0309.domain.CustomBlockRegistry;
import com.hoangluongtran0309.domain.model.CustomBlockDefinition;

// Vanilla RECOMPUTES a Note Block's "instrument" property on every neighbour update,
// based on the block DIRECTLY BELOW it, overwriting whatever value the plugin forced
// when the block was placed. This was confirmed against the real vanilla mechanism, not
// assumed. "note" is NOT affected (it only changes via right-click /
// NoteBlockChangeEvent). Without a correction the block's texture would "spontaneously"
// change or disappear whenever a nearby block changed -- someone building next to it,
// for instance. This listener compares and restores the value as soon as it drifts.
public class CustomBlockInstrumentGuardListener implements Listener {

    private final CustomBlockRegistry registry;
    private final CustomBlockTagService tagService;
    private final NoteBlockStateFactory blockStateFactory;

    public CustomBlockInstrumentGuardListener(CustomBlockRegistry registry, CustomBlockTagService tagService,
            NoteBlockStateFactory blockStateFactory) {
        this.registry = registry;
        this.tagService = tagService;
        this.blockStateFactory = blockStateFactory;
    }

    @EventHandler
    public void onBlockPhysics(BlockPhysicsEvent event) {
        // BlockPhysicsEvent fires THOUSANDS OF TIMES PER SECOND on a busy server (see
        // Paper's own javadoc), so the cheapest fast path -- the Material check -- MUST
        // come first, before touching tagService.
        Block block = event.getBlock();
        if (block.getType() != Material.NOTE_BLOCK) {
            return;
        }

        Optional<String> blockId = tagService.lookup(block);
        if (blockId.isEmpty()) {
            return;
        }

        Optional<CustomBlockDefinition> definition = registry.get(blockId.get());
        if (definition.isEmpty()) {
            return;
        }

        NoteBlock expected = blockStateFactory.createBlockData(definition.get());
        NoteBlock current = (NoteBlock) block.getBlockData();
        if (current.getInstrument() != expected.getInstrument() || !current.getNote().equals(expected.getNote())) {
            // applyPhysics=false is MANDATORY; otherwise this re-triggers the very
            // BlockPhysicsEvent we are handling -> infinite loop.
            block.setBlockData(expected, false);
        }
    }
}
