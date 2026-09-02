package com.hoangluongtran0309.itemforge.dashboard.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.restclient.test.autoconfigure.RestClientTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.client.MockRestServiceServer;

import com.hoangluongtran0309.itemforge.dashboard.config.PluginApiProperties;
import com.hoangluongtran0309.itemforge.dashboard.config.RestClientConfig;
import com.hoangluongtran0309.itemforge.dashboard.dto.BalanceFindingJson;
import com.hoangluongtran0309.itemforge.dashboard.dto.BalanceReportJson;

@RestClientTest({BalanceApiClient.class, RestClientConfig.class})
@EnableConfigurationProperties(PluginApiProperties.class)
@TestPropertySource(properties = {
        "itemforge.api.base-url=http://plugin.local",
        "itemforge.api.token=secret-token"
})
class BalanceApiClientTest {

    @Autowired
    private BalanceApiClient balanceApiClient;

    @Autowired
    private MockRestServiceServer server;

    @Test
    void findAllSendsTheBearerTokenAndReadsTheKebabCaseFields() {
        server.expect(requestTo("http://plugin.local/api/balance"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer secret-token"))
                .andRespond(withSuccess("""
                        {"summary":"Analyzed 1 item(s).","ai-enabled":false,"findings":[\
                        {"target-id":"void_sword","severity":"CRITICAL","rule":"PERMANENT_EFFECT",\
                        "issue":"SPEED never expires.","suggestion":"Raise the cooldown.","source":"RULE"}]}
                        """, MediaType.APPLICATION_JSON));

        BalanceReportJson report = balanceApiClient.findAll();

        assertEquals("Analyzed 1 item(s).", report.summary());
        assertFalse(report.aiEnabled());
        BalanceFindingJson finding = report.findings().get(0);
        assertEquals("void_sword", finding.targetId());
        assertEquals("CRITICAL", finding.severity());
        assertEquals("PERMANENT_EFFECT", finding.rule());
        assertEquals("Raise the cooldown.", finding.suggestion());
        assertEquals("RULE", finding.source());
    }

    @Test
    void findByTargetRequestsThatIdsReport() {
        server.expect(requestTo("http://plugin.local/api/balance/void_sword"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"summary":"Analyzed 'void_sword'.","ai-enabled":true,"findings":[]}
                        """, MediaType.APPLICATION_JSON));

        BalanceReportJson report = balanceApiClient.findByTarget("void_sword");

        assertTrue(report.aiEnabled());
        assertTrue(report.findings().isEmpty());
    }

    @Test
    void anUnknownIdSurfacesAsAPluginApiException() {
        server.expect(requestTo("http://plugin.local/api/balance/nope"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":\"No item or armor piece is registered with the id 'nope'\"}"));

        PluginApiException exception = assertThrows(PluginApiException.class,
                () -> balanceApiClient.findByTarget("nope"));
        assertTrue(exception.getMessage().contains("nope"));
    }
}
