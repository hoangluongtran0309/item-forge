package com.hoangluongtran0309.itemforge.dashboard.web;

import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.ui.Model;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletResponse;

import com.hoangluongtran0309.itemforge.dashboard.client.PluginApiException;
import com.hoangluongtran0309.itemforge.dashboard.client.RecipeApiClient;
import com.hoangluongtran0309.itemforge.dashboard.dto.RecipeJson;
import com.hoangluongtran0309.itemforge.dashboard.form.IngredientRow;
import com.hoangluongtran0309.itemforge.dashboard.form.RecipeForm;
import com.hoangluongtran0309.itemforge.dashboard.mapper.RecipeFormMapper;

@Controller
public class RecipeController {

    private final RecipeApiClient recipeApiClient;
    private final RecipeFormMapper recipeFormMapper;

    public RecipeController(RecipeApiClient recipeApiClient, RecipeFormMapper recipeFormMapper) {
        this.recipeApiClient = recipeApiClient;
        this.recipeFormMapper = recipeFormMapper;
    }

    @GetMapping("/recipes")
    public String list(Model model) {
        model.addAttribute("recipes", recipeApiClient.findAll());
        return "recipes/list";
    }

    @GetMapping("/recipes/new")
    public String newForm(Model model) {
        model.addAttribute("form", new RecipeForm());
        model.addAttribute("editing", false);
        return "recipes/form";
    }

    @GetMapping("/recipes/{id}/edit")
    public String editForm(@PathVariable String id, Model model) {
        RecipeJson recipe = recipeApiClient.findById(id);
        model.addAttribute("form", recipeFormMapper.fromJson(recipe));
        model.addAttribute("editing", true);
        return "recipes/form";
    }

    @PostMapping("/recipes")
    public String create(@Valid @ModelAttribute("form") RecipeForm form, BindingResult binding, Model model, RedirectAttributes redirectAttributes) {
        // Stop as soon as the form has errors: do not call the plugin API, re-render the
        // form so fragments/field-error shows the message under each field.
        if (binding.hasErrors()) {
            model.addAttribute(FormErrors.MODEL_ATTRIBUTE, FormErrors.of(binding));
            model.addAttribute("editing", false);
            return "recipes/form";
        }

        try {
            recipeApiClient.create(recipeFormMapper.toJson(form));
        } catch (PluginApiException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("editing", false);
            return "recipes/form";
        }
        redirectAttributes.addFlashAttribute("toastMessage", "Recipe created");
        return "redirect:/recipes";
    }

    @PutMapping("/recipes/{id}")
    public String update(@PathVariable String id, @Valid @ModelAttribute("form") RecipeForm form, BindingResult binding, Model model,
            RedirectAttributes redirectAttributes) {
        // Stop as soon as the form has errors: do not call the plugin API, re-render the
        // form so fragments/field-error shows the message under each field.
        if (binding.hasErrors()) {
            model.addAttribute(FormErrors.MODEL_ATTRIBUTE, FormErrors.of(binding));
            model.addAttribute("editing", true);
            return "recipes/form";
        }

        try {
            recipeApiClient.update(id, recipeFormMapper.toJson(form));
        } catch (PluginApiException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("editing", true);
            return "recipes/form";
        }
        redirectAttributes.addFlashAttribute("toastMessage", "Recipe saved");
        return "redirect:/recipes";
    }

    @DeleteMapping("/recipes/{id}")
    @ResponseBody
    public String delete(@PathVariable String id, HttpServletResponse response) {
        recipeApiClient.delete(id);
        response.setHeader("HX-Trigger", "{\"toast\":\"Recipe deleted\"}");
        return "";
    }

    @PostMapping("/recipes/ingredients/add-row")
    public String addIngredientRow(@ModelAttribute("form") RecipeForm form, Model model) {
        form.getIngredients().add(new IngredientRow());
        model.addAttribute("form", form);
        return "recipes/form :: ingredients-list";
    }

    @PostMapping("/recipes/ingredients/remove-row")
    public String removeIngredientRow(@ModelAttribute("form") RecipeForm form, @RequestParam int index,
            Model model) {
        if (index >= 0 && index < form.getIngredients().size()) {
            form.getIngredients().remove(index);
        }
        model.addAttribute("form", form);
        return "recipes/form :: ingredients-list";
    }
}
