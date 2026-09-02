package com.hoangluongtran0309.application;

import java.util.Optional;

import com.hoangluongtran0309.application.port.ResourcePackInfo;
import com.hoangluongtran0309.application.port.ResourcePackPort;
import com.hoangluongtran0309.application.port.TextureStoragePort;
import com.hoangluongtran0309.domain.texture.PngTextureValidator;

// Orchestrates a texture upload: validate (domain) -> store the file (port) -> rebuild the
// resource pack. It depends on neither ItemRegistry nor ArmorRegistry -- checking that an id
// exists and mapping armorId -> armorAssetId stays with the API handler, the same way the
// existing CRUD handlers each hold their own registry.
public class TextureUploadService {

    private final TextureStoragePort textureStoragePort;
    private final ResourcePackPort resourcePackPort;

    public TextureUploadService(TextureStoragePort textureStoragePort, ResourcePackPort resourcePackPort) {
        this.textureStoragePort = textureStoragePort;
        this.resourcePackPort = resourcePackPort;
    }

    public String uploadItemTexture(String itemId, byte[] pngBytes) {
        PngTextureValidator.validateIcon(pngBytes);
        textureStoragePort.writeItemTexture(itemId, pngBytes);
        return rebuildAndHash();
    }

    public String uploadArmorIcon(String armorId, byte[] pngBytes) {
        PngTextureValidator.validateIcon(pngBytes);
        textureStoragePort.writeArmorIcon(armorId, pngBytes);
        return rebuildAndHash();
    }

    public String uploadArmorLayer1(String armorAssetId, byte[] pngBytes) {
        PngTextureValidator.validateArmorLayer(pngBytes);
        textureStoragePort.writeArmorLayer1(armorAssetId, pngBytes);
        return rebuildAndHash();
    }

    public String uploadArmorLayer2(String armorAssetId, byte[] pngBytes) {
        PngTextureValidator.validateArmorLayer(pngBytes);
        textureStoragePort.writeArmorLayer2(armorAssetId, pngBytes);
        return rebuildAndHash();
    }

    public boolean itemTextureExists(String itemId) {
        return textureStoragePort.itemTextureExists(itemId);
    }

    public Optional<byte[]> itemTexture(String itemId) {
        return textureStoragePort.readItemTexture(itemId);
    }

    public boolean armorIconExists(String armorId) {
        return textureStoragePort.armorIconExists(armorId);
    }

    public Optional<byte[]> armorIcon(String armorId) {
        return textureStoragePort.readArmorIcon(armorId);
    }

    public boolean armorLayer1Exists(String armorAssetId) {
        return textureStoragePort.armorLayer1Exists(armorAssetId);
    }

    public boolean armorLayer2Exists(String armorAssetId) {
        return textureStoragePort.armorLayer2Exists(armorAssetId);
    }

    public Optional<byte[]> armorLayer1(String armorAssetId) {
        return textureStoragePort.readArmorLayer1(armorAssetId);
    }

    public Optional<byte[]> armorLayer2(String armorAssetId) {
        return textureStoragePort.readArmorLayer2(armorAssetId);
    }

    // Keyed by CustomBlockDefinition.textureId(), not by block id -- the same way armor
    // keys its texture layers by armorAssetId rather than armor id.
    public String uploadBlockTexture(String textureId, byte[] pngBytes) {
        PngTextureValidator.validateIcon(pngBytes);
        textureStoragePort.writeBlockTexture(textureId, pngBytes);
        return rebuildAndHash();
    }

    public boolean blockTextureExists(String textureId) {
        return textureStoragePort.blockTextureExists(textureId);
    }

    // Keyed by textureId like uploadBlockTexture, but does NOT rebuild the resource pack:
    // this is the read path the editor uses to load the current texture back.
    public Optional<byte[]> blockTexture(String textureId) {
        return textureStoragePort.readBlockTexture(textureId);
    }

    // null when resource-pack.host is not configured (currentPack() is empty) -- the
    // dashboard still reports success, there is simply no sha1 to hand to players.
    private String rebuildAndHash() {
        resourcePackPort.rebuild();
        return resourcePackPort.currentPack().map(ResourcePackInfo::sha1Hex).orElse(null);
    }
}
