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
import com.hoangluongtran0309.itemforge.dashboard.dto.ItemJson;
import com.hoangluongtran0309.itemforge.dashboard.dto.ItemTextureStatusResponse;
import com.hoangluongtran0309.itemforge.dashboard.dto.TextureUploadResponse;

@RestClientTest({ItemApiClient.class, RestClientConfig.class})
@EnableConfigurationProperties(PluginApiProperties.class)
@TestPropertySource(properties = {
        "itemforge.api.base-url=http://plugin.local",
        "itemforge.api.token=secret-token"
})
class ItemApiClientTest {

    @Autowired
    private ItemApiClient itemApiClient;

    @Autowired
    private MockRestServiceServer server;

    @Test
    void findAllSendsTheBearerTokenAndUnwrapsTheItemsEnvelope() {
        server.expect(requestTo("http://plugin.local/api/items"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer secret-token"))
                .andRespond(withSuccess("""
                        {"items":[{"id":"void_sword","material":"NETHERITE_SWORD","custom-model-data":1,\
                        "display-name":"Fire Sword","lore":[],"abilities":[]}]}
                        """, MediaType.APPLICATION_JSON));

        List<ItemJson> items = itemApiClient.findAll();

        assertEquals(1, items.size());
        assertEquals("void_sword", items.get(0).id());
        assertEquals(1, items.get(0).customModelData());
    }

    @Test
    void notFoundResponseIsParsedIntoPluginApiException() {
        server.expect(requestTo("http://plugin.local/api/items/missing"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":\"Unknown item id: missing\"}"));

        PluginApiException exception = assertThrows(PluginApiException.class,
                () -> itemApiClient.findById("missing"));

        assertEquals(404, exception.getStatus());
        assertEquals("Unknown item id: missing", exception.getMessage());
    }

    @Test
    void createSendsAPostWithTheItemBody() {
        server.expect(requestTo("http://plugin.local/api/items"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {"id":"void_sword","material":"NETHERITE_SWORD","custom-model-data":7,\
                                "display-name":"Fire Sword","lore":[],"abilities":[]}
                                """));

        ItemJson created = itemApiClient.create(
                new ItemJson("void_sword", "NETHERITE_SWORD", 0, "Fire Sword", List.of(), List.of()));

        assertEquals(7, created.customModelData());
    }

    @Test
    void deleteSendsADeleteRequest() {
        server.expect(requestTo("http://plugin.local/api/items/void_sword"))
                .andExpect(method(HttpMethod.DELETE))
                .andRespond(withStatus(HttpStatus.NO_CONTENT));

        itemApiClient.delete("void_sword");

        server.verify();
    }

    @Test
    void uploadTextureSendsMultipartFormDataWithFilePart() {
        server.expect(requestTo("http://plugin.local/api/items/void_sword/texture"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Content-Type", startsWith("multipart/form-data")))
                .andRespond(withSuccess("""
                        {"rebuilt":true,"sha1":"abc123"}
                        """, MediaType.APPLICATION_JSON));

        TextureUploadResponse response = itemApiClient.uploadTexture("void_sword", new byte[] { 1, 2, 3 },
                "texture.png");

        assertTrue(response.rebuilt());
        assertEquals("abc123", response.sha1());
    }

    @Test
    void textureStatusReturnsParsedResponse() {
        server.expect(requestTo("http://plugin.local/api/items/void_sword/texture-status"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"hasTexture":true}
                        """, MediaType.APPLICATION_JSON));

        ItemTextureStatusResponse status = itemApiClient.textureStatus("void_sword");

        assertTrue(status.hasTexture());
    }

    @Test
    void downloadTextureReturnsRawPngBytes() {
        server.expect(requestTo("http://plugin.local/api/items/void_sword/texture"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(new byte[] { 1, 2, 3 }, MediaType.IMAGE_PNG));

        byte[] bytes = itemApiClient.downloadTexture("void_sword");

        assertArrayEquals(new byte[] { 1, 2, 3 }, bytes);
    }

    @Test
    void downloadTextureNotFoundIsParsedIntoPluginApiException() {
        server.expect(requestTo("http://plugin.local/api/items/void_sword/texture"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":\"No texture uploaded for item id: void_sword\"}"));

        PluginApiException exception = assertThrows(PluginApiException.class,
                () -> itemApiClient.downloadTexture("void_sword"));

        assertEquals(404, exception.getStatus());
    }
}
