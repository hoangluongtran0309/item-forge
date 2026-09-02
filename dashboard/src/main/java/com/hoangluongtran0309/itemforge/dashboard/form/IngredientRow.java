package com.hoangluongtran0309.itemforge.dashboard.form;

/**
 * One ingredient row. For a SHAPED recipe, `key` is the single character representing a
 * slot in the shape grid and `itemId` is the item it maps to. For a SHAPELESS recipe, `key`
 * is ignored and only `itemId` matters. Both kinds share one row shape so the htmx
 * add/remove-row endpoints can serve them both.
 */
public class IngredientRow {

    private String key;
    private String itemId;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }
}
