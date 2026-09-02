import { rectUnion } from './geom.js';
import { guideRectsFor } from './uv.js';

// The viewport: three stacked canvases inside a wrapper with a CSS checkerboard background.
//
//  1. doc canvas     - sized in DOCUMENT PIXELS (16x16, for example), scaled up with
//                      transform: scale(). Scaling is the GPU's job, so the per-frame JS
//                      cost scales with the document size, not the displayed size.
//  2. overlay canvas - the same document-pixel size; tool rubber bands and the UV guide.
//  3. ui canvas      - sized in DEVICE PIXELS; the grid, cursor outline and marching ants.
//                      These need lines thinner than one document pixel, so they have to be
//                      drawn in screen space.
//
// The checkerboard is deliberately NOT drawn into a canvas: it is CSS on the wrapper, fixed
// at 16px in screen space (as in Aseprite), so it does not distort when zooming.

const MIN_ZOOM = 1;
const MAX_ZOOM = 64;

export function createViewport(elements, doc) {
    return new Viewport(elements, doc);
}

class Viewport {
    constructor({ wrapper, stack, docCanvas, overlayCanvas, uiCanvas }, doc) {
        this.wrapper = wrapper;
        this.stack = stack;
        this.docCanvas = docCanvas;
        this.overlayCanvas = overlayCanvas;
        this.uiCanvas = uiCanvas;
        this.doc = doc;

        this.zoom = 1;
        this.panX = 0;
        this.panY = 0;
        this.showGrid = true;
        this.uvOverlay = null;
        this.cursor = null;
        this.mirrorX = false;
        this.mirrorY = false;
        // The cells a shape tool (line/rect/ellipse) proposes during a drag. Drawn on the UI
        // layer rather than written into the layer, so a shape becomes one undo step only on
        // release. Drawn in screen space, so it is correct at every zoom level.
        this.previewPixels = null;

        this._composite = null;
        this._dirty = doc.fullRect();
        this._uiDirty = true;
        this._frame = null;

        this._unsubscribe = doc.subscribe((event) => {
            this.invalidate(event.rect);
        });

        this.syncSize();
        this.fitToWrapper();
    }

    destroy() {
        this._unsubscribe();
        if (this._frame !== null) {
            cancelAnimationFrame(this._frame);
        }
    }

    // --- geometry --------------------------------------------------------

    syncSize() {
        const { width, height } = this.doc;
        [this.docCanvas, this.overlayCanvas].forEach((canvas) => {
            canvas.width = width;
            canvas.height = height;
        });
        this._composite = new Uint8ClampedArray(width * height * 4);
        this._dirty = this.doc.fullRect();
        this._uiDirty = true;
        this.applyTransform();
    }

