package com.hoangluongtran0309.infrastructure.bukkit.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.plugin.PluginMock;
import org.yaml.snakeyaml.Yaml;

import com.hoangluongtran0309.application.AiItemGenerationService;
import com.hoangluongtran0309.application.ArmorConfigLoaderService;
import com.hoangluongtran0309.application.CustomBlockLoaderService;
import com.hoangluongtran0309.application.ItemBalanceAnalysisService;
import com.hoangluongtran0309.application.ItemConfigLoaderService;
import com.hoangluongtran0309.application.RecipeConfigLoaderService;
import com.hoangluongtran0309.application.TextureUploadService;
import com.hoangluongtran0309.application.port.ArmorConfigSourcePort;
import com.hoangluongtran0309.application.port.ConfigSourcePort;
import com.hoangluongtran0309.application.port.CustomBlockConfigSourcePort;
import com.hoangluongtran0309.application.port.RecipeConfigSourcePort;
import com.hoangluongtran0309.application.port.ResourcePackInfo;
import com.hoangluongtran0309.application.port.ResourcePackPort;
import com.hoangluongtran0309.domain.ArmorRegistry;
import com.hoangluongtran0309.domain.CustomBlockRegistry;
import com.hoangluongtran0309.domain.ItemRegistry;
import com.hoangluongtran0309.domain.RecipeRegistry;
import com.hoangluongtran0309.domain.balance.BalanceRuleSet;
import com.hoangluongtran0309.domain.model.ArmorDefinition;
import com.hoangluongtran0309.domain.model.ArmorSlot;
import com.hoangluongtran0309.domain.model.CustomBlockDefinition;
import com.hoangluongtran0309.domain.model.EffectCommand;
import com.hoangluongtran0309.domain.model.ItemDefinition;
import com.hoangluongtran0309.domain.model.PotionEffectAbilityDefinition;
import com.hoangluongtran0309.domain.model.RecipeDefinition;
import com.hoangluongtran0309.domain.model.TriggerType;
import com.hoangluongtran0309.infrastructure.bukkit.ArmorStackFactory;
import com.hoangluongtran0309.infrastructure.bukkit.ItemStackFactory;
import com.hoangluongtran0309.infrastructure.bukkit.block.CustomBlockStackFactory;
import com.hoangluongtran0309.infrastructure.bukkit.block.NoteBlockStateFactory;
import com.hoangluongtran0309.infrastructure.bukkit.model.ArmorModelStrategy;
import com.hoangluongtran0309.infrastructure.bukkit.model.BlockModelStrategy;
import com.hoangluongtran0309.infrastructure.bukkit.model.ItemModelStrategy;
import com.hoangluongtran0309.infrastructure.texture.FileSystemTextureStoragePort;

class DashboardApiServerTest {

    private static final int PORT = 18481;
    private static final String API_KEY = "test-key";
    private static final long MAX_UPLOAD_BYTES = 1024;

    @TempDir
    Path dataFolder;

    private ItemRegistry itemRegistry;
    private ArmorRegistry armorRegistry;
    private CustomBlockRegistry customBlockRegistry;
    private FakeCustomBlockConfigSource customBlockConfigSource;
    private FakeResourcePackPort resourcePackPort;
    private DashboardApiServer server;

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
        PluginMock plugin = MockBukkit.createMockPlugin("ItemForge");

        itemRegistry = new ItemRegistry();
        ItemConfigLoaderService loaderService = new ItemConfigLoaderService(new FakeItemConfigSource(),
                itemRegistry);
        ItemStackFactory itemStackFactory = new ItemStackFactory(plugin, new NoopItemModelStrategy());

        armorRegistry = new ArmorRegistry();
        ArmorConfigLoaderService armorLoaderService = new ArmorConfigLoaderService(new FakeArmorConfigSource(),
                armorRegistry);
        ArmorStackFactory armorStackFactory = new ArmorStackFactory(plugin, new NoopArmorModelStrategy());

