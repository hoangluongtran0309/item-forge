package com.hoangluongtran0309.itemforge.dashboard.client;

import java.util.List;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import com.hoangluongtran0309.itemforge.dashboard.dto.ArmorJson;
import com.hoangluongtran0309.itemforge.dashboard.dto.ArmorTextureStatusResponse;
import com.hoangluongtran0309.itemforge.dashboard.dto.TextureUploadResponse;

@Component
public class ArmorApiClient {

    private final RestClient restClient;

    public ArmorApiClient(RestClient itemforgeRestClient) {
        this.restClient = itemforgeRestClient;
    }

    public List<ArmorJson> findAll() {
        ArmorResponse response = restClient.get().uri("/api/armor").retrieve().body(ArmorResponse.class);
        return response == null ? List.of() : response.armor();
    }

    public ArmorJson findById(String id) {
        return restClient.get().uri("/api/armor/{id}", id).retrieve().body(ArmorJson.class);
    }

    public ArmorJson create(ArmorJson armor) {
        return restClient.post().uri("/api/armor").body(armor).retrieve().body(ArmorJson.class);
    }

    public ArmorJson update(String id, ArmorJson armor) {
        return restClient.put().uri("/api/armor/{id}", id).body(armor).retrieve().body(ArmorJson.class);
    }

    public void delete(String id) {
        restClient.delete().uri("/api/armor/{id}", id).retrieve().toBodilessEntity();
    }

    public TextureUploadResponse uploadIcon(String id, byte[] pngBytes, String filename) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", asResource(pngBytes, filename));
        return restClient.post().uri("/api/armor/{id}/icon", id)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(body)
                .retrieve()
                .body(TextureUploadResponse.class);
    }

    public TextureUploadResponse uploadLayer(String id, String layer, byte[] pngBytes, String filename) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", asResource(pngBytes, filename));
        return restClient.post()
                .uri(uriBuilder -> uriBuilder.path("/api/armor/{id}/texture").queryParam("layer", layer).build(id))
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(body)
                .retrieve()
                .body(TextureUploadResponse.class);
    }

    public ArmorTextureStatusResponse textureStatus(String id) {
        return restClient.get().uri("/api/armor/{id}/texture-status", id)
                .retrieve()
                .body(ArmorTextureStatusResponse.class);
    }

    public byte[] downloadIcon(String id) {
        return restClient.get().uri("/api/armor/{id}/icon", id).retrieve().body(byte[].class);
    }

    public byte[] downloadLayer(String id, String layer) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/armor/{id}/texture").queryParam("layer", layer).build(id))
                .retrieve()
                .body(byte[].class);
    }

    private static ByteArrayResource asResource(byte[] bytes, String filename) {
        return new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                return filename;
            }
        };
    }

    private record ArmorResponse(List<ArmorJson> armor) {
    }
}
