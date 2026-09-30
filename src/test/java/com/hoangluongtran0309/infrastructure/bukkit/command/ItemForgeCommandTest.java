package com.hoangluongtran0309.infrastructure.bukkit.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.bukkit.command.Command;
import org.bukkit.command.RemoteConsoleCommandSender;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.command.ConsoleCommandSenderMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.plugin.PluginMock;

import com.hoangluongtran0309.application.AiItemGenerationService;
import com.hoangluongtran0309.application.ArmorConfigLoaderService;
import com.hoangluongtran0309.application.CustomBlockLoaderService;
import com.hoangluongtran0309.application.ItemBalanceAnalysisService;
import com.hoangluongtran0309.application.ItemConfigLoaderService;
import com.hoangluongtran0309.application.RecipeConfigLoaderService;
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
import com.hoangluongtran0309.domain.balance.BalanceReport;
import com.hoangluongtran0309.domain.balance.BalanceRuleSet;
import com.hoangluongtran0309.domain.model.ArmorDefinition;
import com.hoangluongtran0309.domain.model.ArmorSlot;
import com.hoangluongtran0309.domain.model.CustomBlockDefinition;
import com.hoangluongtran0309.domain.model.DamageBonusAbilityDefinition;
import com.hoangluongtran0309.domain.model.EffectCommand;
import com.hoangluongtran0309.domain.model.ItemDefinition;
import com.hoangluongtran0309.domain.model.PotionEffectAbilityDefinition;
import com.hoangluongtran0309.domain.model.TriggerType;
import com.hoangluongtran0309.infrastructure.bukkit.ArmorStackFactory;
import com.hoangluongtran0309.infrastructure.bukkit.ItemStackFactory;
import com.hoangluongtran0309.infrastructure.bukkit.block.CustomBlockStackFactory;
import com.hoangluongtran0309.infrastructure.bukkit.model.ArmorModelStrategy;
import com.hoangluongtran0309.infrastructure.bukkit.model.BlockModelStrategy;
import com.hoangluongtran0309.infrastructure.bukkit.model.ItemModelStrategy;
import com.hoangluongtran0309.infrastructure.bukkit.recipe.RecipeIngredientResolver;
import com.hoangluongtran0309.infrastructure.bukkit.recipe.RecipeRegistrar;

class ItemForgeCommandTest {

    private ServerMock server;
    private PluginMock plugin;
    private PlayerMock player;
    private ItemRegistry itemRegistry;
    private ArmorRegistry armorRegistry;
    private RecipeRegistry recipeRegistry;
    private CustomBlockRegistry customBlockRegistry;
    private ItemForgeCommand command;
    private FakeResourcePackPort resourcePackPort;
    private AiItemGenerationService aiItemGenerationService;
    private ItemBalanceAnalysisService balanceAnalysisService;
    private int settingsReloads;
    private boolean failOnSettingsReload;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.createMockPlugin("ItemForge");
        player = server.addPlayer();

        itemRegistry = new ItemRegistry();
        armorRegistry = new ArmorRegistry();
        recipeRegistry = new RecipeRegistry();
        customBlockRegistry = new CustomBlockRegistry();

        ItemStackFactory itemStackFactory = new ItemStackFactory(plugin, new NoopItemModelStrategy());
        ArmorStackFactory armorStackFactory = new ArmorStackFactory(plugin, new NoopArmorModelStrategy());
        CustomBlockStackFactory customBlockStackFactory = new CustomBlockStackFactory(plugin,
                new NoopBlockModelStrategy());

        ItemConfigLoaderService loaderService = new ItemConfigLoaderService(new FakeItemConfigSource(),
                itemRegistry);
        ArmorConfigLoaderService armorLoaderService = new ArmorConfigLoaderService(new FakeArmorConfigSource(),
                armorRegistry);
        RecipeConfigLoaderService recipeLoaderService = new RecipeConfigLoaderService(new FakeRecipeConfigSource(),
                recipeRegistry);
        CustomBlockLoaderService customBlockLoaderService = new CustomBlockLoaderService(
                new FakeCustomBlockConfigSource(), customBlockRegistry, itemRegistry, armorRegistry);

