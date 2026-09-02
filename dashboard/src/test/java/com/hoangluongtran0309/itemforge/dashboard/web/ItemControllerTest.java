package com.hoangluongtran0309.itemforge.dashboard.web;

import static org.hamcrest.Matchers.containsString;
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

import com.hoangluongtran0309.itemforge.dashboard.client.ItemApiClient;
import com.hoangluongtran0309.itemforge.dashboard.client.PluginApiException;
import com.hoangluongtran0309.itemforge.dashboard.dto.ItemJson;
import com.hoangluongtran0309.itemforge.dashboard.dto.TextureUploadResponse;
import com.hoangluongtran0309.itemforge.dashboard.mapper.ItemFormMapper;

@WebMvcTest(ItemController.class)
@Import(ItemFormMapper.class)
class ItemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ItemApiClient itemApiClient;

    @Test
    @WithMockUser
    void listRendersTheItemsListView() throws Exception {
        when(itemApiClient.findAll()).thenReturn(List.of(
                new ItemJson("void_sword", "NETHERITE_SWORD", 1, "Fire Sword", List.of(), List.of())));

        mockMvc.perform(get("/items"))
                .andExpect(status().isOk())
                .andExpect(view().name("items/list"));
    }

    @Test
    @WithMockUser
    void newFormRendersAnEmptyForm() throws Exception {
        mockMvc.perform(get("/items/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("items/form"));
    }

    @Test
    @WithMockUser
    void editFormLooksUpTheItemById() throws Exception {
        when(itemApiClient.findById("void_sword")).thenReturn(
                new ItemJson("void_sword", "NETHERITE_SWORD", 1, "Fire Sword", List.of(), List.of()));

        mockMvc.perform(get("/items/void_sword/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("items/form"));
    }

    /**
     * The texture panel is generated from a shared fragment (fragments/texture-panel), so
     * the ids the htmx wiring depends on are NO longer protected at compile time. This test
     * pins them down: changing an id in the fragment would make the Upload button fail
     * silently at runtime rather than raise an error.
     */
    @Test
    @WithMockUser
    void editFormKeepsTheTextureWiringIdsAndStudioTrigger() throws Exception {
        when(itemApiClient.findById("void_sword")).thenReturn(
                new ItemJson("void_sword", "NETHERITE_SWORD", 1, "Fire Sword", List.of(), List.of()));

        mockMvc.perform(get("/items/void_sword/edit"))
                .andExpect(content().string(containsString("id=\"item-texture-file-input\"")))
                .andExpect(content().string(containsString("id=\"item-texture-upload-btn\"")))
                .andExpect(content().string(containsString("id=\"item-texture-upload-status\"")))
                .andExpect(content().string(containsString("id=\"item-texture-preview-img\"")))
                .andExpect(content().string(containsString("hx-post=\"/items/void_sword/texture\"")))
                .andExpect(content().string(containsString("hx-target=\"#item-texture-upload-status\"")))
                // The Studio is triggered through data-* rather than an inline onclick: an ES
                // module creates no global, so onclick could not reach into it.
                .andExpect(content().string(containsString("data-studio-open")))
                .andExpect(content().string(containsString("data-studio-width=\"16\"")))
                .andExpect(content().string(containsString("id=\"studio-modal\"")));
    }

    @Test
    @WithMockUser
    void deleteReturnsErrorBannerFragmentWhenApiFails() throws Exception {
        doThrow(new PluginApiException(404, "Unknown item id: missing")).when(itemApiClient).delete("missing");

        mockMvc.perform(delete("/items/missing").header("HX-Request", "true").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(header().string("HX-Retarget", "#error-banner"))
                .andExpect(header().string("HX-Reswap", "innerHTML"))
                .andExpect(view().name("fragments/error-banner :: banner"));
    }

    @Test
    @WithMockUser
    void uploadTextureShowsSuccessMessageOnSuccess() throws Exception {
        when(itemApiClient.uploadTexture(anyString(), any(byte[].class), anyString()))
                .thenReturn(new TextureUploadResponse(true, "abc123"));

        MockMultipartFile file = new MockMultipartFile("file", "texture.png", "image/png", new byte[] { 1, 2, 3 });

        mockMvc.perform(multipart("/items/void_sword/texture").file(file).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("items/form :: texture-upload-status"))
                .andExpect(model().attribute("textureMessage",
                        "Resource pack rebuilt, SHA-1: abc123. Players need to rejoin to see changes."));
    }

    @Test
    @WithMockUser
    void uploadTextureShowsInlineErrorWhenApiRejects() throws Exception {
        when(itemApiClient.uploadTexture(anyString(), any(byte[].class), anyString()))
                .thenThrow(new PluginApiException(400, "Texture must be square"));

        MockMultipartFile file = new MockMultipartFile("file", "texture.png", "image/png", new byte[] { 1, 2, 3 });

        mockMvc.perform(multipart("/items/void_sword/texture").file(file).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("items/form :: texture-upload-status"))
                .andExpect(model().attribute("textureError", "Texture must be square"));
    }

    @Test
    @WithMockUser
    void downloadTextureReturnsImageBytesOnSuccess() throws Exception {
        when(itemApiClient.downloadTexture("void_sword")).thenReturn(new byte[] { 1, 2, 3 });

        mockMvc.perform(get("/items/void_sword/texture"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(content().bytes(new byte[] { 1, 2, 3 }));
    }

    @Test
    @WithMockUser
    void downloadTextureReturns404WhenItemHasNoTexture() throws Exception {
        when(itemApiClient.downloadTexture("void_sword")).thenThrow(new PluginApiException(404, "No texture"));

        mockMvc.perform(get("/items/void_sword/texture"))
                .andExpect(status().isNotFound());
    }

    @Test
    void unauthenticatedRequestIsRejected() throws Exception {
        // This slice test does not import the app's SecurityConfig (with its rule
        // form-login /login rieng), nen no roi ve auto-config security mac
        // Boot's default applies instead -- a 401 here rather than a redirect to /login. The
        // point of this test is only to confirm that authentication is enforced.
        mockMvc.perform(get("/items"))
                .andExpect(status().isUnauthorized());
    }
}
