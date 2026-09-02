package com.hoangluongtran0309.itemforge.dashboard.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClient;

import com.hoangluongtran0309.itemforge.dashboard.client.PluginApiErrorHandler;

@Configuration
public class RestClientConfig {

    private static final Logger LOG = LoggerFactory.getLogger(RestClientConfig.class);

    // Uses the RestClient.Builder Spring Boot autoconfigures (rather than the static
    // RestClient.builder()) so @RestClientTest/MockRestServiceServer can inject a builder
    // customized for tests.
    @Bean
    RestClient itemforgeRestClient(RestClient.Builder restClientBuilder, PluginApiProperties properties) {
        String token = properties.token();
        // Unlike the plugin, this cannot refuse to start -- the dashboard still has to run
        // so an admin can log in and fix the config. So it warns loudly instead, to stop
        // anyone running production on the placeholder: every request will just get a 401
        // from the plugin until the token is changed.
        if (token == null || token.isBlank() || token.equalsIgnoreCase("CHANGE_ME")
                || token.equalsIgnoreCase("changeme")) {
            LOG.warn("ITEMFORGE_API_TOKEN is not configured (still the placeholder). "
                    + "Every request to the plugin will be rejected (401) until you set a real token "
                    + "matching the plugin's dashboard-api.api-key.");
        }

        return restClientBuilder
                .baseUrl(properties.baseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .defaultStatusHandler(HttpStatusCode::isError, PluginApiErrorHandler::handle)
                .build();
    }
}
