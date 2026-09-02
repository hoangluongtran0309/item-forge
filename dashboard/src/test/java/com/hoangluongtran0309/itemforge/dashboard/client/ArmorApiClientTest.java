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
import com.hoangluongtran0309.itemforge.dashboard.dto.ArmorJson;
import com.hoangluongtran0309.itemforge.dashboard.dto.ArmorTextureStatusResponse;
import com.hoangluongtran0309.itemforge.dashboard.dto.TextureUploadResponse;

@RestClientTest({ArmorApiClient.class, RestClientConfig.class})
@EnableConfigurationProperties(PluginApiProperties.class)
@TestPropertySource(properties = {
        "itemforge.api.base-url=http://plugin.local",
        "itemforge.api.token=secret-token"
})
class ArmorApiClientTest {

    @Autowired
    private ArmorApiClient armorApiClient;

    @Autowired
    private MockRestServiceServer server;

    @Test
    void findAllSendsTheBearerTokenAndUnwrapsTheArmorEnvelope() {
        server.expect(requestTo("http://plugin.local/api/armor"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer secret-token"))
                .andRespond(withSuccess("""
                        {"armor":[{"id":"void_helmet","material":"NETHERITE_HELMET","slot":"HELMET",\
                        "armor-asset-id":"void_armor","display-name":"Knight Helmet","lore":[]}]}
                        """, MediaType.APPLICATION_JSON));

        List<ArmorJson> armor = armorApiClient.findAll();

        assertEquals(1, armor.size());
        assertEquals("void_helmet", armor.get(0).id());
        assertEquals("void_armor", armor.get(0).armorAssetId());
    }

    @Test
    void notFoundResponseIsParsedIntoPluginApiException() {
        server.expect(requestTo("http://plugin.local/api/armor/missing"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":\"Unknown armor id: missing\"}"));

        PluginApiException exception = assertThrows(PluginApiException.class,
                () -> armorApiClient.findById("missing"));

        assertEquals(404, exception.getStatus());
        assertEquals("Unknown armor id: missing", exception.getMessage());
    }

    @Test
    void createSendsAPostWithTheArmorBody() {
        server.expect(requestTo("http://plugin.local/api/armor"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {"id":"void_helmet","material":"NETHERITE_HELMET","slot":"HELMET",\
                                "armor-asset-id":"void_armor","display-name":"Knight Helmet","lore":[]}
                                """));

        ArmorJson created = armorApiClient.create(
                new ArmorJson("void_helmet", "NETHERITE_HELMET", "HELMET", "void_armor", "Knight Helmet", List.of()));

        assertEquals("void_armor", created.armorAssetId());
    }

    @Test
    void deleteSendsADeleteRequest() {
        server.expect(requestTo("http://plugin.local/api/armor/void_helmet"))
                .andExpect(method(HttpMethod.DELETE))
                .andRespond(withStatus(HttpStatus.NO_CONTENT));

        armorApiClient.delete("void_helmet");

        server.verify();
    }

    @Test
    void uploadIconSendsMultipartFormData() {
        server.expect(requestTo("http://plugin.local/api/armor/void_helmet/icon"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Content-Type", startsWith("multipart/form-data")))
                .andRespond(withSuccess("""
                        {"rebuilt":true,"sha1":"abc123"}
                        """, MediaType.APPLICATION_JSON));

        TextureUploadResponse response = armorApiClient.uploadIcon("void_helmet", new byte[] { 1, 2, 3 },
                "icon.png");

        assertTrue(response.rebuilt());
        assertEquals("abc123", response.sha1());
    }

    @Test
    void uploadLayerSendsMultipartFormDataWithLayerQueryParam() {
        server.expect(requestTo("http://plugin.local/api/armor/void_helmet/texture?layer=humanoid"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Content-Type", startsWith("multipart/form-data")))
                .andRespond(withSuccess("""
                        {"rebuilt":true,"sha1":"def456"}
                        """, MediaType.APPLICATION_JSON));

        TextureUploadResponse response = armorApiClient.uploadLayer("void_helmet", "humanoid",
                new byte[] { 1, 2, 3 }, "layer1.png");

        assertEquals("def456", response.sha1());
    }

    @Test
    void textureStatusReturnsParsedResponse() {
        server.expect(requestTo("http://plugin.local/api/armor/void_helmet/texture-status"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"hasIcon":true,"armorAssetId":"void_armor","hasHumanoidLayer":true,\
                        "hasHumanoidLeggingsLayer":false}
                        """, MediaType.APPLICATION_JSON));

        ArmorTextureStatusResponse status = armorApiClient.textureStatus("void_helmet");

        assertTrue(status.hasIcon());
        assertEquals("void_armor", status.armorAssetId());
        assertTrue(status.hasHumanoidLayer());
    }

    @Test
    void downloadIconReturnsRawPngBytes() {
        server.expect(requestTo("http://plugin.local/api/armor/void_helmet/icon"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(new byte[] { 1, 2, 3 }, MediaType.IMAGE_PNG));

        byte[] bytes = armorApiClient.downloadIcon("void_helmet");

        assertArrayEquals(new byte[] { 1, 2, 3 }, bytes);
    }

    @Test
    void downloadLayerSendsLayerQueryParamAndReturnsRawPngBytes() {
        server.expect(requestTo("http://plugin.local/api/armor/void_helmet/texture?layer=humanoid"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(new byte[] { 4, 5, 6 }, MediaType.IMAGE_PNG));

        byte[] bytes = armorApiClient.downloadLayer("void_helmet", "humanoid");

        assertArrayEquals(new byte[] { 4, 5, 6 }, bytes);
    }

    @Test
    void downloadLayerNotFoundIsParsedIntoPluginApiException() {
        server.expect(requestTo("http://plugin.local/api/armor/void_helmet/texture?layer=humanoid"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":\"No 'humanoid' texture uploaded for armor id: void_helmet\"}"));

        PluginApiException exception = assertThrows(PluginApiException.class,
                () -> armorApiClient.downloadLayer("void_helmet", "humanoid"));

        assertEquals(404, exception.getStatus());
    }
}
