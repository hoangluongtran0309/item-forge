package com.hoangluongtran0309.itemforge.dashboard.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.hoangluongtran0309.itemforge.dashboard.client.ArmorApiClient;
import com.hoangluongtran0309.itemforge.dashboard.client.PluginApiException;
import com.hoangluongtran0309.itemforge.dashboard.dto.ArmorJson;
import com.hoangluongtran0309.itemforge.dashboard.dto.TextureUploadResponse;
import com.hoangluongtran0309.itemforge.dashboard.mapper.ArmorFormMapper;

@WebMvcTest(ArmorController.class)
@Import(ArmorFormMapper.class)
class ArmorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ArmorApiClient armorApiClient;

    @Test
    @WithMockUser
    void listShowsNamesWithoutMinecraftColourCodes() throws Exception {
        when(armorApiClient.findAll()).thenReturn(List.of(
                new ArmorJson("void_helmet", "NETHERITE_HELMET", "HELMET", "void_armor", "&5Void Netherite Helmet",
                        List.of())));

        mockMvc.perform(get("/armor"))
                .andExpect(content().string(containsString("Void Netherite Helmet")))
                .andExpect(content().string(not(containsString("&amp;5"))));
    }

    @Test
    @WithMockUser
    void listRendersTheArmorListView() throws Exception {
        when(armorApiClient.findAll()).thenReturn(List.of(
                new ArmorJson("void_helmet", "NETHERITE_HELMET", "HELMET", "void_armor", "Knight Helmet",
                        List.of())));

        mockMvc.perform(get("/armor"))
                .andExpect(status().isOk())
                .andExpect(view().name("armor/list"));
    }

    @Test
    @WithMockUser
    void newFormRendersAnEmptyForm() throws Exception {
        mockMvc.perform(get("/armor/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("armor/form"));
    }

    @Test
    @WithMockUser
    void editFormLooksUpTheArmorById() throws Exception {
        when(armorApiClient.findById("void_helmet")).thenReturn(
                new ArmorJson("void_helmet", "NETHERITE_HELMET", "HELMET", "void_armor", "Knight Helmet",
                        List.of()));

        mockMvc.perform(get("/armor/void_helmet/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("armor/form"));
    }

    // See the equivalent note in ItemControllerTest: all three armor panels go through the
    // shared fragment, so their ids and hx-* attributes need pinning down by tests.
    @Test
    @WithMockUser
    void editFormKeepsAllThreeTexturePanelsWired() throws Exception {
        when(armorApiClient.findById("void_helmet")).thenReturn(
                new ArmorJson("void_helmet", "NETHERITE_HELMET", "HELMET", "void_armor", "Knight Helmet",
                        List.of()));

        mockMvc.perform(get("/armor/void_helmet/edit"))
                .andExpect(content().string(containsString("id=\"armor-icon-upload-btn\"")))
                .andExpect(content().string(containsString("id=\"armor-layer1-upload-btn\"")))
                .andExpect(content().string(containsString("id=\"armor-layer2-upload-btn\"")))
                .andExpect(content().string(containsString("id=\"armor-icon-upload-status\"")))
                .andExpect(content().string(containsString("id=\"armor-layer1-upload-status\"")))
                .andExpect(content().string(containsString("id=\"armor-layer2-upload-status\"")))
                .andExpect(content().string(containsString(
                        "hx-post=\"/armor/void_helmet/texture?layer=humanoid\"")))
                .andExpect(content().string(containsString(
                        "hx-post=\"/armor/void_helmet/texture?layer=humanoid_leggings\"")))
                // 64x32 plus the UV overlay is what lets the Studio draw the humanoid layout correctly.
                .andExpect(content().string(containsString("data-studio-height=\"32\"")))
                .andExpect(content().string(containsString("data-studio-uv-overlay=\"humanoid\"")))
                .andExpect(content().string(containsString("data-studio-uv-overlay=\"humanoid_leggings\"")))
                // armorAssetId has to appear: the layer is shared by the whole set, so the user
                // needs to know what they are actually editing.
                .andExpect(content().string(containsString("void_armor")));
    }

    @Test
    @WithMockUser
    void deleteReturnsErrorBannerFragmentWhenApiFails() throws Exception {
        doThrow(new PluginApiException(404, "Unknown armor id: missing")).when(armorApiClient).delete("missing");

        mockMvc.perform(delete("/armor/missing").header("HX-Request", "true").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(header().string("HX-Retarget", "#error-banner"))
                .andExpect(header().string("HX-Reswap", "innerHTML"))
                .andExpect(view().name("fragments/error-banner :: banner"));
    }

    @Test
    @WithMockUser
    void uploadIconShowsSuccessMessageOnSuccess() throws Exception {
        when(armorApiClient.uploadIcon(anyString(), any(byte[].class), anyString()))
                .thenReturn(new TextureUploadResponse(true, "abc123"));

        MockMultipartFile file = new MockMultipartFile("file", "icon.png", "image/png", new byte[] { 1, 2, 3 });

        mockMvc.perform(multipart("/armor/void_helmet/icon").file(file).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("armor/form :: icon-upload-status"))
                .andExpect(model().attribute("iconMessage",
                        "Resource pack rebuilt, SHA-1: abc123. Players need to rejoin to see changes."));
    }

    @Test
    @WithMockUser
    void uploadIconShowsInlineErrorWhenApiRejects() throws Exception {
        when(armorApiClient.uploadIcon(anyString(), any(byte[].class), anyString()))
                .thenThrow(new PluginApiException(400, "Texture must be square"));

        MockMultipartFile file = new MockMultipartFile("file", "icon.png", "image/png", new byte[] { 1, 2, 3 });

        mockMvc.perform(multipart("/armor/void_helmet/icon").file(file).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("armor/form :: icon-upload-status"))
                .andExpect(model().attribute("iconError", "Texture must be square"));
    }

    @Test
    @WithMockUser
    void uploadLayerTextureHumanoidTargetsLayer1Fragment() throws Exception {
        when(armorApiClient.uploadLayer(anyString(), anyString(), any(byte[].class), anyString()))
                .thenReturn(new TextureUploadResponse(true, "abc123"));

        MockMultipartFile file = new MockMultipartFile("file", "layer1.png", "image/png", new byte[] { 1, 2, 3 });

        mockMvc.perform(multipart("/armor/void_helmet/texture").file(file).param("layer", "humanoid").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("armor/form :: layer1-upload-status"))
                .andExpect(model().attribute("layer1Message",
                        "Resource pack rebuilt, SHA-1: abc123. Players need to rejoin to see changes."));
    }

    @Test
    @WithMockUser
    void uploadLayerTextureHumanoidLeggingsTargetsLayer2Fragment() throws Exception {
        when(armorApiClient.uploadLayer(anyString(), anyString(), any(byte[].class), anyString()))
                .thenReturn(new TextureUploadResponse(true, "abc123"));

        MockMultipartFile file = new MockMultipartFile("file", "layer2.png", "image/png", new byte[] { 1, 2, 3 });

        mockMvc.perform(multipart("/armor/void_leggings/texture").file(file)
                        .param("layer", "humanoid_leggings").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("armor/form :: layer2-upload-status"))
                .andExpect(model().attribute("layer2Message",
                        "Resource pack rebuilt, SHA-1: abc123. Players need to rejoin to see changes."));
    }

    @Test
    @WithMockUser
    void downloadIconReturnsImageBytesOnSuccess() throws Exception {
        when(armorApiClient.downloadIcon("void_helmet")).thenReturn(new byte[] { 1, 2, 3 });

        mockMvc.perform(get("/armor/void_helmet/icon"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(content().bytes(new byte[] { 1, 2, 3 }));
    }

    @Test
    @WithMockUser
    void downloadIconReturns404WhenArmorHasNoIcon() throws Exception {
        when(armorApiClient.downloadIcon("void_helmet")).thenThrow(new PluginApiException(404, "No icon"));

        mockMvc.perform(get("/armor/void_helmet/icon"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void downloadLayerTextureReturnsImageBytesOnSuccess() throws Exception {
        when(armorApiClient.downloadLayer("void_helmet", "humanoid")).thenReturn(new byte[] { 4, 5, 6 });

        mockMvc.perform(get("/armor/void_helmet/texture").param("layer", "humanoid"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(content().bytes(new byte[] { 4, 5, 6 }));
    }

    @Test
    @WithMockUser
    void downloadLayerTextureReturns404WhenLayerMissing() throws Exception {
        when(armorApiClient.downloadLayer("void_helmet", "humanoid"))
                .thenThrow(new PluginApiException(404, "No texture"));

        mockMvc.perform(get("/armor/void_helmet/texture").param("layer", "humanoid"))
                .andExpect(status().isNotFound());
    }

    @Test
    void unauthenticatedRequestIsRejected() throws Exception {
        mockMvc.perform(get("/armor"))
                .andExpect(status().isUnauthorized());
    }
}
