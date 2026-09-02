import { hsvToRgb, parseHex, rgbToHsv, toCss, toHex } from './colors.js';

// A circular color picker: an outer hue ring around an inner saturation/value square, like
// Minecraft's item-rename picker.
//
// Plain canvas, no extra library. The hue ring is drawn ONCE and cached (it depends only on
// size); the SV square is redrawn when the hue changes; the cursors are drawn every frame on
// a separate canvas above.
//
// The most important design point: the component OWNS h/s/v as state rather than deriving it
// back from RGB on every render. The old version (three Hue/Sat/Val sliders) recomputed HSV
// from RGB with an `hsv.s || 1` fallback, so on a black or grey color (s = 0) the hue lost
// all meaning and dragging the Hue slider jumped straight to full saturation. Keeping the
// state separately means dragging hue on black stays smooth -- and stays black.

const RING_WIDTH = 18;
const CURSOR_RADIUS = 6;
const HUE_SEGMENTS = 360;

export function createColorWheel(container, { size = 200, onChange } = {}) {
    return new ColorWheel(container, size, onChange);
}

class ColorWheel {
    constructor(container, size, onChange) {
        this.size = size;
        this.onChange = onChange || (() => {});
        this.h = 0;
        this.s = 0;
        this.v = 0;
        this.a = 255;
        this._drag = null;
        // Breaks the loop: onChange -> studio.setPrimary -> setColor -> onChange...
        this._emitting = false;

        this.outerRadius = size / 2 - 2;
        this.innerRadius = this.outerRadius - RING_WIDTH;
        // The largest square inscribed in the inner circle.
        this.squareSide = Math.floor(this.innerRadius * Math.SQRT2) - 2;
        this.squareOrigin = (size - this.squareSide) / 2;

        this._build(container);
        this._ringCache = this._renderRing();
        this._wirePointer();
        this.redraw();
    }

    // --- DOM -------------------------------------------------------------

    _build(container) {
        container.textContent = '';
        container.className = 'flex flex-col gap-3';

        const stage = document.createElement('div');
        stage.className = 'relative mx-auto';
        stage.style.width = this.size + 'px';
        stage.style.height = this.size + 'px';
        stage.style.touchAction = 'none';

        this.wheelCanvas = this._canvas();
        this.cursorCanvas = this._canvas();
        stage.append(this.wheelCanvas, this.cursorCanvas);
        container.appendChild(stage);
        this.stage = stage;

        container.appendChild(this._buildReadout());
        container.appendChild(this._buildAlpha());
    }

    _canvas() {
        const canvas = document.createElement('canvas');
        const ratio = window.devicePixelRatio || 1;
        canvas.width = this.size * ratio;
        canvas.height = this.size * ratio;
        canvas.style.width = this.size + 'px';
        canvas.style.height = this.size + 'px';
        canvas.className = 'absolute left-0 top-0';
        canvas.getContext('2d').scale(ratio, ratio);
        return canvas;
    }

    _buildReadout() {
        const row = document.createElement('div');
        row.className = 'flex items-center gap-2';

        // The preview swatch sits on a checkerboard, so low alpha reads immediately as transparency.
        this.preview = document.createElement('div');
        this.preview.className = 'if-swatch h-8 w-8 shrink-0 cursor-default';
        this.preview.setAttribute('aria-label', 'Selected colour');

        this.hexInput = document.createElement('input');
        this.hexInput.type = 'text';
        this.hexInput.spellcheck = false;
        this.hexInput.className = 'input input-bordered input-sm w-full font-mono uppercase';
        this.hexInput.setAttribute('aria-label', 'Hex colour');
        this.hexInput.addEventListener('change', () => this._commitHexInput());
        // The Studio's shortcuts already ignore keys while the cursor is inside an input
        // (see typingInside in shortcuts.js), so nothing extra needs blocking here.
        this.hexInput.addEventListener('keydown', (event) => {
            if (event.key === 'Enter') {
                event.preventDefault();
                this._commitHexInput();
            }
        });

        row.append(this.preview, this.hexInput);
        return row;
    }

