package com.hoangluongtran0309.itemforge.dashboard.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.RestClientException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.hoangluongtran0309.itemforge.dashboard.client.PluginApiException;

/**
 * Surfaces errors from the plugin API without breaking the page. For htmx requests
 * (delete-row, AI-generate, add/remove-row) the response is retargeted to the shared
 * #error-banner instead of the element's own hx-target, so the originating row or form is
 * left completely untouched. Ordinary (non-htmx) POST/PUT form requests never reach here
 * with a PluginApiException -- the controller catches it in place and re-renders the form
 * with the error in the model. They reach here ONLY when the connection drops, which is why
 * the fallback branch below stays generic (redirect, no form re-render); the message travels
 * through a flash attribute so it survives the redirect instead of being lost silently.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(PluginApiException.class)
    public String handlePluginApiException(PluginApiException ex, HttpServletRequest request,
            HttpServletResponse response, Model model, RedirectAttributes redirectAttributes) {
        return respondWithError(ex.getMessage(), request, response, model, redirectAttributes);
    }

    // Covers connection-level failures (the plugin is not running, a wrong base-url, a
    // timeout, ...). ResourceAccessException and its siblings are RestClientExceptions but
    // NOT PluginApiExceptions -- that type only comes from a real HTTP error response parsed
    // by PluginApiErrorHandler.
    @ExceptionHandler(RestClientException.class)
    public String handleRestClientException(RestClientException ex, HttpServletRequest request,
            HttpServletResponse response, Model model, RedirectAttributes redirectAttributes) {
        return respondWithError("Could not reach the ItemForge plugin API. Is the plugin running and is "
                + "itemforge.api.base-url configured correctly?", request, response, model, redirectAttributes);
    }

    private String respondWithError(String message, HttpServletRequest request, HttpServletResponse response,
            Model model, RedirectAttributes redirectAttributes) {
        boolean isHtmxRequest = "true".equals(request.getHeader("HX-Request"));

        if (isHtmxRequest) {
            response.setHeader("HX-Retarget", "#error-banner");
            response.setHeader("HX-Reswap", "innerHTML");
            model.addAttribute("message", message);
            return "fragments/error-banner :: banner";
        }

        String referer = request.getHeader("Referer");
        redirectAttributes.addFlashAttribute("errorBanner", message);
        return "redirect:" + (referer != null ? referer : "/");
    }
}
