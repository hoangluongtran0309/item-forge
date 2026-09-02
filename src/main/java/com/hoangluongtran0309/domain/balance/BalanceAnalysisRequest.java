package com.hoangluongtran0309.domain.balance;

import java.util.List;

import com.hoangluongtran0309.domain.model.ArmorDefinition;
import com.hoangluongtran0309.domain.model.ItemDefinition;
import com.hoangluongtran0309.domain.model.RecipeDefinition;

/**
 * Everything an analysis needs, snapshotted out of the registries by the application layer so
 * the domain never touches one.
 *
 * <p>The whole config is always carried, even when {@code targetId} narrows the findings to one
 * item: comparing an item against its peers is what makes an outlier visible, and that
 * comparison would be meaningless with only the target in hand.
 *
 * @param targetId the single id to report on, or empty for the whole config
 */
public record BalanceAnalysisRequest(
        List<ItemDefinition> items,
        List<ArmorDefinition> armor,
        List<RecipeDefinition> recipes,
        String targetId) {

    public BalanceAnalysisRequest {
        items = items == null ? List.of() : List.copyOf(items);
        armor = armor == null ? List.of() : List.copyOf(armor);
        recipes = recipes == null ? List.of() : List.copyOf(recipes);
        targetId = targetId == null ? "" : targetId;
    }

    public static BalanceAnalysisRequest wholeConfig(List<ItemDefinition> items, List<ArmorDefinition> armor,
            List<RecipeDefinition> recipes) {
        return new BalanceAnalysisRequest(items, armor, recipes, "");
    }

    public BalanceAnalysisRequest scopedTo(String targetId) {
        return new BalanceAnalysisRequest(items, armor, recipes, targetId);
    }

    public boolean isScoped() {
        return !targetId.isEmpty();
    }

    public boolean covers(String id) {
        return !isScoped() || targetId.equals(id);
    }
}
