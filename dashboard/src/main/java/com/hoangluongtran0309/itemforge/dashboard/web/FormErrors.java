package com.hoangluongtran0309.itemforge.dashboard.web;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

/**
 * Turns a BindingResult into a simple "field name -> first message" Map for the model.
 *
 * <p>{@code th:errors} / {@code #fields} are deliberately NOT used in the templates: they
 * require a BindingResult to exist in the render context. But
 * {@code items/form :: form-fields} is also returned on its own by the AI-generate endpoint
 * and by the abilities add/remove-row endpoints, where the model holds only "form" and no
 * BindingResult at all -- and {@code #fields} would throw. A plain Map renders in both
 * cases.
 */
final class FormErrors {

    static final String MODEL_ATTRIBUTE = "fieldErrors";

    private FormErrors() {
    }

    static Map<String, String> of(BindingResult binding) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError error : binding.getFieldErrors()) {
            // Keep only the first message per field: a cluster of messages under one input
            // is more confusing than helpful.
            errors.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        return errors;
    }
}
