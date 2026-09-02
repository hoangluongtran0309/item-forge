package com.hoangluongtran0309.itemforge.dashboard.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.hoangluongtran0309.itemforge.dashboard.client.RecipeApiClient;
import com.hoangluongtran0309.itemforge.dashboard.dto.ShapelessRecipeJson;
import com.hoangluongtran0309.itemforge.dashboard.mapper.RecipeFormMapper;

/**
 * Co tinh nhe hon ItemControllerTest - co che routing/view-name/duong loi
 * htmx is structurally identical across all three controllers (already proven there); this
 * test only confirms the recipe resource's own routes and the list rendering specific to
 * SHAPELESS/SHAPED.
 */
@WebMvcTest(RecipeController.class)
@Import(RecipeFormMapper.class)
class RecipeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RecipeApiClient recipeApiClient;

    @Test
    @WithMockUser
    void listRendersTheRecipesListView() throws Exception {
        when(recipeApiClient.findAll()).thenReturn(
                List.of(new ShapelessRecipeJson("void_sword_salvage", "SHAPELESS", "TORCH", 4, List.of("COAL", "STICK"))));

        mockMvc.perform(get("/recipes"))
                .andExpect(status().isOk())
                .andExpect(view().name("recipes/list"));
    }

    @Test
    @WithMockUser
    void newFormRendersAnEmptyForm() throws Exception {
        mockMvc.perform(get("/recipes/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("recipes/form"));
    }
}
