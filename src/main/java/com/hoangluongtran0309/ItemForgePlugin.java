package com.hoangluongtran0309;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import com.hoangluongtran0309.application.AbilityDispatchService;
import com.hoangluongtran0309.application.AiItemGenerationService;
import com.hoangluongtran0309.application.ArmorConfigLoaderService;
import com.hoangluongtran0309.application.CooldownService;
import com.hoangluongtran0309.application.CustomBlockLoaderService;
import com.hoangluongtran0309.application.ItemBalanceAnalysisService;
import com.hoangluongtran0309.application.ItemConfigLoaderService;
import com.hoangluongtran0309.application.RecipeConfigLoaderService;
import com.hoangluongtran0309.application.ServerVersion;
import com.hoangluongtran0309.application.TextureUploadService;
import com.hoangluongtran0309.application.port.ArmorConfigSourcePort;
import com.hoangluongtran0309.application.port.ConfigSourcePort;
import com.hoangluongtran0309.application.port.CustomBlockConfigSourcePort;
import com.hoangluongtran0309.application.port.EffectApplierPort;
import com.hoangluongtran0309.application.port.RecipeConfigSourcePort;
import com.hoangluongtran0309.domain.ArmorRegistry;
import com.hoangluongtran0309.domain.CustomBlockRegistry;
import com.hoangluongtran0309.domain.ItemRegistry;
import com.hoangluongtran0309.domain.RecipeRegistry;
import com.hoangluongtran0309.domain.balance.BalanceRuleSet;
import com.hoangluongtran0309.infrastructure.ai.AiItemGenerator;
import com.hoangluongtran0309.infrastructure.ai.AiProviderFactory;
import com.hoangluongtran0309.infrastructure.ai.ItemBalanceAnalyzer;
import com.hoangluongtran0309.infrastructure.ai.StructuredAiClient;
import com.hoangluongtran0309.infrastructure.bukkit.ArmorStackFactory;
import com.hoangluongtran0309.infrastructure.bukkit.EffectApplierAdapter;
import com.hoangluongtran0309.infrastructure.bukkit.ItemStackFactory;
import com.hoangluongtran0309.infrastructure.bukkit.block.CustomBlockBreakListener;
import com.hoangluongtran0309.infrastructure.bukkit.block.CustomBlockExplosionListener;
import com.hoangluongtran0309.infrastructure.bukkit.block.CustomBlockInstrumentGuardListener;
import com.hoangluongtran0309.infrastructure.bukkit.block.CustomBlockPistonListener;
import com.hoangluongtran0309.infrastructure.bukkit.block.CustomBlockPlaceListener;
import com.hoangluongtran0309.infrastructure.bukkit.block.CustomBlockStackFactory;
import com.hoangluongtran0309.infrastructure.bukkit.block.CustomBlockTagService;
import com.hoangluongtran0309.infrastructure.bukkit.block.NoteBlockStateFactory;
import com.hoangluongtran0309.infrastructure.bukkit.command.ItemForgeCommand;
import com.hoangluongtran0309.infrastructure.bukkit.http.ArmorApiHandler;
import com.hoangluongtran0309.infrastructure.bukkit.http.BalanceApiHandler;
import com.hoangluongtran0309.infrastructure.bukkit.http.BlocksApiHandler;
import com.hoangluongtran0309.infrastructure.bukkit.http.DashboardApiServer;
import com.hoangluongtran0309.infrastructure.bukkit.http.ItemsApiHandler;
import com.hoangluongtran0309.infrastructure.bukkit.http.RecipesApiHandler;
import com.hoangluongtran0309.infrastructure.bukkit.hud.ActionBarNotifier;
import com.hoangluongtran0309.infrastructure.bukkit.hud.CooldownBossBarService;
import com.hoangluongtran0309.infrastructure.bukkit.listener.ItemInteractListener;
import com.hoangluongtran0309.infrastructure.bukkit.listener.PlayerJoinPackListener;
import com.hoangluongtran0309.infrastructure.bukkit.model.ArmorModelStrategy;
import com.hoangluongtran0309.infrastructure.bukkit.model.ArmorModelStrategyFactory;
import com.hoangluongtran0309.infrastructure.bukkit.model.BlockModelStrategy;
import com.hoangluongtran0309.infrastructure.bukkit.model.BlockModelStrategyFactory;
import com.hoangluongtran0309.infrastructure.bukkit.model.ItemModelStrategy;
import com.hoangluongtran0309.infrastructure.bukkit.model.ItemModelStrategyFactory;
import com.hoangluongtran0309.infrastructure.bukkit.model.ServerVersionDetector;
import com.hoangluongtran0309.infrastructure.bukkit.recipe.RecipeIngredientResolver;
import com.hoangluongtran0309.infrastructure.bukkit.recipe.RecipeRegistrar;
import com.hoangluongtran0309.infrastructure.config.YamlArmorConfigAdapter;
import com.hoangluongtran0309.infrastructure.config.YamlConfigAdapter;
import com.hoangluongtran0309.infrastructure.config.YamlCustomBlockConfigAdapter;
import com.hoangluongtran0309.infrastructure.config.YamlRecipeConfigAdapter;
import com.hoangluongtran0309.infrastructure.resourcepack.ArmorTextureFileCopier;
import com.hoangluongtran0309.infrastructure.resourcepack.BlockModelJsonGenerator;
import com.hoangluongtran0309.infrastructure.resourcepack.BlockStateJsonGenerator;
import com.hoangluongtran0309.infrastructure.resourcepack.BlockTextureFileCopier;
import com.hoangluongtran0309.infrastructure.resourcepack.EquipmentAssetGenerator;
import com.hoangluongtran0309.infrastructure.resourcepack.ModelJsonGenerator;
import com.hoangluongtran0309.infrastructure.resourcepack.PackHttpServer;
import com.hoangluongtran0309.infrastructure.resourcepack.ResourcePackBuilder;
import com.hoangluongtran0309.infrastructure.resourcepack.TextureFileCopier;
import com.hoangluongtran0309.infrastructure.texture.FileSystemTextureStoragePort;

