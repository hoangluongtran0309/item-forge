package com.hoangluongtran0309.application;

public record ServerVersion(int major, int minor, int patch) {

    public boolean isAtLeast(int major, int minor, int patch) {
        if (this.major != major) {
            return this.major > major;
        }
        if (this.minor != minor) {
            return this.minor > minor;
        }
        return this.patch >= patch;
    }

    @Override
    public String toString() {
        return major + "." + minor + "." + patch;
    }
}
