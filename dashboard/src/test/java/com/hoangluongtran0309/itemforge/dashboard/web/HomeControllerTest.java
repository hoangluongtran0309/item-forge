package com.hoangluongtran0309.itemforge.dashboard.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.ResourceAccessException;

import com.hoangluongtran0309.itemforge.dashboard.client.ArmorApiClient;
import com.hoangluongtran0309.itemforge.dashboard.client.BlockApiClient;
import com.hoangluongtran0309.itemforge.dashboard.client.ItemApiClient;
import com.hoangluongtran0309.itemforge.dashboard.client.RecipeApiClient;
import com.hoangluongtran0309.itemforge.dashboard.dto.ArmorJson;
import com.hoangluongtran0309.itemforge.dashboard.dto.BlockJson;
import com.hoangluongtran0309.itemforge.dashboard.dto.ItemJson;
import com.hoangluongtran0309.itemforge.dashboard.dto.ShapelessRecipeJson;

@WebMvcTest(HomeController.class)
@Import(ActiveNavAdvice.class)
class HomeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ItemApiClient itemApiClient;

    @MockitoBean
    private ArmorApiClient armorApiClient;

    @MockitoBean
    private BlockApiClient blockApiClient;

    @MockitoBean
    private RecipeApiClient recipeApiClient;

    @BeforeEach
    void stubEmptyByDefault() {
        when(itemApiClient.findAll()).thenReturn(List.of());
        when(armorApiClient.findAll()).thenReturn(List.of());
        when(blockApiClient.findAll()).thenReturn(List.of());
        when(recipeApiClient.findAll()).thenReturn(List.of());
    }

    @Test
    @WithMockUser
    void namesAreShownWithoutMinecraftColourCodes() throws Exception {
        when(itemApiClient.findAll()).thenReturn(List.of(
                new ItemJson("void_sword", "NETHERITE_SWORD", 1, "&5Void Netherite Sword", List.of(), List.of())));
        when(blockApiClient.findAll()).thenReturn(List.of(
                new BlockJson("void_block", "BASS_GUITAR", 12, "void_block", "GLOWSTONE", "&5Void Netherite Block",
                        2001, List.of())));

        mockMvc.perform(get("/"))
                .andExpect(content().string(containsString("Void Netherite Sword")))
                .andExpect(content().string(containsString("Void Netherite Block")))
                .andExpect(content().string(not(containsString("&amp;5"))))
                // The block thumbnail falls back to the first letter of the name, not to "&".
                .andExpect(content().string(containsString("thumb-fallback hidden\">V<")));
    }

    @Test
    @WithMockUser
    void homeRendersInsideTheAppShell() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("home"))
                // Confirms the layout dialect really decorates: elements that exist only in
                // layout/base.html must appear in the child page's output.
                .andExpect(content().string(containsString("id=\"toast-container\"")))
                .andExpect(content().string(containsString("id=\"error-banner\"")))
                .andExpect(content().string(containsString("id=\"if-drawer\"")))
                .andExpect(content().string(containsString("<title>Overview - ItemForge Dashboard</title>")));
    }

    @Test
    @WithMockUser
    void activeNavIsOverviewOnTheHomePage() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(model().attribute("activeNav", "overview"));
    }

    @Test
    @WithMockUser
    void countsComeFromEachApiClient() throws Exception {
        when(itemApiClient.findAll()).thenReturn(List.of(
                new ItemJson("void_sword", "NETHERITE_SWORD", 1, "Fire Sword", List.of(), List.of()),
                new ItemJson("void_pickaxe", "IRON_AXE", 2, "Frost Axe", List.of(), List.of())));
        when(armorApiClient.findAll()).thenReturn(List.of(
                new ArmorJson("void_helmet", "NETHERITE_HELMET", "HELMET", "void_armor", "Dragon Helmet",
                        List.of())));
        when(blockApiClient.findAll()).thenReturn(List.of(
                new BlockJson("lantern", "BASS_GUITAR", 12, "lantern_tex", "GLOWSTONE", "Lantern", 3001, List.of())));
        when(recipeApiClient.findAll()).thenReturn(List.of(
                new ShapelessRecipeJson("recipe_a", "SHAPELESS", "void_sword", 1, List.of())));

        mockMvc.perform(get("/"))
                .andExpect(model().attribute("itemCount", 2))
                .andExpect(model().attribute("armorCount", 1))
                .andExpect(model().attribute("blockCount", 1))
                .andExpect(model().attribute("recipeCount", 1))
                // Real thumbnails must point at the GET texture endpoint, no longer the
                // placeholder letter of the previous version.
                .andExpect(content().string(containsString("/items/void_sword/texture")))
                .andExpect(content().string(containsString("/blocks/lantern/texture")));
    }

    /**
     * If the exception reaches GlobalExceptionHandler it redirects to the Referer --
     * defaulting to "/" -- which is this very page, creating an infinite redirect loop.
     * This test pins down the contract: 200 with a zero-state, NOT a 3xx.
     */
    @Test
    @WithMockUser
    void pluginApiBeingDownRendersZeroStateInsteadOfRedirecting() throws Exception {
        when(itemApiClient.findAll()).thenThrow(new ResourceAccessException("connection refused"));

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("home"))
                .andExpect(model().attribute("itemCount", 0))
                .andExpect(content().string(containsString("Could not reach the ItemForge plugin API")));
    }
}
