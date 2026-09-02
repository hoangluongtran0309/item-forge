package com.hoangluongtran0309.application.port;

import java.util.Optional;

public interface TextureStoragePort {

    void writeItemTexture(String itemId, byte[] pngBytes);

    boolean itemTextureExists(String itemId);

    Optional<byte[]> readItemTexture(String itemId);

    void writeArmorIcon(String armorId, byte[] pngBytes);

    boolean armorIconExists(String armorId);

    Optional<byte[]> readArmorIcon(String armorId);

    void writeArmorLayer1(String armorAssetId, byte[] pngBytes);

    void writeArmorLayer2(String armorAssetId, byte[] pngBytes);

    boolean armorLayer1Exists(String armorAssetId);

    boolean armorLayer2Exists(String armorAssetId);

    Optional<byte[]> readArmorLayer1(String armorAssetId);

    Optional<byte[]> readArmorLayer2(String armorAssetId);

    void writeBlockTexture(String textureId, byte[] pngBytes);

    boolean blockTextureExists(String textureId);

    Optional<byte[]> readBlockTexture(String textureId);
}
