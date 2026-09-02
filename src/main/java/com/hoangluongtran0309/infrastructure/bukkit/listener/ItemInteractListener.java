package com.hoangluongtran0309.infrastructure.bukkit.listener;

import java.util.List;
import java.util.Optional;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import com.hoangluongtran0309.application.AbilityDispatchService;
import com.hoangluongtran0309.domain.model.TriggerType;
import com.hoangluongtran0309.domain.model.TriggeredAbility;
import com.hoangluongtran0309.infrastructure.bukkit.ItemStackFactory;
import com.hoangluongtran0309.infrastructure.bukkit.hud.ActionBarNotifier;
import com.hoangluongtran0309.infrastructure.bukkit.hud.CooldownBossBarService;

public class ItemInteractListener implements Listener {

    private final AbilityDispatchService dispatchService;
    private final ItemStackFactory itemStackFactory;
    private final ActionBarNotifier actionBarNotifier;
    private final CooldownBossBarService cooldownBossBarService;

    public ItemInteractListener(AbilityDispatchService dispatchService, ItemStackFactory itemStackFactory,
            ActionBarNotifier actionBarNotifier, CooldownBossBarService cooldownBossBarService) {
        this.dispatchService = dispatchService;
        this.itemStackFactory = itemStackFactory;
        this.actionBarNotifier = actionBarNotifier;
        this.cooldownBossBarService = cooldownBossBarService;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        ItemStack item = event.getItem();
        if (item == null) {
            return;
        }

        Optional<String> itemId = itemStackFactory.extractItemId(item);
        if (itemId.isEmpty()) {
            return;
        }

        TriggerType trigger = mapTrigger(event.getAction());
        if (trigger == null) {
            return;
        }

        Player player = event.getPlayer();
        List<TriggeredAbility> triggeredAbilities = dispatchService.dispatch(player.getUniqueId(), itemId.get(),
                trigger);
        for (TriggeredAbility triggered : triggeredAbilities) {
            actionBarNotifier.notify(player, triggered);
            cooldownBossBarService.show(player, triggered.definition());
        }
    }

    private TriggerType mapTrigger(Action action) {
        return switch (action) {
            case RIGHT_CLICK_AIR, RIGHT_CLICK_BLOCK -> TriggerType.RIGHT_CLICK;
            case LEFT_CLICK_AIR, LEFT_CLICK_BLOCK -> TriggerType.LEFT_CLICK;
            default -> null;
        };
    }
}
