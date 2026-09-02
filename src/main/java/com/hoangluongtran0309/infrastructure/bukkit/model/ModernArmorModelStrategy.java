package com.hoangluongtran0309.infrastructure.bukkit.model;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.EquippableComponent;

import com.hoangluongtran0309.domain.model.ArmorDefinition;
import com.hoangluongtran0309.domain.model.ArmorSlot;
import com.hoangluongtran0309.infrastructure.resourcepack.ArmorTextureFileCopier;
import com.hoangluongtran0309.infrastructure.resourcepack.EquipmentAssetGenerator;
import com.hoangluongtran0309.infrastructure.resourcepack.ModelJsonGenerator;

public class ModernArmorModelStrategy implements ArmorModelStrategy {

    private static final String NAMESPACE = "itemforge";

    private final EquipmentAssetGenerator equipmentAssetGenerator;
    private final ArmorTextureFileCopier armorTextureFileCopier;
    private final ModelJsonGenerator modelJsonGenerator;
    private final Logger logger;

    public ModernArmorModelStrategy(EquipmentAssetGenerator equipmentAssetGenerator,
            ArmorTextureFileCopier armorTextureFileCopier, ModelJsonGenerator modelJsonGenerator, Logger logger) {
        this.equipmentAssetGenerator = equipmentAssetGenerator;
        this.armorTextureFileCopier = armorTextureFileCopier;
        this.modelJsonGenerator = modelJsonGenerator;
        this.logger = logger;
    }

    @Override
    public void applyModel(ItemMeta meta, ArmorDefinition definition) {
        // ItemMeta#getEquippable() is always @NotNull: when an item has no equippable of
        // its own, Paper hands back an EMPTY instance (slot defaults to HEAD) rather than
        // deriving one from the Material. So the slot has to be set explicitly from the
        // config -- otherwise every armor piece that calls setEquippable gets forced into
        // the head slot regardless of its material.
        EquippableComponent component = meta.getEquippable();
        component.setSlot(toEquipmentSlot(definition.slot()));

        // Always set model/item_model, whether or not the asset really exists in the
        // resource pack. With a missing texture the client shows the purple-black missing
        // model block, instead of silently falling back to the material's vanilla icon.
        component.setModel(new NamespacedKey(NAMESPACE, definition.armorAssetId()));
        meta.setEquippable(component);

        // item_model drives the icon in the inventory, in hand and on the ground -- it is
        // independent of the equippable component, which only drives the texture drawn on
        // the player while worn.
        meta.setItemModel(new NamespacedKey(NAMESPACE, definition.id()));
    }

    @Override
    public void generateResourcePackFiles(List<ArmorDefinition> armors, Path textureSourceDir, String namespace,
            Path outputDir) throws IOException {
        // Icons are per-piece (a helmet and a chestplate need different inventory icons),
        // independent of whether those pieces share an equipment asset.
        //
        // The model/client item is ALWAYS written, texture present or not -- same
        // mechanism as ModernModelStrategy for regular items: keep it at "the model
        // exists, only the texture is missing" rather than "the model does not exist
        // either". Those are two different missing-asset branches in the client: if the
        // model/asset is not defined at all the client may render something else entirely,
        // with no guarantee of the familiar MissingTextureAtlasSprite block.
        for (ArmorDefinition armor : armors) {
            boolean hasIcon = armorTextureFileCopier.copyIcon(armor, textureSourceDir, namespace, outputDir);
            if (!hasIcon) {
                logger.warning("No icon texture found for armor '" + armor.id()
                        + "'; layer0 will point at a texture that does not exist (shows the missing-texture block in the inventory).");
            }
            modelJsonGenerator.writeItemModel(armor.id(), namespace, outputDir);
            modelJsonGenerator.writeClientItem(armor.id(), namespace, outputDir);
        }

        // The worn textures (layer_1/layer_2) and the equipment asset JSON are grouped by
        // armorAssetId instead -- several pieces sharing one asset generate it exactly
        // once, avoiding duplicate files when a full set of four all points at the same
        // asset.
        //
        // The equipment asset JSON is ALWAYS written too, for the reason above: even with
        // layer_1 missing the JSON file exists, and its "texture" entry points at a PNG
        // that was never copied -> the same missing-texture mechanism as icons and regular
        // items.
        Map<String, List<ArmorDefinition>> byAssetId = armors.stream()
                .collect(Collectors.groupingBy(ArmorDefinition::armorAssetId));

        for (Map.Entry<String, List<ArmorDefinition>> entry : byAssetId.entrySet()) {
            String assetId = entry.getKey();

            boolean hasLayer1 = armorTextureFileCopier.copyLayer1(assetId, textureSourceDir, namespace, outputDir);
            if (!hasLayer1) {
                logger.warning("No layer_1 texture found for armor asset '" + assetId
                        + "'; the equipment asset will point at a texture that does not exist (shows the missing-texture block when worn).");
            }
            boolean hasLayer2 = armorTextureFileCopier.copyLayer2IfExists(assetId, textureSourceDir, namespace,
                    outputDir);
            equipmentAssetGenerator.writeEquipmentAsset(assetId, namespace, outputDir, hasLayer2);
        }
    }

    // Package-private so tests can call it directly without MockBukkit (EquipmentSlot is
    // a plain enum and needs no server bootstrap).
    EquipmentSlot toEquipmentSlot(ArmorSlot slot) {
        return switch (slot) {
            case HELMET -> EquipmentSlot.HEAD;
            case CHESTPLATE -> EquipmentSlot.CHEST;
            case LEGGINGS -> EquipmentSlot.LEGS;
            case BOOTS -> EquipmentSlot.FEET;
        };
    }
}
