import { createDoc } from './doc.js';
import { createViewport } from './render.js';
import { getTool, TOOLS } from './tools.js';
import { installShortcuts } from './shortcuts.js';
import { decodePng } from './png.js';
import { mountPreview } from './preview.js';
import { mountPanels, setSaveState, showToast, confirmDialog } from './ui.js';

const MAX_BRUSH = 8;
const DRAFT_PREFIX = 'itemforge-studio-draft:';
const DRAFT_DEBOUNCE_MS = 2000;

export function createStudio(root, host) {
    return new Studio(root, host);
}

class Studio {
    constructor(root, host) {
        this.root = root;
        this.host = host;
        this.config = host.config;

        this.doc = createDoc({ width: this.config.width, height: this.config.height });
        this.primary = [0, 0, 0, 255];
        this.brushSize = 1;
        this.mirrorX = false;
        this.mirrorY = false;
        this.filledShapes = false;
        this.activeToolId = 'pencil';
        this._spaceHeld = false;
        this._panning = null;
        this._draftTimer = null;

        this.viewport = createViewport({
            wrapper: root.querySelector('[data-studio-viewport]'),
            stack: root.querySelector('[data-studio-stack]'),
            docCanvas: root.querySelector('[data-studio-canvas]'),
            overlayCanvas: root.querySelector('[data-studio-overlay]'),
            uiCanvas: root.querySelector('[data-studio-ui]'),
        }, this.doc);
        this.viewport.uvOverlay = this.config.uvOverlay || null;
        this.viewport.invalidateUi();

        this._panels = mountPanels(root, this);
        this._removeShortcuts = installShortcuts(this);
        this._wirePointer();
        this._wireDocChanges();
        this._mountPreview();

        this._loadInitialImage();
    }

    _mountPreview() {
        const container = this.root.querySelector('[data-studio-preview]');
        if (!container) {
            return;
        }
        this._preview = mountPreview(container, this.config.previewKind || 'item');

        // The preview does NOT composite again on its own: viewport.onFrame hands back the
        // very buffer it computed this frame, along with the dirty rect -- so a brush stroke
        // redraws only the faces whose UV intersects that region.
        this.viewport.onFrame((composite, dirtyRect) => {
            this._preview.update(composite, this.doc.width, this.doc.height, dirtyRect);
        });

        if (this.config.siblingLoadUrl && this._preview.setSiblingImage) {
            this._loadSiblingLayer();
        }
    }

    // The other armor layer (layer 1 while editing layer 2, and vice versa) is loaded ONCE,
    // purely so the preview matches the game. A 404 means it does not exist yet -- hide that
    // box tree.
    async _loadSiblingLayer() {
        try {
            const response = await fetch(this.config.siblingLoadUrl);
            if (!response.ok) {
                this._preview.setSiblingImage(null);
                return;
            }
            const { width, height, rgba } = await decodePng(await response.blob());
            this._preview.setSiblingImage(rgba, width, height);
        } catch (e) {
            this._preview.setSiblingImage(null);
        }
    }

    destroy() {
        this._removeShortcuts();
        this._panels.destroy();
        if (this._preview) {
            this._preview.destroy();
        }
        this.viewport.destroy();
        if (this._draftTimer !== null) {
            clearTimeout(this._draftTimer);
        }
    }

    // --- tool plumbing ---------------------------------------------------

    tool() {
        return getTool(this.activeToolId);
    }

    setTool(id) {
        if (!TOOLS.some((t) => t.id === id)) {
            return;
        }
        this.activeToolId = id;
        this._panels.refreshToolbar();
    }

    toolContext() {
        return {
            doc: this.doc,
            viewport: this.viewport,
            primary: this.primary,
            brushSize: this.brushSize,
            mirrorX: this.mirrorX,
            mirrorY: this.mirrorY,
            filledShapes: this.filledShapes,
            setPrimary: (rgba) => this.setPrimary(rgba),
            previewShape: (rasterise) => {
                const pixels = [];
                rasterise((x, y) => pixels.push({ x, y }));
                this.viewport.previewPixels = pixels;
                this.viewport.invalidateUi();
            },
            clearPreview: () => {
                this.viewport.previewPixels = null;
                this.viewport.invalidateUi();
            },
        };
    }

    // The color wheel calls in here while the user drags, AND this is also the path the
    // eyedropper and initialisation take. refreshColour() pushes the value back to the wheel
    // so the ring and square cursors always match -- the wheel breaks the loop itself, see
    // _emitting.
    setPrimary(rgba) {
        this.primary = rgba.slice();
        this._panels.refreshColour();
    }

    setBrushSize(size) {
        this.brushSize = Math.max(1, Math.min(MAX_BRUSH, size));
        this._panels.refreshToolbar();
    }

    toggleGrid() {
        this.viewport.showGrid = !this.viewport.showGrid;
        this.viewport.invalidateUi();
        this._panels.refreshToolbar();
    }

