package com.hoangluongtran0309.infrastructure.bukkit.block;

import java.util.Optional;
import java.util.logging.Logger;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import com.hoangluongtran0309.domain.CustomBlockRegistry;
import com.hoangluongtran0309.domain.ItemRegistry;
import com.hoangluongtran0309.domain.model.CustomBlockDefinition;
import com.hoangluongtran0309.infrastructure.bukkit.ItemStackFactory;

public class CustomBlockBreakListener implements Listener {

    private final CustomBlockRegistry registry;
    private final CustomBlockTagService tagService;
    private final ItemRegistry itemRegistry;
    private final ItemStackFactory itemStackFactory;
    private final Logger logger;

    public CustomBlockBreakListener(CustomBlockRegistry registry, CustomBlockTagService tagService,
            ItemRegistry itemRegistry, ItemStackFactory itemStackFactory, Logger logger) {
        this.registry = registry;
        this.tagService = tagService;
        this.itemRegistry = itemRegistry;
        this.itemStackFactory = itemStackFactory;
        this.logger = logger;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.NOTE_BLOCK) {
            return;
        }

        Optional<String> blockId = tagService.lookup(block);
        // THE MOST IMPORTANT LINE in this listener: a Note Block a player placed
        // themselves (not via /itemforge give) is NOT in tagService, so this returns empty
        // and we do nothing, letting vanilla drop an ordinary note block.
        if (blockId.isEmpty()) {
            return;
        }

        event.setDropItems(false);
        tagService.untag(block);

        Optional<CustomBlockDefinition> definition = registry.get(blockId.get());
        if (definition.isEmpty()) {
            // The config edited or deleted this definition after the block was already
            // placed in the world -- clear the tag and drop nothing, rather than throwing.
            logger.warning("Broke a tagged custom block '" + blockId.get()
                    + "' that no longer exists in blocks.yml; dropping nothing.");
            return;
        }

        resolveDrop(definition.get())
                .ifPresent(drop -> block.getWorld().dropItemNaturally(block.getLocation(), drop));
    }

    private Optional<ItemStack> resolveDrop(CustomBlockDefinition definition) {
        String dropItemId = definition.dropItemId();

        Optional<com.hoangluongtran0309.domain.model.ItemDefinition> customItem = itemRegistry.get(dropItemId);
        if (customItem.isPresent()) {
            return Optional.of(itemStackFactory.create(customItem.get()));
        }

        Material material = Material.matchMaterial(dropItemId);
        if (material != null) {
            return Optional.of(new ItemStack(material));
        }

        logger.warning("Custom block '" + definition.id() + "' has drop-item-id '" + dropItemId
                + "' that matches neither a registered item nor a vanilla material; dropping nothing.");
        return Optional.empty();
    }
}
