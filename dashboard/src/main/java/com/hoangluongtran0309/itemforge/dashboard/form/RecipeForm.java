package com.hoangluongtran0309.itemforge.dashboard.form;

import java.util.ArrayList;
import java.util.List;

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
public class RecipeForm {

    // The empty string is allowed through: Bean Validation's @Pattern STILL runs on an
    // empty string, so disallowing it would let the "only letters..." message cover up
    // @NotBlank's "is required" message on the same field.
    public static final String SAFE_ID = "^$|^[A-Za-z0-9_-]+(\\.[A-Za-z0-9_-]+)*$";
    static final String SAFE_ID_MESSAGE =
            "only letters, digits, '_', '-' and single dots between them are allowed";

    @NotBlank(message = "ID is required")
    @Pattern(regexp = SAFE_ID, message = SAFE_ID_MESSAGE)
    private String id;
    private String type = "SHAPED";
    @NotBlank(message = "Result item/armor ID is required")
    private String result;
    @NotNull(message = "Result count is required")
    @Min(value = 1, message = "Result count must be at least 1")
    private Integer resultCount = 1;
    // Three fixed slots for the grid rows of a SHAPED recipe (3x3 at most, per ShapedRecipeDefinition).
    private List<String> shapeLines = new ArrayList<>(List.of("", "", ""));
    // SHAPED: key+itemId pairs. SHAPELESS: each row uses only itemId.
    private List<IngredientRow> ingredients = new ArrayList<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public Integer getResultCount() {
        return resultCount;
    }

    public void setResultCount(Integer resultCount) {
        this.resultCount = resultCount;
    }

    public List<String> getShapeLines() {
        return shapeLines;
    }

    public void setShapeLines(List<String> shapeLines) {
        this.shapeLines = shapeLines;
    }

    public List<IngredientRow> getIngredients() {
        return ingredients;
    }

    public void setIngredients(List<IngredientRow> ingredients) {
        this.ingredients = ingredients;
    }
}