    setMirror(axis, on) {
        if (axis === 'x') {
            this.mirrorX = on;
            this.viewport.mirrorX = on;
        } else {
            this.mirrorY = on;
            this.viewport.mirrorY = on;
        }
        this.viewport.invalidateUi();
        this._panels.refreshToolbar();
    }

    selectAll() {
        this.doc.selection = this.doc.fullRect();
        this.viewport.invalidateUi();
    }

    deselect() {
        this.doc.selection = null;
        this.viewport.previewPixels = null;
        this.viewport.invalidateUi();
    }

    undo() {
        if (this.doc.undo()) {
            this._panels.refreshHistory();
        }
    }

    redo() {
        if (this.doc.redo()) {
            this._panels.refreshHistory();
        }
    }

    setSpaceHeld(held) {
        this._spaceHeld = held;
        const wrapper = this.root.querySelector('[data-studio-viewport]');
        wrapper.classList.toggle('cursor-grab', held);
    }

    // --- pointer ---------------------------------------------------------

    _wirePointer() {
        const wrapper = this.root.querySelector('[data-studio-viewport]');

        wrapper.addEventListener('pointerdown', (event) => {
            wrapper.setPointerCapture(event.pointerId);
            // Middle mouse or Space pans, whichever tool is selected.
            if (event.button === 1 || this._spaceHeld) {
                this._panning = { x: event.clientX, y: event.clientY };
                return;
            }
            const p = this.viewport.screenToPixel(event.clientX, event.clientY);
            this.tool().onDown(this.toolContext(), p, event);
        });

        wrapper.addEventListener('pointermove', (event) => {
            if (this._panning) {
                this.viewport.panBy(event.clientX - this._panning.x, event.clientY - this._panning.y);
                this._panning = { x: event.clientX, y: event.clientY };
                return;
            }

            const p = this.viewport.screenToPixel(event.clientX, event.clientY);
            this.viewport.cursor = { x: p.x, y: p.y, size: this.brushSize };
            this.viewport.invalidateUi();
            this._panels.refreshPosition(p);

            if (event.buttons > 0) {
                this.tool().onMove(this.toolContext(), p, event);
            }
        });

        const finish = (event) => {
            if (this._panning) {
                this._panning = null;
                return;
            }
            const p = this.viewport.screenToPixel(event.clientX, event.clientY);
            this.tool().onUp(this.toolContext(), p, event);
        };
        wrapper.addEventListener('pointerup', finish);
        wrapper.addEventListener('pointercancel', finish);

        wrapper.addEventListener('pointerleave', () => {
            this.viewport.cursor = null;
            this.viewport.invalidateUi();
        });

        wrapper.addEventListener('wheel', (event) => {
            event.preventDefault();
            const direction = event.deltaY < 0 ? 1 : -1;
            this.viewport.setZoom(this.viewport.zoom + direction, { x: event.clientX, y: event.clientY });
            this._panels.refreshToolbar();
        }, { passive: false });

        wrapper.addEventListener('contextmenu', (event) => event.preventDefault());
    }

    _wireDocChanges() {
        this.doc.subscribe(() => {
            this.host.notifyDirty(this.doc.isDirty());
            setSaveState(this.root, this.doc.isDirty());
            this._panels.refreshHistory();
            this._panels.refreshLayers();
            this._queueDraft();
        });
    }

    // --- images ----------------------------------------------------------

    async _loadInitialImage() {
        if (!this.config.loadUrl) {
            this._offerDraftIfAny();
            return;
        }
        const loaded = await this.loadFromUrl(this.config.loadUrl, { silent: true });
        // The document treats the freshly loaded state as "saved": the user has changed nothing yet.
        this.doc.markSaved();
        setSaveState(this.root, false);
        if (!loaded) {
            showToast(this.root, 'No texture saved yet - starting from a blank canvas.', 'info');
        }
        this._offerDraftIfAny();
    }

    async loadFromUrl(url, { silent = false } = {}) {
        try {
            // Cache-bust: the plugin sends no per-version ETag, so immediately after an upload
            // the browser could serve the old image from cache.
            const separator = url.includes('?') ? '&' : '?';
            const response = await fetch(url + separator + 't=' + Date.now());
            if (!response.ok) {
                return false;
            }
            const { width, height, rgba } = await decodePng(await response.blob());
            this.doc.putImageBytes(rgba, width, height, { label: 'Load existing' });
            this.viewport.syncSize();
            this.viewport.fitToWrapper();
            this._panels.refreshAll();
            return true;
        } catch (e) {
            if (!silent) {
                showToast(this.root, 'Could not load that image: ' + e.message, 'error');
            }
            return false;
        }
    }

