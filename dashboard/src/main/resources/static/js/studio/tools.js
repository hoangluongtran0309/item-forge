import { ellipse, floodFill, line, rect as rasterRect, rectContains } from './geom.js';
import { sameColor } from './colors.js';

// Every tool is a plain object with the same shape. `ctx` is the ToolContext main.js
// builds, holding the document, viewport, color, brush and mirror state -- a tool never
// reaches into the DOM itself.
//
//   { id, label, shortcut, hint,
//     onDown(ctx, p, evt), onMove(ctx, p, evt), onUp(ctx, p, evt) }
//
// Convention: any tool that changes pixels must call ctx.doc.beginStroke()/endStroke()
// itself -- one such pair is EXACTLY ONE undo step.

function paintAt(ctx, x, y, colour) {
    const size = ctx.brushSize;
    // The brush anchors on its top-left corner, which keeps both odd and even sizes
    // predictable -- the same way Aseprite handles square brushes.
    const offset = Math.floor((size - 1) / 2);
    for (let dy = 0; dy < size; dy += 1) {
        for (let dx = 0; dx < size; dx += 1) {
            const px = x - offset + dx;
            const py = y - offset + dy;
            if (ctx.doc.selection && !rectContains(ctx.doc.selection, px, py)) {
                continue;
            }
            ctx.doc.setPixel(px, py, colour);
            mirrorPaint(ctx, px, py, colour);
        }
    }
}

function mirrorPaint(ctx, x, y, colour) {
    const { doc } = ctx;
    if (ctx.mirrorX) {
        doc.setPixel(doc.width - 1 - x, y, colour);
    }
    if (ctx.mirrorY) {
        doc.setPixel(x, doc.height - 1 - y, colour);
    }
    if (ctx.mirrorX && ctx.mirrorY) {
        doc.setPixel(doc.width - 1 - x, doc.height - 1 - y, colour);
    }
}

// Holding Shift while drawing freehand constrains to a straight line from the start point,
// as in the reference editor.
function constrainedTo(start, p, evt) {
    if (!evt.shiftKey || !start) {
        return p;
    }
    const dx = Math.abs(p.x - start.x);
    const dy = Math.abs(p.y - start.y);
    return dx >= dy ? { x: p.x, y: start.y } : { x: start.x, y: p.y };
}

function freehand(id, label, shortcut, colourFor) {
    return {
        id,
        label,
        shortcut,
        onDown(ctx, p, evt) {
            ctx.doc.beginStroke(label);
            this._last = p;
            this._start = p;
            paintAt(ctx, p.x, p.y, colourFor(ctx));
            void evt;
        },
        onMove(ctx, p, evt) {
            const target = constrainedTo(this._start, p, evt);
            const from = evt.shiftKey ? this._start : this._last;
            // Join consecutive samples with Bresenham: during a fast drag two pointermove
            // events can be many pixels apart, and without joining the stroke has gaps.
            line(from.x, from.y, target.x, target.y, (x, y) => paintAt(ctx, x, y, colourFor(ctx)));
            this._last = target;
        },
        onUp(ctx) {
            ctx.doc.endStroke();
            this._last = null;
            this._start = null;
        },
    };
}

const pencil = freehand('pencil', 'Pencil', 'b', (ctx) => ctx.primary);
const eraser = freehand('eraser', 'Eraser', 'e', () => [0, 0, 0, 0]);

const bucket = {
    id: 'bucket',
    label: 'Fill',
    shortcut: 'g',
    onDown(ctx, p) {
        const { doc } = ctx;
        if (p.x < 0 || p.y < 0 || p.x >= doc.width || p.y >= doc.height) {
            return;
        }
        const layer = doc.activeLayer();
        const target = doc.getPixel(p.x, p.y, layer);
        if (sameColor(target, ctx.primary)) {
            return;
        }
        doc.beginStroke('Fill');
        floodFill(
            doc.width,
            doc.height,
            p.x,
            p.y,
            (x, y) => {
                if (doc.selection && !rectContains(doc.selection, x, y)) {
                    return false;
                }
                return sameColor(doc.getPixel(x, y, layer), target);
            },
            (x, y) => doc.setPixel(x, y, ctx.primary),
        );
        doc.endStroke();
    },
    onMove() {},
    onUp() {},
};