        resourcePackPort = new FakeResourcePackPort();
        FileSystemTextureStoragePort textureStoragePort = new FileSystemTextureStoragePort(dataFolder,
                Logger.getAnonymousLogger());
        TextureUploadService textureUploadService = new TextureUploadService(textureStoragePort, resourcePackPort);

        ItemsApiHandler itemsHandler = new ItemsApiHandler(itemRegistry, loaderService, itemStackFactory,
                new AiItemGenerationService(null, itemRegistry, loaderService), textureUploadService,
                MAX_UPLOAD_BYTES);
        ArmorApiHandler armorHandler = new ArmorApiHandler(armorRegistry, armorLoaderService, armorStackFactory,
                textureUploadService, MAX_UPLOAD_BYTES);

        customBlockRegistry = new CustomBlockRegistry();
        customBlockConfigSource = new FakeCustomBlockConfigSource();
        CustomBlockLoaderService customBlockLoaderService = new CustomBlockLoaderService(customBlockConfigSource,
                customBlockRegistry, itemRegistry, armorRegistry);
        CustomBlockStackFactory customBlockStackFactory = new CustomBlockStackFactory(plugin,
                new NoopBlockModelStrategy());
        NoteBlockStateFactory noteBlockStateFactory = new NoteBlockStateFactory();
        BlocksApiHandler blocksHandler = new BlocksApiHandler(customBlockRegistry, customBlockLoaderService,
                customBlockStackFactory, noteBlockStateFactory, itemRegistry, textureUploadService,
                MAX_UPLOAD_BYTES);

        RecipeRegistry recipeRegistry = new RecipeRegistry();
        RecipeConfigLoaderService recipeLoaderService = new RecipeConfigLoaderService(new FakeRecipeConfigSource(),
                recipeRegistry);
        RecipesApiHandler recipesHandler = new RecipesApiHandler(recipeRegistry, recipeLoaderService);

        // Null AI port: the balance endpoint has to answer from the rules alone.
        BalanceApiHandler balanceHandler = new BalanceApiHandler(new ItemBalanceAnalysisService(itemRegistry,
                armorRegistry, recipeRegistry, new BalanceRuleSet(), null));

