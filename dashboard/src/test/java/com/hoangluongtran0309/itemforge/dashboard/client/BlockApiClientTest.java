package com.hoangluongtran0309.itemforge.dashboard.client;

import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.restclient.test.autoconfigure.RestClientTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.client.MockRestServiceServer;

import com.hoangluongtran0309.itemforge.dashboard.config.PluginApiProperties;
import com.hoangluongtran0309.itemforge.dashboard.config.RestClientConfig;
import com.hoangluongtran0309.itemforge.dashboard.dto.BlockJson;
import com.hoangluongtran0309.itemforge.dashboard.dto.BlockTextureStatusResponse;
import com.hoangluongtran0309.itemforge.dashboard.dto.TextureUploadResponse;

@RestClientTest({BlockApiClient.class, RestClientConfig.class})
@EnableConfigurationProperties(PluginApiProperties.class)
@TestPropertySource(properties = {
        "itemforge.api.base-url=http://plugin.local",
        "itemforge.api.token=secret-token"
})
class BlockApiClientTest {

    @Autowired
    private BlockApiClient blockApiClient;

    @Autowired
    private MockRestServiceServer server;

    @Test
    void findAllSendsTheBearerTokenAndUnwrapsTheBlocksEnvelope() {
        server.expect(requestTo("http://plugin.local/api/blocks"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer secret-token"))
                .andRespond(withSuccess("""
                        {"blocks":[{"id":"void_netherite_block","instrument":"BASS_GUITAR","note":12,\
                        "texture-id":"void_netherite_block","drop-item-id":"GLOWSTONE",\
                        "display-name":"Glowing Lantern","custom-model-data":3001,"lore":[]}]}
                        """, MediaType.APPLICATION_JSON));

        List<BlockJson> blocks = blockApiClient.findAll();

        assertEquals(1, blocks.size());
        assertEquals("void_netherite_block", blocks.get(0).id());
        assertEquals("BASS_GUITAR", blocks.get(0).instrument());
    }

    @Test
    void notFoundResponseIsParsedIntoPluginApiException() {
        server.expect(requestTo("http://plugin.local/api/blocks/missing"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":\"Unknown block id: missing\"}"));

        PluginApiException exception = assertThrows(PluginApiException.class,
                () -> blockApiClient.findById("missing"));

        assertEquals(404, exception.getStatus());
        assertEquals("Unknown block id: missing", exception.getMessage());
    }

    @Test
    void createSendsAPostWithTheBlockBody() {
        server.expect(requestTo("http://plugin.local/api/blocks"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {"id":"void_netherite_block","instrument":"BASS_GUITAR","note":12,\
                                "texture-id":"void_netherite_block","drop-item-id":"GLOWSTONE",\
                                "display-name":"Glowing Lantern","custom-model-data":3001,"lore":[]}
                                """));

        BlockJson created = blockApiClient.create(new BlockJson("void_netherite_block", "BASS_GUITAR", 12,
                "void_netherite_block", "GLOWSTONE", "Glowing Lantern", 3001, List.of()));

        assertEquals("void_netherite_block", created.textureId());
    }

    @Test
    void deleteSendsADeleteRequest() {
        server.expect(requestTo("http://plugin.local/api/blocks/void_netherite_block"))
                .andExpect(method(HttpMethod.DELETE))
                .andRespond(withStatus(HttpStatus.NO_CONTENT));

        blockApiClient.delete("void_netherite_block");

        server.verify();
    }

    @Test
    void uploadTextureSendsMultipartFormData() {
        server.expect(requestTo("http://plugin.local/api/blocks/void_netherite_block/texture"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Content-Type", startsWith("multipart/form-data")))
                .andRespond(withSuccess("""
                        {"rebuilt":true,"sha1":"abc123"}
                        """, MediaType.APPLICATION_JSON));

        TextureUploadResponse response = blockApiClient.uploadTexture("void_netherite_block", new byte[] { 1, 2, 3 },
                "texture.png");

        assertTrue(response.rebuilt());
        assertEquals("abc123", response.sha1());
    }

    @Test
    void textureStatusReturnsParsedResponse() {
        server.expect(requestTo("http://plugin.local/api/blocks/void_netherite_block/texture-status"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"hasTexture":true}
                        """, MediaType.APPLICATION_JSON));

        BlockTextureStatusResponse status = blockApiClient.textureStatus("void_netherite_block");

        assertTrue(status.hasTexture());
    }

    @Test
    void downloadTextureReturnsRawPngBytes() {
        server.expect(requestTo("http://plugin.local/api/blocks/void_netherite_block/texture"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(new byte[] { 1, 2, 3 }, MediaType.IMAGE_PNG));

        byte[] bytes = blockApiClient.downloadTexture("void_netherite_block");

        assertArrayEquals(new byte[] { 1, 2, 3 }, bytes);
    }

    @Test
    void downloadTextureNotFoundIsParsedIntoPluginApiException() {
        server.expect(requestTo("http://plugin.local/api/blocks/void_netherite_block/texture"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":\"Block 'void_netherite_block' has no texture yet\"}"));

        PluginApiException exception = assertThrows(PluginApiException.class,
                () -> blockApiClient.downloadTexture("void_netherite_block"));

        assertEquals(404, exception.getStatus());
    }
}
