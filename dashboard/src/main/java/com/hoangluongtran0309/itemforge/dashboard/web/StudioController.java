package com.hoangluongtran0309.itemforge.dashboard.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import com.hoangluongtran0309.itemforge.dashboard.client.ArmorApiClient;
import com.hoangluongtran0309.itemforge.dashboard.client.BlockApiClient;
import com.hoangluongtran0309.itemforge.dashboard.client.ItemApiClient;
import com.hoangluongtran0309.itemforge.dashboard.dto.ArmorJson;
import com.hoangluongtran0309.itemforge.dashboard.dto.BlockJson;
import com.hoangluongtran0309.itemforge.dashboard.dto.ItemJson;

/**
 * The full-page Texture Studio. The same workspace as the modal (the
 * fragments/studio-workspace fragment), differing only in being deep-linkable and
 * shareable by URL.
 *
 * <p>Uses discrete `type` + `id` + `layer` parameters rather than one composite target
 * string like "item:void_sword": easier to bind, to validate, and to test.
 */
@Controller
public class StudioController {

    private static final int ICON_SIZE = 16;
    private static final int ARMOR_LAYER_WIDTH = 64;
    private static final int ARMOR_LAYER_HEIGHT = 32;
    private static final String LAYER_BODY = "humanoid";
    private static final String LAYER_LEGGINGS = "humanoid_leggings";

    private final ItemApiClient itemApiClient;
    private final ArmorApiClient armorApiClient;
    private final BlockApiClient blockApiClient;

    public StudioController(ItemApiClient itemApiClient, ArmorApiClient armorApiClient,
            BlockApiClient blockApiClient) {
        this.itemApiClient = itemApiClient;
        this.armorApiClient = armorApiClient;
        this.blockApiClient = blockApiClient;
    }

    @GetMapping("/studio")
    public String studio(@RequestParam(required = false) String type,
            @RequestParam(required = false) String id,
            @RequestParam(required = false) String layer,
            Model model) {

        // The Studio takes the whole viewport: layout/base.html drops <main>'s padding when
        // it sees this flag.
        model.addAttribute("fullBleed", true);
        model.addAttribute("siblingLoadUrl", null);

        if (type == null || type.isBlank()) {
            return scratch(model);
        }
        if (id == null || id.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing required parameter: id");
        }

        switch (type) {
            case "item" -> item(id, model);
            case "block" -> block(id, model);
            case "armor-icon" -> armorIcon(id, model);
            case "armor-layer" -> armorLayer(id, layer, model);
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Unknown studio target type '" + type + "' (expected item, block, armor-icon or armor-layer)");
        }
        return "studio";
    }

    // A blank canvas with no upload destination -- just draw and download the PNG. It
    // still starts at 16x16; the user can drag in any PNG to change that.
    private String scratch(Model model) {
        model.addAttribute("studioTitle", "Scratch canvas");
        model.addAttribute("studioSubtitle", "Not linked to any item - draw and download the PNG");
        model.addAttribute("docWidth", ICON_SIZE);
        model.addAttribute("docHeight", ICON_SIZE);
        model.addAttribute("loadUrl", null);
        model.addAttribute("uploadUrl", null);
        model.addAttribute("uvOverlay", null);
        model.addAttribute("previewKind", "item");
        model.addAttribute("backUrl", "/");
        model.addAttribute("draftKey", "scratch");
        model.addAttribute("downloadName", "texture");
        return "studio";
    }

    private void item(String id, Model model) {
        ItemJson item = itemApiClient.findById(id);
        model.addAttribute("studioTitle", item.displayName());
        model.addAttribute("studioSubtitle", item.id() + " - " + item.material());
        model.addAttribute("docWidth", ICON_SIZE);
        model.addAttribute("docHeight", ICON_SIZE);
        model.addAttribute("loadUrl", "/items/" + id + "/texture");
        model.addAttribute("uploadUrl", "/items/" + id + "/texture");
        model.addAttribute("uvOverlay", null);
        model.addAttribute("previewKind", "item");
        model.addAttribute("backUrl", "/items/" + id + "/edit");
        model.addAttribute("draftKey", "item:" + id);
        model.addAttribute("downloadName", id);
    }

    private void block(String id, Model model) {
        BlockJson block = blockApiClient.findById(id);
        model.addAttribute("studioTitle", block.displayName());
        model.addAttribute("studioSubtitle", "texture id " + block.textureId() + " - all six faces");
        model.addAttribute("docWidth", ICON_SIZE);
        model.addAttribute("docHeight", ICON_SIZE);
        model.addAttribute("loadUrl", "/blocks/" + id + "/texture");
        model.addAttribute("uploadUrl", "/blocks/" + id + "/texture");
        model.addAttribute("uvOverlay", null);
        model.addAttribute("previewKind", "cube");
        model.addAttribute("backUrl", "/blocks/" + id + "/edit");
        model.addAttribute("draftKey", "block:" + id);
        model.addAttribute("downloadName", block.textureId());
    }

    private void armorIcon(String id, Model model) {
        ArmorJson armor = armorApiClient.findById(id);
        model.addAttribute("studioTitle", armor.displayName() + " - inventory icon");
        model.addAttribute("studioSubtitle", armor.id() + " - " + armor.material());
        model.addAttribute("docWidth", ICON_SIZE);
        model.addAttribute("docHeight", ICON_SIZE);
        model.addAttribute("loadUrl", "/armor/" + id + "/icon");
        model.addAttribute("uploadUrl", "/armor/" + id + "/icon");
        model.addAttribute("uvOverlay", null);
        model.addAttribute("previewKind", "item");
        model.addAttribute("backUrl", "/armor/" + id + "/edit");
        model.addAttribute("draftKey", "armor-icon:" + id);
        model.addAttribute("downloadName", id);
    }

    private void armorLayer(String id, String layer, Model model) {
        if (!LAYER_BODY.equals(layer) && !LAYER_LEGGINGS.equals(layer)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid layer '" + layer + "' (expected '" + LAYER_BODY + "' or '" + LAYER_LEGGINGS + "')");
        }
        ArmorJson armor = armorApiClient.findById(id);
        boolean leggings = LAYER_LEGGINGS.equals(layer);
        String sibling = leggings ? LAYER_BODY : LAYER_LEGGINGS;

        model.addAttribute("studioTitle",
                armor.displayName() + (leggings ? " - leggings layer" : " - body layer"));
        // armorAssetId has to be stated explicitly: the layer is shared by the WHOLE SET, so
        // editing it here also changes every other piece with the same asset id.
        model.addAttribute("studioSubtitle",
                "asset id " + armor.armorAssetId() + " - shared by every piece in the set");
        model.addAttribute("docWidth", ARMOR_LAYER_WIDTH);
        model.addAttribute("docHeight", ARMOR_LAYER_HEIGHT);
        model.addAttribute("loadUrl", "/armor/" + id + "/texture?layer=" + layer);
        model.addAttribute("uploadUrl", "/armor/" + id + "/texture?layer=" + layer);
        // The other layer is loaded for PREVIEW only: the 3D model has to show both layers
        // to match what the game renders.
        model.addAttribute("siblingLoadUrl", "/armor/" + id + "/texture?layer=" + sibling);
        model.addAttribute("uvOverlay", layer);
        model.addAttribute("previewKind", leggings ? "humanoid_leggings" : "humanoid");
        model.addAttribute("backUrl", "/armor/" + id + "/edit");
        model.addAttribute("draftKey", "armor-" + layer + ":" + id);
        model.addAttribute("downloadName", armor.armorAssetId() + (leggings ? "_layer_2" : "_layer_1"));
    }
}