const eyedropper = {
    id: 'eyedropper',
    label: 'Pick colour',
    shortcut: 'i',
    onDown(ctx, p) {
        const { doc } = ctx;
        if (p.x < 0 || p.y < 0 || p.x >= doc.width || p.y >= doc.height) {
            return;
        }
        // Picks the VISIBLE color (after compositing), not the selected layer's color -- the
        // user points at the color they see on screen.
        ctx.setPrimary(doc.getCompositePixel(p.x, p.y));
    },
    onMove(ctx, p, evt) {
        if (evt.buttons > 0) {
            this.onDown(ctx, p, evt);
        }
    },
    onUp() {},
};

// "Preview then commit" tools: they sketch onto the overlay during the drag and write into
// the layer only on release -- so one rectangle is EXACTLY ONE undo step.
function shapeTool(id, label, shortcut, rasterise) {
    return {
        id,
        label,
        shortcut,
        onDown(ctx, p) {
            this._start = p;
            this._end = p;
            ctx.previewShape((plot) => rasterise(this._start, this._end, ctx.filledShapes, plot));
        },
        onMove(ctx, p, evt) {
            if (!this._start) {
                return;
            }
            // Shift = vuong / tron deu.
            let end = p;
            if (evt.shiftKey) {
                const dx = p.x - this._start.x;
                const dy = p.y - this._start.y;
                const side = Math.max(Math.abs(dx), Math.abs(dy));
                end = {
                    x: this._start.x + Math.sign(dx || 1) * side,
                    y: this._start.y + Math.sign(dy || 1) * side,
                };
            }
            this._end = end;
            ctx.previewShape((plot) => rasterise(this._start, this._end, ctx.filledShapes, plot));
        },
        onUp(ctx) {
            if (!this._start) {
                return;
            }
            ctx.clearPreview();
            ctx.doc.beginStroke(label);
            rasterise(this._start, this._end, ctx.filledShapes, (x, y) => paintAt(ctx, x, y, ctx.primary));
            ctx.doc.endStroke();
            this._start = null;
            this._end = null;
        },
    };
}

const lineTool = shapeTool('line', 'Line', 'l', (start, end, filled, plot) => {
    void filled;
    line(start.x, start.y, end.x, end.y, plot);
});

const rectTool = shapeTool('rect', 'Rectangle', 'u', (start, end, filled, plot) => {
    rasterRect(start.x, start.y, end.x, end.y, filled, plot);
});

const ellipseTool = shapeTool('ellipse', 'Ellipse', 'o', (start, end, filled, plot) => {
    ellipse(start.x, start.y, end.x, end.y, filled, plot);
});

const select = {
    id: 'select',
    label: 'Select',
    shortcut: 'm',
    onDown(ctx, p) {
        this._start = p;
        ctx.doc.selection = null;
        ctx.viewport.invalidateUi();
    },
    onMove(ctx, p) {
        if (!this._start) {
            return;
        }
        ctx.doc.selection = normaliseRect(this._start, p, ctx.doc);
        ctx.viewport.invalidateUi();
    },
    onUp(ctx, p) {
        if (!this._start) {
            return;
        }
        const rect = normaliseRect(this._start, p, ctx.doc);
        // Dragging a 1x1 region is nearly always a click to deselect, not an attempt to
        // select exactly one pixel.
        ctx.doc.selection = rect.w <= 1 && rect.h <= 1 ? null : rect;
        this._start = null;
        ctx.viewport.invalidateUi();
    },
};