    _buildAlpha() {
        const wrap = document.createElement('label');
        wrap.className = 'flex items-center gap-2';

        const label = document.createElement('span');
        label.className = 'itemforge-tech-label w-10';
        label.textContent = 'Alpha';

        this.alphaInput = document.createElement('input');
        this.alphaInput.type = 'range';
        this.alphaInput.min = '0';
        this.alphaInput.max = '255';
        this.alphaInput.className = 'range range-xs flex-1';
        this.alphaInput.addEventListener('input', () => {
            this.a = Number(this.alphaInput.value);
            this._syncReadout();
            this._emit();
        });

        this.alphaReadout = document.createElement('span');
        this.alphaReadout.className = 'w-8 text-right font-mono text-xs tabular-nums';

        wrap.append(label, this.alphaInput, this.alphaReadout);
        return wrap;
    }

    // --- ve --------------------------------------------------------------

    // The hue ring depends only on size, so it is drawn once into an offscreen canvas.
    // Each small arc overlaps the next by one degree so no seam shows between segments.
    _renderRing() {
        const ratio = window.devicePixelRatio || 1;
        const cache = document.createElement('canvas');
        cache.width = this.size * ratio;
        cache.height = this.size * ratio;
        const ctx = cache.getContext('2d');
        ctx.scale(ratio, ratio);

        const centre = this.size / 2;
        const step = (Math.PI * 2) / HUE_SEGMENTS;
        ctx.lineWidth = RING_WIDTH;

        for (let i = 0; i < HUE_SEGMENTS; i += 1) {
            const start = i * step;
            const [r, g, b] = hsvToRgb(i, 1, 1);
            ctx.strokeStyle = `rgb(${r}, ${g}, ${b})`;
            ctx.beginPath();
            ctx.arc(centre, centre, this.outerRadius - RING_WIDTH / 2, start, start + step * 1.5);
            ctx.stroke();
        }
        return cache;
    }

    redraw() {
        this._drawWheel();
        this._drawCursors();
        this._syncReadout();
    }

    _drawWheel() {
        const ctx = this.wheelCanvas.getContext('2d');
        ctx.clearRect(0, 0, this.size, this.size);
        ctx.drawImage(this._ringCache, 0, 0, this.size, this.size);

        // The SV square: a pure-hue background, overlaid with a white gradient horizontally
        // (saturation) and a black one vertically (value).
        const [r, g, b] = hsvToRgb(this.h, 1, 1);
        const x = this.squareOrigin;
        const y = this.squareOrigin;
        const side = this.squareSide;

        ctx.fillStyle = `rgb(${r}, ${g}, ${b})`;
        ctx.fillRect(x, y, side, side);

        const white = ctx.createLinearGradient(x, 0, x + side, 0);
        white.addColorStop(0, 'rgba(255,255,255,1)');
        white.addColorStop(1, 'rgba(255,255,255,0)');
        ctx.fillStyle = white;
        ctx.fillRect(x, y, side, side);

        const black = ctx.createLinearGradient(0, y, 0, y + side);
        black.addColorStop(0, 'rgba(0,0,0,0)');
        black.addColorStop(1, 'rgba(0,0,0,1)');
        ctx.fillStyle = black;
        ctx.fillRect(x, y, side, side);

        ctx.strokeStyle = 'rgba(0,0,0,0.35)';
        ctx.lineWidth = 1;
        ctx.strokeRect(x + 0.5, y + 0.5, side - 1, side - 1);
    }

    _drawCursors() {
        const ctx = this.cursorCanvas.getContext('2d');
        ctx.clearRect(0, 0, this.size, this.size);

        const centre = this.size / 2;
        const angle = (this.h * Math.PI) / 180;
        const ringRadius = this.outerRadius - RING_WIDTH / 2;
        ring(ctx, centre + Math.cos(angle) * ringRadius, centre + Math.sin(angle) * ringRadius);

        const sx = this.squareOrigin + this.s * this.squareSide;
        const sy = this.squareOrigin + (1 - this.v) * this.squareSide;
        ring(ctx, sx, sy);
    }

    // --- tuong tac -------------------------------------------------------

