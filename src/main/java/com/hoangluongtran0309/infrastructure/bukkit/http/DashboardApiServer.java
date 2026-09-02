package com.hoangluongtran0309.infrastructure.bukkit.http;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import com.hoangluongtran0309.domain.exception.InvalidItemDefinitionException;
import com.hoangluongtran0309.domain.exception.InvalidTextureException;

/**
 * The HTTP server behind the dashboard REST API (items/armor/blocks/recipes/balance), following the same
 * lifecycle model as PackHttpServer. Every request must carry an
 * "Authorization: Bearer <api-key>" header matching the dashboard-api.api-key config.
 */
public class DashboardApiServer {

    private static final String AUTH_PREFIX = "Bearer ";

    private final int port;
    private final String apiKey;
    private final ItemsApiHandler itemsHandler;
    private final ArmorApiHandler armorHandler;
    private final BlocksApiHandler blocksHandler;
    private final RecipesApiHandler recipesHandler;
    private final BalanceApiHandler balanceHandler;
    private final Logger logger;
    private HttpServer server;
    private ExecutorService executor;

    public DashboardApiServer(int port, String apiKey, ItemsApiHandler itemsHandler, ArmorApiHandler armorHandler,
            BlocksApiHandler blocksHandler, RecipesApiHandler recipesHandler, BalanceApiHandler balanceHandler,
            Logger logger) {
        this.port = port;
        this.apiKey = apiKey;
        this.itemsHandler = itemsHandler;
        this.armorHandler = armorHandler;
        this.blocksHandler = blocksHandler;
        this.recipesHandler = recipesHandler;
        this.balanceHandler = balanceHandler;
        this.logger = logger;
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        executor = Executors.newFixedThreadPool(4);
        server.setExecutor(executor);
        server.createContext("/api/items", exchange -> dispatch(exchange, itemsHandler));
        server.createContext("/api/armor", exchange -> dispatch(exchange, armorHandler));
        server.createContext("/api/blocks", exchange -> dispatch(exchange, blocksHandler));
        server.createContext("/api/recipes", exchange -> dispatch(exchange, recipesHandler));
        server.createContext("/api/balance", exchange -> dispatch(exchange, balanceHandler));
        server.start();
        logger.info("Dashboard API server started on port " + port);
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    private void dispatch(HttpExchange exchange, ApiResourceHandler handler) {
        try {
            if (!isAuthorized(exchange)) {
                HttpJson.sendError(exchange, 401, "Unauthorized");
                return;
            }

            handler.handle(exchange);
        } catch (ApiException e) {
            sendErrorQuietly(exchange, e.status(), e.getMessage());
        } catch (InvalidItemDefinitionException e) {
            sendErrorQuietly(exchange, 400, e.getMessage());
        } catch (InvalidTextureException e) {
            sendErrorQuietly(exchange, 400, e.getMessage());
        } catch (RuntimeException | IOException e) {
            logger.warning("Unhandled error in dashboard API: " + e.getMessage());
            sendErrorQuietly(exchange, 500, "Internal server error");
        } finally {
            exchange.close();
        }
    }

    private void sendErrorQuietly(HttpExchange exchange, int status, String message) {
        try {
            HttpJson.sendError(exchange, status, message);
        } catch (IOException e) {
            logger.warning("Failed to write dashboard API error response: " + e.getMessage());
        }
    }

    private boolean isAuthorized(HttpExchange exchange) {
        String header = exchange.getRequestHeaders().getFirst("Authorization");
        if (header == null || !header.startsWith(AUTH_PREFIX)) {
            return false;
        }
        String token = header.substring(AUTH_PREFIX.length());
        return constantTimeEquals(token, apiKey);
    }

    // Constant-time comparison: an ordinary String comparison stops at the first differing
    // byte, which leaks the token through timing.
    private static boolean constantTimeEquals(String a, String b) {
        byte[] aBytes = a.getBytes(StandardCharsets.UTF_8);
        byte[] bBytes = b.getBytes(StandardCharsets.UTF_8);
        int diff = aBytes.length ^ bBytes.length;
        for (int i = 0; i < aBytes.length && i < bBytes.length; i++) {
            diff |= aBytes[i] ^ bBytes[i];
        }
        return diff == 0;
    }
}
