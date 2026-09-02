import { rectUnion } from './geom.js';
import { encodePng } from './png.js';

// The Studio document: size, layers, selection, and the undo/redo history.
//
// Every layer holds its own pixels in a STRAIGHT-alpha Uint8ClampedArray that we own; the
// canvas is used for DISPLAY only. The reason: a 2D canvas's backing store is premultiplied
// in every major browser, so putImageData -> getImageData does NOT round-trip values with
// alpha < 255. For an editor with an alpha slider that is an unacceptable loss, so the
// canvas is never the source of truth. (The previous editor used the canvas's ImageData as
// its source, and that was its silent bug.)
//
// The history is a list of commands, and each command stores only the affected rectangle
// (before/after) rather than a snapshot of the whole image. A 16x16 brush stroke costs
// ~36 bytes per side, which makes "unlimited undo" literal rather than aspirational: the
// limit is a BYTE budget, not a step count.
const HISTORY_BYTE_BUDGET = 64 * 1024 * 1024;

export function createDoc({ width, height, layerName = 'Layer 1' }) {
    return new Doc(width, height, layerName);
}

function createLayer(id, name, width, height) {
    return {
        id,
        name,
        visible: true,
        opacity: 1,
        data: new Uint8ClampedArray(width * height * 4),
    };
}

function layerMeta(layer) {
    return { id: layer.id, name: layer.name, visible: layer.visible, opacity: layer.opacity };
}

class Doc {
    constructor(width, height, layerName) {
        this.width = width;
        this.height = height;
        this._nextLayerId = 1;
        this.layers = [createLayer(this._takeLayerId(), layerName, width, height)];
        this.activeIndex = 0;
        this.selection = null;
        this.revision = 0;

        this._history = [];
        this._historyIndex = 0;
        this._savedHistoryIndex = 0;
        this._historyBytes = 0;
        this._listeners = new Set();
        this._stroke = null;
    }

    _takeLayerId() {
        const id = 'l' + this._nextLayerId;
        this._nextLayerId += 1;
        return id;
    }

    // --- observers -------------------------------------------------------

    subscribe(listener) {
        this._listeners.add(listener);
        return () => this._listeners.delete(listener);
    }

    _emit(type, rect) {
        this.revision += 1;
        const event = { type, rect: rect || this.fullRect() };
        this._listeners.forEach((listener) => listener(event));
    }

    fullRect() {
        return { x: 0, y: 0, w: this.width, h: this.height };
    }

    // --- layers ----------------------------------------------------------

    activeLayer() {
        return this.layers[this.activeIndex];
    }

    layerById(id) {
        return this.layers.find((layer) => layer.id === id) || null;
    }

    addLayer(name) {
        const layer = createLayer(this._takeLayerId(), name || 'Layer ' + (this.layers.length + 1),
            this.width, this.height);
        const insertAt = this.activeIndex + 1;
        this._applyStructural('Add layer', () => {
            this.layers.splice(insertAt, 0, layer);
            this.activeIndex = insertAt;
        });
    }

    removeLayer(id) {
        if (this.layers.length <= 1) {
            return false;
        }
        const index = this.layers.findIndex((layer) => layer.id === id);
        if (index < 0) {
            return false;
        }
        this._applyStructural('Delete layer', () => {
            this.layers.splice(index, 1);
            this.activeIndex = Math.min(this.activeIndex, this.layers.length - 1);
        });
        return true;
    }

    moveLayer(id, delta) {
        const from = this.layers.findIndex((layer) => layer.id === id);
        const to = from + delta;
        if (from < 0 || to < 0 || to >= this.layers.length) {
            return false;
        }
        this._applyStructural('Reorder layers', () => {
            const [layer] = this.layers.splice(from, 1);
            this.layers.splice(to, 0, layer);
            this.activeIndex = to;
        });
        return true;
    }

    setLayerVisible(id, visible) {
        const layer = this.layerById(id);
        if (!layer || layer.visible === visible) {
            return;
        }
        this._applyStructural(visible ? 'Show layer' : 'Hide layer', () => {
            layer.visible = visible;
        });
    }

    setLayerOpacity(id, opacity) {
        const layer = this.layerById(id);
        if (!layer) {
            return;
        }
        const clamped = Math.max(0, Math.min(1, opacity));
        if (layer.opacity === clamped) {
            return;
        }
        this._applyStructural('Layer opacity', () => {
            layer.opacity = clamped;
        });
    }