const move = {
    id: 'move',
    label: 'Move',
    shortcut: 'v',
    onDown(ctx, p) {
        const { doc } = ctx;
        const region = doc.selection || doc.fullRect();
        this._origin = p;
        this._region = region;
        this._snapshot = new Uint8ClampedArray(region.w * region.h * 4);
        const layer = doc.activeLayer();
        for (let row = 0; row < region.h; row += 1) {
            const src = ((region.y + row) * doc.width + region.x) * 4;
            this._snapshot.set(layer.data.subarray(src, src + region.w * 4), row * region.w * 4);
        }
        doc.beginStroke('Move');
    },
    onMove(ctx, p) {
        if (!this._origin) {
            return;
        }
        const { doc } = ctx;
        const region = this._region;
        const dx = p.x - this._origin.x;
        const dy = p.y - this._origin.y;

        for (let row = 0; row < region.h; row += 1) {
            for (let col = 0; col < region.w; col += 1) {
                doc.setPixel(region.x + col, region.y + row, [0, 0, 0, 0]);
            }
        }
        for (let row = 0; row < region.h; row += 1) {
            for (let col = 0; col < region.w; col += 1) {
                const i = (row * region.w + col) * 4;
                doc.setPixel(region.x + col + dx, region.y + row + dy, [
                    this._snapshot[i], this._snapshot[i + 1], this._snapshot[i + 2], this._snapshot[i + 3],
                ]);
            }
        }
    },
    onUp(ctx) {
        if (!this._origin) {
            return;
        }
        ctx.doc.endStroke();
        this._origin = null;
        this._snapshot = null;
    },
};

function normaliseRect(a, b, doc) {
    const x = Math.max(0, Math.min(a.x, b.x));
    const y = Math.max(0, Math.min(a.y, b.y));
    const right = Math.min(doc.width, Math.max(a.x, b.x) + 1);
    const bottom = Math.min(doc.height, Math.max(a.y, b.y) + 1);
    return { x, y, w: Math.max(0, right - x), h: Math.max(0, bottom - y) };
}

export const TOOLS = [pencil, eraser, bucket, eyedropper, lineTool, rectTool, ellipseTool, select, move];

export function getTool(id) {
    return TOOLS.find((tool) => tool.id === id) || pencil;
}

// Non-tool actions (nothing pointer-driven) -- flipping, clearing the selection.
export const ACTIONS = {
    flipHorizontal(ctx) {
        flip(ctx, true);
    },
    flipVertical(ctx) {
        flip(ctx, false);
    },
    clearSelection(ctx) {
        const { doc } = ctx;
        const region = doc.selection || doc.fullRect();
        doc.beginStroke('Clear');
        for (let y = region.y; y < region.y + region.h; y += 1) {
            for (let x = region.x; x < region.x + region.w; x += 1) {
                doc.setPixel(x, y, [0, 0, 0, 0]);
            }
        }
        doc.endStroke();
    },
};

function flip(ctx, horizontal) {
    const { doc } = ctx;
    const region = doc.selection || doc.fullRect();
    const layer = doc.activeLayer();

    const snapshot = new Uint8ClampedArray(region.w * region.h * 4);
    for (let row = 0; row < region.h; row += 1) {
        const src = ((region.y + row) * doc.width + region.x) * 4;
        snapshot.set(layer.data.subarray(src, src + region.w * 4), row * region.w * 4);
    }

    doc.beginStroke(horizontal ? 'Flip horizontal' : 'Flip vertical');
    for (let row = 0; row < region.h; row += 1) {
        for (let col = 0; col < region.w; col += 1) {
            const srcCol = horizontal ? region.w - 1 - col : col;
            const srcRow = horizontal ? row : region.h - 1 - row;
            const i = (srcRow * region.w + srcCol) * 4;
            doc.setPixel(region.x + col, region.y + row, [
                snapshot[i], snapshot[i + 1], snapshot[i + 2], snapshot[i + 3],
            ]);
        }
    }
    doc.endStroke();
}
