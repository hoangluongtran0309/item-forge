package com.hoangluongtran0309.itemforge.dashboard.form;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * These constraints deliberately mirror the rules of the plugin's domain layer, so the
 * user sees the error at the field itself instead of submitting and getting back a generic
 * banner.
 *
 * <p>{@code SAFE_ID} matches the pattern FileSystemTextureStoragePort uses to block path
 * traversal: id/textureId/armorAssetId all become FILE NAMES, so a value like "../../evil"
 * has to be rejected -- and rejecting it here gives a far clearer message than an
 * IllegalArgumentException from the storage layer.
 */
public class ArmorForm {

    // The empty string is allowed through: Bean Validation's @Pattern STILL runs on an
    // empty string, so disallowing it would let the "only letters..." message cover up
    // @NotBlank's "is required" message on the same field.
    public static final String SAFE_ID = "^$|^[A-Za-z0-9_-]+(\\.[A-Za-z0-9_-]+)*$";
    static final String SAFE_ID_MESSAGE =
            "only letters, digits, '_', '-' and single dots between them are allowed";

    @NotBlank(message = "ID is required")
    @Pattern(regexp = SAFE_ID, message = SAFE_ID_MESSAGE)
    private String id;
    @NotBlank(message = "Material is required")
    private String material;
    private String slot = "HELMET";
    // May be left blank: the mapper then uses the id itself as the asset id.
    @Pattern(regexp = SAFE_ID, message = SAFE_ID_MESSAGE)
    private String armorAssetId;
    @NotBlank(message = "Display name is required")
    private String displayName;
    private String loreText = "";

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMaterial() {
        return material;
    }

    public void setMaterial(String material) {
        this.material = material;
    }

    public String getSlot() {
        return slot;
    }

    public void setSlot(String slot) {
        this.slot = slot;
    }

    public String getArmorAssetId() {
        return armorAssetId;
    }

    public void setArmorAssetId(String armorAssetId) {
        this.armorAssetId = armorAssetId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getLoreText() {
        return loreText;
    }

    public void setLoreText(String loreText) {
        this.loreText = loreText;
    }
}