    renameLayer(id, name) {
        const layer = this.layerById(id);
        if (!layer || layer.name === name) {
            return;
        }
        this._applyStructural('Rename layer', () => {
            layer.name = name;
        });
    }

    // Merges the selected layer into the one directly below. This is a SINGLE command
    // holding one pixel patch plus one structural patch, so a single Ctrl+Z undoes both.
    mergeDown() {
        if (this.activeIndex === 0) {
            return false;
        }
        const top = this.layers[this.activeIndex];
        const below = this.layers[this.activeIndex - 1];
        const rect = this.fullRect();

        const pixelPatch = {
            kind: 'pixels',
            layerId: below.id,
            rect,
            before: below.data.slice(),
            after: null,
        };

        const merged = below.data.slice();
        compositeOnto(merged, top.data, this.width, this.height, top.opacity, top.visible);
        pixelPatch.after = merged;

        const structural = this._structuralPatch(() => {
            this.layers.splice(this.activeIndex, 1);
            this.activeIndex -= 1;
        });

        this._pushCommand({
            label: 'Merge down',
            patches: [pixelPatch, structural],
            bytes: pixelPatch.before.length * 2,
        });
        below.data.set(merged);
        this._emit('merge', rect);
        return true;
    }

    // --- pixel writes ----------------------------------------------------

    // A "stroke" covers every pixel change between beginStroke/endStroke and becomes
    // EXACTLY ONE undo step. Original pixels are memoised lazily the first time each cell is
    // touched, so the cost scales with the number of cells actually changed.
    beginStroke(label, layerId) {
        const layer = layerId ? this.layerById(layerId) : this.activeLayer();
        if (!layer) {
            return;
        }
        this._stroke = { label: label || 'Paint', layer, originals: new Map(), rect: null };
    }

    strokeActive() {
        return this._stroke !== null;
    }

    setPixel(x, y, rgba) {
        if (x < 0 || y < 0 || x >= this.width || y >= this.height) {
            return;
        }
        const stroke = this._stroke;
        if (!stroke) {
            return;
        }
        const layer = stroke.layer;
        const index = (y * this.width + x) * 4;

        if (!stroke.originals.has(index)) {
            stroke.originals.set(index, [
                layer.data[index], layer.data[index + 1], layer.data[index + 2], layer.data[index + 3],
            ]);
        }
        // Overwrite rather than blend: in pixel art, "paint this color" has to produce
        // exactly this color, even when its alpha is lower than what is already there --
        // blending would make it impossible to ever reduce a painted cell's alpha.
        layer.data[index] = rgba[0];
        layer.data[index + 1] = rgba[1];
        layer.data[index + 2] = rgba[2];
        layer.data[index + 3] = rgba[3] === undefined ? 255 : rgba[3];

        stroke.rect = rectUnion(stroke.rect, { x, y, w: 1, h: 1 });
    }

    clearPixel(x, y) {
        this.setPixel(x, y, [0, 0, 0, 0]);
    }

    getPixel(x, y, layer) {
        if (x < 0 || y < 0 || x >= this.width || y >= this.height) {
            return [0, 0, 0, 0];
        }
        const target = layer || this.activeLayer();
        const index = (y * this.width + x) * 4;
        return [target.data[index], target.data[index + 1], target.data[index + 2], target.data[index + 3]];
    }

    // The color visible at (x, y) after compositing -- used by the eyedropper, because the
    // user picks the color THEY SEE, not the selected layer's color.
    getCompositePixel(x, y) {
        const out = new Uint8ClampedArray(4);
        for (const layer of this.layers) {
            if (!layer.visible || layer.opacity === 0) {
                continue;
            }
            const index = (y * this.width + x) * 4;
            blendPixel(out, 0, layer.data, index, layer.opacity);
        }
        return [out[0], out[1], out[2], out[3]];
    }

