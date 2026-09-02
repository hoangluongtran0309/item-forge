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

import com.hoangluongtran0309.itemforge.dashboard.client.BlockApiClient;
import com.hoangluongtran0309.itemforge.dashboard.client.PluginApiException;
import com.hoangluongtran0309.itemforge.dashboard.dto.BlockJson;
import com.hoangluongtran0309.itemforge.dashboard.dto.TextureUploadResponse;
import com.hoangluongtran0309.itemforge.dashboard.form.BlockForm;
import com.hoangluongtran0309.itemforge.dashboard.mapper.BlockFormMapper;

@Controller
public class BlockController {

    private final BlockApiClient blockApiClient;
    private final BlockFormMapper blockFormMapper;

    public BlockController(BlockApiClient blockApiClient, BlockFormMapper blockFormMapper) {
        this.blockApiClient = blockApiClient;
        this.blockFormMapper = blockFormMapper;
    }

    @GetMapping("/blocks")
    public String list(Model model) {
        model.addAttribute("blocks", blockApiClient.findAll());
        return "blocks/list";
    }

    @GetMapping("/blocks/new")
    public String newForm(Model model) {
        model.addAttribute("form", new BlockForm());
        model.addAttribute("editing", false);
        return "blocks/form";
    }

    @GetMapping("/blocks/{id}/edit")
    public String editForm(@PathVariable String id, Model model) {
        BlockJson block = blockApiClient.findById(id);
        model.addAttribute("form", blockFormMapper.fromJson(block));
        model.addAttribute("editing", true);
        return "blocks/form";
    }

    @PostMapping("/blocks")
    public String create(@Valid @ModelAttribute("form") BlockForm form, BindingResult binding, Model model,
            RedirectAttributes redirectAttributes) {
        // Stop as soon as the form has errors: do not call the plugin API, re-render the
        // form so fragments/field-error shows the message under each field.
        if (binding.hasErrors()) {
            model.addAttribute(FormErrors.MODEL_ATTRIBUTE, FormErrors.of(binding));
            model.addAttribute("editing", false);
            return "blocks/form";
        }

        try {
            blockApiClient.create(blockFormMapper.toJson(form));
        } catch (PluginApiException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("editing", false);
            return "blocks/form";
        }
        redirectAttributes.addFlashAttribute("toastMessage", "Custom block created");
        return "redirect:/blocks";
    }

    @PutMapping("/blocks/{id}")
    public String update(@PathVariable String id, @Valid @ModelAttribute("form") BlockForm form, BindingResult binding, Model model,
            RedirectAttributes redirectAttributes) {
        // Stop as soon as the form has errors: do not call the plugin API, re-render the
        // form so fragments/field-error shows the message under each field.
        if (binding.hasErrors()) {
            model.addAttribute(FormErrors.MODEL_ATTRIBUTE, FormErrors.of(binding));
            model.addAttribute("editing", true);
            return "blocks/form";
        }

        try {
            blockApiClient.update(id, blockFormMapper.toJson(form));
        } catch (PluginApiException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("editing", true);
            return "blocks/form";
        }
        redirectAttributes.addFlashAttribute("toastMessage", "Custom block saved");
        return "redirect:/blocks";
    }

    @DeleteMapping("/blocks/{id}")
    @ResponseBody
    public String delete(@PathVariable String id, HttpServletResponse response) {
        blockApiClient.delete(id);
        response.setHeader("HX-Trigger", "{\"toast\":\"Custom block deleted\"}");
        return "";
    }

    @PostMapping(value = "/blocks/{id}/texture", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String uploadTexture(@PathVariable String id, @RequestParam("file") MultipartFile file, Model model)
            throws IOException {
        try {
            TextureUploadResponse response = blockApiClient.uploadTexture(id, file.getBytes(),
                    file.getOriginalFilename());
            model.addAttribute("textureMessage", TextureMessages.success(response.sha1()));
        } catch (PluginApiException e) {
            model.addAttribute("textureError", e.getMessage());
        }
        return "blocks/form :: texture-upload-status";
    }

    // For the Studio's "Load existing" and the thumbnails on the list page -- 404 when the
    // block has no texture yet, letting the client decide (falling back to a letter) rather
    // than failing silently. Same contract as ItemController.downloadTexture.
    @GetMapping(value = "/blocks/{id}/texture", produces = MediaType.IMAGE_PNG_VALUE)
    @ResponseBody
    public ResponseEntity<byte[]> downloadTexture(@PathVariable String id) {
        return TexturePngResponse.of(() -> blockApiClient.downloadTexture(id));
    }
}
