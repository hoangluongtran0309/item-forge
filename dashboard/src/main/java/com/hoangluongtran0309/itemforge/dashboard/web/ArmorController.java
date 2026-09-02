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

import com.hoangluongtran0309.itemforge.dashboard.client.ArmorApiClient;
import com.hoangluongtran0309.itemforge.dashboard.client.PluginApiException;
import com.hoangluongtran0309.itemforge.dashboard.dto.ArmorJson;
import com.hoangluongtran0309.itemforge.dashboard.dto.TextureUploadResponse;
import com.hoangluongtran0309.itemforge.dashboard.form.ArmorForm;
import com.hoangluongtran0309.itemforge.dashboard.mapper.ArmorFormMapper;

@Controller
public class ArmorController {

    private final ArmorApiClient armorApiClient;
    private final ArmorFormMapper armorFormMapper;

    public ArmorController(ArmorApiClient armorApiClient, ArmorFormMapper armorFormMapper) {
        this.armorApiClient = armorApiClient;
        this.armorFormMapper = armorFormMapper;
    }

    @GetMapping("/armor")
    public String list(Model model) {
        model.addAttribute("armorPieces", armorApiClient.findAll());
        return "armor/list";
    }

    @GetMapping("/armor/new")
    public String newForm(Model model) {
        model.addAttribute("form", new ArmorForm());
        model.addAttribute("editing", false);
        return "armor/form";
    }

    @GetMapping("/armor/{id}/edit")
    public String editForm(@PathVariable String id, Model model) {
        ArmorJson armor = armorApiClient.findById(id);
        model.addAttribute("form", armorFormMapper.fromJson(armor));
        model.addAttribute("editing", true);
        return "armor/form";
    }

    @PostMapping("/armor")
    public String create(@Valid @ModelAttribute("form") ArmorForm form, BindingResult binding, Model model, RedirectAttributes redirectAttributes) {
        // Stop as soon as the form has errors: do not call the plugin API, re-render the
        // form so fragments/field-error shows the message under each field.
        if (binding.hasErrors()) {
            model.addAttribute(FormErrors.MODEL_ATTRIBUTE, FormErrors.of(binding));
            model.addAttribute("editing", false);
            return "armor/form";
        }

        try {
            armorApiClient.create(armorFormMapper.toJson(form));
        } catch (PluginApiException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("editing", false);
            return "armor/form";
        }
        redirectAttributes.addFlashAttribute("toastMessage", "Armor piece created");
        return "redirect:/armor";
    }

    @PutMapping("/armor/{id}")
    public String update(@PathVariable String id, @Valid @ModelAttribute("form") ArmorForm form, BindingResult binding, Model model,
            RedirectAttributes redirectAttributes) {
        // Stop as soon as the form has errors: do not call the plugin API, re-render the
        // form so fragments/field-error shows the message under each field.
        if (binding.hasErrors()) {
            model.addAttribute(FormErrors.MODEL_ATTRIBUTE, FormErrors.of(binding));
            model.addAttribute("editing", true);
            return "armor/form";
        }

        try {
            armorApiClient.update(id, armorFormMapper.toJson(form));
        } catch (PluginApiException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("editing", true);
            return "armor/form";
        }
        redirectAttributes.addFlashAttribute("toastMessage", "Armor piece saved");
        return "redirect:/armor";
    }

    @DeleteMapping("/armor/{id}")
    @ResponseBody
    public String delete(@PathVariable String id, HttpServletResponse response) {
        armorApiClient.delete(id);
        response.setHeader("HX-Trigger", "{\"toast\":\"Armor piece deleted\"}");
        return "";
    }

    @PostMapping(value = "/armor/{id}/icon", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String uploadIcon(@PathVariable String id, @RequestParam("file") MultipartFile file, Model model)
            throws IOException {
        try {
            TextureUploadResponse response = armorApiClient.uploadIcon(id, file.getBytes(),
                    file.getOriginalFilename());
            model.addAttribute("iconMessage", TextureMessages.success(response.sha1()));
        } catch (PluginApiException e) {
            model.addAttribute("iconError", e.getMessage());
        }
        return "armor/form :: icon-upload-status";
    }

    @PostMapping(value = "/armor/{id}/texture", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String uploadLayerTexture(@PathVariable String id, @RequestParam String layer,
            @RequestParam("file") MultipartFile file, Model model) throws IOException {
        boolean isLeggings = "humanoid_leggings".equals(layer);
        try {
            TextureUploadResponse response = armorApiClient.uploadLayer(id, layer, file.getBytes(),
                    file.getOriginalFilename());
            model.addAttribute(isLeggings ? "layer2Message" : "layer1Message", TextureMessages.success(response.sha1()));
        } catch (PluginApiException e) {
            model.addAttribute(isLeggings ? "layer2Error" : "layer1Error", e.getMessage());
        }
        return isLeggings ? "armor/form :: layer2-upload-status" : "armor/form :: layer1-upload-status";
    }

    // For the Pixel Art Editor's "Load existing" -- see the equivalent note in ItemController
    @GetMapping(value = "/armor/{id}/icon", produces = MediaType.IMAGE_PNG_VALUE)
    @ResponseBody
    public ResponseEntity<byte[]> downloadIcon(@PathVariable String id) {
        return TexturePngResponse.of(() -> armorApiClient.downloadIcon(id));
    }

    @GetMapping(value = "/armor/{id}/texture", produces = MediaType.IMAGE_PNG_VALUE)
    @ResponseBody
    public ResponseEntity<byte[]> downloadLayerTexture(@PathVariable String id, @RequestParam String layer) {
        return TexturePngResponse.of(() -> armorApiClient.downloadLayer(id, layer));
    }
}