    endStroke() {
        const stroke = this._stroke;
        this._stroke = null;
        if (!stroke || !stroke.rect || stroke.originals.size === 0) {
            return;
        }

        const rect = stroke.rect;
        const size = rect.w * rect.h * 4;
        const before = new Uint8ClampedArray(size);
        const after = new Uint8ClampedArray(size);

        // `after` is the region's current state; `before` is that same current state with the
        // memoised cells written back over it -- so cells INSIDE the bbox that the stroke
        // never touched are preserved exactly.
        for (let row = 0; row < rect.h; row += 1) {
            const srcStart = ((rect.y + row) * this.width + rect.x) * 4;
            const dstStart = row * rect.w * 4;
            after.set(stroke.layer.data.subarray(srcStart, srcStart + rect.w * 4), dstStart);
            before.set(stroke.layer.data.subarray(srcStart, srcStart + rect.w * 4), dstStart);
        }
        stroke.originals.forEach((rgba, index) => {
            const pixel = index / 4;
            const localX = (pixel % this.width) - rect.x;
            const localY = Math.floor(pixel / this.width) - rect.y;
            const offset = (localY * rect.w + localX) * 4;
            before[offset] = rgba[0];
            before[offset + 1] = rgba[1];
            before[offset + 2] = rgba[2];
            before[offset + 3] = rgba[3];
        });

        this._pushCommand({
            label: stroke.label,
            patches: [{ kind: 'pixels', layerId: stroke.layer.id, rect, before, after }],
            bytes: size * 2,
        });
        this._emit('paint', rect);
    }

    // --- history ---------------------------------------------------------

    _structuralPatch(mutate) {
        const before = { layers: this.layers.map(layerMeta), activeIndex: this.activeIndex };
        const beforeLayers = this.layers.slice();
        mutate();
        return {
            kind: 'structure',
            before,
            beforeLayers,
            after: { layers: this.layers.map(layerMeta), activeIndex: this.activeIndex },
            afterLayers: this.layers.slice(),
        };
    }

    _applyStructural(label, mutate) {
        const patch = this._structuralPatch(mutate);
        this._pushCommand({ label, patches: [patch], bytes: 256 });
        this._emit('structure', this.fullRect());
    }

    _pushCommand(command) {
        // Drop the pending redo branch: a new action after an undo makes the old branch
        // meaningless.
        if (this._historyIndex < this._history.length) {
            const dropped = this._history.splice(this._historyIndex);
            dropped.forEach((c) => {
                this._historyBytes -= c.bytes;
            });
        }
        this._history.push(command);
        this._historyIndex = this._history.length;
        this._historyBytes += command.bytes;

        // "Unlimited" is enforced through a byte budget: trim from the BOTTOM only when the
        // budget is exceeded, rather than capping the step count as the previous version did
        // (20 snapshots).
        while (this._historyBytes > HISTORY_BYTE_BUDGET && this._history.length > 1) {
            const oldest = this._history.shift();
            this._historyBytes -= oldest.bytes;
            this._historyIndex -= 1;
            // The "saved" marker has to slide along too, otherwise isDirty() goes wrong as
            // soon as the history starts being trimmed.
            this._savedHistoryIndex = Math.max(0, this._savedHistoryIndex - 1);
        }
    }

    canUndo() {
        return this._historyIndex > 0;
    }

    canRedo() {
        return this._historyIndex < this._history.length;
    }

    undo() {
        if (!this.canUndo()) {
            return false;
        }
        this._historyIndex -= 1;
        const command = this._history[this._historyIndex];
        // Reverse order: a command like "merge down" holds a pixel patch then a structural
        // patch, so undoing has to walk it back to front.
        for (let i = command.patches.length - 1; i >= 0; i -= 1) {
            this._applyPatch(command.patches[i], 'before');
        }
        this._emit('undo', patchesRect(command, this));
        return true;
    }

    redo() {
        if (!this.canRedo()) {
            return false;
        }
        const command = this._history[this._historyIndex];
        this._historyIndex += 1;
        command.patches.forEach((patch) => this._applyPatch(patch, 'after'));
        this._emit('redo', patchesRect(command, this));
        return true;
    }

    _applyPatch(patch, direction) {
        if (patch.kind === 'pixels') {
            const layer = this.layerById(patch.layerId);
            if (!layer) {
                return;
            }
            const source = patch[direction];
            const { rect } = patch;
            for (let row = 0; row < rect.h; row += 1) {
                const dstStart = ((rect.y + row) * this.width + rect.x) * 4;
                const srcStart = row * rect.w * 4;
                layer.data.set(source.subarray(srcStart, srcStart + rect.w * 4), dstStart);
            }
            return;
        }

        if (patch.kind === 'structure') {
            const snapshot = direction === 'before' ? patch.before : patch.after;
            const layerList = direction === 'before' ? patch.beforeLayers : patch.afterLayers;
            this.layers = layerList.slice();
            // Metadata is reapplied from the snapshot: layer objects are shared between
            // before/after, so their visible/opacity/name properties may have changed since
            // the patch was created.
            snapshot.layers.forEach((meta) => {
                const layer = this.layerById(meta.id);
                if (layer) {
                    layer.name = meta.name;
                    layer.visible = meta.visible;
                    layer.opacity = meta.opacity;
                }
            });
            this.activeIndex = Math.min(snapshot.activeIndex, this.layers.length - 1);
        }
    }

