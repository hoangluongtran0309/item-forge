package com.hoangluongtran0309.itemforge.dashboard.client;

import java.util.List;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import com.hoangluongtran0309.itemforge.dashboard.dto.GenerateItemRequest;
import com.hoangluongtran0309.itemforge.dashboard.dto.ItemJson;
import com.hoangluongtran0309.itemforge.dashboard.dto.ItemTextureStatusResponse;
import com.hoangluongtran0309.itemforge.dashboard.dto.TextureUploadResponse;

@Component
public class ItemApiClient {

    private final RestClient restClient;

    public ItemApiClient(RestClient itemforgeRestClient) {
        this.restClient = itemforgeRestClient;
    }

    public List<ItemJson> findAll() {
        ItemsResponse response = restClient.get().uri("/api/items").retrieve().body(ItemsResponse.class);
        return response == null ? List.of() : response.items();
    }

    public ItemJson findById(String id) {
        return restClient.get().uri("/api/items/{id}", id).retrieve().body(ItemJson.class);
    }

    public ItemJson create(ItemJson item) {
        return restClient.post().uri("/api/items").body(item).retrieve().body(ItemJson.class);
    }

    public ItemJson update(String id, ItemJson item) {
        return restClient.put().uri("/api/items/{id}", id).body(item).retrieve().body(ItemJson.class);
    }

    public void delete(String id) {
        restClient.delete().uri("/api/items/{id}", id).retrieve().toBodilessEntity();
    }

    public ItemJson generateDraft(String id, String description) {
        return restClient.post().uri("/api/items/generate")
                .body(new GenerateItemRequest(id, description))
                .retrieve()
                .body(ItemJson.class);
    }

    public TextureUploadResponse uploadTexture(String id, byte[] pngBytes, String filename) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", asResource(pngBytes, filename));
        return restClient.post().uri("/api/items/{id}/texture", id)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(body)
                .retrieve()
                .body(TextureUploadResponse.class);
    }

    public ItemTextureStatusResponse textureStatus(String id) {
        return restClient.get().uri("/api/items/{id}/texture-status", id)
                .retrieve()
                .body(ItemTextureStatusResponse.class);
    }

    public byte[] downloadTexture(String id) {
        return restClient.get().uri("/api/items/{id}/texture", id).retrieve().body(byte[].class);
    }

    private static ByteArrayResource asResource(byte[] bytes, String filename) {
        return new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                return filename;
            }
        };
    }

    private record ItemsResponse(List<ItemJson> items) {
    }
}
