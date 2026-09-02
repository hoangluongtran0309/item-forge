package com.hoangluongtran0309.infrastructure.bukkit.block;

import java.util.List;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;

// tagService is a Location -> id map kept OUTSIDE the block (a Note Block has no
// TileState/PDC -- see CustomBlockTagService). When a piston pushes or pulls a block,
// vanilla moves the BlockData (instrument/note) along with it, but our map does NOT
// follow on its own. Without migrating it, the old position keeps a "ghost" tag (a
// vanilla Note Block later placed there would be mistaken for a custom block) and the
// block that actually moved loses its tag (breaking it would drop a plain note block
// instead of the right item). This listener only rewrites the KEY in the registry; it
// never touches BlockData, which vanilla already handles.
public class CustomBlockPistonListener implements Listener {

    private final CustomBlockTagService tagService;

    public CustomBlockPistonListener(CustomBlockTagService tagService) {
        this.tagService = tagService;
    }

    @EventHandler
    public void onPistonExtend(BlockPistonExtendEvent event) {
        migrateAll(event.getBlocks(), event.getDirection());
    }

    @EventHandler
    public void onPistonRetract(BlockPistonRetractEvent event) {
        migrateAll(event.getBlocks(), event.getDirection());
    }

    private void migrateAll(List<Block> movedBlocks, BlockFace direction) {
        for (Block moved : movedBlocks) {
            tagService.lookup(moved).ifPresent(id -> tagService.move(moved, moved.getRelative(direction)));
        }
    }
}
