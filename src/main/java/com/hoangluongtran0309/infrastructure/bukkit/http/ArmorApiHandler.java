package com.hoangluongtran0309.infrastructure.bukkit.http;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.sun.net.httpserver.HttpExchange;

import com.hoangluongtran0309.application.ArmorConfigLoaderService;
import com.hoangluongtran0309.application.TextureUploadService;
import com.hoangluongtran0309.domain.ArmorRegistry;
import com.hoangluongtran0309.domain.model.ArmorDefinition;
import com.hoangluongtran0309.infrastructure.bukkit.ArmorStackFactory;
import com.hoangluongtran0309.infrastructure.config.ArmorDefinitionMapper;

public class ArmorApiHandler implements ApiResourceHandler {

    private final ArmorRegistry registry;
    private final ArmorConfigLoaderService loaderService;
    private final ArmorStackFactory armorStackFactory;
    private final TextureUploadService textureUploadService;
    private final long maxUploadBytes;

    public ArmorApiHandler(ArmorRegistry registry, ArmorConfigLoaderService loaderService,
            ArmorStackFactory armorStackFactory, TextureUploadService textureUploadService, long maxUploadBytes) {
        this.registry = registry;
        this.loaderService = loaderService;
        this.armorStackFactory = armorStackFactory;
        this.textureUploadService = textureUploadService;
        this.maxUploadBytes = maxUploadBytes;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String subPath = ApiPaths.subPath(exchange, "/api/armor");

        if (subPath.isEmpty()) {
            switch (method) {
                case "GET" -> list(exchange);
                case "POST" -> create(exchange);
                default -> throw new ApiException(405, "Method not allowed");
            }
            return;
        }

        int slash = subPath.indexOf('/');
        if (slash >= 0) {
            String id = subPath.substring(0, slash);
            String rest = subPath.substring(slash + 1);
            switch (rest) {
                case "icon" -> {
                    switch (method) {
                        case "POST" -> uploadIcon(exchange, id);
                        case "GET" -> downloadIcon(exchange, id);
                        default -> throw new ApiException(405, "Method not allowed");
                    }
                }
                case "texture" -> {
                    switch (method) {
                        case "POST" -> uploadLayerTexture(exchange, id);
                        case "GET" -> downloadLayerTexture(exchange, id);
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

    private void uploadIcon(HttpExchange exchange, String id) throws IOException {
        ArmorDefinition armor = registry.get(id).orElseThrow(() -> new ApiException(404, "Unknown armor id: " + id));
        byte[] bytes = MultipartParser.extractSingleFilePart(exchange, maxUploadBytes);
        String sha1 = textureUploadService.uploadArmorIcon(armor.id(), bytes);
        HttpJson.send(exchange, 200, textureUploadResponse(sha1));
    }

    private void uploadLayerTexture(HttpExchange exchange, String id) throws IOException {
        ArmorDefinition armor = registry.get(id).orElseThrow(() -> new ApiException(404, "Unknown armor id: " + id));
        String layer = ApiPaths.queryParams(exchange).get("layer");
        if (layer == null) {
            throw new ApiException(400, "Missing required query parameter: layer");
        }
        byte[] bytes = MultipartParser.extractSingleFilePart(exchange, maxUploadBytes);
        String sha1 = switch (layer) {
            case "humanoid" -> textureUploadService.uploadArmorLayer1(armor.armorAssetId(), bytes);
            case "humanoid_leggings" -> textureUploadService.uploadArmorLayer2(armor.armorAssetId(), bytes);
            default -> throw new ApiException(400,
                    "Invalid layer value '" + layer + "' (expected 'humanoid' or 'humanoid_leggings')");
        };
        HttpJson.send(exchange, 200, textureUploadResponse(sha1));
    }

    private void downloadIcon(HttpExchange exchange, String id) throws IOException {
        ArmorDefinition armor = registry.get(id).orElseThrow(() -> new ApiException(404, "Unknown armor id: " + id));
        byte[] bytes = textureUploadService.armorIcon(armor.id())
                .orElseThrow(() -> new ApiException(404, "No icon uploaded for armor id: " + id));
        HttpBinary.send(exchange, 200, "image/png", bytes);
    }

    private void downloadLayerTexture(HttpExchange exchange, String id) throws IOException {
        ArmorDefinition armor = registry.get(id).orElseThrow(() -> new ApiException(404, "Unknown armor id: " + id));
        String layer = ApiPaths.queryParams(exchange).get("layer");
        if (layer == null) {
            throw new ApiException(400, "Missing required query parameter: layer");
        }
        Optional<byte[]> bytes = switch (layer) {
            case "humanoid" -> textureUploadService.armorLayer1(armor.armorAssetId());
            case "humanoid_leggings" -> textureUploadService.armorLayer2(armor.armorAssetId());
            default -> throw new ApiException(400,
                    "Invalid layer value '" + layer + "' (expected 'humanoid' or 'humanoid_leggings')");
        };
        HttpBinary.send(exchange, 200, "image/png",
                bytes.orElseThrow(() -> new ApiException(404, "No '" + layer + "' texture uploaded for armor id: " + id)));
    }

    private void textureStatus(HttpExchange exchange, String id) throws IOException {
        ArmorDefinition armor = registry.get(id).orElseThrow(() -> new ApiException(404, "Unknown armor id: " + id));
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("hasIcon", textureUploadService.armorIconExists(armor.id()));
        json.put("armorAssetId", armor.armorAssetId());
        json.put("hasHumanoidLayer", textureUploadService.armorLayer1Exists(armor.armorAssetId()));
        json.put("hasHumanoidLeggingsLayer", textureUploadService.armorLayer2Exists(armor.armorAssetId()));
        HttpJson.send(exchange, 200, json);
    }

    private Map<String, Object> textureUploadResponse(String sha1) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("rebuilt", true);
        json.put("sha1", sha1);
        return json;
    }

    private void list(HttpExchange exchange) throws IOException {
        List<Map<String, Object>> armor = registry.getAll().stream().map(this::toJson).toList();
        HttpJson.send(exchange, 200, Map.of("armor", armor));
    }

    private void get(HttpExchange exchange, String id) throws IOException {
        ArmorDefinition definition = registry.get(id)
                .orElseThrow(() -> new ApiException(404, "Unknown armor id: " + id));
        HttpJson.send(exchange, 200, toJson(definition));
    }

    private void create(HttpExchange exchange) throws IOException {
        Map<String, Object> body = HttpJson.readBody(exchange);
        String id = requireId(body);

        if (registry.get(id).isPresent()) {
            throw new ApiException(409, "Armor id '" + id + "' already exists");
        }

        ArmorDefinition definition = ArmorDefinitionMapper.fromMap(id, body);
        validateMaterial(definition);
        loaderService.save(definition);
        HttpJson.send(exchange, 201, toJson(definition));
    }

    private void update(HttpExchange exchange, String id) throws IOException {
        if (registry.get(id).isEmpty()) {
            throw new ApiException(404, "Unknown armor id: " + id);
        }

        Map<String, Object> body = HttpJson.readBody(exchange);
        ArmorDefinition definition = ArmorDefinitionMapper.fromMap(id, body);

        validateMaterial(definition);
        loaderService.save(definition);
        HttpJson.send(exchange, 200, toJson(definition));
    }

    private void delete(HttpExchange exchange, String id) throws IOException {
        if (registry.get(id).isEmpty()) {
            throw new ApiException(404, "Unknown armor id: " + id);
        }

        loaderService.delete(id);
        HttpJson.send(exchange, 204, null);
    }

    private void validateMaterial(ArmorDefinition definition) {
        try {
            armorStackFactory.create(definition);
        } catch (RuntimeException e) {
            throw new ApiException(400, "Invalid armor definition: " + e.getMessage());
        }
    }

    private String requireId(Map<String, Object> body) {
        Object id = body.get("id");
        if (id == null || id.toString().isBlank()) {
            throw new ApiException(400, "Missing required field: id");
        }
        return id.toString();
    }

    private Map<String, Object> toJson(ArmorDefinition definition) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("id", definition.id());
        json.putAll(ArmorDefinitionMapper.toMap(definition));
        return json;
    }
}
