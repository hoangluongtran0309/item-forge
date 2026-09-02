package com.hoangluongtran0309.itemforge.dashboard.web;

import java.util.List;
import java.util.function.Supplier;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.client.RestClientException;

import com.hoangluongtran0309.itemforge.dashboard.client.ArmorApiClient;
import com.hoangluongtran0309.itemforge.dashboard.client.BlockApiClient;
import com.hoangluongtran0309.itemforge.dashboard.client.ItemApiClient;
import com.hoangluongtran0309.itemforge.dashboard.client.PluginApiException;
import com.hoangluongtran0309.itemforge.dashboard.client.RecipeApiClient;
import com.hoangluongtran0309.itemforge.dashboard.dto.ArmorJson;
import com.hoangluongtran0309.itemforge.dashboard.dto.BlockJson;
import com.hoangluongtran0309.itemforge.dashboard.dto.ItemJson;
import com.hoangluongtran0309.itemforge.dashboard.dto.RecipeJson;

@Controller
public class HomeController {

    private static final int RECENT_LIMIT = 6;

    private final ItemApiClient itemApiClient;
    private final ArmorApiClient armorApiClient;
    private final BlockApiClient blockApiClient;
    private final RecipeApiClient recipeApiClient;

    public HomeController(ItemApiClient itemApiClient, ArmorApiClient armorApiClient,
            BlockApiClient blockApiClient, RecipeApiClient recipeApiClient) {
        this.itemApiClient = itemApiClient;
        this.armorApiClient = armorApiClient;
        this.blockApiClient = blockApiClient;
        this.recipeApiClient = recipeApiClient;
    }

    /**
     * The Overview must NOT let an exception reach GlobalExceptionHandler. That handler
     * redirects to the Referer header, defaulting to "/" when absent -- which is this very
     * page -- so a plugin API that is down would create an infinite redirect loop right on
     * the home page. Each call is therefore guarded individually and the page still renders
     * as a zero-state with a warning.
     */
    @GetMapping("/")
    public String home(Model model) {
        List<ItemJson> items = safely(itemApiClient::findAll, model);
        List<ArmorJson> armorPieces = safely(armorApiClient::findAll, model);
        List<BlockJson> blocks = safely(blockApiClient::findAll, model);
        List<RecipeJson> recipes = safely(recipeApiClient::findAll, model);

        model.addAttribute("itemCount", items.size());
        model.addAttribute("armorCount", armorPieces.size());
        model.addAttribute("blockCount", blocks.size());
        model.addAttribute("recipeCount", recipes.size());
        model.addAttribute("recentItems", items.stream().limit(RECENT_LIMIT).toList());
        model.addAttribute("recentBlocks", blocks.stream().limit(RECENT_LIMIT).toList());
        return "home";
    }

    // Catches both branches: PluginApiException is a RuntimeException (the plugin returned
    // an error status), while RestClientException covers connection-level failures (the
    // plugin is not running, a wrong base-url, a timeout). Model.addAttribute overwrites, so
    // four calls failing for the same reason show one banner rather than four identical
    // ones.
    private <T> List<T> safely(Supplier<List<T>> call, Model model) {
        try {
            return call.get();
        } catch (PluginApiException e) {
            model.addAttribute("errorBanner", e.getMessage());
            return List.of();
        } catch (RestClientException e) {
            model.addAttribute("errorBanner", "Could not reach the ItemForge plugin API. Is the plugin running and "
                    + "is itemforge.api.base-url configured correctly?");
            return List.of();
        }
    }
}
