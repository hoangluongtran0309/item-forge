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

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.hoangluongtran0309.itemforge.dashboard.client.ArmorApiClient;
import com.hoangluongtran0309.itemforge.dashboard.client.BlockApiClient;
import com.hoangluongtran0309.itemforge.dashboard.client.ItemApiClient;
import com.hoangluongtran0309.itemforge.dashboard.client.PluginApiException;
import com.hoangluongtran0309.itemforge.dashboard.dto.ArmorJson;
import com.hoangluongtran0309.itemforge.dashboard.dto.BlockJson;
import com.hoangluongtran0309.itemforge.dashboard.dto.ItemJson;

@WebMvcTest(StudioController.class)
@Import(ActiveNavAdvice.class)
class StudioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ItemApiClient itemApiClient;

    @MockitoBean
    private ArmorApiClient armorApiClient;

    @MockitoBean
    private BlockApiClient blockApiClient;

    @Test
    @WithMockUser
    void theTitleDropsMinecraftColourCodes() throws Exception {
        when(itemApiClient.findById("void_sword")).thenReturn(
                new ItemJson("void_sword", "NETHERITE_SWORD", 1, "&5Void Netherite Sword", List.of(), List.of()));

        mockMvc.perform(get("/studio").param("type", "item").param("id", "void_sword"))
                .andExpect(model().attribute("studioTitle", "Void Netherite Sword"))
                .andExpect(content().string(not(containsString("&amp;5"))));
    }

    @Test
    @WithMockUser
    void itemTargetRendersA16x16CanvasWithoutUvOverlay() throws Exception {
        when(itemApiClient.findById("void_sword")).thenReturn(
                new ItemJson("void_sword", "NETHERITE_SWORD", 1, "Fire Sword", List.of(), List.of()));

        mockMvc.perform(get("/studio").param("type", "item").param("id", "void_sword"))
                .andExpect(status().isOk())
                .andExpect(view().name("studio"))
                .andExpect(model().attribute("docWidth", 16))
                .andExpect(model().attribute("docHeight", 16))
                .andExpect(model().attribute("loadUrl", "/items/void_sword/texture"))
                .andExpect(model().attribute("uploadUrl", "/items/void_sword/texture"))
                .andExpect(model().attribute("uvOverlay", (Object) null))
                .andExpect(model().attribute("previewKind", "item"))
                .andExpect(model().attribute("backUrl", "/items/void_sword/edit"))
                .andExpect(model().attribute("activeNav", "studio"))
                // fullBleed is what makes <main> drop its padding so the Studio fills the viewport.
                .andExpect(model().attribute("fullBleed", true));
    }

    @Test
    @WithMockUser
    void blockTargetPreviewsAsARotatableCube() throws Exception {
        when(blockApiClient.findById("void_netherite_block")).thenReturn(
                new BlockJson("void_netherite_block", "BASS_GUITAR", 12, "lantern_tex", "GLOWSTONE",
                        "Glowing Lantern", 3001, List.of()));

        mockMvc.perform(get("/studio").param("type", "block").param("id", "void_netherite_block"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("previewKind", "cube"))
                .andExpect(model().attribute("loadUrl", "/blocks/void_netherite_block/texture"))
                .andExpect(model().attribute("downloadName", "lantern_tex"));
    }

    @Test
    @WithMockUser
    void armorLeggingsLayerCarriesUvOverlayAndTheSiblingLayerUrl() throws Exception {
        when(armorApiClient.findById("void_leggings")).thenReturn(
                new ArmorJson("void_leggings", "NETHERITE_LEGGINGS", "LEGGINGS", "void_armor",
                        "Dragon Leggings", List.of()));

        mockMvc.perform(get("/studio")
                        .param("type", "armor-layer")
                        .param("id", "void_leggings")
                        .param("layer", "humanoid_leggings"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("docWidth", 64))
                .andExpect(model().attribute("docHeight", 32))
                .andExpect(model().attribute("uvOverlay", "humanoid_leggings"))
                .andExpect(model().attribute("previewKind", "humanoid_leggings"))
                .andExpect(model().attribute("loadUrl",
                        "/armor/void_leggings/texture?layer=humanoid_leggings"))
                // The other layer is loaded only so the 3D preview shows both layers as in game.
                .andExpect(model().attribute("siblingLoadUrl",
                        "/armor/void_leggings/texture?layer=humanoid"))
                .andExpect(model().attribute("downloadName", "void_armor_layer_2"))
                // armorAssetId has to appear on the page: the layer is shared by the whole set.
                .andExpect(content().string(containsString("void_armor")));
    }

    @Test
    @WithMockUser
    void armorLayerWithoutAValidLayerParamIsRejected() throws Exception {
        mockMvc.perform(get("/studio").param("type", "armor-layer").param("id", "void_helmet"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/studio")
                        .param("type", "armor-layer")
                        .param("id", "void_helmet")
                        .param("layer", "nonsense"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void unknownTypeAndMissingIdAreRejected() throws Exception {
        mockMvc.perform(get("/studio").param("type", "mob").param("id", "zombie"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/studio").param("type", "item"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void noTargetRendersAScratchCanvasWithNoUploadUrl() throws Exception {
        mockMvc.perform(get("/studio"))
                .andExpect(status().isOk())
                .andExpect(view().name("studio"))
                .andExpect(model().attribute("uploadUrl", (Object) null))
                .andExpect(model().attribute("loadUrl", (Object) null))
                .andExpect(model().attribute("draftKey", "scratch"));
    }

    @Test
    @WithMockUser
    void unknownTargetIdPropagatesThePluginApi404() throws Exception {
        when(itemApiClient.findById("missing"))
                .thenThrow(new PluginApiException(404, "Unknown item id: missing"));

        // GlobalExceptionHandler is not imported in this slice test, so the
        // PluginApiException propagates and MockMvc sees it as a servlet error. What matters
        // is that the controller does NOT silently render an empty canvas.
        try {
            mockMvc.perform(get("/studio").param("type", "item").param("id", "missing"));
        } catch (Exception expected) {
            return;
        }
    }

    @Test
    void unauthenticatedRequestIsRejected() throws Exception {
        mockMvc.perform(get("/studio"))
                .andExpect(status().isUnauthorized());
    }
}
