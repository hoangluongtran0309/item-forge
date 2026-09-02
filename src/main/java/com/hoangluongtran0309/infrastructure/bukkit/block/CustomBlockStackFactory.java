package com.hoangluongtran0309.infrastructure.bukkit.block;

import java.util.List;
import java.util.Optional;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import com.hoangluongtran0309.domain.model.CustomBlockDefinition;
import com.hoangluongtran0309.infrastructure.bukkit.model.BlockModelStrategy;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

// Mirrors ItemStackFactory/ArmorStackFactory exactly: one dedicated NamespacedKey.
// Follows the "<domain>-id" convention: the PDC tag is written in create() and read
// back when CustomBlockPlaceListener needs to know whether the held item is a custom
// block.
public class CustomBlockStackFactory {

    private static final LegacyComponentSerializer COLOR_SERIALIZER = LegacyComponentSerializer.legacyAmpersand();

    private final BlockModelStrategy modelStrategy;
    private final NamespacedKey blockIdKey;

    public CustomBlockStackFactory(Plugin plugin, BlockModelStrategy modelStrategy) {
        this.modelStrategy = modelStrategy;
        this.blockIdKey = new NamespacedKey(plugin, "block-id");
    }

    public ItemStack create(CustomBlockDefinition definition) {
        ItemStack item = new ItemStack(Material.NOTE_BLOCK);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(COLOR_SERIALIZER.deserialize(definition.displayName()));
        meta.lore(toLoreComponents(definition.lore()));

        modelStrategy.applyModel(meta, definition);

        meta.getPersistentDataContainer().set(blockIdKey, PersistentDataType.STRING, definition.id());

        item.setItemMeta(meta);
        return item;
    }

    public Optional<String> extractBlockId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return Optional.empty();
        }

        String id = item.getItemMeta().getPersistentDataContainer().get(blockIdKey, PersistentDataType.STRING);
        return Optional.ofNullable(id);
    }

    private List<Component> toLoreComponents(List<String> lore) {
        return lore.stream().<Component>map(COLOR_SERIALIZER::deserialize).toList();
    }
}
