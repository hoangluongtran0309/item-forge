package com.hoangluongtran0309.infrastructure.bukkit.http;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.sun.net.httpserver.HttpExchange;

import com.hoangluongtran0309.application.AiItemGenerationService;
import com.hoangluongtran0309.application.ItemConfigLoaderService;
import com.hoangluongtran0309.application.TextureUploadService;
import com.hoangluongtran0309.application.exception.AiRequestException;
import com.hoangluongtran0309.domain.ItemRegistry;
import com.hoangluongtran0309.domain.model.ItemDefinition;
import com.hoangluongtran0309.infrastructure.bukkit.ItemStackFactory;
import com.hoangluongtran0309.infrastructure.config.ItemDefinitionMapper;

public class ItemsApiHandler implements ApiResourceHandler {

    private final ItemRegistry registry;
    private final ItemConfigLoaderService loaderService;
    private final ItemStackFactory itemStackFactory;
    private final AiItemGenerationService aiItemGenerationService;
    private final TextureUploadService textureUploadService;
    private final long maxUploadBytes;

    public ItemsApiHandler(ItemRegistry registry, ItemConfigLoaderService loaderService,
            ItemStackFactory itemStackFactory, AiItemGenerationService aiItemGenerationService,
            TextureUploadService textureUploadService, long maxUploadBytes) {
        this.registry = registry;
        this.loaderService = loaderService;
        this.itemStackFactory = itemStackFactory;
        this.aiItemGenerationService = aiItemGenerationService;
        this.textureUploadService = textureUploadService;
        this.maxUploadBytes = maxUploadBytes;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String subPath = ApiPaths.subPath(exchange, "/api/items");

        if (subPath.isEmpty()) {
            switch (method) {
                case "GET" -> list(exchange);
                case "POST" -> create(exchange);
                default -> throw new ApiException(405, "Method not allowed");
            }
            return;
        }

        if (subPath.equals("generate")) {
            if (!method.equals("POST")) {
                throw new ApiException(405, "Method not allowed");
            }
            generate(exchange);
            return;
        }

        int slash = subPath.indexOf('/');
        if (slash >= 0) {
            String id = subPath.substring(0, slash);
            String rest = subPath.substring(slash + 1);
            switch (rest) {
                case "texture" -> {
                    switch (method) {
                        case "POST" -> uploadTexture(exchange, id);
                        case "GET" -> downloadTexture(exchange, id);
                        default -> throw new ApiException(405, "Method not allowed");
                    }
                }
                case "texture-status" -> {
                    if (!method.equals("GET")) {
                        throw new ApiException(405, "Method not allowed");
                    }
                    textureStatus(exchange, id);
                }
                default -> throw new ApiException(404, "Not found: " + subPath);
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

    private void uploadTexture(HttpExchange exchange, String id) throws IOException {
        if (registry.get(id).isEmpty()) {
            throw new ApiException(404, "Unknown item id: " + id);
        }
        byte[] bytes = MultipartParser.extractSingleFilePart(exchange, maxUploadBytes);
        String sha1 = textureUploadService.uploadItemTexture(id, bytes);
        HttpJson.send(exchange, 200, textureUploadResponse(sha1));
    }

    private void downloadTexture(HttpExchange exchange, String id) throws IOException {
        if (registry.get(id).isEmpty()) {
            throw new ApiException(404, "Unknown item id: " + id);
        }
        byte[] bytes = textureUploadService.itemTexture(id)
                .orElseThrow(() -> new ApiException(404, "No texture uploaded for item id: " + id));
        HttpBinary.send(exchange, 200, "image/png", bytes);
    }

    private void textureStatus(HttpExchange exchange, String id) throws IOException {
        if (registry.get(id).isEmpty()) {
            throw new ApiException(404, "Unknown item id: " + id);
        }
        HttpJson.send(exchange, 200, Map.of("hasTexture", textureUploadService.itemTextureExists(id)));
    }

    private Map<String, Object> textureUploadResponse(String sha1) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("rebuilt", true);
        json.put("sha1", sha1);
        return json;
    }

    private void list(HttpExchange exchange) throws IOException {
        List<Map<String, Object>> items = registry.getAll().stream().map(this::toJson).toList();
        HttpJson.send(exchange, 200, Map.of("items", items));
    }

    private void get(HttpExchange exchange, String id) throws IOException {
        ItemDefinition definition = registry.get(id)
                .orElseThrow(() -> new ApiException(404, "Unknown item id: " + id));
        HttpJson.send(exchange, 200, toJson(definition));
    }

    private void create(HttpExchange exchange) throws IOException {
        Map<String, Object> body = HttpJson.readBody(exchange);
        String id = requireId(body);

        if (registry.get(id).isPresent()) {
            throw new ApiException(409, "Item id '" + id + "' already exists");
        }

        ItemDefinition definition = ItemDefinitionMapper.fromMap(id, body);
        if (definition.customModelData() == 0) {
            definition = new ItemDefinition(id, definition.material(), registry.nextAvailableCustomModelData(),
                    definition.displayName(), definition.lore(), definition.abilities());
        }

        validateMaterial(definition);
        loaderService.save(definition);
        HttpJson.send(exchange, 201, toJson(definition));
    }

    private void update(HttpExchange exchange, String id) throws IOException {
        if (registry.get(id).isEmpty()) {
            throw new ApiException(404, "Unknown item id: " + id);
        }

        Map<String, Object> body = HttpJson.readBody(exchange);
        ItemDefinition definition = ItemDefinitionMapper.fromMap(id, body);

        validateMaterial(definition);
        loaderService.save(definition);
        HttpJson.send(exchange, 200, toJson(definition));
    }

    private void delete(HttpExchange exchange, String id) throws IOException {
        if (registry.get(id).isEmpty()) {
            throw new ApiException(404, "Unknown item id: " + id);
        }

        loaderService.delete(id);
        HttpJson.send(exchange, 204, null);
    }

    private void generate(HttpExchange exchange) throws IOException {
        if (!aiItemGenerationService.isEnabled()) {
            throw new ApiException(400, "AI item generation is disabled");
        }

        Map<String, Object> body = HttpJson.readBody(exchange);
        String id = requireId(body);
        Object descriptionValue = body.get("description");
        if (descriptionValue == null || descriptionValue.toString().isBlank()) {
            throw new ApiException(400, "Missing required field: description");
        }

        if (registry.get(id).isPresent()) {
            throw new ApiException(409, "Item id '" + id + "' already exists");
        }

        ItemDefinition draft;
        try {
            draft = aiItemGenerationService.generateDraft(id, descriptionValue.toString());
        } catch (AiRequestException e) {
            throw new ApiException(502, e.getMessage());
        }

        HttpJson.send(exchange, 200, toJson(draft));
    }

    private void validateMaterial(ItemDefinition definition) {
        try {
            itemStackFactory.create(definition);
        } catch (RuntimeException e) {
            throw new ApiException(400, "Invalid item definition: " + e.getMessage());
        }
    }

    private String requireId(Map<String, Object> body) {
        Object id = body.get("id");
        if (id == null || id.toString().isBlank()) {
            throw new ApiException(400, "Missing required field: id");
        }
        return id.toString();
    }

    private Map<String, Object> toJson(ItemDefinition definition) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("id", definition.id());
        json.putAll(ItemDefinitionMapper.toMap(definition));
        return json;
    }
}
