package com.hoangluongtran0309.itemforge.dashboard.client;

/**
 * Thrown when the plugin's REST API returns an error status. It carries the HTTP status so
 * GlobalExceptionHandler and the call site can react accordingly (409 for a duplicate id,
 * 404 for not found, and so on).
 */
public class PluginApiException extends RuntimeException {

    private final int status;

    public PluginApiException(int status, String message) {
        super(message);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}
