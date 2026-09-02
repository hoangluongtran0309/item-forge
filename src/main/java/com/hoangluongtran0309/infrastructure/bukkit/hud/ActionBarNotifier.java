package com.hoangluongtran0309.infrastructure.bukkit.hud;

import org.bukkit.entity.Player;

import com.hoangluongtran0309.domain.model.DamageModifierEffect;
import com.hoangluongtran0309.domain.model.EffectCommand;
import com.hoangluongtran0309.domain.model.TriggeredAbility;

import net.kyori.adventure.text.Component;

public class ActionBarNotifier {

    public void notify(Player player, TriggeredAbility triggered) {
        Component message = switch (triggered.effect()) {
            case EffectCommand ec -> Component.text("Activated: " + ec.type() + " (" + ec.durationSeconds() + "s)");
            case DamageModifierEffect dm -> Component.text("Damage bonus active: +" + dm.bonusPercent() + "%");
        };
        player.sendActionBar(message);
    }
}