        RecipeIngredientResolver resolver = new RecipeIngredientResolver(itemRegistry, armorRegistry,
                itemStackFactory, armorStackFactory);
        RecipeRegistrar recipeRegistrar = new RecipeRegistrar(plugin, resolver, Logger.getAnonymousLogger());

        resourcePackPort = new FakeResourcePackPort();

        // Null AI ports throughout: /itemforge generate is disabled, and /itemforge analyze runs
        // on rules alone, which is the path an admin without an API key actually gets.
        aiItemGenerationService = new AiItemGenerationService(null, itemRegistry,
                loaderService);
        balanceAnalysisService = new ItemBalanceAnalysisService(itemRegistry,
                armorRegistry, recipeRegistry, new BalanceRuleSet(), null);

        command = new ItemForgeCommand(itemRegistry, itemStackFactory, loaderService, resourcePackPort,
                armorRegistry, armorStackFactory, armorLoaderService, recipeRegistry, recipeLoaderService,
                recipeRegistrar, aiItemGenerationService, balanceAnalysisService, customBlockRegistry,
                customBlockStackFactory, customBlockLoaderService, this::reloadSettings, plugin);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void reloadReportsCleanErrorInsteadOfThrowing() {
        resourcePackPort.failOnRebuild = true;

        assertTrue(command.onCommand(player, mockCommand(), "itemforge", new String[] { "reload" }));

        String message = player.nextMessage();
        assertNotNull(message);
        assertTrue(message.startsWith("Reload failed:"));
    }

    @Test
    void reloadSucceedsAndReportsCounts() {
        assertTrue(command.onCommand(player, mockCommand(), "itemforge", new String[] { "reload" }));

        String message = player.nextMessage();
        assertNotNull(message);
        assertTrue(message.startsWith("Reloaded "));
    }

    @Test
    void reloadRereadsTheSettingsAlongWithTheContent() {
        assertTrue(command.onCommand(player, mockCommand(), "itemforge", new String[] { "reload" }));

        assertEquals(1, settingsReloads);
    }

    @Test
    void reloadReportsCleanErrorWhenTheSettingsCannotBeReread() {
        failOnSettingsReload = true;

        assertTrue(command.onCommand(player, mockCommand(), "itemforge", new String[] { "reload" }));

        String message = player.nextMessage();
        assertNotNull(message);
        assertTrue(message.startsWith("Reload failed:"));
    }

    @Test
    void listIncludesAbilitiesForEachItem() {
        itemRegistry.register(new ItemDefinition("void_pickaxe", "NETHERITE_PICKAXE", 1, "Power Axe", List.of(),
                List.of(new DamageBonusAbilityDefinition(TriggerType.ON_HIT, 10, 15.0))));

        assertTrue(command.onCommand(player, mockCommand(), "itemforge", new String[] { "list" }));

        assertEquals("- void_pickaxe", player.nextMessage());
        String abilityLine = player.nextMessage();
        assertNotNull(abilityLine);
        assertTrue(abilityLine.contains("ON_HIT"));
        assertTrue(abilityLine.contains("cooldown 10s"));
    }

    @Test
    void giveAddsItemToPlayerInventory() {
        itemRegistry.register(new ItemDefinition("void_sword", "NETHERITE_SWORD", 1, "Fire Sword", List.of(),
                List.of()));

        assertTrue(command.onCommand(player, mockCommand(), "itemforge", new String[] { "give", "void_sword" }));

        ItemStack found = player.getInventory().getContents()[0];
        assertNotNull(found);
    }

