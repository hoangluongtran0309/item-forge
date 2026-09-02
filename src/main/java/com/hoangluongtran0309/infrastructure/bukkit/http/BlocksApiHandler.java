package com.hoangluongtran0309.infrastructure.bukkit.http;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Material;

import com.sun.net.httpserver.HttpExchange;

import com.hoangluongtran0309.application.CustomBlockLoaderService;
import com.hoangluongtran0309.application.TextureUploadService;
import com.hoangluongtran0309.domain.CustomBlockRegistry;
import com.hoangluongtran0309.domain.ItemRegistry;
import com.hoangluongtran0309.domain.model.CustomBlockDefinition;
import com.hoangluongtran0309.infrastructure.bukkit.block.CustomBlockStackFactory;
import com.hoangluongtran0309.infrastructure.bukkit.block.NoteBlockStateFactory;
import com.hoangluongtran0309.infrastructure.config.CustomBlockDefinitionMapper;

public class BlocksApiHandler implements ApiResourceHandler {

    private final CustomBlockRegistry registry;
    private final CustomBlockLoaderService loaderService;
    private final CustomBlockStackFactory customBlockStackFactory;
    private final NoteBlockStateFactory noteBlockStateFactory;
    private final ItemRegistry itemRegistry;
    private final TextureUploadService textureUploadService;
    private final long maxUploadBytes;

    public BlocksApiHandler(CustomBlockRegistry registry, CustomBlockLoaderService loaderService,
            CustomBlockStackFactory customBlockStackFactory, NoteBlockStateFactory noteBlockStateFactory,
            ItemRegistry itemRegistry, TextureUploadService textureUploadService, long maxUploadBytes) {
        this.registry = registry;
        this.loaderService = loaderService;
        this.customBlockStackFactory = customBlockStackFactory;
        this.noteBlockStateFactory = noteBlockStateFactory;
        this.itemRegistry = itemRegistry;
        this.textureUploadService = textureUploadService;
        this.maxUploadBytes = maxUploadBytes;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String subPath = ApiPaths.subPath(exchange, "/api/blocks");

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

    // Keyed by textureId, NOT by block id -- the same way ArmorApiHandler keys texture
    // layers by armorAssetId rather than armor id (see TextureUploadService).
    private void uploadTexture(HttpExchange exchange, String id) throws IOException {
        CustomBlockDefinition definition = registry.get(id)
                .orElseThrow(() -> new ApiException(404, "Unknown block id: " + id));
        byte[] bytes = MultipartParser.extractSingleFilePart(exchange, maxUploadBytes);
        String sha1 = textureUploadService.uploadBlockTexture(definition.textureId(), bytes);
        HttpJson.send(exchange, 200, textureUploadResponse(sha1));
    }

    // textureId always comes from the registry, NEVER from the URL path -- the id in the
    // URL has to resolve to an already-registered block first, like every other route.
    // texture khac
    private void downloadTexture(HttpExchange exchange, String id) throws IOException {
        CustomBlockDefinition definition = registry.get(id)
                .orElseThrow(() -> new ApiException(404, "Unknown block id: " + id));
        byte[] bytes = textureUploadService.blockTexture(definition.textureId())
                .orElseThrow(() -> new ApiException(404, "Block '" + id + "' has no texture yet"));
        HttpBinary.send(exchange, 200, "image/png", bytes);
    }

    private void textureStatus(HttpExchange exchange, String id) throws IOException {
        CustomBlockDefinition definition = registry.get(id)
                .orElseThrow(() -> new ApiException(404, "Unknown block id: " + id));
        HttpJson.send(exchange, 200,
                Map.of("hasTexture", textureUploadService.blockTextureExists(definition.textureId())));
    }

    private Map<String, Object> textureUploadResponse(String sha1) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("rebuilt", true);
        json.put("sha1", sha1);
        return json;
    }

    private void list(HttpExchange exchange) throws IOException {
        List<Map<String, Object>> blocks = registry.getAll().stream().map(this::toJson).toList();
        HttpJson.send(exchange, 200, Map.of("blocks", blocks));
    }

    private void get(HttpExchange exchange, String id) throws IOException {
        CustomBlockDefinition definition = registry.get(id)
                .orElseThrow(() -> new ApiException(404, "Unknown block id: " + id));
        HttpJson.send(exchange, 200, toJson(definition));
    }

