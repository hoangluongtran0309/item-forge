package com.hoangluongtran0309.infrastructure.bukkit;

import java.util.List;
import java.util.Optional;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import com.hoangluongtran0309.domain.model.ItemDefinition;
import com.hoangluongtran0309.infrastructure.bukkit.model.ItemModelStrategy;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class ItemStackFactory {

    private static final LegacyComponentSerializer COLOR_SERIALIZER = LegacyComponentSerializer.legacyAmpersand();

    private final ItemModelStrategy modelStrategy;
    private final NamespacedKey itemIdKey;

    public ItemStackFactory(Plugin plugin, ItemModelStrategy modelStrategy) {
        this.modelStrategy = modelStrategy;
        this.itemIdKey = new NamespacedKey(plugin, "item-id");
    }

    public ItemStack create(ItemDefinition definition) {
        Material material = Material.matchMaterial(definition.material());
        if (material == null) {
            throw new IllegalStateException("Unknown Bukkit material: " + definition.material());
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(COLOR_SERIALIZER.deserialize(definition.displayName()));
        meta.lore(toLoreComponents(definition.lore()));

        modelStrategy.applyModel(meta, definition);

        meta.getPersistentDataContainer().set(itemIdKey, PersistentDataType.STRING, definition.id());

        item.setItemMeta(meta);
        return item;
    }

    public Optional<String> extractItemId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return Optional.empty();
        }

        String id = item.getItemMeta().getPersistentDataContainer().get(itemIdKey, PersistentDataType.STRING);
        return Optional.ofNullable(id);
    }

    private List<Component> toLoreComponents(List<String> lore) {
        return lore.stream().<Component>map(COLOR_SERIALIZER::deserialize).toList();
    }
}