    @Test
    void generateReportsDisabledWhenNoProviderIsConfigured() {
        assertTrue(command.onCommand(player, mockCommand(), "itemforge",
                new String[] { "generate", "test_item", "a", "shiny", "sword" }));

        String message = player.nextMessage();
        assertNotNull(message);
        assertTrue(message.contains("disabled"));
    }

    @Test
    void analyzeReportsRuleFindingsWithoutAnAiProvider() {
        itemRegistry.register(permanentSpeedSword());

        assertTrue(command.onCommand(player, mockCommand(), "itemforge", new String[] { "analyze" }));

        String summary = player.nextMessage();
        assertNotNull(summary);
        assertTrue(summary.startsWith("Analyzed "));

        String finding = player.nextMessage();
        assertNotNull(finding);
        assertTrue(finding.startsWith("[CRITICAL] void_sword:"));
        assertTrue(finding.contains("SPEED"));

        String suggestion = player.nextMessage();
        assertNotNull(suggestion);
        assertTrue(suggestion.startsWith("    -> "));
    }

    @Test
    void analyzeAcceptsASingleIdAndReportsOnlyThatItem() {
        itemRegistry.register(permanentSpeedSword());
        itemRegistry.register(new ItemDefinition("void_axe", "NETHERITE_AXE", 2, "Axe", List.of(), List.of()));

        assertTrue(command.onCommand(player, mockCommand(), "itemforge",
                new String[] { "analyze", "void_sword" }));

        String summary = player.nextMessage();
        assertNotNull(summary);
        assertTrue(summary.contains("'void_sword'"));
    }

    @Test
    void analyzeReportsACleanErrorForAnUnknownId() {
        assertTrue(command.onCommand(player, mockCommand(), "itemforge", new String[] { "analyze", "nope" }));

        String message = player.nextMessage();
        assertNotNull(message);
        assertTrue(message.startsWith("Analysis failed: "));
        assertTrue(message.contains("nope"));
    }

    @Test
    void analyzeSaysSoWhenThereIsNothingToReport() {
        assertTrue(command.onCommand(player, mockCommand(), "itemforge", new String[] { "analyze" }));

        player.nextMessage();
        assertEquals("Nothing to report.", player.nextMessage());
    }

    @Test
    void analyzeOverRconPrintsTheAiBackedReportToTheServerConsole() {
        itemRegistry.register(permanentSpeedSword());
        balanceAnalysisService.useAiPort((request, ruleFindings) -> BalanceReport.of("", List.of()));
        List<String> rconReplies = new ArrayList<>();

        assertTrue(command.onCommand(rconSender(rconReplies), mockCommand(), "itemforge",
                new String[] { "analyze" }));
        runScheduledTasks();

        assertTrue(rconReplies.stream().anyMatch(reply -> reply.contains("server console")));
        assertTrue(drain(console()).stream().anyMatch(line -> line.startsWith("[CRITICAL] void_sword")));
    }

    @Test
    void analyzeFromAPlayerStillRepliesToThatPlayerWhenAiIsEnabled() {
        itemRegistry.register(permanentSpeedSword());
        balanceAnalysisService.useAiPort((request, ruleFindings) -> BalanceReport.of("", List.of()));

        assertTrue(command.onCommand(player, mockCommand(), "itemforge", new String[] { "analyze" }));
        runScheduledTasks();

        assertTrue(drain(player).stream().anyMatch(line -> line.startsWith("[CRITICAL] void_sword")));
        assertTrue(drain(console()).isEmpty());
    }

    @Test
    void generateOverRconPrintsTheOutcomeToTheServerConsole() {
        aiItemGenerationService.useProvider((itemId, description) -> new ItemDefinition(itemId, "STICK", 0,
                "Generated " + itemId, List.of(), List.of()));
        List<String> rconReplies = new ArrayList<>();

        assertTrue(command.onCommand(rconSender(rconReplies), mockCommand(), "itemforge",
                new String[] { "generate", "test_item", "a", "plain", "stick" }));
        runScheduledTasks();

        assertTrue(rconReplies.stream().anyMatch(reply -> reply.contains("server console")));
        assertTrue(drain(console()).stream().anyMatch(line -> line.startsWith("Created item 'test_item'")));
    }

