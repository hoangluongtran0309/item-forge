package com.hoangluongtran0309.infrastructure.bukkit.http;

import java.io.IOException;

import com.sun.net.httpserver.HttpExchange;

interface ApiResourceHandler {

    void handle(HttpExchange exchange) throws IOException;
}
