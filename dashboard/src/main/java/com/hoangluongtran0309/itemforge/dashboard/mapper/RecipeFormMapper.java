package com.hoangluongtran0309.itemforge.dashboard.mapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.hoangluongtran0309.itemforge.dashboard.dto.RecipeJson;
import com.hoangluongtran0309.itemforge.dashboard.dto.ShapedRecipeJson;
import com.hoangluongtran0309.itemforge.dashboard.dto.ShapelessRecipeJson;
import com.hoangluongtran0309.itemforge.dashboard.form.IngredientRow;
import com.hoangluongtran0309.itemforge.dashboard.form.RecipeForm;

@Component
public class RecipeFormMapper {

    public RecipeJson toJson(RecipeForm form) {
        int resultCount = form.getResultCount() == null ? 1 : form.getResultCount();

        if ("SHAPELESS".equals(form.getType())) {
            List<String> ingredientIds = form.getIngredients().stream()
                    .map(IngredientRow::getItemId)
                    .filter(id -> id != null && !id.isBlank())
                    .toList();
            return new ShapelessRecipeJson(form.getId(), "SHAPELESS", form.getResult(), resultCount, ingredientIds);
        }

        List<String> shape = form.getShapeLines().stream()
                .filter(line -> line != null && !line.isEmpty())
                .toList();

        Map<String, String> ingredients = new LinkedHashMap<>();
        for (IngredientRow row : form.getIngredients()) {
            if (isBlank(row.getKey()) || isBlank(row.getItemId())) {
                continue;
            }
            ingredients.put(row.getKey(), row.getItemId());
        }

        return new ShapedRecipeJson(form.getId(), "SHAPED", form.getResult(), resultCount, shape, ingredients);
    }

    public RecipeForm fromJson(RecipeJson json) {
        RecipeForm form = new RecipeForm();
        form.setId(json.id());
        form.setType(json.type());
        form.setResult(json.result());
        form.setResultCount(json.resultCount());

        switch (json) {
            case ShapedRecipeJson shaped -> {
                List<String> shapeLines = new ArrayList<>(shaped.shape());
                while (shapeLines.size() < 3) {
                    shapeLines.add("");
                }
                form.setShapeLines(shapeLines);

                List<IngredientRow> rows = new ArrayList<>();
                shaped.ingredients().forEach((key, itemId) -> {
                    IngredientRow row = new IngredientRow();
                    row.setKey(key);
                    row.setItemId(itemId);
                    rows.add(row);
                });
                form.setIngredients(rows);
            }
            case ShapelessRecipeJson shapeless -> {
                List<IngredientRow> rows = new ArrayList<>();
                for (String itemId : shapeless.ingredients()) {
                    IngredientRow row = new IngredientRow();
                    row.setItemId(itemId);
                    rows.add(row);
                }
                form.setIngredients(rows);
            }
        }

        return form;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
