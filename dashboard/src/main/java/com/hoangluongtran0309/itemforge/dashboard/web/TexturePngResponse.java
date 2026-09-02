package com.hoangluongtran0309.itemforge.dashboard.web;

import java.util.function.Supplier;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.DigestUtils;

import com.hoangluongtran0309.itemforge.dashboard.client.PluginApiException;

/**
 * Shared by the five GET texture endpoints (item, armor icon, two armor layers, block).
 * The grid cards now load real images, so one page load produces N image requests; the
 * plugin's HttpBinary sends no ETag or Cache-Control at all, so caching is added here --
 * the dashboard is that API's only consumer.
 *
 * <p>noCache() rather than noStore(): the browser still revalidates and gets a 304 with no
 * body on later loads, while the {@code ?t=<timestamp>} cache-bust parameter the editor
 * already uses still forces a reload immediately after an upload.
 */
final class TexturePngResponse {

    private TexturePngResponse() {
    }

    static ResponseEntity<byte[]> of(Supplier<byte[]> download) {
        try {
            byte[] bytes = download.get();
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .eTag("\"" + DigestUtils.md5DigestAsHex(bytes) + "\"")
                    .cacheControl(CacheControl.noCache().cachePrivate())
                    .body(bytes);
        } catch (PluginApiException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
