package com.hoangluongtran0309.infrastructure.bukkit.model;

import org.bukkit.Bukkit;

import com.hoangluongtran0309.application.ServerVersion;

public class ServerVersionDetector {

    public ServerVersion detect() {
        // Bukkit.getBukkitVersion() has had a stable format for a long time:
        // "1.20.6-R0.1-SNAPSHOT"
        String versionPart = Bukkit.getBukkitVersion().split("-")[0]; // "1.20.6"
        String[] parts = versionPart.split("\\.");

        int major = Integer.parseInt(parts[0]);
        int minor = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
        int patch = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;

        return new ServerVersion(major, minor, patch);
    }
}
