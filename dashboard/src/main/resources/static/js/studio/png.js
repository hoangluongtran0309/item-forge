// Hand-written 8-bit RGBA PNG encoding and decoding.
//
// Why not canvas.toBlob / drawImage+getImageData: a 2D canvas's backing store uses
// premultiplied alpha, so a pixel of rgba(255,0,0,4) put through a canvas comes back with
// different RGB. For an editor with an alpha slider that is real data loss. Encoding it
// ourselves means the alpha byte written out is exactly the byte the user painted.
//
// Deflate/inflate use the browser's built-in CompressionStream/DecompressionStream --
// 'deflate' in the spec is the zlib format (RFC 1950), which is exactly what the IDAT chunk
// requires. When the browser lacks them we fall back to the canvas path and log a warning
// (alpha precision is lost, but it still works).

const SIGNATURE = [0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a];

const CRC_TABLE = (() => {
    const table = new Uint32Array(256);
    for (let n = 0; n < 256; n += 1) {
        let c = n;
        for (let k = 0; k < 8; k += 1) {
            c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1;
        }
        table[n] = c >>> 0;
    }
    return table;
})();

function crc32(bytes) {
    let c = 0xffffffff;
    for (let i = 0; i < bytes.length; i += 1) {
        c = CRC_TABLE[(c ^ bytes[i]) & 0xff] ^ (c >>> 8);
    }
    return (c ^ 0xffffffff) >>> 0;
}

function chunk(type, body) {
    const typeBytes = new Uint8Array([...type].map((ch) => ch.charCodeAt(0)));
    const out = new Uint8Array(12 + body.length);
    const view = new DataView(out.buffer);
    view.setUint32(0, body.length);
    out.set(typeBytes, 4);
    out.set(body, 8);
    const crcInput = new Uint8Array(4 + body.length);
    crcInput.set(typeBytes, 0);
    crcInput.set(body, 4);
    view.setUint32(8 + body.length, crc32(crcInput));
    return out;
}

async function deflate(bytes) {
    const stream = new Blob([bytes]).stream().pipeThrough(new CompressionStream('deflate'));
    return new Uint8Array(await new Response(stream).arrayBuffer());
}

async function inflate(bytes) {
    const stream = new Blob([bytes]).stream().pipeThrough(new DecompressionStream('deflate'));
    return new Uint8Array(await new Response(stream).arrayBuffer());
}

export function hasNativeCompression() {
    return typeof CompressionStream !== 'undefined' && typeof DecompressionStream !== 'undefined';
}

export async function encodePng(rgba, width, height) {
    if (!hasNativeCompression()) {
        console.warn('[studio] CompressionStream unavailable, falling back to canvas encoding '
            + '(partial alpha may be altered).');
        return encodeViaCanvas(rgba, width, height);
    }

    // Each row is prefixed with one filter-type byte = 0 (None). No other filter is used:
    // at 16x16..64x64 the compression gain is not worth the complexity.
    const raw = new Uint8Array(height * (width * 4 + 1));
    for (let y = 0; y < height; y += 1) {
        const rowStart = y * (width * 4 + 1);
        raw[rowStart] = 0;
        raw.set(rgba.subarray(y * width * 4, (y + 1) * width * 4), rowStart + 1);
    }

    const ihdr = new Uint8Array(13);
    const ihdrView = new DataView(ihdr.buffer);
    ihdrView.setUint32(0, width);
    ihdrView.setUint32(4, height);
    ihdr[8] = 8; // bit depth
    ihdr[9] = 6; // colour type 6 = truecolour with alpha
    ihdr[10] = 0; // deflate
    ihdr[11] = 0; // adaptive filtering
    ihdr[12] = 0; // no interlace

    const parts = [
        new Uint8Array(SIGNATURE),
        chunk('IHDR', ihdr),
        chunk('IDAT', await deflate(raw)),
        chunk('IEND', new Uint8Array(0)),
    ];
    return new Blob(parts, { type: 'image/png' });
}

function encodeViaCanvas(rgba, width, height) {
    const canvas = document.createElement('canvas');
    canvas.width = width;
    canvas.height = height;
    const ctx = canvas.getContext('2d');
    ctx.putImageData(new ImageData(new Uint8ClampedArray(rgba), width, height), 0, 0);
    return new Promise((resolve) => canvas.toBlob(resolve, 'image/png'));
}

/**
 * Reads a PNG into {width, height, rgba}. Only the most common case is handled directly
 * (8-bit, non-interlaced, colour type 6/2/0); everything else falls through to the
 * createImageBitmap path with premultiplyAlpha:'none'.
 */
export async function decodePng(blob) {
    const bytes = new Uint8Array(await blob.arrayBuffer());
    try {
        const direct = await decodeDirect(bytes);
        if (direct) {
            return direct;
        }
    } catch (e) {
        console.warn('[studio] direct PNG decode failed, falling back to ImageBitmap', e);
    }
    return decodeViaBitmap(blob);
}