    async importFile(file) {
        try {
            const { width, height, rgba } = await decodePng(file);
            const sizeChanged = width !== this.doc.width || height !== this.doc.height;
            this.doc.putImageBytes(rgba, width, height, {
                asNewLayer: !sizeChanged,
                label: 'Import ' + file.name,
            });
            this.viewport.syncSize();
            this.viewport.fitToWrapper();
            this._panels.refreshAll();
            showToast(this.root, sizeChanged
                ? `Imported ${file.name} and resized the canvas to ${width}x${height}.`
                : `Imported ${file.name} as a new layer.`, 'success');
        } catch (e) {
            showToast(this.root, 'Could not read that PNG: ' + e.message, 'error');
        }
    }

    async downloadPng() {
        const blob = await this.doc.toBlob();
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = (this.config.downloadName || 'texture') + '.png';
        link.click();
        URL.revokeObjectURL(url);
    }

    // --- save ------------------------------------------------------------

    async save() {
        setSaveState(this.root, this.doc.isDirty(), true);
        const blob = await this.doc.toBlob();
        const result = await this.host.save(blob);

        if (result.ok) {
            this.doc.markSaved();
            this._clearDraft();
            setSaveState(this.root, false);
            showToast(this.root, result.message || 'Texture saved. The resource pack was rebuilt.', 'success');
            if (this.config.closeOnSave) {
                this.host.close();
            }
            return;
        }
        setSaveState(this.root, true);
        showToast(this.root, result.message || 'Save failed.', 'error');
    }

    async requestClose() {
        if (!this.doc.isDirty()) {
            this.host.close();
            return;
        }
        const discard = await confirmDialog('Discard unsaved changes?',
            'The drawing has not been uploaded. It stays in this browser as a draft, but the server keeps the old texture.');
        if (discard) {
            this.host.close();
        }
    }

    // --- drafts ----------------------------------------------------------

    // Drafts live in localStorage only and are NEVER sent to the server: every upload drags
    // a rebuild, a rezip and a SHA-1 of the entire resource pack with it, which would make
    // server-side autosave very expensive. That is why the Studio has none.
    _draftKey() {
        return DRAFT_PREFIX + (this.config.draftKey || 'scratch');
    }

    _queueDraft() {
        if (this._draftTimer !== null) {
            clearTimeout(this._draftTimer);
        }
        this._draftTimer = setTimeout(() => {
            this._draftTimer = null;
            this._writeDraft();
        }, DRAFT_DEBOUNCE_MS);
    }

    _writeDraft() {
        if (!this.doc.isDirty()) {
            this._clearDraft();
            return;
        }
        try {
            localStorage.setItem(this._draftKey(), JSON.stringify({
                width: this.doc.width,
                height: this.doc.height,
                savedAt: Date.now(),
                layers: this.doc.layers.map((layer) => ({
                    name: layer.name,
                    visible: layer.visible,
                    opacity: layer.opacity,
                    data: bytesToBase64(layer.data),
                })),
            }));
        } catch (e) {
            // Quota exceeded: the draft is a convenience, not a contract -- ignore it.
        }
    }

    _clearDraft() {
        try {
            localStorage.removeItem(this._draftKey());
        } catch (e) {
            // localStorage is unavailable.
        }
    }

    async _offerDraftIfAny() {
        let draft;
        try {
            const raw = localStorage.getItem(this._draftKey());
            if (!raw) {
                return;
            }
            draft = JSON.parse(raw);
        } catch (e) {
            return;
        }
        if (!draft || !draft.layers || draft.layers.length === 0) {
            return;
        }

        const age = Math.round((Date.now() - draft.savedAt) / 60000);
        const restore = await confirmDialog('Restore unsaved work?',
            `A draft from about ${age} minute(s) ago was found for this texture. Restore it?`);
        if (!restore) {
            this._clearDraft();
            return;
        }

        this.doc.resizeTo(draft.width, draft.height);
        this.doc.layers = draft.layers.map((saved, index) => ({
            id: 'draft' + index,
            name: saved.name,
            visible: saved.visible,
            opacity: saved.opacity,
            data: base64ToBytes(saved.data),
        }));
        this.doc.activeIndex = this.doc.layers.length - 1;
        this.doc.revision += 1;
        this.viewport.syncSize();
        this.viewport.fitToWrapper();
        this.viewport.invalidate(this.doc.fullRect());
        this._panels.refreshAll();
    }
}

function bytesToBase64(bytes) {
    let binary = '';
    const chunkSize = 0x8000;
    for (let i = 0; i < bytes.length; i += chunkSize) {
        binary += String.fromCharCode.apply(null, bytes.subarray(i, i + chunkSize));
    }
    return btoa(binary);
}

function base64ToBytes(base64) {
    const binary = atob(base64);
    const bytes = new Uint8ClampedArray(binary.length);
    for (let i = 0; i < binary.length; i += 1) {
        bytes[i] = binary.charCodeAt(i);
    }
    return bytes;
}
