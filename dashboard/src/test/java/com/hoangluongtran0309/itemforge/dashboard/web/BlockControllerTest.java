package com.hoangluongtran0309.itemforge.dashboard.web;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

import com.hoangluongtran0309.itemforge.dashboard.client.BlockApiClient;
import com.hoangluongtran0309.itemforge.dashboard.client.PluginApiException;
import com.hoangluongtran0309.itemforge.dashboard.dto.BlockJson;
import com.hoangluongtran0309.itemforge.dashboard.dto.TextureUploadResponse;
import com.hoangluongtran0309.itemforge.dashboard.mapper.BlockFormMapper;

@WebMvcTest(BlockController.class)
@Import(BlockFormMapper.class)
class BlockControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BlockApiClient blockApiClient;

    @Test
    @WithMockUser
    void listRendersTheBlockListView() throws Exception {
        when(blockApiClient.findAll()).thenReturn(List.of(
                new BlockJson("void_netherite_block", "BASS_GUITAR", 12, "void_netherite_block", "GLOWSTONE",
                        "Glowing Lantern", 3001, List.of())));

        mockMvc.perform(get("/blocks"))
                .andExpect(status().isOk())
                .andExpect(view().name("blocks/list"));
    }

    @Test
    @WithMockUser
    void newFormRendersAnEmptyForm() throws Exception {
        mockMvc.perform(get("/blocks/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("blocks/form"));
    }

    @Test
    @WithMockUser
    void editFormLooksUpTheBlockById() throws Exception {
        when(blockApiClient.findById("void_netherite_block")).thenReturn(
                new BlockJson("void_netherite_block", "BASS_GUITAR", 12, "void_netherite_block", "GLOWSTONE",
                        "Glowing Lantern", 3001, List.of()));

        mockMvc.perform(get("/blocks/void_netherite_block/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("blocks/form"));
    }

    /**
     * Blocks used to have no entry point into the editor at all, and their Upload button was
     * missing its `id` -- which is exactly why the editor could not hook into it. This test
     * pins down both
     * hai thu do.
     */
    @Test
    @WithMockUser
    void editFormNowHasATextureUploadIdAndAStudioTrigger() throws Exception {
        when(blockApiClient.findById("void_netherite_block")).thenReturn(
                new BlockJson("void_netherite_block", "BASS_GUITAR", 12, "void_netherite_block", "GLOWSTONE",
                        "Glowing Lantern", 3001, List.of()));

        mockMvc.perform(get("/blocks/void_netherite_block/edit"))
                .andExpect(content().string(containsString("id=\"block-texture-upload-btn\"")))
                .andExpect(content().string(containsString("id=\"block-texture-file-input\"")))
                .andExpect(content().string(containsString("id=\"block-texture-upload-status\"")))
                .andExpect(content().string(containsString("hx-post=\"/blocks/void_netherite_block/texture\"")))
                .andExpect(content().string(containsString("data-studio-open")))
                .andExpect(content().string(containsString("id=\"studio-modal\"")));
    }

    @Test
    @WithMockUser
    void deleteReturnsErrorBannerFragmentWhenApiFails() throws Exception {
        doThrow(new PluginApiException(404, "Unknown block id: missing")).when(blockApiClient).delete("missing");

        mockMvc.perform(delete("/blocks/missing").header("HX-Request", "true").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(header().string("HX-Retarget", "#error-banner"))
                .andExpect(header().string("HX-Reswap", "innerHTML"))
                .andExpect(view().name("fragments/error-banner :: banner"));
    }

    @Test
    @WithMockUser
    void uploadTextureShowsSuccessMessageOnSuccess() throws Exception {
        when(blockApiClient.uploadTexture(anyString(), any(byte[].class), anyString()))
                .thenReturn(new TextureUploadResponse(true, "abc123"));

        MockMultipartFile file = new MockMultipartFile("file", "texture.png", "image/png", new byte[] { 1, 2, 3 });

        mockMvc.perform(multipart("/blocks/void_netherite_block/texture").file(file).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("blocks/form :: texture-upload-status"))
                .andExpect(model().attribute("textureMessage",
                        "Resource pack rebuilt, SHA-1: abc123. Players need to rejoin to see changes."));
    }

    @Test
    @WithMockUser
    void uploadTextureShowsInlineErrorWhenApiRejects() throws Exception {
        when(blockApiClient.uploadTexture(anyString(), any(byte[].class), anyString()))
                .thenThrow(new PluginApiException(400, "Texture must be square"));

        MockMultipartFile file = new MockMultipartFile("file", "texture.png", "image/png", new byte[] { 1, 2, 3 });

        mockMvc.perform(multipart("/blocks/void_netherite_block/texture").file(file).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("blocks/form :: texture-upload-status"))
                .andExpect(model().attribute("textureError", "Texture must be square"));
    }

    @Test
    @WithMockUser
    void downloadTextureReturnsImageBytesOnSuccess() throws Exception {
        when(blockApiClient.downloadTexture("void_netherite_block")).thenReturn(new byte[] { 1, 2, 3 });

        mockMvc.perform(get("/blocks/void_netherite_block/texture"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(content().bytes(new byte[] { 1, 2, 3 }));
    }

    @Test
    @WithMockUser
    void downloadTextureReturns404WhenBlockHasNoTexture() throws Exception {
        when(blockApiClient.downloadTexture("void_netherite_block"))
                .thenThrow(new PluginApiException(404, "Block 'void_netherite_block' has no texture yet"));

        mockMvc.perform(get("/blocks/void_netherite_block/texture"))
                .andExpect(status().isNotFound());
    }

    /**
     * An invalid form must NOT call the plugin API: a failed call drags a resource pack
     * rebuild along with it, and produces a far less useful error message.
     */
    @Test
    @WithMockUser
    void invalidFormIsRejectedBeforeTouchingThePluginApi() throws Exception {
        mockMvc.perform(post("/blocks").with(csrf())
                        .param("id", "../../evil")
                        .param("instrument", "BASS_GUITAR")
                        .param("note", "99")
                        .param("textureId", "")
                        .param("dropItemId", "GLOWSTONE")
                        .param("displayName", "Evil"))
                .andExpect(status().isOk())
                .andExpect(view().name("blocks/form"))
                .andExpect(content().string(containsString("only letters, digits")))
                .andExpect(content().string(containsString("Note must be between 0 and 24")))
                .andExpect(content().string(containsString("Texture ID is required")));

        verify(blockApiClient, never()).create(any());
    }

    @Test
    @WithMockUser
    void validFormStillReachesThePluginApi() throws Exception {
        mockMvc.perform(post("/blocks").with(csrf())
                        .param("id", "void_netherite_block")
                        .param("instrument", "BASS_GUITAR")
                        .param("note", "12")
                        .param("textureId", "void_netherite_block")
                        .param("dropItemId", "GLOWSTONE")
                        .param("displayName", "Glowing Lantern"))
                .andExpect(status().is3xxRedirection());

        verify(blockApiClient).create(any());
    }

    @Test
    void unauthenticatedRequestIsRejected() throws Exception {
        mockMvc.perform(get("/blocks"))
                .andExpect(status().isUnauthorized());
    }
}
