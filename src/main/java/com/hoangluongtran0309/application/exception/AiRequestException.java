package com.hoangluongtran0309.application.exception;

/**
 * Parent of every failure raised while talking to an AI provider. The transport shared by all
 * AI features throws this, while each feature has its own subclass so a caller can still tell
 * item generation apart from balance analysis.
 */
public class AiRequestException extends RuntimeException {

    public AiRequestException(String message) {
        super(message);
    }
}