    fitToWrapper() {
        const bounds = this.wrapper.getBoundingClientRect();
        if (bounds.width === 0 || bounds.height === 0) {
            return;
        }
        // Integer zoom: a fractional zoom produces seams between pixels, because coordinates
        // round differently on each side.
        const fit = Math.floor(Math.min(
            (bounds.width - 32) / this.doc.width,
            (bounds.height - 32) / this.doc.height,
        ));
        this.setZoom(Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, fit || 1)), null);
        this.centre();
    }

    centre() {
        const bounds = this.wrapper.getBoundingClientRect();
        this.panX = Math.round((bounds.width - this.doc.width * this.zoom) / 2);
        this.panY = Math.round((bounds.height - this.doc.height * this.zoom) / 2);
        this.applyTransform();
    }

    /**
     * @param anchor the screen (client) coordinate to hold fixed while zooming, so the
     *        cursor does not "slide off" the pixel it is pointing at during a wheel scroll.
     */
    setZoom(zoom, anchor) {
        const next = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, Math.round(zoom)));
        if (next === this.zoom) {
            return;
        }
        if (anchor) {
            const bounds = this.wrapper.getBoundingClientRect();
            const localX = anchor.x - bounds.left - this.panX;
            const localY = anchor.y - bounds.top - this.panY;
            const ratio = next / this.zoom;
            this.panX -= localX * (ratio - 1);
            this.panY -= localY * (ratio - 1);
        }
        this.zoom = next;
        this.applyTransform();
        this._uiDirty = true;
        this.scheduleFrame();
    }

    panBy(dx, dy) {
        this.panX += dx;
        this.panY += dy;
        this.applyTransform();
        this._uiDirty = true;
        this.scheduleFrame();
    }

    applyTransform() {
        const w = this.doc.width * this.zoom;
        const h = this.doc.height * this.zoom;
        this.stack.style.width = w + 'px';
        this.stack.style.height = h + 'px';
        this.stack.style.transform = `translate(${Math.round(this.panX)}px, ${Math.round(this.panY)}px)`;

        const ratio = window.devicePixelRatio || 1;
        this.uiCanvas.width = Math.max(1, Math.round(w * ratio));
        this.uiCanvas.height = Math.max(1, Math.round(h * ratio));
    }

    screenToPixel(clientX, clientY) {
        const bounds = this.wrapper.getBoundingClientRect();
        return {
            x: Math.floor((clientX - bounds.left - this.panX) / this.zoom),
            y: Math.floor((clientY - bounds.top - this.panY) / this.zoom),
        };
    }

    // --- painting --------------------------------------------------------

    invalidate(rect) {
        this._dirty = rectUnion(this._dirty, rect || this.doc.fullRect());
        this.scheduleFrame();
    }

    invalidateUi() {
        this._uiDirty = true;
        this.scheduleFrame();
    }

    scheduleFrame() {
        if (this._frame !== null) {
            return;
        }
        this._frame = requestAnimationFrame(() => {
            this._frame = null;
            this.paint();
        });
    }

    // The callback is invoked once per frame WITH the composite just computed, so the 3D
    // preview reuses that very buffer instead of compositing a second time.
    onFrame(callback) {
        this._frameCallback = callback;
    }

    paint() {
        if (this._dirty) {
            const composite = this.doc.composite(this._composite);
            const ctx = this.docCanvas.getContext('2d');
            ctx.imageSmoothingEnabled = false;
            // putImageData for the whole image: at 64x64 that is 16 KB, far cheaper than
            // managing slices and the edge cases of dirty rects.
            ctx.putImageData(new ImageData(new Uint8ClampedArray(composite), this.doc.width, this.doc.height), 0, 0);

            if (this._frameCallback) {
                this._frameCallback(composite, this._dirty);
            }
            this._dirty = null;
        }

        if (this._uiDirty) {
            this.paintOverlay();
            this.paintUi();
            this._uiDirty = false;
        }
    }

    paintOverlay() {
        const ctx = this.overlayCanvas.getContext('2d');
        ctx.clearRect(0, 0, this.overlayCanvas.width, this.overlayCanvas.height);
        if (!this.uvOverlay) {
            return;
        }
        guideRectsFor(this.uvOverlay).forEach((rect, index) => {
            ctx.fillStyle = index % 2 === 0 ? 'rgba(56,189,248,0.30)' : 'rgba(244,114,182,0.30)';
            ctx.fillRect(rect.x, rect.y, rect.w, rect.h);
            ctx.strokeStyle = 'rgba(0,0,0,0.55)';
            ctx.lineWidth = 0.15;
            ctx.strokeRect(rect.x, rect.y, rect.w, rect.h);
        });
    }

    paintUi() {
        const ctx = this.uiCanvas.getContext('2d');
        const ratio = window.devicePixelRatio || 1;
        ctx.setTransform(ratio, 0, 0, ratio, 0, 0);
        ctx.clearRect(0, 0, this.uiCanvas.width / ratio, this.uiCanvas.height / ratio);

        const w = this.doc.width * this.zoom;
        const h = this.doc.height * this.zoom;

        // The grid only appears while there is room to see it -- below 5px per cell it fills
        // in solid and becomes noise.
        if (this.showGrid && this.zoom >= 5) {
            ctx.lineWidth = 1 / ratio;
            for (let x = 1; x < this.doc.width; x += 1) {
                const isMajor = x % 16 === 0;
                ctx.strokeStyle = isMajor ? 'rgba(255,255,255,0.30)' : 'rgba(255,255,255,0.10)';
                ctx.beginPath();
                ctx.moveTo(x * this.zoom + 0.5 / ratio, 0);
                ctx.lineTo(x * this.zoom + 0.5 / ratio, h);
                ctx.stroke();
            }
            for (let y = 1; y < this.doc.height; y += 1) {
                const isMajor = y % 16 === 0;
                ctx.strokeStyle = isMajor ? 'rgba(255,255,255,0.30)' : 'rgba(255,255,255,0.10)';
                ctx.beginPath();
                ctx.moveTo(0, y * this.zoom + 0.5 / ratio);
                ctx.lineTo(w, y * this.zoom + 0.5 / ratio);
                ctx.stroke();
            }
        }

        if (this.mirrorX) {
            drawDashedLine(ctx, w / 2, 0, w / 2, h, 'rgba(201,162,39,0.9)');
        }
        if (this.mirrorY) {
            drawDashedLine(ctx, 0, h / 2, w, h / 2, 'rgba(201,162,39,0.9)');
        }

        if (this.doc.selection) {
            const s = this.doc.selection;
            ctx.save();
            ctx.setLineDash([4, 4]);
            ctx.strokeStyle = 'rgba(255,255,255,0.9)';
            ctx.lineWidth = 1;
            ctx.strokeRect(s.x * this.zoom, s.y * this.zoom, s.w * this.zoom, s.h * this.zoom);
            ctx.restore();
        }

        if (this.previewPixels && this.previewPixels.length > 0) {
            ctx.fillStyle = 'rgba(255,255,255,0.55)';
            this.previewPixels.forEach(({ x, y }) => {
                ctx.fillRect(x * this.zoom, y * this.zoom, this.zoom, this.zoom);
            });
        }

        if (this.cursor && this.zoom >= 4) {
            ctx.strokeStyle = 'rgba(255,255,255,0.85)';
            ctx.lineWidth = 1;
            ctx.strokeRect(
                this.cursor.x * this.zoom + 0.5,
                this.cursor.y * this.zoom + 0.5,
                this.cursor.size * this.zoom - 1,
                this.cursor.size * this.zoom - 1,
            );
        }

        ctx.strokeStyle = 'rgba(255,255,255,0.25)';
        ctx.lineWidth = 1;
        ctx.strokeRect(0.5, 0.5, w - 1, h - 1);
    }
}

function drawDashedLine(ctx, x0, y0, x1, y1, colour) {
    ctx.save();
    ctx.setLineDash([3, 3]);
    ctx.strokeStyle = colour;
    ctx.lineWidth = 1;
    ctx.beginPath();
    ctx.moveTo(x0, y0);
    ctx.lineTo(x1, y1);
    ctx.stroke();
    ctx.restore();
}