public class ItemForgePlugin extends JavaPlugin {

    private PackHttpServer packHttpServer;
    private DashboardApiServer dashboardApiServer;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        ItemRegistry itemRegistry = new ItemRegistry();
        ArmorRegistry armorRegistry = new ArmorRegistry();
        CustomBlockRegistry customBlockRegistry = new CustomBlockRegistry();

        ModelJsonGenerator modelJsonGenerator = new ModelJsonGenerator();
        EquipmentAssetGenerator equipmentAssetGenerator = new EquipmentAssetGenerator();
        ArmorTextureFileCopier armorTextureFileCopier = new ArmorTextureFileCopier(getLogger());
        TextureFileCopier textureFileCopier = new TextureFileCopier(getLogger());
        BlockModelJsonGenerator blockModelJsonGenerator = new BlockModelJsonGenerator();
        BlockTextureFileCopier blockTextureFileCopier = new BlockTextureFileCopier(getLogger());
        BlockStateJsonGenerator blockStateJsonGenerator = new BlockStateJsonGenerator();
        ServerVersion serverVersion = new ServerVersionDetector().detect();
        ItemModelStrategy modelStrategy = new ItemModelStrategyFactory().create(serverVersion, modelJsonGenerator,
                textureFileCopier);
        ArmorModelStrategy armorModelStrategy = new ArmorModelStrategyFactory().create(serverVersion,
                equipmentAssetGenerator, armorTextureFileCopier, modelJsonGenerator, getLogger());
        BlockModelStrategy blockModelStrategy = new BlockModelStrategyFactory().create(serverVersion,
                blockModelJsonGenerator, blockTextureFileCopier, blockStateJsonGenerator);
        getLogger().info("Detected server version " + serverVersion + " -> using "
                + modelStrategy.getClass().getSimpleName() + " / " + armorModelStrategy.getClass().getSimpleName()
                + " / " + blockModelStrategy.getClass().getSimpleName());
        ItemStackFactory itemStackFactory = new ItemStackFactory(this, modelStrategy);
        ArmorStackFactory armorStackFactory = new ArmorStackFactory(this, armorModelStrategy);
        CustomBlockStackFactory customBlockStackFactory = new CustomBlockStackFactory(this, blockModelStrategy);
        NoteBlockStateFactory noteBlockStateFactory = new NoteBlockStateFactory();
        CustomBlockTagService customBlockTagService = new CustomBlockTagService(
                getDataFolder().toPath().resolve("custom-block-tags.yml"), getLogger());
        customBlockTagService.load();

        CooldownService cooldownService = new CooldownService();
        EffectApplierPort effectApplier = new EffectApplierAdapter(getLogger());
        AbilityDispatchService abilityDispatchService = new AbilityDispatchService(itemRegistry, cooldownService,
                effectApplier);

        ActionBarNotifier actionBarNotifier = new ActionBarNotifier();
        long bossBarIntervalTicks = getConfig().getLong("hud.bossbar-update-interval-ticks", 5);
        CooldownBossBarService cooldownBossBarService = new CooldownBossBarService(this, bossBarIntervalTicks);

