package com.hoangluongtran0309.itemforge.dashboard.web;

import java.io.IOException;
import java.util.List;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.hoangluongtran0309.itemforge.dashboard.service.ReferenceLibraryService;
import com.hoangluongtran0309.itemforge.dashboard.service.ReferenceLibraryService.ReferenceTexture;

/**
 * The reference texture library. Entirely local to the dashboard: it calls no plugin API
 * and triggers no resource pack rebuild.
 */
@Controller
public class ReferenceLibraryController {

    private static final int SEARCH_LIMIT = 300;

    private final ReferenceLibraryService referenceLibrary;

    public ReferenceLibraryController(ReferenceLibraryService referenceLibrary) {
        this.referenceLibrary = referenceLibrary;
    }

    @GetMapping("/reference")
    public String index(@RequestParam(required = false) String q, Model model) {
        model.addAttribute("packs", referenceLibrary.packs());
        model.addAttribute("textures", referenceLibrary.search(q, SEARCH_LIMIT));
        model.addAttribute("query", q == null ? "" : q);
        model.addAttribute("searchLimit", SEARCH_LIMIT);
        return "reference/index";
    }

    /** The results grid as a fragment, for the htmx search box. */
    @GetMapping("/reference/search")
    public String search(@RequestParam(required = false) String q, Model model) {
        model.addAttribute("textures", referenceLibrary.search(q, SEARCH_LIMIT));
        model.addAttribute("searchLimit", SEARCH_LIMIT);
        return "reference/index :: texture-grid";
    }

    /** JSON for the Studio's "Import from reference" picker. */
    @GetMapping("/reference/api/search")
    @ResponseBody
    public List<ReferenceTexture> searchJson(@RequestParam(required = false) String q) {
        return referenceLibrary.search(q, SEARCH_LIMIT);
    }

    @PostMapping("/reference/import")
    public String importPack(@RequestParam("file") MultipartFile file,
            RedirectAttributes redirectAttributes) throws IOException {
        try {
            var result = referenceLibrary.importPack(file.getOriginalFilename(), file.getInputStream());
            redirectAttributes.addFlashAttribute("toastMessage",
                    "Imported " + result.imported() + " textures into '" + result.pack() + "'.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorBanner", e.getMessage());
        }
        return "redirect:/reference";
    }

    @DeleteMapping("/reference/{pack}")
    @ResponseBody
    public String deletePack(@PathVariable String pack, jakarta.servlet.http.HttpServletResponse response) {
        referenceLibrary.deletePack(pack);
        response.setHeader("HX-Trigger", "{\"toast\":\"Reference pack removed\"}");
        response.setHeader("HX-Refresh", "true");
        return "";
    }

    // The sub-path lives in the remainder of the URL, so a single @PathVariable will not
    // do; ReferenceLibraryService re-checks that it stays inside the pack directory before
    // reading.
    @GetMapping(value = "/reference/file/{pack}/**", produces = MediaType.IMAGE_PNG_VALUE)
    @ResponseBody
    public ResponseEntity<byte[]> file(@PathVariable String pack,
            jakarta.servlet.http.HttpServletRequest request) {
        String prefix = "/reference/file/" + pack + "/";
        String uri = request.getRequestURI();
        int start = uri.indexOf(prefix);
        if (start < 0) {
            return ResponseEntity.notFound().build();
        }
        String relative = uri.substring(start + prefix.length());

        try {
            return referenceLibrary.read(pack, relative)
                    .map(bytes -> ResponseEntity.ok()
                            .contentType(MediaType.IMAGE_PNG)
                            // Reference assets never change in place, so they can be cached for a long time.
                            .cacheControl(CacheControl.maxAge(java.time.Duration.ofDays(7)).cachePrivate())
                            .body(bytes))
                    .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
