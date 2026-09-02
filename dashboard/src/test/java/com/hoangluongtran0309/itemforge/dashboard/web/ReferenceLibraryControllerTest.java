package com.hoangluongtran0309.itemforge.dashboard.web;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.hoangluongtran0309.itemforge.dashboard.service.ReferenceLibraryService;
import com.hoangluongtran0309.itemforge.dashboard.service.ReferenceLibraryService.ReferenceTexture;

@WebMvcTest(ReferenceLibraryController.class)
@Import(ActiveNavAdvice.class)
class ReferenceLibraryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReferenceLibraryService referenceLibrary;

    @Test
    @WithMockUser
    void indexListsPacksAndTextures() throws Exception {
        when(referenceLibrary.packs()).thenReturn(List.of("vanilla"));
        when(referenceLibrary.search(any(), anyInt())).thenReturn(List.of(
                new ReferenceTexture("vanilla", "assets/minecraft/textures/item/apple.png", "apple.png")));

        mockMvc.perform(get("/reference"))
                .andExpect(status().isOk())
                .andExpect(view().name("reference/index"))
                .andExpect(content().string(containsString("apple.png")))
                .andExpect(content().string(
                        containsString("/reference/file/vanilla/assets/minecraft/textures/item/apple.png")))
                // Must state explicitly that no Mojang asset is bundled.
                .andExpect(content().string(containsString("ships no Minecraft assets")));
    }

    @Test
    @WithMockUser
    void searchReturnsOnlyTheGridFragment() throws Exception {
        when(referenceLibrary.search(anyString(), anyInt())).thenReturn(List.of());

        mockMvc.perform(get("/reference/search").param("q", "sword"))
                .andExpect(status().isOk())
                .andExpect(view().name("reference/index :: texture-grid"));
    }

    @Test
    @WithMockUser
    void importSurfacesTheRejectionReasonInsteadOfFailingSilently() throws Exception {
        when(referenceLibrary.importPack(anyString(), any()))
                .thenThrow(new IllegalArgumentException("No textures found"));

        MockMultipartFile file = new MockMultipartFile("file", "pack.zip", "application/zip", new byte[] { 1 });

        mockMvc.perform(multipart("/reference/import").file(file).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("errorBanner", "No textures found"));
    }

    @Test
    @WithMockUser
    void fileServesPngBytesAndPassesTheFullSubPathThrough() throws Exception {
        when(referenceLibrary.read("vanilla", "assets/minecraft/textures/item/apple.png"))
                .thenReturn(Optional.of(new byte[] { 1, 2, 3 }));

        mockMvc.perform(get("/reference/file/vanilla/assets/minecraft/textures/item/apple.png"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(content().bytes(new byte[] { 1, 2, 3 }));
    }

    @Test
    @WithMockUser
    void fileReturns400WhenTheServiceRefusesThePath() throws Exception {
        when(referenceLibrary.read(anyString(), anyString()))
                .thenThrow(new IllegalArgumentException("outside the reference library"));

        mockMvc.perform(get("/reference/file/vanilla/../../etc/passwd"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void unauthenticatedRequestIsRejected() throws Exception {
        mockMvc.perform(get("/reference"))
                .andExpect(status().isUnauthorized());
    }
}