        server = new DashboardApiServer(PORT, API_KEY, itemsHandler, armorHandler, blocksHandler, recipesHandler,
                balanceHandler, Logger.getAnonymousLogger());
        try {
            server.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop();
        }
        MockBukkit.unmock();
    }

    @Test
    void missingAuthorizationHeaderReturns401() throws Exception {
        HttpResponse<String> response = send("GET", "/api/items", null, false);
        assertEquals(401, response.statusCode());
    }

    @Test
    void wrongApiKeyReturns401() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + PORT + "/api/items"))
                .header("Authorization", "Bearer wrong-key")
                .GET()
                .build();
        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(401, response.statusCode());
    }

    @Test
    void balanceReportsRuleFindingsAndSaysTheAiIsOff() throws Exception {
        itemRegistry.register(new ItemDefinition("void_sword", "NETHERITE_SWORD", 1, "Void Sword", List.of(),
                List.of(new PotionEffectAbilityDefinition(TriggerType.RIGHT_CLICK,
                        EffectCommand.EffectType.SPEED, 60, 30))));

        HttpResponse<String> response = send("GET", "/api/balance", null, true);

        assertEquals(200, response.statusCode());
        Map<String, Object> body = new Yaml().load(response.body());
        assertEquals(false, body.get("ai-enabled"));
        assertTrue(body.get("summary").toString().startsWith("Analyzed "));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> findings = (List<Map<String, Object>>) body.get("findings");
        assertTrue(findings.stream().anyMatch(finding -> finding.get("rule").equals("PERMANENT_EFFECT")));
        assertEquals("RULE", findings.get(0).get("source"));
    }

    @Test
    void balanceForOneIdReportsOnlyThatId() throws Exception {
        itemRegistry.register(new ItemDefinition("void_sword", "NETHERITE_SWORD", 1, "Void Sword", List.of(),
                List.of()));
        itemRegistry.register(new ItemDefinition("void_axe", "NETHERITE_AXE", 2, "Void Axe", List.of(), List.of()));

        HttpResponse<String> response = send("GET", "/api/balance/void_sword", null, true);

        assertEquals(200, response.statusCode());
        Map<String, Object> body = new Yaml().load(response.body());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> findings = (List<Map<String, Object>>) body.get("findings");
        assertTrue(findings.stream().allMatch(finding -> finding.get("target-id").equals("void_sword")));
    }

    @Test
    void balanceForAnUnknownIdReturns404() throws Exception {
        assertEquals(404, send("GET", "/api/balance/nope", null, true).statusCode());
    }

    @Test
    void balanceRejectsAnythingButGet() throws Exception {
        assertEquals(405, send("POST", "/api/balance", "{}", true).statusCode());
    }

    @Test
    void listReturnsEmptyItemsInitially() throws Exception {
        HttpResponse<String> response = send("GET", "/api/items", null, true);

        assertEquals(200, response.statusCode());
        Map<String, Object> body = new Yaml().load(response.body());
        assertEquals(List.of(), body.get("items"));
    }

    @Test
    void createGetAndDeleteRoundTrip() throws Exception {
        String createBody = """
                {"id":"void_sword","material":"NETHERITE_SWORD","display-name":"Fire Sword","lore":[],"abilities":[]}
                """;

        HttpResponse<String> created = send("POST", "/api/items", createBody, true);
        assertEquals(201, created.statusCode());

        HttpResponse<String> fetched = send("GET", "/api/items/void_sword", null, true);
        assertEquals(200, fetched.statusCode());
        Map<String, Object> fetchedBody = new Yaml().load(fetched.body());
        assertEquals("NETHERITE_SWORD", fetchedBody.get("material"));
        assertTrue(itemRegistry.get("void_sword").isPresent());

        HttpResponse<String> deleted = send("DELETE", "/api/items/void_sword", null, true);
        assertEquals(204, deleted.statusCode());
        assertTrue(itemRegistry.get("void_sword").isEmpty());

        HttpResponse<String> afterDelete = send("GET", "/api/items/void_sword", null, true);
        assertEquals(404, afterDelete.statusCode());
    }

    @Test
    void creatingDuplicateIdReturns409() throws Exception {
        String createBody = """
                {"id":"void_sword","material":"NETHERITE_SWORD","display-name":"Fire Sword","lore":[],"abilities":[]}
                """;

        assertEquals(201, send("POST", "/api/items", createBody, true).statusCode());
        assertEquals(409, send("POST", "/api/items", createBody, true).statusCode());
    }

    @Test
    void invalidMaterialReturns400WithoutPersisting() throws Exception {
        String createBody = """
                {"id":"broken","material":"NOT_A_REAL_MATERIAL","display-name":"Broken","lore":[],"abilities":[]}
                """;

        HttpResponse<String> response = send("POST", "/api/items", createBody, true);

        assertEquals(400, response.statusCode());
        assertTrue(itemRegistry.get("broken").isEmpty());
    }

    @Test
    void uploadItemTextureWritesFileAndReturnsSha1() throws Exception {
        registerItem("void_sword");
        resourcePackPort.sha1 = "abc123";

        HttpResponse<String> response = sendMultipart("POST", "/api/items/void_sword/texture", png(32, 32));

        assertEquals(200, response.statusCode());
        Map<String, Object> body = new Yaml().load(response.body());
        assertEquals("abc123", body.get("sha1"));
        assertEquals(1, resourcePackPort.rebuildCount);

        HttpResponse<String> status = send("GET", "/api/items/void_sword/texture-status", null, true);
        assertEquals(200, status.statusCode());
        Map<String, Object> statusBody = new Yaml().load(status.body());
        assertTrue((Boolean) statusBody.get("hasTexture"));
    }

    @Test
    void uploadItemTextureRejectsNonPngWith400() throws Exception {
        registerItem("void_sword");

        HttpResponse<String> response = sendMultipart("POST", "/api/items/void_sword/texture",
                "not a png".getBytes(StandardCharsets.UTF_8));

        assertEquals(400, response.statusCode());
        assertEquals(0, resourcePackPort.rebuildCount);
    }

    @Test
    void uploadItemTextureRejectsWrongDimensionsWith400() throws Exception {
        registerItem("void_sword");

        HttpResponse<String> response = sendMultipart("POST", "/api/items/void_sword/texture", png(20, 20));

        assertEquals(400, response.statusCode());
    }

    @Test
    void uploadItemTextureRejectsOversizedFileWith413() throws Exception {
        registerItem("void_sword");

        byte[] oversized = new byte[(int) MAX_UPLOAD_BYTES + 1];
        HttpResponse<String> response = sendMultipart("POST", "/api/items/void_sword/texture", oversized);

        assertEquals(413, response.statusCode());
    }

    @Test
    void uploadItemTextureForUnknownIdReturns404() throws Exception {
        HttpResponse<String> response = sendMultipart("POST", "/api/items/unknown/texture", png(32, 32));
        assertEquals(404, response.statusCode());
    }

    @Test
    void downloadItemTextureReturnsUploadedBytes() throws Exception {
        registerItem("void_sword");
        byte[] uploaded = png(32, 32);
        assertEquals(200, sendMultipart("POST", "/api/items/void_sword/texture", uploaded).statusCode());

        HttpResponse<byte[]> response = sendGetBinary("/api/items/void_sword/texture");

        assertEquals(200, response.statusCode());
        assertEquals("image/png", response.headers().firstValue("Content-Type").orElse(null));
        assertTrue(java.util.Arrays.equals(uploaded, response.body()));
    }

    @Test
    void downloadItemTextureForItemWithoutTextureReturns404() throws Exception {
        registerItem("void_sword");
        assertEquals(404, sendGetBinary("/api/items/void_sword/texture").statusCode());
    }

    @Test
    void downloadItemTextureForUnknownIdReturns404() throws Exception {
        assertEquals(404, sendGetBinary("/api/items/unknown/texture").statusCode());
    }

    @Test
    void uploadArmorIconIsKeyedByOwnId() throws Exception {
        registerArmor("void_helmet", "void_armor");
        registerArmor("void_boots", "void_armor");

        HttpResponse<String> response = sendMultipart("POST", "/api/armor/void_helmet/icon", png(16, 16));
        assertEquals(200, response.statusCode());

        HttpResponse<String> helmetStatus = send("GET", "/api/armor/void_helmet/texture-status", null, true);
        Map<String, Object> helmetBody = new Yaml().load(helmetStatus.body());
        assertTrue((Boolean) helmetBody.get("hasIcon"));

        HttpResponse<String> bootsStatus = send("GET", "/api/armor/void_boots/texture-status", null, true);
        Map<String, Object> bootsBody = new Yaml().load(bootsStatus.body());
        assertFalse((Boolean) bootsBody.get("hasIcon"));
    }

    @Test
    void uploadArmorLayerIsKeyedByArmorAssetIdAndSharedAcrossPieces() throws Exception {
        registerArmor("void_helmet", "void_armor");
        registerArmor("void_boots", "void_armor");

        HttpResponse<String> response = sendMultipart("POST",
                "/api/armor/void_helmet/texture?layer=humanoid", png(64, 32));
        assertEquals(200, response.statusCode());

        HttpResponse<String> bootsStatus = send("GET", "/api/armor/void_boots/texture-status", null, true);
        Map<String, Object> bootsBody = new Yaml().load(bootsStatus.body());
        assertEquals("void_armor", bootsBody.get("armorAssetId"));
        assertTrue((Boolean) bootsBody.get("hasHumanoidLayer"));
        assertFalse((Boolean) bootsBody.get("hasHumanoidLeggingsLayer"));
    }

    @Test
    void uploadArmorLeggingsLayerOnlyAffectsLeggingsLayerFlag() throws Exception {
        registerArmor("void_leggings", "void_armor");

        HttpResponse<String> response = sendMultipart("POST",
                "/api/armor/void_leggings/texture?layer=humanoid_leggings", png(64, 32));
        assertEquals(200, response.statusCode());

        HttpResponse<String> status = send("GET", "/api/armor/void_leggings/texture-status", null, true);
        Map<String, Object> body = new Yaml().load(status.body());
        assertFalse((Boolean) body.get("hasHumanoidLayer"));
        assertTrue((Boolean) body.get("hasHumanoidLeggingsLayer"));
    }

    @Test
    void uploadArmorLayerRejectsWrongDimensionsWith400() throws Exception {
        registerArmor("void_helmet", "void_armor");

        HttpResponse<String> response = sendMultipart("POST",
                "/api/armor/void_helmet/texture?layer=humanoid", png(32, 32));

        assertEquals(400, response.statusCode());
    }

    @Test
    void uploadArmorLayerWithInvalidLayerValueReturns400() throws Exception {
        registerArmor("void_helmet", "void_armor");

        HttpResponse<String> response = sendMultipart("POST",
                "/api/armor/void_helmet/texture?layer=bogus", png(64, 32));

        assertEquals(400, response.statusCode());
    }

    @Test
    void uploadArmorIconForUnknownIdReturns404() throws Exception {
        HttpResponse<String> response = sendMultipart("POST", "/api/armor/unknown/icon", png(16, 16));
        assertEquals(404, response.statusCode());
    }

    @Test
    void downloadArmorIconReturnsUploadedBytes() throws Exception {
        registerArmor("void_helmet", "void_armor");
        byte[] uploaded = png(16, 16);
        assertEquals(200, sendMultipart("POST", "/api/armor/void_helmet/icon", uploaded).statusCode());

        HttpResponse<byte[]> response = sendGetBinary("/api/armor/void_helmet/icon");

        assertEquals(200, response.statusCode());
        assertTrue(java.util.Arrays.equals(uploaded, response.body()));
    }

    @Test
    void downloadArmorIconForArmorWithoutIconReturns404() throws Exception {
        registerArmor("void_helmet", "void_armor");
        assertEquals(404, sendGetBinary("/api/armor/void_helmet/icon").statusCode());
    }

    @Test
    void downloadArmorLayerReturnsUploadedBytesPerLayer() throws Exception {
        registerArmor("void_helmet", "void_armor");
        byte[] layer1 = png(64, 32);
        byte[] layer2 = png(64, 32);
        assertEquals(200,
                sendMultipart("POST", "/api/armor/void_helmet/texture?layer=humanoid", layer1).statusCode());
        assertEquals(200, sendMultipart("POST", "/api/armor/void_helmet/texture?layer=humanoid_leggings", layer2)
                .statusCode());

        HttpResponse<byte[]> layer1Response = sendGetBinary("/api/armor/void_helmet/texture?layer=humanoid");
        HttpResponse<byte[]> layer2Response = sendGetBinary(
                "/api/armor/void_helmet/texture?layer=humanoid_leggings");

        assertEquals(200, layer1Response.statusCode());
        assertEquals(200, layer2Response.statusCode());
        assertTrue(java.util.Arrays.equals(layer1, layer1Response.body()));
        assertTrue(java.util.Arrays.equals(layer2, layer2Response.body()));
    }

    @Test
    void downloadArmorLayerForMissingLayerReturns404() throws Exception {
        registerArmor("void_helmet", "void_armor");
        assertEquals(404, sendGetBinary("/api/armor/void_helmet/texture?layer=humanoid").statusCode());
    }

    @Test
    void downloadArmorLayerWithoutLayerParamReturns400() throws Exception {
        registerArmor("void_helmet", "void_armor");
        assertEquals(400, sendGetBinary("/api/armor/void_helmet/texture").statusCode());
    }

    @Test
    void listReturnsEmptyBlocksInitially() throws Exception {
        HttpResponse<String> response = send("GET", "/api/blocks", null, true);

        assertEquals(200, response.statusCode());
        Map<String, Object> body = new Yaml().load(response.body());
        assertEquals(List.of(), body.get("blocks"));
    }

    @Test
    void createBlockAutoAssignsCustomModelDataWhenZero() throws Exception {
        String createBody = """
                {"id":"void_netherite_block","instrument":"BASS_GUITAR","note":12,"texture-id":"void_netherite_block",
                 "drop-item-id":"GLOWSTONE","display-name":"Glowing Lantern","lore":[]}
                """;

        HttpResponse<String> created = send("POST", "/api/blocks", createBody, true);
        assertEquals(201, created.statusCode());
        Map<String, Object> body = new Yaml().load(created.body());
        assertEquals(1, body.get("custom-model-data"));
        assertTrue(customBlockRegistry.get("void_netherite_block").isPresent());
    }

    @Test
    void createGetUpdateAndDeleteBlockRoundTrip() throws Exception {
        String createBody = """
                {"id":"void_netherite_block","instrument":"BASS_GUITAR","note":12,"texture-id":"void_netherite_block",
                 "drop-item-id":"GLOWSTONE","display-name":"Glowing Lantern","custom-model-data":3001,"lore":[]}
                """;
        assertEquals(201, send("POST", "/api/blocks", createBody, true).statusCode());

        HttpResponse<String> fetched = send("GET", "/api/blocks/void_netherite_block", null, true);
        assertEquals(200, fetched.statusCode());
        Map<String, Object> fetchedBody = new Yaml().load(fetched.body());
        assertEquals("BASS_GUITAR", fetchedBody.get("instrument"));

        String updateBody = """
                {"instrument":"BASS_GUITAR","note":13,"texture-id":"void_netherite_block",
                 "drop-item-id":"GLOWSTONE","display-name":"Glowing Lantern v2","custom-model-data":3001,"lore":[]}
                """;
        HttpResponse<String> updated = send("PUT", "/api/blocks/void_netherite_block", updateBody, true);
        assertEquals(200, updated.statusCode());
        assertEquals(13, customBlockRegistry.get("void_netherite_block").orElseThrow().note());

        HttpResponse<String> deleted = send("DELETE", "/api/blocks/void_netherite_block", null, true);
        assertEquals(204, deleted.statusCode());
        assertTrue(customBlockRegistry.get("void_netherite_block").isEmpty());

        HttpResponse<String> afterDelete = send("GET", "/api/blocks/void_netherite_block", null, true);
        assertEquals(404, afterDelete.statusCode());
    }

    @Test
    void creatingDuplicateBlockIdReturns409() throws Exception {
        String createBody = """
                {"id":"void_netherite_block","instrument":"BASS_GUITAR","note":12,"texture-id":"void_netherite_block",
                 "drop-item-id":"GLOWSTONE","display-name":"Glowing Lantern","lore":[]}
                """;

        assertEquals(201, send("POST", "/api/blocks", createBody, true).statusCode());
        assertEquals(409, send("POST", "/api/blocks", createBody, true).statusCode());
    }

    @Test
    void creatingCollidingInstrumentAndNoteReturns409WithoutPersistingToConfigSource() throws Exception {
        registerBlock("void_netherite_block", "BASS_GUITAR", 12);

        String createBody = """
                {"id":"other_void_block","instrument":"BASS_GUITAR","note":12,"texture-id":"other_void_block",
                 "drop-item-id":"GLOWSTONE","display-name":"Other Lantern","lore":[]}
                """;

        HttpResponse<String> response = send("POST", "/api/blocks", createBody, true);

        assertEquals(409, response.statusCode());
        assertTrue(customBlockRegistry.get("other_void_block").isEmpty());
        assertTrue(customBlockConfigSource.saved.isEmpty());
    }

    @Test
    void creatingBlockWithUnknownDropItemIdReturns400() throws Exception {
        String createBody = """
                {"id":"void_netherite_block","instrument":"BASS_GUITAR","note":12,"texture-id":"void_netherite_block",
                 "drop-item-id":"NOT_A_REAL_MATERIAL_OR_ITEM","display-name":"Glowing Lantern","lore":[]}
                """;

        HttpResponse<String> response = send("POST", "/api/blocks", createBody, true);
        assertEquals(400, response.statusCode());
        assertTrue(customBlockRegistry.get("void_netherite_block").isEmpty());
    }

    @Test
    void creatingBlockWithUnknownInstrumentReturns400() throws Exception {
        String createBody = """
                {"id":"void_netherite_block","instrument":"NOT_A_REAL_INSTRUMENT","note":12,"texture-id":"void_netherite_block",
                 "drop-item-id":"GLOWSTONE","display-name":"Glowing Lantern","lore":[]}
                """;

        HttpResponse<String> response = send("POST", "/api/blocks", createBody, true);
        assertEquals(400, response.statusCode());
    }

    @Test
    void uploadBlockTextureIsKeyedByTextureIdAndReturnsSha1() throws Exception {
        customBlockRegistry.register(new CustomBlockDefinition("void_netherite_block", "BASS_GUITAR", 12,
                "void_netherite_texture", "GLOWSTONE", "name", 1, List.of()));
        resourcePackPort.sha1 = "def456";

        HttpResponse<String> response = sendMultipart("POST", "/api/blocks/void_netherite_block/texture", png(16, 16));

        assertEquals(200, response.statusCode());
        Map<String, Object> body = new Yaml().load(response.body());
        assertEquals("def456", body.get("sha1"));

        HttpResponse<String> status = send("GET", "/api/blocks/void_netherite_block/texture-status", null, true);
        assertEquals(200, status.statusCode());
        Map<String, Object> statusBody = new Yaml().load(status.body());
        assertTrue((Boolean) statusBody.get("hasTexture"));
    }

    @Test
    void uploadBlockTextureForUnknownIdReturns404() throws Exception {
        HttpResponse<String> response = sendMultipart("POST", "/api/blocks/unknown/texture", png(16, 16));
        assertEquals(404, response.statusCode());
    }

    @Test
    void downloadBlockTextureReturnsUploadedBytes() throws Exception {
        registerBlock("void_netherite_block", "void_netherite_texture");
        byte[] uploaded = png(16, 16);
        assertEquals(200, sendMultipart("POST", "/api/blocks/void_netherite_block/texture", uploaded).statusCode());

        HttpResponse<byte[]> response = sendGetBinary("/api/blocks/void_netherite_block/texture");

        assertEquals(200, response.statusCode());
        assertEquals("image/png", response.headers().firstValue("Content-Type").orElse(null));
        assertTrue(java.util.Arrays.equals(uploaded, response.body()));
    }

    @Test
    void downloadBlockTextureForBlockWithoutTextureReturns404() throws Exception {
        registerBlock("void_netherite_block", "void_netherite_texture");
        assertEquals(404, sendGetBinary("/api/blocks/void_netherite_block/texture").statusCode());
    }

    @Test
    void downloadBlockTextureForUnknownIdReturns404() throws Exception {
        assertEquals(404, sendGetBinary("/api/blocks/unknown/texture").statusCode());
    }

    // Guards the newly added `default` branch of switch(method): this route used to accept
    // POST only, and now accepts POST/GET, so every other method must return 405.
    @Test
    void blockTextureRejectsOtherMethodsWith405() throws Exception {
        registerBlock("void_netherite_block", "void_netherite_texture");
        assertEquals(405, send("DELETE", "/api/blocks/void_netherite_block/texture", null, true).statusCode());
    }

    private void registerBlock(String id, String textureId) {
        customBlockRegistry.register(new CustomBlockDefinition(id, "BASS_GUITAR", 12, textureId, "GLOWSTONE",
                "name", 1, List.of()));
    }

    private void registerItem(String id) {
        itemRegistry.register(new ItemDefinition(id, "NETHERITE_SWORD", 1, "name", List.of(), List.of()));
    }

    private void registerArmor(String id, String armorAssetId) {
        armorRegistry.register(
                new ArmorDefinition(id, "NETHERITE_HELMET", ArmorSlot.HELMET, armorAssetId, 0, "name", List.of()));
    }

    private void registerBlock(String id, String instrument, int note) {
        customBlockRegistry.register(new CustomBlockDefinition(id, instrument, note, id, "GLOWSTONE", "name", 1,
                List.of()));
    }

    private static byte[] png(int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        ImageIO.write(image, "png", buffer);
        return buffer.toByteArray();
    }

    private HttpResponse<String> sendMultipart(String method, String path, byte[] fileBytes) throws Exception {
        String boundary = "----test-boundary-" + UUID.randomUUID();
        String prefix = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"file\"; filename=\"upload.png\"\r\n"
                + "Content-Type: application/octet-stream\r\n\r\n";
        String suffix = "\r\n--" + boundary + "--\r\n";

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(prefix.getBytes(StandardCharsets.ISO_8859_1));
        out.write(fileBytes);
        out.write(suffix.getBytes(StandardCharsets.ISO_8859_1));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + PORT + path))
                .header("Authorization", "Bearer " + API_KEY)
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .method(method, HttpRequest.BodyPublishers.ofByteArray(out.toByteArray()))
                .build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<byte[]> sendGetBinary(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + PORT + path))
                .header("Authorization", "Bearer " + API_KEY)
                .GET()
                .build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofByteArray());
    }

    private HttpResponse<String> send(String method, String path, String body, boolean withAuth) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder().uri(URI.create("http://127.0.0.1:" + PORT + path));
        if (withAuth) {
            builder.header("Authorization", "Bearer " + API_KEY);
        }
        HttpRequest.BodyPublisher publisher = body == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body);
        builder.method(method, publisher);
        if (body != null) {
            builder.header("Content-Type", "application/json");
        }
        return HttpClient.newHttpClient().send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static final class FakeItemConfigSource implements ConfigSourcePort {
        @Override
        public List<ItemDefinition> loadAll() {
            return List.of();
        }

        @Override
        public void save(ItemDefinition definition) {
        }

        @Override
        public void delete(String id) {
        }
    }

    private static final class FakeArmorConfigSource implements ArmorConfigSourcePort {
        @Override
        public List<ArmorDefinition> loadAll() {
            return List.of();
        }

        @Override
        public void save(ArmorDefinition definition) {
        }

        @Override
        public void delete(String id) {
        }
    }

    private static final class FakeCustomBlockConfigSource implements CustomBlockConfigSourcePort {
        private final List<CustomBlockDefinition> saved = new java.util.ArrayList<>();

        @Override
        public List<CustomBlockDefinition> loadAll() {
            return List.of();
        }

        @Override
        public void save(CustomBlockDefinition definition) {
            saved.add(definition);
        }

        @Override
        public void delete(String id) {
        }
    }

    private static final class FakeRecipeConfigSource implements RecipeConfigSourcePort {
        @Override
        public List<RecipeDefinition> loadAll() {
            return List.of();
        }

        @Override
        public void save(RecipeDefinition definition) {
        }

        @Override
        public void delete(String id) {
        }
    }

    private static final class NoopItemModelStrategy implements ItemModelStrategy {
        @Override
        public void applyModel(ItemMeta meta, ItemDefinition definition) {
        }

        @Override
        public void generateResourcePackFiles(List<ItemDefinition> items, Path textureSourceDir, String namespace,
                Path outputDir) throws IOException {
        }
    }

    private static final class NoopArmorModelStrategy implements ArmorModelStrategy {
        @Override
        public void applyModel(ItemMeta meta, ArmorDefinition definition) {
        }

        @Override
        public void generateResourcePackFiles(List<ArmorDefinition> armors, Path textureSourceDir, String namespace,
                Path outputDir) throws IOException {
        }
    }

    private static final class NoopBlockModelStrategy implements BlockModelStrategy {
        @Override
        public void applyModel(ItemMeta meta, CustomBlockDefinition definition) {
        }

        @Override
        public void generateResourcePackFiles(List<CustomBlockDefinition> blocks, Path textureSourceDir,
                String namespace, Path outputDir) throws IOException {
        }
    }

    private static final class FakeResourcePackPort implements ResourcePackPort {
        private String sha1;
        private int rebuildCount = 0;

        @Override
        public void rebuild() {
            rebuildCount++;
        }

        @Override
        public Optional<ResourcePackInfo> currentPack() {
            return sha1 == null ? Optional.empty() : Optional.of(new ResourcePackInfo("http://host", sha1));
        }
    }
}
