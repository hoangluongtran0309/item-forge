package com.hoangluongtran0309.itemforge.dashboard.client;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.hoangluongtran0309.itemforge.dashboard.dto.RecipeJson;

@Component
public class RecipeApiClient {

    private final RestClient restClient;

    public RecipeApiClient(RestClient itemforgeRestClient) {
        this.restClient = itemforgeRestClient;
    }

    public List<RecipeJson> findAll() {
        RecipesResponse response = restClient.get().uri("/api/recipes").retrieve().body(RecipesResponse.class);
        return response == null ? List.of() : response.recipes();
    }

    public RecipeJson findById(String id) {
        return restClient.get().uri("/api/recipes/{id}", id).retrieve().body(RecipeJson.class);
    }

    public RecipeJson create(RecipeJson recipe) {
        return restClient.post().uri("/api/recipes").body(recipe).retrieve().body(RecipeJson.class);
    }

    public RecipeJson update(String id, RecipeJson recipe) {
        return restClient.put().uri("/api/recipes/{id}", id).body(recipe).retrieve().body(RecipeJson.class);
    }

    public void delete(String id) {
        restClient.delete().uri("/api/recipes/{id}", id).retrieve().toBodilessEntity();
    }

    private record RecipesResponse(List<RecipeJson> recipes) {
    }
}
