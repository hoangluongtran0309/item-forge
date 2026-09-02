package com.hoangluongtran0309.itemforge.dashboard.client;

import java.util.List;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import com.hoangluongtran0309.itemforge.dashboard.dto.BlockJson;
import com.hoangluongtran0309.itemforge.dashboard.dto.BlockTextureStatusResponse;
import com.hoangluongtran0309.itemforge.dashboard.dto.TextureUploadResponse;

@Component
public class BlockApiClient {

    private final RestClient restClient;

    public BlockApiClient(RestClient itemforgeRestClient) {
        this.restClient = itemforgeRestClient;
    }

    public List<BlockJson> findAll() {
        BlocksResponse response = restClient.get().uri("/api/blocks").retrieve().body(BlocksResponse.class);
        return response == null ? List.of() : response.blocks();
    }

    public BlockJson findById(String id) {
        return restClient.get().uri("/api/blocks/{id}", id).retrieve().body(BlockJson.class);
    }

    public BlockJson create(BlockJson block) {
        return restClient.post().uri("/api/blocks").body(block).retrieve().body(BlockJson.class);
    }

    public BlockJson update(String id, BlockJson block) {
        return restClient.put().uri("/api/blocks/{id}", id).body(block).retrieve().body(BlockJson.class);
    }

    public void delete(String id) {
        restClient.delete().uri("/api/blocks/{id}", id).retrieve().toBodilessEntity();
    }

    public TextureUploadResponse uploadTexture(String id, byte[] pngBytes, String filename) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", asResource(pngBytes, filename));
        return restClient.post().uri("/api/blocks/{id}/texture", id)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(body)
                .retrieve()
                .body(TextureUploadResponse.class);
    }

    public BlockTextureStatusResponse textureStatus(String id) {
        return restClient.get().uri("/api/blocks/{id}/texture-status", id)
                .retrieve()
                .body(BlockTextureStatusResponse.class);
    }

    public byte[] downloadTexture(String id) {
        return restClient.get().uri("/api/blocks/{id}/texture", id).retrieve().body(byte[].class);
    }

    private static ByteArrayResource asResource(byte[] bytes, String filename) {
        return new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                return filename;
            }
        };
    }

    private record BlocksResponse(List<BlockJson> blocks) {
    }
}
