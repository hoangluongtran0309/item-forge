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
public class BlockForm {

    // The empty string is allowed through: Bean Validation's @Pattern STILL runs on an
    // empty string, so disallowing it would let the "only letters..." message cover up
    // @NotBlank's "is required" message on the same field.
    public static final String SAFE_ID = "^$|^[A-Za-z0-9_-]+(\\.[A-Za-z0-9_-]+)*$";
    static final String SAFE_ID_MESSAGE =
            "only letters, digits, '_', '-' and single dots between them are allowed";

    @NotBlank(message = "ID is required")
    @Pattern(regexp = SAFE_ID, message = SAFE_ID_MESSAGE)
    private String id;
    @NotBlank(message = "Instrument is required")
    private String instrument;
    @NotNull(message = "Note is required")
    @Min(value = 0, message = "Note must be between 0 and 24")
    @Max(value = 24, message = "Note must be between 0 and 24")
    private Integer note;
    @NotBlank(message = "Texture ID is required")
    @Pattern(regexp = SAFE_ID, message = SAFE_ID_MESSAGE)
    private String textureId;
    @NotBlank(message = "Drop item ID is required")
    private String dropItemId;
    @NotBlank(message = "Display name is required")
    private String displayName;
    private Integer customModelData;
    // One lore line per line of text, like ItemForm/ArmorForm -- no need for extra
    // htmx add/remove-row
    private String loreText = "";

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getInstrument() {
        return instrument;
    }

    public void setInstrument(String instrument) {
        this.instrument = instrument;
    }

    public Integer getNote() {
        return note;
    }

    public void setNote(Integer note) {
        this.note = note;
    }

    public String getTextureId() {
        return textureId;
    }

    public void setTextureId(String textureId) {
        this.textureId = textureId;
    }

    public String getDropItemId() {
        return dropItemId;
    }

    public void setDropItemId(String dropItemId) {
        this.dropItemId = dropItemId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Integer getCustomModelData() {
        return customModelData;
    }

    public void setCustomModelData(Integer customModelData) {
        this.customModelData = customModelData;
    }

    public String getLoreText() {
        return loreText;
    }

    public void setLoreText(String loreText) {
        this.loreText = loreText;
    }
}