    private void create(HttpExchange exchange) throws IOException {
        Map<String, Object> body = HttpJson.readBody(exchange);
        String id = requireId(body);

        if (registry.get(id).isPresent()) {
            throw new ApiException(409, "Block id '" + id + "' already exists");
        }

        CustomBlockDefinition definition = CustomBlockDefinitionMapper.fromMap(id, body);
        if (definition.customModelData() == 0) {
            definition = new CustomBlockDefinition(id, definition.instrument(), definition.note(),
                    definition.textureId(), definition.dropItemId(), definition.displayName(),
                    registry.nextAvailableCustomModelData(), definition.lore());
        }

        validateDropItemId(definition);
        validateBlockDefinition(definition);

        if (!loaderService.save(definition)) {
            throw comboCollision(definition);
        }
        HttpJson.send(exchange, 201, toJson(definition));
    }

    private void update(HttpExchange exchange, String id) throws IOException {
        if (registry.get(id).isEmpty()) {
            throw new ApiException(404, "Unknown block id: " + id);
        }

        Map<String, Object> body = HttpJson.readBody(exchange);
        CustomBlockDefinition definition = CustomBlockDefinitionMapper.fromMap(id, body);

        validateDropItemId(definition);
        validateBlockDefinition(definition);

        if (!loaderService.save(definition)) {
            throw comboCollision(definition);
        }
        HttpJson.send(exchange, 200, toJson(definition));
    }

    private void delete(HttpExchange exchange, String id) throws IOException {
        if (registry.get(id).isEmpty()) {
            throw new ApiException(404, "Unknown block id: " + id);
        }

        loaderService.delete(id);
        HttpJson.send(exchange, 204, null);
    }

    // CustomBlockStackFactory.create() does NOT validate the instrument name itself (only
    // NoteBlockStateFactory calls Instrument.valueOf), so both have to be called to catch
    // a bad instrument name and still build the complete ItemStack (model/PDC/lore...).
    // resolveInstrument() is used (Instrument.valueOf only, never touching
    // Material.NOTE_BLOCK.createBlockData()) rather than the full createBlockData():
    // building real BlockData is only needed when actually placing the block in the world
    // (CustomBlockPlaceListener), and is neither necessary nor advisable during API
    // validation.
    private void validateBlockDefinition(CustomBlockDefinition definition) {
        try {
            noteBlockStateFactory.resolveInstrument(definition.instrument());
            customBlockStackFactory.create(definition);
        } catch (RuntimeException e) {
            throw new ApiException(400, "Invalid block definition: " + e.getMessage());
        }
    }

    // Stricter than the runtime read of blocks.yml (CustomBlockBreakListener tolerates an
    // unresolvable drop-item-id, logging a warning and dropping nothing). The dashboard
    // should report the problem clearly at create/edit time instead of leaving an admin to
    // discover it after mining the block and seeing nothing drop. It follows exactly the
    // resolution order CustomBlockBreakListener.resolveDrop() uses, so it can never
    // contradict what really happens when the block is broken.
    private void validateDropItemId(CustomBlockDefinition definition) {
        String dropItemId = definition.dropItemId();
        if (itemRegistry.get(dropItemId).isPresent()) {
            return;
        }
        if (Material.matchMaterial(dropItemId) != null) {
            return;
        }
        throw new ApiException(400, "Unknown drop-item-id '" + dropItemId
                + "' (matches neither a registered item nor a vanilla material)");
    }

    private ApiException comboCollision(CustomBlockDefinition definition) {
        return new ApiException(409, "Custom block '" + definition.id() + "' cannot use instrument '"
                + definition.instrument() + "' + note " + definition.note()
                + ": that combination is already used by another block");
    }

    private String requireId(Map<String, Object> body) {
        Object id = body.get("id");
        if (id == null || id.toString().isBlank()) {
            throw new ApiException(400, "Missing required field: id");
        }
        return id.toString();
    }

    private Map<String, Object> toJson(CustomBlockDefinition definition) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("id", definition.id());
        json.putAll(CustomBlockDefinitionMapper.toMap(definition));
        return json;
    }
}
