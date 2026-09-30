package com.hoangluongtran0309.infrastructure.bukkit.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.RemoteConsoleCommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hoangluongtran0309.application.AiItemGenerationService;
import com.hoangluongtran0309.application.ArmorConfigLoaderService;
import com.hoangluongtran0309.application.CustomBlockLoaderService;
import com.hoangluongtran0309.application.ItemBalanceAnalysisService;
import com.hoangluongtran0309.application.ItemConfigLoaderService;
import com.hoangluongtran0309.application.RecipeConfigLoaderService;
import com.hoangluongtran0309.application.port.ResourcePackPort;
import com.hoangluongtran0309.domain.ArmorRegistry;
import com.hoangluongtran0309.domain.CustomBlockRegistry;
import com.hoangluongtran0309.domain.ItemRegistry;
import com.hoangluongtran0309.domain.RecipeRegistry;
import com.hoangluongtran0309.domain.balance.BalanceReport;
import com.hoangluongtran0309.domain.model.ItemDefinition;
import com.hoangluongtran0309.infrastructure.bukkit.ArmorStackFactory;
import com.hoangluongtran0309.infrastructure.bukkit.ItemStackFactory;
import com.hoangluongtran0309.infrastructure.bukkit.block.CustomBlockStackFactory;
import com.hoangluongtran0309.infrastructure.bukkit.recipe.RecipeRegistrar;

