package com.hoangluongtran0309.infrastructure.bukkit.block;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

// An explosion (TNT, a creeper) breaking a block does NOT fire BlockBreakEvent, so
// CustomBlockBreakListener never runs and the tag in CustomBlockTagService would be
// orphaned forever unless it is cleaned up here. This only clears the tag and never
// spawns drops of its own -- the explosion decides what, if anything, drops under its
// own vanilla rules.
public class CustomBlockExplosionListener implements Listener {

    private final CustomBlockTagService tagService;

    public CustomBlockExplosionListener(CustomBlockTagService tagService) {
        this.tagService = tagService;
    }

    @EventHandler
    public void onBlockExplode(BlockExplodeEvent event) {
        untagAll(event.blockList());
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        untagAll(event.blockList());
    }

    private void untagAll(List<Block> blocks) {
        for (Block block : blocks) {
            if (block.getType() == Material.NOTE_BLOCK) {
                tagService.untag(block);
            }
        }
    }
}
