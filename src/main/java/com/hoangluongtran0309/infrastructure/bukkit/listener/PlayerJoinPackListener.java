package com.hoangluongtran0309.infrastructure.bukkit.listener;

import java.util.logging.Logger;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;

import com.hoangluongtran0309.application.port.ResourcePackPort;

public class PlayerJoinPackListener implements Listener {

    private final ResourcePackPort resourcePackPort;
    private final Logger logger;

    public PlayerJoinPackListener(ResourcePackPort resourcePackPort, Logger logger) {
        this.resourcePackPort = resourcePackPort;
        this.logger = logger;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        resourcePackPort.currentPack().ifPresent(pack -> {
            logger.info("Sending resource pack to " + event.getPlayer().getName()
                    + " (url=" + pack.url() + ", sha1=" + pack.sha1Hex() + ")");
            event.getPlayer().setResourcePack(pack.url(), pack.sha1Hex());
        });
    }

    // The client reports the pack download result here. Nothing used to be logged, which
    // made it impossible to tell FAILED_DOWNLOAD from INVALID_URL or DECLINED when
    // debugging.
    @EventHandler
    public void onResourcePackStatus(PlayerResourcePackStatusEvent event) {
        logger.info("Resource pack status for " + event.getPlayer().getName() + ": " + event.getStatus());
    }
}
