package com.hoangluongtran0309.infrastructure.ai;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.logging.Logger;

import com.hoangluongtran0309.application.exception.AiRequestException;

/**
 * The blocking send shared by every adapter: one HttpClient per adapter, one timeout applied to
 * both the connect and the read, and the same error wording whichever provider failed.
 *
 * <p>The caller is responsible for running this off the server's main thread.
 */
final class AiHttpCall {

    private AiHttpCall() {
    }

    static HttpClient newHttpClient(int timeoutSeconds) {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(timeoutSeconds))
                .build();
    }

    /**
     * @param providerName used in error messages only -- never pass a URI, since Gemini carries
     *        the api-key in its query string
     * @return the response body of a 2xx response
     */
    static String send(HttpClient httpClient, HttpRequest request, String providerName, Logger logger) {
        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            logger.warning("Failed to reach " + providerName + " API: " + e.getMessage());
            throw new AiRequestException("Failed to reach " + providerName + " API: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AiRequestException(providerName + " API call was interrupted");
        }

        if (response.statusCode() != 200) {
            throw new AiRequestException(
                    providerName + " API returned HTTP " + response.statusCode() + ": " + response.body());
        }

        return response.body();
    }
}
