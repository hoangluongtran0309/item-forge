package com.hoangluongtran0309.infrastructure.bukkit.block;

import java.util.Optional;
import java.util.logging.Logger;

import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

import com.hoangluongtran0309.domain.CustomBlockRegistry;
import com.hoangluongtran0309.domain.exception.InvalidItemDefinitionException;
import com.hoangluongtran0309.domain.model.CustomBlockDefinition;

public class CustomBlockPlaceListener implements Listener {

    private final CustomBlockRegistry registry;
    private final CustomBlockStackFactory stackFactory;
    private final NoteBlockStateFactory blockStateFactory;
    private final CustomBlockTagService tagService;
    private final Logger logger;

    public CustomBlockPlaceListener(CustomBlockRegistry registry, CustomBlockStackFactory stackFactory,
            NoteBlockStateFactory blockStateFactory, CustomBlockTagService tagService, Logger logger) {
        this.registry = registry;
        this.stackFactory = stackFactory;
        this.blockStateFactory = blockStateFactory;
        this.tagService = tagService;
        this.logger = logger;
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Optional<String> blockId = stackFactory.extractBlockId(event.getItemInHand());
        // THE MOST IMPORTANT LINE in the whole feature: when the held item is not a custom
        // block (an ordinary vanilla Note Block, say), do NOTHING and let vanilla behave
        // normally.
        if (blockId.isEmpty()) {
            return;
        }

        Optional<CustomBlockDefinition> definition = registry.get(blockId.get());
        if (definition.isEmpty()) {
            event.getPlayer().sendMessage("This custom block no longer exists in the config.");
            event.setCancelled(true);
            return;
        }

        Block block = event.getBlockPlaced();
        try {
            block.setBlockData(blockStateFactory.createBlockData(definition.get()), true);
        } catch (InvalidItemDefinitionException e) {
            logger.warning("Failed to place custom block '" + blockId.get() + "': " + e.getMessage());
            event.setCancelled(true);
            return;
        }

        tagService.tag(block, definition.get().id());
    }
}
