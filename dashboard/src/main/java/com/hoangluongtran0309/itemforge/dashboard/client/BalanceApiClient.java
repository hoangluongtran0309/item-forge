package com.hoangluongtran0309.itemforge.dashboard.client;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.hoangluongtran0309.itemforge.dashboard.dto.BalanceReportJson;

@Component
public class BalanceApiClient {

    private final RestClient restClient;

    public BalanceApiClient(RestClient itemforgeRestClient) {
        this.restClient = itemforgeRestClient;
    }

    public BalanceReportJson findAll() {
        return restClient.get().uri("/api/balance").retrieve().body(BalanceReportJson.class);
    }

    public BalanceReportJson findByTarget(String targetId) {
        return restClient.get().uri("/api/balance/{id}", targetId).retrieve().body(BalanceReportJson.class);
    }
}