        ItemConfigLoaderService loaderService = createLoaderService(itemRegistry);
        loaderService.loadAll();
        ArmorConfigLoaderService armorLoaderService = createArmorLoaderService(armorRegistry);
        armorLoaderService.loadAll();
        CustomBlockLoaderService customBlockLoaderService = createCustomBlockLoaderService(customBlockRegistry,
                itemRegistry, armorRegistry);
        customBlockLoaderService.reloadAll(getLogger()::warning);

        RecipeRegistry recipeRegistry = new RecipeRegistry();
        RecipeConfigLoaderService recipeLoaderService = createRecipeLoaderService(recipeRegistry);
        recipeLoaderService.loadAll();

        RecipeIngredientResolver recipeIngredientResolver = new RecipeIngredientResolver(itemRegistry, armorRegistry,
                itemStackFactory, armorStackFactory);
        RecipeRegistrar recipeRegistrar = new RecipeRegistrar(this, recipeIngredientResolver, getLogger());
        recipeRegistrar.registerAll(recipeRegistry.getAll());

        ResourcePackBuilder resourcePackBuilder = createResourcePackBuilder(itemRegistry, modelStrategy,
                armorRegistry, armorModelStrategy, customBlockRegistry, blockModelStrategy, serverVersion);
        resourcePackBuilder.rebuild();

        FileSystemTextureStoragePort textureStoragePort = new FileSystemTextureStoragePort(getDataFolder().toPath(),
                getLogger());
        TextureUploadService textureUploadService = new TextureUploadService(textureStoragePort, resourcePackBuilder);

        PluginManager pluginManager = getServer().getPluginManager();
        pluginManager.registerEvents(new ItemInteractListener(abilityDispatchService, itemStackFactory,
                actionBarNotifier, cooldownBossBarService), this);
        pluginManager.registerEvents(new PlayerJoinPackListener(resourcePackBuilder, getLogger()), this);

        pluginManager.registerEvents(new CustomBlockPlaceListener(customBlockRegistry, customBlockStackFactory,
                noteBlockStateFactory, customBlockTagService, getLogger()), this);
        pluginManager.registerEvents(new CustomBlockBreakListener(customBlockRegistry, customBlockTagService,
                itemRegistry, itemStackFactory, getLogger()), this);
        pluginManager.registerEvents(new CustomBlockInstrumentGuardListener(customBlockRegistry,
                customBlockTagService, noteBlockStateFactory), this);
        pluginManager.registerEvents(new CustomBlockPistonListener(customBlockTagService), this);
        pluginManager.registerEvents(new CustomBlockExplosionListener(customBlockTagService), this);

        AiItemGenerationService aiItemGenerationService = new AiItemGenerationService(null, itemRegistry,
                loaderService);
        ItemBalanceAnalysisService balanceAnalysisService = new ItemBalanceAnalysisService(itemRegistry,
                armorRegistry, recipeRegistry, new BalanceRuleSet(), null);
        applyAiSettings(aiItemGenerationService, balanceAnalysisService);

        startDashboardApiServerIfEnabled(itemRegistry, loaderService, itemStackFactory, aiItemGenerationService,
                armorRegistry, armorLoaderService, armorStackFactory, recipeRegistry, recipeLoaderService,
                textureUploadService, customBlockRegistry, customBlockLoaderService, customBlockStackFactory,
                noteBlockStateFactory, balanceAnalysisService);

