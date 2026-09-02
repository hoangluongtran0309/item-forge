package com.hoangluongtran0309.infrastructure.bukkit.http;

import java.io.IOException;

import com.sun.net.httpserver.HttpExchange;

import com.hoangluongtran0309.application.ItemBalanceAnalysisService;
import com.hoangluongtran0309.application.exception.UnknownBalanceTargetException;
import com.hoangluongtran0309.domain.balance.BalanceReport;
import com.hoangluongtran0309.infrastructure.config.BalanceReportMapper;

/**
 * Serves the balance report to the dashboard.
 *
 * <p>Unlike POST /api/items/generate there is no "AI is disabled" failure here: the rule
 * findings need no provider, so the endpoint always answers and reports {@code ai-enabled} in
 * the body for the dashboard to show.
 */
public class BalanceApiHandler implements ApiResourceHandler {

    private final ItemBalanceAnalysisService analysisService;

    public BalanceApiHandler(ItemBalanceAnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equals("GET")) {
            throw new ApiException(405, "Method not allowed");
        }

        String targetId = ApiPaths.subPath(exchange, "/api/balance");
        BalanceReport report;
        try {
            report = targetId.isEmpty()
                    ? analysisService.analyzeAll()
                    : analysisService.analyzeTarget(targetId);
        } catch (UnknownBalanceTargetException e) {
            throw new ApiException(404, e.getMessage());
        }

        HttpJson.send(exchange, 200, BalanceReportMapper.toMap(report, analysisService.isAiEnabled()));
    }
}