public class ItemForgeCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of("give", "reload", "list", "generate", "analyze");

    // A whole-config report can run long; the rest stays available through "analyze <id>".
    private static final int MAX_FINDINGS_SHOWN = 20;

    private final ItemRegistry registry;
    private final ItemStackFactory itemStackFactory;
    private final ItemConfigLoaderService loaderService;
    private final ResourcePackPort resourcePackPort;
    private final ArmorRegistry armorRegistry;
    private final ArmorStackFactory armorStackFactory;
    private final ArmorConfigLoaderService armorLoaderService;
    private final RecipeRegistry recipeRegistry;
    private final RecipeConfigLoaderService recipeLoaderService;
    private final RecipeRegistrar recipeRegistrar;
    private final AiItemGenerationService aiItemGenerationService;
    private final ItemBalanceAnalysisService balanceAnalysisService;
    private final CustomBlockRegistry customBlockRegistry;
    private final CustomBlockStackFactory customBlockStackFactory;
    private final CustomBlockLoaderService customBlockLoaderService;
    private final Runnable settingsReloader;
    private final Plugin plugin;

    public ItemForgeCommand(ItemRegistry registry, ItemStackFactory itemStackFactory,
            ItemConfigLoaderService loaderService, ResourcePackPort resourcePackPort,
            ArmorRegistry armorRegistry, ArmorStackFactory armorStackFactory,
            ArmorConfigLoaderService armorLoaderService,
            RecipeRegistry recipeRegistry, RecipeConfigLoaderService recipeLoaderService,
            RecipeRegistrar recipeRegistrar, AiItemGenerationService aiItemGenerationService,
            ItemBalanceAnalysisService balanceAnalysisService,
            CustomBlockRegistry customBlockRegistry, CustomBlockStackFactory customBlockStackFactory,
            CustomBlockLoaderService customBlockLoaderService, Runnable settingsReloader, Plugin plugin) {
        this.registry = registry;
        this.itemStackFactory = itemStackFactory;
        this.loaderService = loaderService;
        this.resourcePackPort = resourcePackPort;
        this.armorRegistry = armorRegistry;
        this.armorStackFactory = armorStackFactory;
        this.armorLoaderService = armorLoaderService;
        this.recipeRegistry = recipeRegistry;
        this.recipeLoaderService = recipeLoaderService;
        this.recipeRegistrar = recipeRegistrar;
        this.aiItemGenerationService = aiItemGenerationService;
        this.balanceAnalysisService = balanceAnalysisService;
        this.customBlockRegistry = customBlockRegistry;
        this.customBlockStackFactory = customBlockStackFactory;
        this.customBlockLoaderService = customBlockLoaderService;
        this.settingsReloader = settingsReloader;
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            String[] args) {
        if (args.length == 0) {
            sender.sendMessage("Usage: /itemforge <give|reload|list|generate|analyze>");
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "give" -> handleGive(sender, args);
            case "reload" -> handleReload(sender);
            case "list" -> handleList(sender);
            case "generate" -> handleGenerate(sender, args);
            case "analyze" -> handleAnalyze(sender, args);
            default -> sender.sendMessage("Unknown subcommand: " + args[0]);
        }
        return true;
    }

    private void handleGive(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use /itemforge give.");
            return;
        }

        if (args.length < 2) {
            sender.sendMessage("Usage: /itemforge give <id>");
            return;
        }

        String id = args[1];
        // Items win first, then armor, then blocks when an id collides across the three
        // registries -- an explicit convention rather than a silent bug.
        var itemDefinition = registry.get(id);
        if (itemDefinition.isPresent()) {
            giveOrReportError(sender, player, id, () -> itemStackFactory.create(itemDefinition.get()));
            return;
        }

        var armorDefinition = armorRegistry.get(id);
        if (armorDefinition.isPresent()) {
            giveOrReportError(sender, player, id, () -> armorStackFactory.create(armorDefinition.get()));
            return;
        }

        customBlockRegistry.get(id).ifPresentOrElse(
                definition -> giveOrReportError(sender, player, id, () -> customBlockStackFactory.create(definition)),
                () -> sender.sendMessage("Unknown item id: " + id));
    }

    private void giveOrReportError(CommandSender sender, Player player, String id,
            Supplier<ItemStack> stackSupplier) {
        try {
            player.getInventory().addItem(stackSupplier.get());
        } catch (RuntimeException e) {
            sender.sendMessage("Failed to create item '" + id + "': " + e.getMessage());
        }
    }

    private void handleReload(CommandSender sender) {
        try {
            settingsReloader.run();
            loaderService.loadAll();
            armorLoaderService.loadAll();
            recipeLoaderService.loadAll();
            customBlockLoaderService.reloadAll(sender::sendMessage);
            recipeRegistrar.reload(recipeRegistry.getAll());
            resourcePackPort.rebuild();
        } catch (RuntimeException e) {
            sender.sendMessage("Reload failed: " + e.getMessage());
            return;
        }
        sender.sendMessage("Reloaded " + registry.size() + " item(s), " + armorRegistry.size()
                + " armor piece(s), " + recipeRegistry.size() + " recipe(s), " + customBlockRegistry.size()
                + " custom block(s) and rebuilt the resource pack.");
    }

    private void handleGenerate(CommandSender sender, String[] args) {
        if (!aiItemGenerationService.isEnabled()) {
            sender.sendMessage("AI item generation is disabled. Enable it under 'ai:' in config.yml.");
            return;
        }

        if (args.length < 3) {
            sender.sendMessage("Usage: /itemforge generate <id> <description...>");
            return;
        }

        String id = args[1];
        String description = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        sender.sendMessage("Generating item '" + id + "'...");
        CommandSender replyTo = lateReplyTarget(sender);

        // The API call is blocking I/O, so it has to run asynchronously to avoid freezing
        // the server's main thread. The result is brought back onto the main thread before
        // touching ItemStackFactory/ItemRegistry/the inventory, as the Bukkit API requires.
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            ItemDefinition draft;
            try {
                draft = aiItemGenerationService.generateDraft(id, description);
            } catch (RuntimeException e) {
                Bukkit.getScheduler().runTask(plugin,
                        () -> replyTo.sendMessage("Generation failed: " + e.getMessage()));
                return;
            }
            Bukkit.getScheduler().runTask(plugin, () -> finishGenerate(replyTo, id, draft));
        });
    }

    private void finishGenerate(CommandSender sender, String id, ItemDefinition draft) {
        ItemStack stack;
        try {
            stack = itemStackFactory.create(draft);
        } catch (RuntimeException e) {
            sender.sendMessage("Generated item has an invalid material, discarding: " + e.getMessage());
            return;
        }

        aiItemGenerationService.persist(draft);
        resourcePackPort.rebuild();

        sender.sendMessage("Created item '" + id + "': " + draft.displayName() + " (" + draft.material() + ")");
        draft.lore().forEach(line -> sender.sendMessage("  " + line));
        draft.abilities().forEach(ability -> sender.sendMessage("  - " + ability.trigger() + ": "
                + ability.getClass().getSimpleName() + " (cooldown " + ability.cooldownSeconds() + "s)"));

        if (sender instanceof Player player) {
            player.getInventory().addItem(stack);
        }
    }

    private void handleAnalyze(CommandSender sender, String[] args) {
        String targetId = args.length > 1 ? args[1] : "";

        // The rules are pure computation, so with no AI configured there is nothing to wait for
        // and the report can be produced right here. Only the AI path needs the async hop.
        if (!balanceAnalysisService.isAiEnabled()) {
            runAnalysis(sender, targetId);
            return;
        }

        sender.sendMessage(targetId.isEmpty()
                ? "Analyzing every registered item..."
                : "Analyzing '" + targetId + "'...");
        CommandSender replyTo = lateReplyTarget(sender);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            BalanceReport report;
            try {
                report = analyze(targetId);
            } catch (RuntimeException e) {
                Bukkit.getScheduler().runTask(plugin,
                        () -> replyTo.sendMessage("Analysis failed: " + e.getMessage()));
                return;
            }
            Bukkit.getScheduler().runTask(plugin, () -> sendReport(replyTo, report));
        });
    }

    // An RCON connection only carries what is sent before the command returns, so a result
    // that arrives from a background task would reach nobody -- not the RCON client, not the
    // log. Those go to the server console instead, and the RCON client is told where to look.
    private CommandSender lateReplyTarget(CommandSender sender) {
        if (!(sender instanceof RemoteConsoleCommandSender)) {
            return sender;
        }
        sender.sendMessage("The result will be printed to the server console.");
        return Bukkit.getConsoleSender();
    }

    private void runAnalysis(CommandSender sender, String targetId) {
        try {
            sendReport(sender, analyze(targetId));
        } catch (RuntimeException e) {
            sender.sendMessage("Analysis failed: " + e.getMessage());
        }
    }

    private BalanceReport analyze(String targetId) {
        return targetId.isEmpty()
                ? balanceAnalysisService.analyzeAll()
                : balanceAnalysisService.analyzeTarget(targetId);
    }

    private void sendReport(CommandSender sender, BalanceReport report) {
        sender.sendMessage(report.summary());

        if (report.findings().isEmpty()) {
            sender.sendMessage("Nothing to report.");
            return;
        }

        report.findings().stream().limit(MAX_FINDINGS_SHOWN).forEach(finding -> {
            sender.sendMessage("[" + finding.severity() + "] " + finding.targetId() + ": " + finding.issue());
            if (!finding.suggestion().isBlank()) {
                sender.sendMessage("    -> " + finding.suggestion());
            }
        });

        int hidden = report.findings().size() - MAX_FINDINGS_SHOWN;
        if (hidden > 0) {
            sender.sendMessage("...and " + hidden + " more. Use /itemforge analyze <id> to narrow this down.");
        }
    }

    private void handleList(CommandSender sender) {
        if (registry.size() == 0 && armorRegistry.size() == 0 && recipeRegistry.size() == 0
                && customBlockRegistry.size() == 0) {
            sender.sendMessage("No items registered.");
            return;
        }
        registry.getAll().forEach(definition -> {
            sender.sendMessage("- " + definition.id());
            definition.abilities().forEach(ability -> sender.sendMessage("    - " + ability.trigger() + ": "
                    + ability.getClass().getSimpleName() + " (cooldown " + ability.cooldownSeconds() + "s)"));
        });
        armorRegistry.getAll().forEach(definition -> sender.sendMessage("- [armor] " + definition.id()));
        recipeRegistry.getAll().forEach(definition -> sender.sendMessage("- [recipe] " + definition.id()));
        customBlockRegistry.getAll().forEach(definition -> sender.sendMessage("- [block] " + definition.id()));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
            @NotNull String alias, String[] args) {
        if (args.length == 1) {
            return SUBCOMMANDS;
        }

        if (args.length == 2 && "give".equalsIgnoreCase(args[0])) {
            List<String> ids = new ArrayList<>(registry.getAll().stream().map(definition -> definition.id()).toList());
            armorRegistry.getAll().forEach(definition -> ids.add(definition.id()));
            customBlockRegistry.getAll().forEach(definition -> ids.add(definition.id()));
            return ids;
        }

        // Blocks are left out: they carry no abilities or recipes, so there is nothing to analyze.
        if (args.length == 2 && "analyze".equalsIgnoreCase(args[0])) {
            List<String> ids = new ArrayList<>(registry.getAll().stream().map(definition -> definition.id()).toList());
            armorRegistry.getAll().forEach(definition -> ids.add(definition.id()));
            return ids;
        }

        return List.of();
    }
}
