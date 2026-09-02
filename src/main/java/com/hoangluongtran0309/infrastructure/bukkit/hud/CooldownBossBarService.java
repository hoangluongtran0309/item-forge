package com.hoangluongtran0309.infrastructure.bukkit.hud;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.hoangluongtran0309.domain.model.AbilityDefinition;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;

public class CooldownBossBarService {

    private final Plugin plugin;
    private final long updateIntervalTicks;

    // This service tracks its own start time and total duration, so CooldownService does
    // not need an extra remaining() API -- the change stays contained in infra/hud.
    private final Map<UUID, ActiveBar> activeBars = new ConcurrentHashMap<>();

    public CooldownBossBarService(Plugin plugin, long updateIntervalTicks) {
        this.plugin = plugin;
        this.updateIntervalTicks = updateIntervalTicks;
    }

    public void show(Player player, AbilityDefinition definition) {
        Component title = Component.text("Cooldown: " + definition.trigger());
        show(player, title, Duration.ofSeconds(definition.cooldownSeconds()));
    }

    public void show(Player player, Component title, Duration total) {
        hide(player);
        if (total.isZero() || total.isNegative()) {
            return;
        }

        BossBar bar = BossBar.bossBar(title, 1.0f, BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS);
        player.showBossBar(bar);

        UUID playerId = player.getUniqueId();
        int taskId = Bukkit.getScheduler()
                .runTaskTimer(plugin, () -> tick(playerId), 0L, updateIntervalTicks)
                .getTaskId();
        activeBars.put(playerId, new ActiveBar(bar, Instant.now(), total, taskId));
    }

    public void hide(Player player) {
        ActiveBar existing = activeBars.remove(player.getUniqueId());
        if (existing == null) {
            return;
        }
        Bukkit.getScheduler().cancelTask(existing.taskId());
        player.hideBossBar(existing.bar());
    }

    private void tick(UUID playerId) {
        ActiveBar active = activeBars.get(playerId);
        if (active == null) {
            return;
        }

        Player player = Bukkit.getPlayer(playerId);
        if (player == null) {
            // The player left the server -- cancel the task, there is nobody left to hide the
            // boss bar from.
            activeBars.remove(playerId);
            Bukkit.getScheduler().cancelTask(active.taskId());
            return;
        }

        Duration elapsed = Duration.between(active.startedAt(), Instant.now());
        double fraction = 1.0 - ((double) elapsed.toMillis() / active.total().toMillis());
        if (fraction <= 0) {
            hide(player);
            return;
        }
        active.bar().progress((float) Math.min(1.0, fraction));
    }

    private record ActiveBar(BossBar bar, Instant startedAt, Duration total, int taskId) {
    }
}
