package com.hoangluongtran0309.itemforge.dashboard.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type", visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = ShapedRecipeJson.class, name = "SHAPED"),
        @JsonSubTypes.Type(value = ShapelessRecipeJson.class, name = "SHAPELESS")
})
public sealed interface RecipeJson permits ShapedRecipeJson, ShapelessRecipeJson {

    String id();

    String type();

    String result();

    int resultCount();
}