        ItemForgeCommand command = new ItemForgeCommand(itemRegistry, itemStackFactory, loaderService,
                resourcePackBuilder, armorRegistry, armorStackFactory, armorLoaderService,
                recipeRegistry, recipeLoaderService, recipeRegistrar, aiItemGenerationService,
                balanceAnalysisService, customBlockRegistry, customBlockStackFactory, customBlockLoaderService,
                () -> {
                    reloadConfig();
                    applyAiSettings(aiItemGenerationService, balanceAnalysisService);
                }, this);
        getCommand("itemforge").setExecutor(command);
        getCommand("itemforge").setTabCompleter(command);
    }

    @Override
    public void onDisable() {
        if (packHttpServer != null) {
            packHttpServer.stop();
        }
        if (dashboardApiServer != null) {
            dashboardApiServer.stop();
        }
    }

    private ItemConfigLoaderService createLoaderService(ItemRegistry itemRegistry) {
        File dataFolder = getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        File itemsFile = new File(dataFolder, "items.yml");
        if (!itemsFile.exists()) {
            saveResource("items.yml", false);
        }

        ConfigSourcePort configSource = new YamlConfigAdapter(itemsFile.toPath(), getLogger());
        return new ItemConfigLoaderService(configSource, itemRegistry);
    }

    private ArmorConfigLoaderService createArmorLoaderService(ArmorRegistry armorRegistry) {
        File dataFolder = getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        File armorFile = new File(dataFolder, "armor.yml");
        if (!armorFile.exists()) {
            saveResource("armor.yml", false);
        }

        ArmorConfigSourcePort configSource = new YamlArmorConfigAdapter(armorFile.toPath(), getLogger());
        return new ArmorConfigLoaderService(configSource, armorRegistry);
    }

    private CustomBlockLoaderService createCustomBlockLoaderService(CustomBlockRegistry customBlockRegistry,
            ItemRegistry itemRegistry, ArmorRegistry armorRegistry) {
        File dataFolder = getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        File blocksFile = new File(dataFolder, "blocks.yml");
        if (!blocksFile.exists()) {
            saveResource("blocks.yml", false);
        }

        CustomBlockConfigSourcePort configSource = new YamlCustomBlockConfigAdapter(blocksFile.toPath(), getLogger());
        return new CustomBlockLoaderService(configSource, customBlockRegistry, itemRegistry, armorRegistry);
    }

    private RecipeConfigLoaderService createRecipeLoaderService(RecipeRegistry recipeRegistry) {
        File dataFolder = getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        File recipesFile = new File(dataFolder, "recipes.yml");
        if (!recipesFile.exists()) {
            saveResource("recipes.yml", false);
        }

        RecipeConfigSourcePort configSource = new YamlRecipeConfigAdapter(recipesFile.toPath(), getLogger());
        return new RecipeConfigLoaderService(configSource, recipeRegistry);
    }

    // Reads the ai section and points both AI features at it. Called on enable and again on
    // /itemforge reload, which is what lets an admin change the provider, key or model (or turn
    // AI on and off) without a restart. With ai.enabled = false both services stay in place
    // with no provider: generation reports itself disabled and the balance analysis runs on
    // its rules alone, which need no provider and cost nothing.
    private void applyAiSettings(AiItemGenerationService aiItemGenerationService,
            ItemBalanceAnalysisService balanceAnalysisService) {
        if (!getConfig().getBoolean("ai.enabled", false)) {
            aiItemGenerationService.useProvider(null);
            balanceAnalysisService.useAiPort(null);
            return;
        }

        StructuredAiClient client = createStructuredAiClient();
        aiItemGenerationService.useProvider(new AiItemGenerator(client));
        balanceAnalysisService.useAiPort(new ItemBalanceAnalyzer(client, getLogger()));
    }

    // Both AI features share one provider, one key and one set of limits, so they share this
    // client too. Only call it once ai.enabled has been checked.
    private StructuredAiClient createStructuredAiClient() {
        String provider = getConfig().getString("ai.provider", "claude");
        String prefix = "ai." + provider.toLowerCase(Locale.ROOT) + ".";
        return new AiProviderFactory().create(provider,
                getConfig().getString(prefix + "api-key", ""),
                getConfig().getString(prefix + "model", defaultModelFor(provider)),
                getConfig().getInt(prefix + "max-tokens", 2048),
                getConfig().getInt(prefix + "timeout-seconds", 30),
                getLogger());
    }

    // Fallbacks in case the "model" key gets deleted from config.yml by accident. These
    // values go stale over time -- prefer the real values in config.yml over these.
    private static String defaultModelFor(String provider) {
        return switch (provider.toLowerCase(Locale.ROOT)) {
            case "claude" -> "claude-haiku-4-5";
            case "chatgpt" -> "gpt-4o-mini";
            case "deepseek" -> "deepseek-chat";
            case "gemini" -> "gemini-2.0-flash";
            default -> "";
        };
    }

    private void startDashboardApiServerIfEnabled(ItemRegistry itemRegistry, ItemConfigLoaderService loaderService,
            ItemStackFactory itemStackFactory, AiItemGenerationService aiItemGenerationService,
            ArmorRegistry armorRegistry, ArmorConfigLoaderService armorLoaderService,
            ArmorStackFactory armorStackFactory, RecipeRegistry recipeRegistry,
            RecipeConfigLoaderService recipeLoaderService, TextureUploadService textureUploadService,
            CustomBlockRegistry customBlockRegistry, CustomBlockLoaderService customBlockLoaderService,
            CustomBlockStackFactory customBlockStackFactory, NoteBlockStateFactory noteBlockStateFactory,
            ItemBalanceAnalysisService balanceAnalysisService) {
        if (!getConfig().getBoolean("dashboard-api.enabled", false)) {
            return;
        }

        int port = getConfig().getInt("dashboard-api.port", 8081);
        String apiKey = getConfig().getString("dashboard-api.api-key", "CHANGE_ME");
        long maxUploadBytes = getConfig().getLong("dashboard-api.max-upload-bytes", 2_097_152L);

        // The same fail-safe convention as resource-pack.host: refuse to start the HTTP
        // server while the api-key is still the placeholder. Otherwise an admin who enables
        // dashboard-api.enabled but forgets to change the key turns "CHANGE_ME" into a valid
        // bearer token for an API that can create, edit and delete real items.
        if (apiKey == null || apiKey.isBlank() || apiKey.equalsIgnoreCase("CHANGE_ME")) {
            getLogger().warning("dashboard-api.api-key is not configured in config.yml (still 'CHANGE_ME')."
                    + " Skipping: the dashboard API server will not start.");
            return;
        }

        ItemsApiHandler itemsHandler = new ItemsApiHandler(itemRegistry, loaderService, itemStackFactory,
                aiItemGenerationService, textureUploadService, maxUploadBytes);
        ArmorApiHandler armorHandler = new ArmorApiHandler(armorRegistry, armorLoaderService, armorStackFactory,
                textureUploadService, maxUploadBytes);
        BlocksApiHandler blocksHandler = new BlocksApiHandler(customBlockRegistry, customBlockLoaderService,
                customBlockStackFactory, noteBlockStateFactory, itemRegistry, textureUploadService, maxUploadBytes);
        RecipesApiHandler recipesHandler = new RecipesApiHandler(recipeRegistry, recipeLoaderService);
        BalanceApiHandler balanceHandler = new BalanceApiHandler(balanceAnalysisService);

        dashboardApiServer = new DashboardApiServer(port, apiKey, itemsHandler, armorHandler, blocksHandler,
                recipesHandler, balanceHandler, getLogger());
        try {
            dashboardApiServer.start();
        } catch (IOException e) {
            getLogger().severe("Could not start the dashboard API server on port " + port + ": "
                    + e.getMessage());
            dashboardApiServer = null;
        }
    }

    private ResourcePackBuilder createResourcePackBuilder(ItemRegistry itemRegistry, ItemModelStrategy modelStrategy,
            ArmorRegistry armorRegistry, ArmorModelStrategy armorModelStrategy,
            CustomBlockRegistry customBlockRegistry, BlockModelStrategy blockModelStrategy,
            ServerVersion serverVersion) {
        String host = normalizeHost(getConfig().getString("resource-pack.host", "CHANGE_ME"));
        int port = getConfig().getInt("resource-pack.port", 8080);
        boolean hostConfigured = host != null && !host.isBlank() && !host.equalsIgnoreCase("CHANGE_ME");

        String publicBaseUrl = hostConfigured
                ? "http://" + host + ":" + port + PackHttpServer.CONTEXT_PATH
                : null;

        ResourcePackBuilder resourcePackBuilder = new ResourcePackBuilder(itemRegistry, modelStrategy,
                armorRegistry, armorModelStrategy, customBlockRegistry, blockModelStrategy,
                getDataFolder().toPath(), publicBaseUrl, serverVersion, getLogger());

        if (hostConfigured) {
            packHttpServer = new PackHttpServer(port, resourcePackBuilder::zipFilePath, getLogger());
            try {
                packHttpServer.start();
            } catch (IOException e) {
                getLogger().severe("Could not start the resource pack HTTP server on port " + port + ": "
                        + e.getMessage());
                packHttpServer = null;
            }
        } else {
            getLogger().warning("resource-pack.host is not configured in config.yml (still 'CHANGE_ME')."
                    + " Skipping: no HTTP server will start and no resource pack will be sent to players.");
        }

        return resourcePackBuilder;
    }

    // Handles a common mistake: putting the whole "http://host:port" into
    // resource-pack.host instead of just the bare host, which would otherwise concatenate
    // into a broken URL like "http://http://host:port:port/...".
    private String normalizeHost(String rawHost) {
        if (rawHost == null) {
            return rawHost;
        }
        String host = rawHost.trim().replaceFirst("^[a-zA-Z][a-zA-Z0-9+.-]*://", "");
        int colonIndex = host.indexOf(':');
        if (colonIndex >= 0) {
            host = host.substring(0, colonIndex);
        }
        int slashIndex = host.indexOf('/');
        if (slashIndex >= 0) {
            host = host.substring(0, slashIndex);
        }
        return host;
    }
}
