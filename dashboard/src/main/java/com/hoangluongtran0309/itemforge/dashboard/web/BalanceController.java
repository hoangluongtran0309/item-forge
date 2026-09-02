package com.hoangluongtran0309.itemforge.dashboard.web;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestClientException;

import com.hoangluongtran0309.itemforge.dashboard.client.BalanceApiClient;
import com.hoangluongtran0309.itemforge.dashboard.client.PluginApiException;
import com.hoangluongtran0309.itemforge.dashboard.dto.BalanceReportJson;

@Controller
public class BalanceController {

    private final BalanceApiClient balanceApiClient;

    public BalanceController(BalanceApiClient balanceApiClient) {
        this.balanceApiClient = balanceApiClient;
    }

    @GetMapping("/balance")
    public String report(@RequestParam(required = false) String target, Model model) {
        loadReport(target, model);
        return "balance/report";
    }

    /**
     * Re-runs the analysis and swaps only the results back in, so the page does not reload
     * around what can be a slow AI call -- the same htmx fragment approach as generating an
     * item draft on the item form.
     */
    @PostMapping("/balance/refresh")
    public String refresh(@RequestParam(required = false) String target, Model model) {
        loadReport(target, model);
        return "balance/report :: report-body";
    }

    /**
     * Like the Overview, this page renders its own error rather than letting
     * GlobalExceptionHandler redirect: a failure here is expected whenever the plugin is not
     * running, and the page is still useful with an explanation on it.
     */
    private void loadReport(String target, Model model) {
        String targetId = target == null ? "" : target.trim();
        model.addAttribute("target", targetId);

        try {
            model.addAttribute("report", targetId.isEmpty()
                    ? balanceApiClient.findAll()
                    : balanceApiClient.findByTarget(targetId));
        } catch (PluginApiException e) {
            model.addAttribute("reportError", e.getMessage());
            model.addAttribute("report", emptyReport());
        } catch (RestClientException e) {
            model.addAttribute("reportError", "Could not reach the ItemForge plugin API. Is the plugin running and "
                    + "is itemforge.api.base-url configured correctly?");
            model.addAttribute("report", emptyReport());
        }
    }

    private static BalanceReportJson emptyReport() {
        return new BalanceReportJson("", false, List.of());
    }
}