    historyDepth() {
        return { index: this._historyIndex, total: this._history.length, bytes: this._historyBytes };
    }

    // --- dirty state -----------------------------------------------------

    // Compares the POSITION IN HISTORY, not `revision`: revision increments on undo too, so
    // using it would report "unsaved" even after undoing back to the saved state.
    isDirty() {
        return this._historyIndex !== this._savedHistoryIndex;
    }

    markSaved() {
        this._savedHistoryIndex = this._historyIndex;
        this._listeners.forEach((listener) => listener({ type: 'saved', rect: null }));
    }

    // --- size / import ---------------------------------------------------

    // Resizes to match a newly loaded image. Deliberately not part of the history: loading a
    // completely different texture starts a new session, it is not an edit step.
    resizeTo(width, height) {
        if (this.width === width && this.height === height) {
            return;
        }
        this.width = width;
        this.height = height;
        this.layers.forEach((layer) => {
            layer.data = new Uint8ClampedArray(width * height * 4);
        });
        this._history = [];
        this._historyIndex = 0;
        this._savedHistoryIndex = 0;
        this._historyBytes = 0;
        this.selection = null;
        this._emit('resize', this.fullRect());
    }

    // Writes RGBA straight into a layer, used when loading an existing texture from the
    // server. `asNewLayer` is used when importing an arbitrary PNG, so it does not overwrite
    // what is already there.
    putImageBytes(rgba, width, height, { asNewLayer = false, label = 'Load image' } = {}) {
        if (width !== this.width || height !== this.height) {
            this.resizeTo(width, height);
        }
        if (asNewLayer) {
            this.addLayer(label);
        }
        const layer = this.activeLayer();
        this.beginStroke(label, layer.id);
        for (let y = 0; y < height; y += 1) {
            for (let x = 0; x < width; x += 1) {
                const i = (y * width + x) * 4;
                this.setPixel(x, y, [rgba[i], rgba[i + 1], rgba[i + 2], rgba[i + 3]]);
            }
        }
        this.endStroke();
    }

    // --- composite / export ----------------------------------------------

    composite(out) {
        const buffer = out && out.length === this.width * this.height * 4
            ? out
            : new Uint8ClampedArray(this.width * this.height * 4);
        buffer.fill(0);
        for (const layer of this.layers) {
            compositeOnto(buffer, layer.data, this.width, this.height, layer.opacity, layer.visible);
        }
        return buffer;
    }

    toBlob() {
        return encodePng(this.composite(), this.width, this.height);
    }
}

function compositeOnto(dst, src, width, height, opacity, visible) {
    if (!visible || opacity === 0) {
        return;
    }
    const count = width * height;
    for (let p = 0; p < count; p += 1) {
        blendPixel(dst, p * 4, src, p * 4, opacity);
    }
}

// source-over with STRAIGHT alpha: accumulate in premultiplied space and divide back
// exactly once at the end, so rounding does not compound layer by layer.
function blendPixel(dst, dstIndex, src, srcIndex, opacity) {
    const srcAlpha = (src[srcIndex + 3] / 255) * opacity;
    if (srcAlpha <= 0) {
        return;
    }
    const dstAlpha = dst[dstIndex + 3] / 255;
    const outAlpha = srcAlpha + dstAlpha * (1 - srcAlpha);
    if (outAlpha <= 0) {
        dst[dstIndex] = 0;
        dst[dstIndex + 1] = 0;
        dst[dstIndex + 2] = 0;
        dst[dstIndex + 3] = 0;
        return;
    }
    for (let c = 0; c < 3; c += 1) {
        const premultiplied = src[srcIndex + c] * srcAlpha + dst[dstIndex + c] * dstAlpha * (1 - srcAlpha);
        dst[dstIndex + c] = Math.round(premultiplied / outAlpha);
    }
    dst[dstIndex + 3] = Math.round(outAlpha * 255);
}

function patchesRect(command, doc) {
    return command.patches.reduce(
        (acc, patch) => (patch.kind === 'pixels' ? rectUnion(acc, patch.rect) : doc.fullRect()),
        null,
    ) || doc.fullRect();
}
