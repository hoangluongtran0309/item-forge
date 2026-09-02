package com.hoangluongtran0309.infrastructure.bukkit.http;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.sun.net.httpserver.HttpExchange;

import com.hoangluongtran0309.application.RecipeConfigLoaderService;
import com.hoangluongtran0309.domain.RecipeRegistry;
import com.hoangluongtran0309.domain.model.RecipeDefinition;
import com.hoangluongtran0309.infrastructure.config.RecipeDefinitionMapper;

public class RecipesApiHandler implements ApiResourceHandler {

    private final RecipeRegistry registry;
    private final RecipeConfigLoaderService loaderService;

    public RecipesApiHandler(RecipeRegistry registry, RecipeConfigLoaderService loaderService) {
        this.registry = registry;
        this.loaderService = loaderService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String subPath = ApiPaths.subPath(exchange, "/api/recipes");

        if (subPath.isEmpty()) {
            switch (method) {
                case "GET" -> list(exchange);
                case "POST" -> create(exchange);
                default -> throw new ApiException(405, "Method not allowed");
            }
            return;
        }

        switch (method) {
            case "GET" -> get(exchange, subPath);
            case "PUT" -> update(exchange, subPath);
            case "DELETE" -> delete(exchange, subPath);
            default -> throw new ApiException(405, "Method not allowed");
        }
    }

    private void list(HttpExchange exchange) throws IOException {
        List<Map<String, Object>> recipes = registry.getAll().stream().map(this::toJson).toList();
        HttpJson.send(exchange, 200, Map.of("recipes", recipes));
    }

    private void get(HttpExchange exchange, String id) throws IOException {
        RecipeDefinition definition = registry.get(id)
                .orElseThrow(() -> new ApiException(404, "Unknown recipe id: " + id));
        HttpJson.send(exchange, 200, toJson(definition));
    }

    private void create(HttpExchange exchange) throws IOException {
        Map<String, Object> body = HttpJson.readBody(exchange);
        String id = requireId(body);

        if (registry.get(id).isPresent()) {
            throw new ApiException(409, "Recipe id '" + id + "' already exists");
        }

        RecipeDefinition definition = RecipeDefinitionMapper.fromMap(id, body);
        loaderService.save(definition);
        HttpJson.send(exchange, 201, toJson(definition));
    }

    private void update(HttpExchange exchange, String id) throws IOException {
        if (registry.get(id).isEmpty()) {
            throw new ApiException(404, "Unknown recipe id: " + id);
        }

        Map<String, Object> body = HttpJson.readBody(exchange);
        RecipeDefinition definition = RecipeDefinitionMapper.fromMap(id, body);

        loaderService.save(definition);
        HttpJson.send(exchange, 200, toJson(definition));
    }

    private void delete(HttpExchange exchange, String id) throws IOException {
        if (registry.get(id).isEmpty()) {
            throw new ApiException(404, "Unknown recipe id: " + id);
        }

        loaderService.delete(id);
        HttpJson.send(exchange, 204, null);
    }

    private String requireId(Map<String, Object> body) {
        Object id = body.get("id");
        if (id == null || id.toString().isBlank()) {
            throw new ApiException(400, "Missing required field: id");
        }
        return id.toString();
    }

    private Map<String, Object> toJson(RecipeDefinition definition) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("id", definition.id());
        json.putAll(RecipeDefinitionMapper.toMap(definition));
        return json;
    }
}
