package com.hoangluongtran0309.itemforge.dashboard.web;

final class TextureMessages {

    private TextureMessages() {
    }

    static String success(String sha1) {
        return "Resource pack rebuilt" + (sha1 != null ? ", SHA-1: " + sha1 : "")
                + ". Players need to rejoin to see changes.";
    }
}
