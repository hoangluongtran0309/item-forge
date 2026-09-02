// Pure geometry rasterisation on an integer pixel grid. It touches neither the DOM nor a
// canvas, so it can be tested on its own.

// Bresenham. It exists because two consecutive pointermove events can be many pixels apart
// during a fast drag, and the previous editor painted only those two points, leaving gaps
// in the stroke. Every freehand tool must join consecutive samples with this function.
export function line(x0, y0, x1, y1, plot) {
    let x = x0;
    let y = y0;
    const dx = Math.abs(x1 - x0);
    const dy = Math.abs(y1 - y0);
    const sx = x0 < x1 ? 1 : -1;
    const sy = y0 < y1 ? 1 : -1;
    let err = dx - dy;

    for (;;) {
        plot(x, y);
        if (x === x1 && y === y1) {
            return;
        }
        const e2 = 2 * err;
        if (e2 > -dy) {
            err -= dy;
            x += sx;
        }
        if (e2 < dx) {
            err += dx;
            y += sy;
        }
    }
}

export function rect(x0, y0, x1, y1, filled, plot) {
    const left = Math.min(x0, x1);
    const right = Math.max(x0, x1);
    const top = Math.min(y0, y1);
    const bottom = Math.max(y0, y1);

    if (filled) {
        for (let y = top; y <= bottom; y += 1) {
            for (let x = left; x <= right; x += 1) {
                plot(x, y);
            }
        }
        return;
    }
    for (let x = left; x <= right; x += 1) {
        plot(x, top);
        plot(x, bottom);
    }
    for (let y = top; y <= bottom; y += 1) {
        plot(left, y);
        plot(right, y);
    }
}

// An ellipse inscribed in the rectangle dragged from (x0,y0) to (x1,y1). It uses the
// midpoint algorithm over half the curve and mirrors into four quadrants, giving perfect
// symmetry -- something the "compute sin/cos then round" version never achieved at small
// sizes.
export function ellipse(x0, y0, x1, y1, filled, plot) {
    const left = Math.min(x0, x1);
    const right = Math.max(x0, x1);
    const top = Math.min(y0, y1);
    const bottom = Math.max(y0, y1);
    const a = (right - left) / 2;
    const b = (bottom - top) / 2;
    const cx = left + a;
    const cy = top + b;

    if (a < 0.5 || b < 0.5) {
        line(left, top, right, bottom, plot);
        return;
    }

    // Scan row by row rather than a full midpoint pass: simpler, still symmetric because
    // xLeft/xRight come from the same dx, and at 16..64 px the cost is negligible.
    for (let y = Math.ceil(cy - b); y <= Math.floor(cy + b); y += 1) {
        const dy = (y - cy) / b;
        const inner = 1 - dy * dy;
        if (inner < 0) {
            continue;
        }
        const dx = a * Math.sqrt(inner);
        const xRight = Math.round(cx + dx);
        const xLeft = Math.round(cx - dx);
        if (filled) {
            for (let x = xLeft; x <= xRight; x += 1) {
                plot(x, y);
            }
        } else {
            const isEdgeRow = y === Math.ceil(cy - b) || y === Math.floor(cy + b);
            if (isEdgeRow) {
                for (let x = xLeft; x <= xRight; x += 1) {
                    plot(x, y);
                }
            } else {
                plot(xLeft, y);
                plot(xRight, y);
            }
        }
    }
}

// A 4-way flood fill using scanlines and an explicit stack -- not recursion, which
// overflows the stack on large regions.
export function floodFill(width, height, startX, startY, matches, plot) {
    if (startX < 0 || startY < 0 || startX >= width || startY >= height) {
        return;
    }
    const seen = new Uint8Array(width * height);
    const stack = [startX, startY];

    while (stack.length > 0) {
        const y = stack.pop();
        const x = stack.pop();
        const index = y * width + x;
        if (seen[index] || !matches(x, y)) {
            continue;
        }
        seen[index] = 1;
        plot(x, y);

        if (x > 0) stack.push(x - 1, y);
        if (x < width - 1) stack.push(x + 1, y);
        if (y > 0) stack.push(x, y - 1);
        if (y < height - 1) stack.push(x, y + 1);
    }
}

export function rectUnion(a, b) {
    if (!a) return b;
    if (!b) return a;
    const x = Math.min(a.x, b.x);
    const y = Math.min(a.y, b.y);
    return {
        x,
        y,
        w: Math.max(a.x + a.w, b.x + b.w) - x,
        h: Math.max(a.y + a.h, b.y + b.h) - y,
    };
}

export function rectIntersect(a, b) {
    if (!a || !b) return null;
    const x = Math.max(a.x, b.x);
    const y = Math.max(a.y, b.y);
    const right = Math.min(a.x + a.w, b.x + b.w);
    const bottom = Math.min(a.y + a.h, b.y + b.h);
    if (right <= x || bottom <= y) {
        return null;
    }
    return { x, y, w: right - x, h: bottom - y };
}

export function rectContains(rect2, x, y) {
    return !!rect2 && x >= rect2.x && y >= rect2.y && x < rect2.x + rect2.w && y < rect2.y + rect2.h;
}
