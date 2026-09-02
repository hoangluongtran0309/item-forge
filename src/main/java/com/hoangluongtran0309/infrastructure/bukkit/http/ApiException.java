package com.hoangluongtran0309.infrastructure.bukkit.http;

/**
 * An error that maps directly onto an HTTP status code when returned to a client of the
 * dashboard REST API (400, 404, 409 and so on).
 */
class ApiException extends RuntimeException {

    private final int status;

    ApiException(int status, String message) {
        super(message);
        this.status = status;
    }

    int status() {
        return status;
    }
}
