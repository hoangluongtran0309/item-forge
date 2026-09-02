package com.hoangluongtran0309.itemforge.dashboard.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.ResourceAccessException;

import com.hoangluongtran0309.itemforge.dashboard.client.BalanceApiClient;
import com.hoangluongtran0309.itemforge.dashboard.dto.BalanceFindingJson;
import com.hoangluongtran0309.itemforge.dashboard.dto.BalanceReportJson;

@WebMvcTest(BalanceController.class)
@Import(ActiveNavAdvice.class)
class BalanceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BalanceApiClient balanceApiClient;

    @BeforeEach
    void stubEmptyByDefault() {
        when(balanceApiClient.findAll()).thenReturn(new BalanceReportJson("", true, List.of()));
    }

    @Test
    @WithMockUser
    void thePageRendersEveryFinding() throws Exception {
        when(balanceApiClient.findAll()).thenReturn(new BalanceReportJson("Analyzed 1 item(s).", true,
                List.of(criticalFinding())));

        mockMvc.perform(get("/balance"))
                .andExpect(status().isOk())
                .andExpect(view().name("balance/report"))
                .andExpect(content().string(containsString("SPEED never expires.")))
                .andExpect(content().string(containsString("Raise the cooldown.")))
                .andExpect(content().string(containsString("PERMANENT_EFFECT")))
                .andExpect(content().string(containsString("CRITICAL")));
    }

    @Test
    @WithMockUser
    void anEmptyReportSaysThereIsNothingToReport() throws Exception {
        mockMvc.perform(get("/balance"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Nothing to report")));
    }

    @Test
    @WithMockUser
    void theAiNoticeAppearsOnlyWhenTheAiIsOff() throws Exception {
        mockMvc.perform(get("/balance"))
                .andExpect(content().string(not(containsString("Showing rule-based findings only"))));

        when(balanceApiClient.findAll()).thenReturn(new BalanceReportJson("", false, List.of()));

        mockMvc.perform(get("/balance"))
                .andExpect(content().string(containsString("Showing rule-based findings only")));
    }

    @Test
    @WithMockUser
    void aTargetParameterNarrowsTheReportToThatId() throws Exception {
        when(balanceApiClient.findByTarget("void_sword"))
                .thenReturn(new BalanceReportJson("Analyzed 'void_sword'.", true, List.of(criticalFinding())));

        mockMvc.perform(get("/balance").param("target", "void_sword"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("target", "void_sword"))
                .andExpect(content().string(containsString("Analyzed &#39;void_sword&#39;.")));
    }

    @Test
    @WithMockUser
    void aPluginThatIsDownRendersAnExplanationInsteadOfRedirecting() throws Exception {
        when(balanceApiClient.findAll()).thenThrow(new ResourceAccessException("connection refused"));

        mockMvc.perform(get("/balance"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("reportError"))
                .andExpect(content().string(containsString("Could not reach the ItemForge plugin API")));
    }

    @Test
    @WithMockUser
    void noErrorBannerIsDrawnWhenTheReportLoadedFine() throws Exception {
        // Regression: th:replace outranks th:if in Thymeleaf, so guarding an included
        // fragment that way drew an empty red banner on every successful load.
        mockMvc.perform(get("/balance"))
                .andExpect(content().string(not(containsString("alert-error"))));
    }

    @Test
    @WithMockUser
    void anErrorDuringARefreshIsShownInTheFragmentRatherThanSilently() throws Exception {
        when(balanceApiClient.findAll()).thenThrow(new ResourceAccessException("connection refused"));

        mockMvc.perform(post("/balance/refresh")
                .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("alert-error")))
                .andExpect(content().string(containsString("Could not reach the ItemForge plugin API")));
    }

    @Test
    @WithMockUser
    void refreshReturnsOnlyTheResultsFragment() throws Exception {
        when(balanceApiClient.findAll()).thenReturn(new BalanceReportJson("Analyzed 1 item(s).", true,
                List.of(criticalFinding())));

        mockMvc.perform(post("/balance/refresh")
                .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("balance/report :: report-body"))
                .andExpect(content().string(containsString("SPEED never expires.")))
                // The surrounding page, including its heading, must not come back with the fragment.
                .andExpect(content().string(not(containsString("Re-run analysis"))));
    }

    @Test
    @WithMockUser
    void refreshPassesTheTargetThrough() throws Exception {
        when(balanceApiClient.findByTarget(anyString()))
                .thenReturn(new BalanceReportJson("Analyzed 'void_sword'.", true, List.of()));

        mockMvc.perform(post("/balance/refresh")
                .param("target", "void_sword")
                .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Analyzed &#39;void_sword&#39;.")));
    }

    private static BalanceFindingJson criticalFinding() {
        return new BalanceFindingJson("void_sword", "CRITICAL", "PERMANENT_EFFECT", "SPEED never expires.",
                "Raise the cooldown.", "RULE");
    }
}
