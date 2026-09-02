package com.hoangluongtran0309.itemforge.dashboard.config;

import java.nio.file.Path;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The reference texture library, loaded by THE ADMIN THEMSELVES.
 *
 * <p>No Mojang asset is bundled or downloaded automatically: Minecraft assets are
 * copyrighted property. The admin imports a resource pack they legally own themselves, and
 * ItemForge only stores and displays it locally.
 */
@ConfigurationProperties(prefix = "itemforge.reference")
public class ReferenceLibraryProperties {

    /** Where imported packs are extracted. Defaults to a location outside the project directory. */
    private Path dir = Path.of(System.getProperty("user.home"), ".itemforge-dashboard", "reference");

    /** Maximum total bytes read out of one zip file (zip bomb protection). */
    private long maxTotalBytes = 256L * 1024 * 1024;

    /** Maximum bytes for a single entry. Minecraft textures are tiny. */
    private long maxEntryBytes = 4L * 1024 * 1024;

    /** Maximum number of entries written out of one pack. */
    private int maxEntries = 20_000;

    public Path getDir() {
        return dir;
    }

    public void setDir(Path dir) {
        this.dir = dir;
    }

    public long getMaxTotalBytes() {
        return maxTotalBytes;
    }

    public void setMaxTotalBytes(long maxTotalBytes) {
        this.maxTotalBytes = maxTotalBytes;
    }

    public long getMaxEntryBytes() {
        return maxEntryBytes;
    }

    public void setMaxEntryBytes(long maxEntryBytes) {
        this.maxEntryBytes = maxEntryBytes;
    }

    public int getMaxEntries() {
        return maxEntries;
    }

    public void setMaxEntries(int maxEntries) {
        this.maxEntries = maxEntries;
    }
}
