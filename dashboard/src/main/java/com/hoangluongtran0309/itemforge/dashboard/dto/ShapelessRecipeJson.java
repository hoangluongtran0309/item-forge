package com.hoangluongtran0309.itemforge.dashboard.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ShapelessRecipeJson(
        String id,
        String type,
        String result,
        @JsonProperty("result-count") int resultCount,
        List<String> ingredients) implements RecipeJson {
}
