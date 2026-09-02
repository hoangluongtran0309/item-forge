package com.hoangluongtran0309.itemforge.dashboard.dto;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ShapedRecipeJson(
        String id,
        String type,
        String result,
        @JsonProperty("result-count") int resultCount,
        List<String> shape,
        Map<String, String> ingredients) implements RecipeJson {
}
