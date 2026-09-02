// A host is where the Studio is embedded and how it SAVES. Both hosts share one contract,
// so the workspace has no idea whether it is inside a modal or on its own page:
//
//   { config, save(blob) -> Promise<{ok, message}>, close(), onDirtyChange(fn) }

/**
 * The modal host keeps the previous editor's trick: pour the drawn image into the existing
 * <input type=file>'s buffer, then click the already hx-post-wired Upload button on the
 * user's behalf. That way the Studio writes NOT ONE line of network code, and inherits CSRF,
 * the spinner, hx-disabled-elt and the exact error/success rendering of a manual upload.
 */
export function createModalHost(config) {
    let dirtyListener = null;

    return {
        config,

        save(blob) {
            return new Promise((resolve) => {
                const input = document.getElementById(config.inputId);
                const uploadBtn = document.getElementById(config.uploadBtnId);
                if (!input || !uploadBtn) {
                    resolve({ ok: false, message: 'Upload target is missing on this page.' });
                    return;
                }

                const file = new File([blob], 'drawn.png', { type: 'image/png' });
                const transfer = new DataTransfer();
                transfer.items.add(file);
                input.files = transfer.files;

                if (window.ItemForgeTexture && config.previewImgId) {
                    window.ItemForgeTexture.preview(file, config.previewImgId);
                }

                const onDone = () => {
                    uploadBtn.removeEventListener('htmx:afterRequest', onDone);
                    // The key point: the controller returns HTTP 200 WITH a textureError when
                    // the plugin rejects the image (wrong dimensions, ...). Trusting
                    // afterRequest alone would treat a failed save as a success and silently
                    // lose the user's drawing -- so the swapped-in status fragment has to be
                    // read.
                    const status = config.statusId ? document.getElementById(config.statusId) : null;
                    const error = status ? status.querySelector('.alert-error') : null;
                    if (error) {
                        resolve({ ok: false, message: error.textContent.trim() });
                    } else {
                        resolve({ ok: true });
                    }
                };
                uploadBtn.addEventListener('htmx:afterRequest', onDone);
                uploadBtn.click();
            });
        },

        close() {
            const modal = document.getElementById('studio-modal');
            if (modal && typeof modal.close === 'function') {
                modal.close();
            }
        },

        onDirtyChange(listener) {
            dirtyListener = listener;
        },

        notifyDirty(isDirty) {
            if (dirtyListener) {
                dirtyListener(isDirty);
            }
        },
    };
}

/**
 * The page host for the /studio route: it POSTs multipart itself with the CSRF token read
 * from the meta tag, like htmx-csrf.js. It also attaches beforeunload according to the dirty
 * state -- added and removed on each change rather than left permanently attached, so
 * navigating away with no changes does not prompt.
 */
export function createPageHost(config) {
    let dirtyListener = null;
    let guardInstalled = false;

    const guard = (event) => {
        event.preventDefault();
        // Modern browsers ignore this string's content, but returnValue still has to be set
        // for the confirmation dialog to appear at all.
        event.returnValue = '';
    };

    return {
        config,

        async save(blob) {
            if (!config.uploadUrl) {
                return { ok: false, message: 'This canvas has no upload target (scratch mode).' };
            }
            const body = new FormData();
            body.append('file', new File([blob], 'drawn.png', { type: 'image/png' }));

            const tokenMeta = document.querySelector('meta[name="_csrf"]');
            const headerMeta = document.querySelector('meta[name="_csrf_header"]');
            const headers = {};
            if (tokenMeta && headerMeta) {
                headers[headerMeta.content] = tokenMeta.content;
            }

            try {
                const response = await fetch(config.uploadUrl, { method: 'POST', body, headers });
                const html = await response.text();
                if (!response.ok) {
                    return { ok: false, message: 'Upload failed with HTTP ' + response.status };
                }
                // The endpoint returns the same status fragment as the htmx path, so errors are
                // read the same way: a 200 does not mean it saved.
                const parsed = new DOMParser().parseFromString(html, 'text/html');
                const error = parsed.querySelector('.alert-error');
                if (error) {
                    return { ok: false, message: error.textContent.trim() };
                }
                const success = parsed.querySelector('.alert-success');
                return { ok: true, message: success ? success.textContent.trim() : undefined };
            } catch (e) {
                return { ok: false, message: 'Could not reach the dashboard: ' + e.message };
            }
        },

        close() {
            if (config.backUrl) {
                window.location.href = config.backUrl;
            }
        },

        onDirtyChange(listener) {
            dirtyListener = listener;
        },

        notifyDirty(isDirty) {
            if (isDirty && !guardInstalled) {
                window.addEventListener('beforeunload', guard);
                guardInstalled = true;
            } else if (!isDirty && guardInstalled) {
                window.removeEventListener('beforeunload', guard);
                guardInstalled = false;
            }
            if (dirtyListener) {
                dirtyListener(isDirty);
            }
        },
    };
}
