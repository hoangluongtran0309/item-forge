package com.hoangluongtran0309.itemforge.dashboard.web;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Supplies `activeNav` to the sidebar so it can highlight the current entry. It is derived
 * from the path here rather than in the template because Thymeleaf 3.1 dropped
 * #httpServletRequest and #request, leaving templates unable to read the URI. Doing it in
 * one place also avoids having to remember an extra @ModelAttribute on every controller.
 */
@ControllerAdvice
public class ActiveNavAdvice {

    @ModelAttribute("activeNav")
    public String activeNav(HttpServletRequest request) {
        String path = request.getRequestURI();
        String context = request.getContextPath();
        if (!context.isEmpty() && path.startsWith(context)) {
            path = path.substring(context.length());
        }

        if (path.startsWith("/items")) {
            return "items";
        }
        if (path.startsWith("/armor")) {
            return "armor";
        }
        if (path.startsWith("/blocks")) {
            return "blocks";
        }
        if (path.startsWith("/recipes")) {
            return "recipes";
        }
        if (path.startsWith("/balance")) {
            return "balance";
        }
        if (path.startsWith("/studio")) {
            return "studio";
        }
        if (path.startsWith("/reference")) {
            return "reference";
        }
        return "overview";
    }
}
