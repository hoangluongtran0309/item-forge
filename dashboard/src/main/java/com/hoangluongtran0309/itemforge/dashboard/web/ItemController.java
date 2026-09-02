package com.hoangluongtran0309.itemforge.dashboard.web;

import java.io.IOException;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletResponse;

import com.hoangluongtran0309.itemforge.dashboard.client.ItemApiClient;
import com.hoangluongtran0309.itemforge.dashboard.client.PluginApiException;
import com.hoangluongtran0309.itemforge.dashboard.dto.ItemJson;
import com.hoangluongtran0309.itemforge.dashboard.dto.TextureUploadResponse;
import com.hoangluongtran0309.itemforge.dashboard.form.AbilityFormRow;
import com.hoangluongtran0309.itemforge.dashboard.form.ItemForm;
import com.hoangluongtran0309.itemforge.dashboard.mapper.ItemFormMapper;

@Controller
public class ItemController {

    private final ItemApiClient itemApiClient;
    private final ItemFormMapper itemFormMapper;

    public ItemController(ItemApiClient itemApiClient, ItemFormMapper itemFormMapper) {
        this.itemApiClient = itemApiClient;
        this.itemFormMapper = itemFormMapper;
    }

    @GetMapping("/items")
    public String list(Model model) {
        model.addAttribute("items", itemApiClient.findAll());
        return "items/list";
    }

    @GetMapping("/items/new")
    public String newForm(Model model) {
        model.addAttribute("form", new ItemForm());
        model.addAttribute("editing", false);
        return "items/form";
    }

    @GetMapping("/items/{id}/edit")
    public String editForm(@PathVariable String id, Model model) {
        ItemJson item = itemApiClient.findById(id);
        model.addAttribute("form", itemFormMapper.fromJson(item));
        model.addAttribute("editing", true);
        return "items/form";
    }

    @PostMapping("/items")
    public String create(@Valid @ModelAttribute("form") ItemForm form, BindingResult binding, Model model, RedirectAttributes redirectAttributes) {
        // Stop as soon as the form has errors: do not call the plugin API, re-render the
        // form so fragments/field-error shows the message under each field.
        if (binding.hasErrors()) {
            model.addAttribute(FormErrors.MODEL_ATTRIBUTE, FormErrors.of(binding));
            model.addAttribute("editing", false);
            return "items/form";
        }

        try {
            itemApiClient.create(itemFormMapper.toJson(form));
        } catch (PluginApiException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("editing", false);
            return "items/form";
        }
        redirectAttributes.addFlashAttribute("toastMessage", "Item created");
        return "redirect:/items";
    }

    @PutMapping("/items/{id}")
    public String update(@PathVariable String id, @Valid @ModelAttribute("form") ItemForm form, BindingResult binding, Model model,
            RedirectAttributes redirectAttributes) {
        // Stop as soon as the form has errors: do not call the plugin API, re-render the
        // form so fragments/field-error shows the message under each field.
        if (binding.hasErrors()) {
            model.addAttribute(FormErrors.MODEL_ATTRIBUTE, FormErrors.of(binding));
            model.addAttribute("editing", true);
            return "items/form";
        }

        try {
            itemApiClient.update(id, itemFormMapper.toJson(form));
        } catch (PluginApiException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("editing", true);
            return "items/form";
        }
        redirectAttributes.addFlashAttribute("toastMessage", "Item saved");
        return "redirect:/items";
    }

    @DeleteMapping("/items/{id}")
    @ResponseBody
    public String delete(@PathVariable String id, HttpServletResponse response) {
        itemApiClient.delete(id);
        response.setHeader("HX-Trigger", "{\"toast\":\"Item deleted\"}");
        return "";
    }

    @PostMapping("/items/generate")
    public String generate(@RequestParam String id, @RequestParam String description, Model model) {
        ItemJson draft = itemApiClient.generateDraft(id, description);
        model.addAttribute("form", itemFormMapper.fromJson(draft));
        return "items/form :: form-fields";
    }

    @PostMapping("/items/abilities/add-row")
    public String addAbilityRow(@ModelAttribute("form") ItemForm form, Model model) {
        form.getAbilities().add(new AbilityFormRow());
        model.addAttribute("form", form);
        return "items/form :: abilities-list";
    }

    @PostMapping("/items/abilities/remove-row")
    public String removeAbilityRow(@ModelAttribute("form") ItemForm form, @RequestParam int index, Model model) {
        if (index >= 0 && index < form.getAbilities().size()) {
            form.getAbilities().remove(index);
        }
        model.addAttribute("form", form);
        return "items/form :: abilities-list";
    }

    @PostMapping(value = "/items/{id}/texture", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String uploadTexture(@PathVariable String id, @RequestParam("file") MultipartFile file, Model model)
            throws IOException {
        try {
            TextureUploadResponse response = itemApiClient.uploadTexture(id, file.getBytes(),
                    file.getOriginalFilename());
            model.addAttribute("textureMessage", TextureMessages.success(response.sha1()));
        } catch (PluginApiException e) {
            model.addAttribute("textureError", e.getMessage());
        }
        return "items/form :: texture-upload-status";
    }

    // For the Pixel Art Editor's "Load existing" -- 404 when the item has no texture yet,
    // letting client-side JS decide (hiding the Load existing button) rather than failing
    // silently.
    @GetMapping(value = "/items/{id}/texture", produces = MediaType.IMAGE_PNG_VALUE)
    @ResponseBody
    public ResponseEntity<byte[]> downloadTexture(@PathVariable String id) {
        return TexturePngResponse.of(() -> itemApiClient.downloadTexture(id));
    }
}