    _wirePointer() {
        const pick = (event) => {
            const bounds = this.stage.getBoundingClientRect();
            return { x: event.clientX - bounds.left, y: event.clientY - bounds.top };
        };

        this.stage.addEventListener('pointerdown', (event) => {
            const point = pick(event);
            // The mode is LOCKED at pointerdown and held for the whole drag: dragging the hue
            // cursor off the ring must not turn into dragging the SV square.
            this._drag = this._hitTest(point);
            if (!this._drag) {
                return;
            }
            this.stage.setPointerCapture(event.pointerId);
            this._apply(point);
        });

        this.stage.addEventListener('pointermove', (event) => {
            if (this._drag) {
                this._apply(pick(event));
            }
        });

        const stop = () => {
            this._drag = null;
        };
        this.stage.addEventListener('pointerup', stop);
        this.stage.addEventListener('pointercancel', stop);
    }

    _hitTest({ x, y }) {
        const inSquare = x >= this.squareOrigin && x <= this.squareOrigin + this.squareSide
            && y >= this.squareOrigin && y <= this.squareOrigin + this.squareSide;
        if (inSquare) {
            return 'square';
        }
        const centre = this.size / 2;
        const distance = Math.hypot(x - centre, y - centre);
        // The ring's hit area is loosened slightly so it does not demand pixel-perfect aim.
        return distance <= this.outerRadius + 4 && distance >= this.innerRadius - 4 ? 'ring' : null;
    }

    _apply({ x, y }) {
        if (this._drag === 'ring') {
            const centre = this.size / 2;
            const angle = Math.atan2(y - centre, x - centre);
            this.h = ((angle * 180) / Math.PI + 360) % 360;
            this._drawWheel();
        } else {
            const clamp = (n) => Math.max(0, Math.min(1, n));
            this.s = clamp((x - this.squareOrigin) / this.squareSide);
            this.v = clamp(1 - (y - this.squareOrigin) / this.squareSide);
        }
        this._drawCursors();
        this._syncReadout();
        this._emit();
    }

    _commitHexInput() {
        const parsed = parseHex(this.hexInput.value);
        if (!parsed) {
            // Written directly rather than through _syncReadout: that function deliberately
            // leaves the hex field alone while it has focus, but here a valid value has to be
            // put back immediately -- otherwise the garbage string stays on screen.
            this.hexInput.value = toHex(this.getColor()).toUpperCase();
            return;
        }
        // Pasting an 8-character code (#rrggbbaa) splits the alpha out into the slider rather
        // than dropping it silently.
        this.setColor(parsed);
        this._emit();
    }

    _syncReadout() {
        const rgba = this.getColor();
        this.preview.style.backgroundColor = toCss(rgba);
        this.alphaInput.value = String(this.a);
        this.alphaReadout.textContent = String(this.a);
        // The hex field always shows six characters: alpha has its own slider, and showing it
        // in both places would suggest it can be changed in either.
        if (document.activeElement !== this.hexInput) {
            this.hexInput.value = toHex(rgba).toUpperCase();
        }
    }

    _emit() {
        if (this._emitting) {
            return;
        }
        this._emitting = true;
        try {
            this.onChange(this.getColor());
        } finally {
            this._emitting = false;
        }
    }

    // --- API ---------------------------------------------------------------

    getColor() {
        const [r, g, b] = hsvToRgb(this.h, this.s, this.v);
        return [r, g, b, this.a];
    }

    /**
     * Syncs back from outside (the eyedropper, a shortcut, initialisation).
     *
     * <p>The current hue/saturation are kept when the incoming color is grey or black: grey
     * RGB carries no hue information, so deriving it would snap the cursor to 0 degrees and
     * lose the user's position.
     */
    setColor(rgba) {
        if (this._emitting) {
            return;
        }
        const hsv = rgbToHsv(rgba[0], rgba[1], rgba[2]);
        this.h = hsv.s === 0 ? this.h : hsv.h;
        this.s = hsv.v === 0 ? this.s : hsv.s;
        this.v = hsv.v;
        this.a = rgba[3] === undefined ? 255 : rgba[3];
        this.redraw();
    }

    destroy() {
        this._drag = null;
    }
}

function ring(ctx, x, y) {
    ctx.lineWidth = 2;
    ctx.strokeStyle = 'rgba(0,0,0,0.75)';
    ctx.beginPath();
    ctx.arc(x, y, CURSOR_RADIUS, 0, Math.PI * 2);
    ctx.stroke();

    ctx.lineWidth = 1.5;
    ctx.strokeStyle = 'rgba(255,255,255,0.95)';
    ctx.beginPath();
    ctx.arc(x, y, CURSOR_RADIUS - 1, 0, Math.PI * 2);
    ctx.stroke();
}
