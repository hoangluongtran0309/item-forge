package com.hoangluongtran0309.itemforge.dashboard.dto;

import java.util.regex.Pattern;

/**
 * Display names and lore carry Minecraft's {@code &}-prefixed formatting codes, which the plugin
 * turns into colours in game. The dashboard has no use for them outside the edit forms.
 */
final class MinecraftText {

    // The same set LegacyComponentSerializer.legacyAmpersand() accepts on the plugin side:
    // colours 0-9 and a-f, formats k-o, and r for reset.
    private static final Pattern COLOR_CODE = Pattern.compile("&[0-9a-fk-or]", Pattern.CASE_INSENSITIVE);

    private MinecraftText() {
    }

    static String stripColorCodes(String text) {
        return text == null ? "" : COLOR_CODE.matcher(text).replaceAll("");
    }
}