async function decodeDirect(bytes) {
    if (!hasNativeCompression()) {
        return null;
    }
    for (let i = 0; i < SIGNATURE.length; i += 1) {
        if (bytes[i] !== SIGNATURE[i]) {
            throw new Error('not a PNG');
        }
    }

    const view = new DataView(bytes.buffer, bytes.byteOffset, bytes.byteLength);
    let offset = 8;
    let header = null;
    const idatParts = [];

    while (offset < bytes.length) {
        const length = view.getUint32(offset);
        const type = String.fromCharCode(bytes[offset + 4], bytes[offset + 5], bytes[offset + 6], bytes[offset + 7]);
        const body = bytes.subarray(offset + 8, offset + 8 + length);

        if (type === 'IHDR') {
            header = {
                width: view.getUint32(offset + 8),
                height: view.getUint32(offset + 12),
                bitDepth: body[8],
                colourType: body[9],
                interlace: body[12],
            };
        } else if (type === 'IDAT') {
            idatParts.push(body.slice());
        } else if (type === 'IEND') {
            break;
        }
        offset += 12 + length;
    }

    if (!header || header.bitDepth !== 8 || header.interlace !== 0) {
        return null;
    }
    const channels = { 0: 1, 2: 3, 4: 2, 6: 4 }[header.colourType];
    if (!channels) {
        return null;
    }

    const totalLength = idatParts.reduce((sum, part) => sum + part.length, 0);
    const compressed = new Uint8Array(totalLength);
    let cursor = 0;
    idatParts.forEach((part) => {
        compressed.set(part, cursor);
        cursor += part.length;
    });

    const raw = await inflate(compressed);
    const { width, height } = header;
    const stride = width * channels;
    const rgba = new Uint8ClampedArray(width * height * 4);
    const previous = new Uint8Array(stride);
    const current = new Uint8Array(stride);

    for (let y = 0; y < height; y += 1) {
        const rowStart = y * (stride + 1);
        const filter = raw[rowStart];
        current.set(raw.subarray(rowStart + 1, rowStart + 1 + stride));
        unfilterRow(filter, current, previous, channels);

        for (let x = 0; x < width; x += 1) {
            const src = x * channels;
            const dst = (y * width + x) * 4;
            if (channels === 4) {
                rgba[dst] = current[src];
                rgba[dst + 1] = current[src + 1];
                rgba[dst + 2] = current[src + 2];
                rgba[dst + 3] = current[src + 3];
            } else if (channels === 3) {
                rgba[dst] = current[src];
                rgba[dst + 1] = current[src + 1];
                rgba[dst + 2] = current[src + 2];
                rgba[dst + 3] = 255;
            } else if (channels === 2) {
                rgba[dst] = current[src];
                rgba[dst + 1] = current[src];
                rgba[dst + 2] = current[src];
                rgba[dst + 3] = current[src + 1];
            } else {
                rgba[dst] = current[src];
                rgba[dst + 1] = current[src];
                rgba[dst + 2] = current[src];
                rgba[dst + 3] = 255;
            }
        }
        previous.set(current);
    }

    return { width, height, rgba };
}

// Unfiltering per RFC 2083 section 6. `bpp` is the byte size of one pixel, used as the
// distance to the pixel "to the left".
function unfilterRow(filter, row, previous, bpp) {
    const length = row.length;
    switch (filter) {
        case 0:
            return;
        case 1:
            for (let i = bpp; i < length; i += 1) {
                row[i] = (row[i] + row[i - bpp]) & 0xff;
            }
            return;
        case 2:
            for (let i = 0; i < length; i += 1) {
                row[i] = (row[i] + previous[i]) & 0xff;
            }
            return;
        case 3:
            for (let i = 0; i < length; i += 1) {
                const left = i >= bpp ? row[i - bpp] : 0;
                row[i] = (row[i] + ((left + previous[i]) >> 1)) & 0xff;
            }
            return;
        case 4:
            for (let i = 0; i < length; i += 1) {
                const a = i >= bpp ? row[i - bpp] : 0;
                const b = previous[i];
                const c = i >= bpp ? previous[i - bpp] : 0;
                row[i] = (row[i] + paeth(a, b, c)) & 0xff;
            }
            return;
        default:
            throw new Error('unknown PNG filter type ' + filter);
    }
}

function paeth(a, b, c) {
    const p = a + b - c;
    const pa = Math.abs(p - a);
    const pb = Math.abs(p - b);
    const pc = Math.abs(p - c);
    if (pa <= pb && pa <= pc) return a;
    return pb <= pc ? b : c;
}

async function decodeViaBitmap(blob) {
    const bitmap = await createImageBitmap(blob, { premultiplyAlpha: 'none' });
    const canvas = document.createElement('canvas');
    canvas.width = bitmap.width;
    canvas.height = bitmap.height;
    const ctx = canvas.getContext('2d');
    ctx.drawImage(bitmap, 0, 0);
    const data = ctx.getImageData(0, 0, bitmap.width, bitmap.height);
    bitmap.close();
    return { width: data.width, height: data.height, rgba: data.data };
}
