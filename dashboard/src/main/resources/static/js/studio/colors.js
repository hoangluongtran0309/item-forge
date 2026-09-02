// Color conversion. A color in the Studio is always an [r, g, b, a] array with each
// component 0..255 and STRAIGHT (non-premultiplied) alpha -- see doc.js for why the
// canvas's ImageData is not used as the source of truth.

export function parseHex(text) {
    const hex = String(text).trim().replace(/^#/, '');
    if (!/^([0-9a-fA-F]{3,4}|[0-9a-fA-F]{6}|[0-9a-fA-F]{8})$/.test(hex)) {
        return null;
    }
    // The short forms (#rgb / #rgba) expand by doubling each character: f -> ff, following
    // the CSS convention rather than f -> f0.
    const expand = hex.length <= 4 ? hex.split('').map((c) => c + c).join('') : hex;
    return [
        parseInt(expand.slice(0, 2), 16),
        parseInt(expand.slice(2, 4), 16),
        parseInt(expand.slice(4, 6), 16),
        expand.length === 8 ? parseInt(expand.slice(6, 8), 16) : 255,
    ];
}

export function toHex(rgba, withAlpha = false) {
    const part = (n) => Math.max(0, Math.min(255, Math.round(n))).toString(16).padStart(2, '0');
    const base = '#' + part(rgba[0]) + part(rgba[1]) + part(rgba[2]);
    return withAlpha ? base + part(rgba[3] === undefined ? 255 : rgba[3]) : base;
}

export function toCss(rgba) {
    const a = (rgba[3] === undefined ? 255 : rgba[3]) / 255;
    return `rgba(${rgba[0]}, ${rgba[1]}, ${rgba[2]}, ${a})`;
}

export function sameColor(a, b) {
    return a[0] === b[0] && a[1] === b[1] && a[2] === b[2] && a[3] === b[3];
}

// h: 0..360, s/v: 0..1
export function hsvToRgb(h, s, v) {
    const c = v * s;
    const hp = ((h % 360) + 360) % 360 / 60;
    const x = c * (1 - Math.abs((hp % 2) - 1));
    const m = v - c;
    let rgb;
    if (hp < 1) rgb = [c, x, 0];
    else if (hp < 2) rgb = [x, c, 0];
    else if (hp < 3) rgb = [0, c, x];
    else if (hp < 4) rgb = [0, x, c];
    else if (hp < 5) rgb = [x, 0, c];
    else rgb = [c, 0, x];
    return rgb.map((n) => Math.round((n + m) * 255));
}

export function rgbToHsv(r, g, b) {
    const rn = r / 255;
    const gn = g / 255;
    const bn = b / 255;
    const max = Math.max(rn, gn, bn);
    const min = Math.min(rn, gn, bn);
    const d = max - min;

    let h = 0;
    if (d !== 0) {
        if (max === rn) h = 60 * (((gn - bn) / d) % 6);
        else if (max === gn) h = 60 * ((bn - rn) / d + 2);
        else h = 60 * ((rn - gn) / d + 4);
    }
    if (h < 0) h += 360;

    return { h, s: max === 0 ? 0 : d / max, v: max };
}