    @Test
    void analyzeTabCompletesItemAndArmorIds() {
        itemRegistry.register(permanentSpeedSword());
        armorRegistry.register(new ArmorDefinition("void_helmet", "NETHERITE_HELMET", ArmorSlot.HELMET,
                "void_armor", 1, "Helmet", List.of()));

        List<String> completions = command.onTabComplete(player, mockCommand(), "itemforge",
                new String[] { "analyze", "" });

        assertNotNull(completions);
        assertTrue(completions.contains("void_sword"));
        assertTrue(completions.contains("void_helmet"));
    }

    @Test
    void analyzeIsOfferedAsASubcommand() {
        List<String> completions = command.onTabComplete(player, mockCommand(), "itemforge", new String[] { "" });

        assertNotNull(completions);
        assertTrue(completions.contains("analyze"));
    }

    private static ItemDefinition permanentSpeedSword() {
        return new ItemDefinition("void_sword", "NETHERITE_SWORD", 1, "Void Sword", List.of(),
                List.of(new PotionEffectAbilityDefinition(TriggerType.RIGHT_CLICK,
                        EffectCommand.EffectType.SPEED, 60, 30)));
    }

    // The async half of a command, then the sync task it schedules to report back.
    private void runScheduledTasks() {
        server.getScheduler().waitAsyncTasksFinished();
        server.getScheduler().performOneTick();
    }

    private ConsoleCommandSenderMock console() {
        return (ConsoleCommandSenderMock) server.getConsoleSender();
    }

    private static List<String> drain(org.mockbukkit.mockbukkit.command.MessageTarget target) {
        List<String> messages = new ArrayList<>();
        for (String message = target.nextMessage(); message != null; message = target.nextMessage()) {
            messages.add(message);
        }
        return messages;
    }

    // MockBukkit has no RCON sender, and only sendMessage(String) matters here.
    private static RemoteConsoleCommandSender rconSender(List<String> received) {
        return (RemoteConsoleCommandSender) Proxy.newProxyInstance(
                RemoteConsoleCommandSender.class.getClassLoader(),
                new Class<?>[] { RemoteConsoleCommandSender.class },
                (proxy, method, args) -> {
                    if (method.getName().equals("sendMessage") && args.length == 1 && args[0] instanceof String text) {
                        received.add(text);
                    }
                    Class<?> returnType = method.getReturnType();
                    if (returnType == boolean.class) {
                        return false;
                    }
                    if (returnType == String.class) {
                        return "Rcon";
                    }
                    return null;
                });
    }

    private void reloadSettings() {
        if (failOnSettingsReload) {
            throw new IllegalStateException("config.yml is not valid YAML");
        }
        settingsReloads++;
    }

    private Command mockCommand() {
        return plugin.getCommand("itemforge");
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
        @Override
        public List<CustomBlockDefinition> loadAll() {
            return List.of();
        }

        @Override
        public void save(CustomBlockDefinition definition) {
        }

        @Override
        public void delete(String id) {
        }
    }

    private static final class FakeRecipeConfigSource implements RecipeConfigSourcePort {
        @Override
        public List<com.hoangluongtran0309.domain.model.RecipeDefinition> loadAll() {
            return List.of();
        }

        @Override
        public void save(com.hoangluongtran0309.domain.model.RecipeDefinition definition) {
        }

        @Override
        public void delete(String id) {
        }
    }

    private static final class FakeResourcePackPort implements ResourcePackPort {
        private boolean failOnRebuild = false;

        @Override
        public void rebuild() {
            if (failOnRebuild) {
                throw new RuntimeException("staging directory is not writable");
            }
        }

        @Override
        public java.util.Optional<ResourcePackInfo> currentPack() {
            return java.util.Optional.empty();
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
}
