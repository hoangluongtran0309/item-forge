import { BOXES, LEGGINGS_BOXES } from './uv.js';
import { rectIntersect } from './geom.js';

// The 3D preview uses CSS 3D transforms: every face of every box is its OWN <canvas>,
// placed inside a transform-style: preserve-3d chain.
//
// Why not three.js: there is no bundler and no CDN, so it would mean vendoring ~600 KB of
// UMD into the repo and maintaining the version by hand -- all to spin a few boxes with
// nearest-neighbour textures, about the least three.js-shaped problem there is.
//
// Why not a hand-written software rasterizer: a single affine matrix only maps a quad
// correctly while it stays a parallelogram on screen, and it breaks the moment a box is
// tilted. Doing it properly means splitting each face into two triangles,
// perspective-correct UV interpolation, and managing a z-buffer. With CSS 3D the compositor
// does all of that -- correct perspective, correct depth ordering for solid boxes -- and
// rotating costs ZERO lines of JS (just two CSS variables).
//
// image-rendering: pixelated survives 3D transforms, so the pixel art stays crisp.

const SCALE = 7; // CSS px per model pixel
const FACES = ['front', 'back', 'right', 'left', 'top', 'bottom'];

export function mountPreview(container, kind) {
    if (kind === 'cube') {
        return new CubePreview(container);
    }
    if (kind === 'humanoid' || kind === 'humanoid_leggings') {
        return new HumanoidPreview(container, kind === 'humanoid_leggings');
    }
    return new ItemPreview(container);
}

// --- shared infrastructure -----------------------------------------------

function createScene(container) {
    container.textContent = '';
    container.classList.add('relative', 'overflow-hidden', 'select-none');

    const scene = document.createElement('div');
    scene.className = 'absolute inset-0 flex items-center justify-center';
    scene.style.perspective = '900px';

    const rotor = document.createElement('div');
    rotor.style.transformStyle = 'preserve-3d';
    rotor.style.transform = 'rotateX(-18deg) rotateY(28deg)';
    rotor.style.transition = 'none';

    scene.appendChild(rotor);
    container.appendChild(scene);
    installRotation(container, rotor);
    return rotor;
}

// Rotating updates EXACTLY ONE transform on the rotor; nothing is redrawn.
function installRotation(container, rotor) {
    let rx = -18;
    let ry = 28;
    let scale = 1;
    let dragging = null;

    const apply = () => {
        rotor.style.transform = `scale(${scale}) rotateX(${rx}deg) rotateY(${ry}deg)`;
    };

    container.addEventListener('pointerdown', (event) => {
        dragging = { x: event.clientX, y: event.clientY };
        container.setPointerCapture(event.pointerId);
        container.classList.add('cursor-grabbing');
    });
    container.addEventListener('pointermove', (event) => {
        if (!dragging) {
            return;
        }
        ry += (event.clientX - dragging.x) * 0.6;
        // Clamped to +/-89 degrees: past the pole the model flips over and loses its bearings.
        rx = Math.max(-89, Math.min(89, rx - (event.clientY - dragging.y) * 0.6));
        dragging = { x: event.clientX, y: event.clientY };
        apply();
    });
    const stop = () => {
        dragging = null;
        container.classList.remove('cursor-grabbing');
    };
    container.addEventListener('pointerup', stop);
    container.addEventListener('pointercancel', stop);
    container.addEventListener('wheel', (event) => {
        event.preventDefault();
        scale = Math.max(0.4, Math.min(3, scale * (event.deltaY < 0 ? 1.1 : 0.9)));
        apply();
    }, { passive: false });

    apply();
}

/**
 * Builds one textured box. `uv` maps a face name to a rect in the texture; `size` is
 * [w, h, d] in model pixels. `mirrored` flips every face horizontally -- the 64x32 layout has
 * no dedicated region for the left limbs, so the left limb reuses the right limb's rect
 * mirrored.
 */
function buildBox({ size, uv, mirrored }, inflate) {
    const [w, h, d] = size.map((n) => n + inflate * 2);
    const box = document.createElement('div');
    box.style.transformStyle = 'preserve-3d';
    box.style.position = 'absolute';

    const faces = {};
    FACES.forEach((face) => {
        const rect = uv[face];
        const canvas = document.createElement('canvas');
        canvas.width = rect.w;
        canvas.height = rect.h;
        canvas.className = 'image-pixelated absolute';

        const dims = faceDimensions(face, w, h, d);
        canvas.style.width = dims.w * SCALE + 'px';
        canvas.style.height = dims.h * SCALE + 'px';
        canvas.style.left = '50%';
        canvas.style.top = '50%';
        canvas.style.backfaceVisibility = 'hidden';
        canvas.style.transform = `translate(-50%, -50%) ${faceTransform(face, w, h, d)}`
            + (mirrored ? ' scaleX(-1)' : '');

        box.appendChild(canvas);
        faces[face] = { canvas, rect };
    });

    return { element: box, faces };
}

function faceDimensions(face, w, h, d) {
    switch (face) {
        case 'front':
        case 'back':
            return { w, h };
        case 'right':
        case 'left':
            return { w: d, h };
        default:
            return { w, h: d };
    }
}

function faceTransform(face, w, h, d) {
    const half = (n) => (n / 2) * SCALE;
    switch (face) {
        case 'front':
            return `translateZ(${half(d)}px)`;
        case 'back':
            return `rotateY(180deg) translateZ(${half(d)}px)`;
        case 'right':
            return `rotateY(-90deg) translateZ(${half(w)}px)`;
        case 'left':
            return `rotateY(90deg) translateZ(${half(w)}px)`;
        case 'top':
            return `rotateX(90deg) translateZ(${half(h)}px)`;
        default:
            return `rotateX(-90deg) translateZ(${half(h)}px)`;
    }
}

// Drawing a face: EXACTLY ONE putImageData, no scaling, no intermediate buffer.
function paintFace(face, image, width, height) {
    const { canvas, rect } = face;
    const ctx = canvas.getContext('2d');
    ctx.clearRect(0, 0, canvas.width, canvas.height);
    if (rect.x + rect.w > width || rect.y + rect.h > height) {
        return;
    }
    ctx.putImageData(image, -rect.x, -rect.y, rect.x, rect.y, rect.w, rect.h);
}

function toImageData(composite, width, height) {
    return new ImageData(new Uint8ClampedArray(composite), width, height);
}

// --- block: a six-faced cube ----------------------------------------------

class CubePreview {
    constructor(container) {
        const rotor = createScene(container);
        // BlockModelJsonGenerator emits parent: block/cube_all, i.e. ONE texture for all six
        // faces -- so every face takes the whole image. The uv structure here is still
        // per-face, so adding per-face textures later means only changing the rects.
        const full = { x: 0, y: 0, w: 16, h: 16 };
        this._box = buildBox({
            size: [16, 16, 16],
            uv: { front: full, back: full, right: full, left: full, top: full, bottom: full },
        }, 0);
        this._box.element.style.position = 'relative';
        rotor.appendChild(this._box.element);
    }

    update(composite, width, height, dirtyRect) {
        const image = toImageData(composite, width, height);
        const full = { x: 0, y: 0, w: width, h: height };
        FACES.forEach((face) => {
            this._box.faces[face].rect = full;
            if (!dirtyRect || rectIntersect(dirtyRect, full)) {
                this._box.faces[face].canvas.width = width;
                this._box.faces[face].canvas.height = height;
                paintFace(this._box.faces[face], image, width, height);
            }
        });
    }

    destroy() {}
}

// --- armor: a six-box humanoid model with two layers -----------------------

class HumanoidPreview {
    constructor(container, editingLeggings) {
        const rotor = createScene(container);
        this._editingLeggings = editingLeggings;
        this._siblingImage = null;

        const skeleton = document.createElement('div');
        skeleton.style.transformStyle = 'preserve-3d';
        skeleton.style.position = 'relative';
        skeleton.style.width = '0';
        skeleton.style.height = '0';
        rotor.appendChild(skeleton);

        // Three nested box trees. The increasing inflation is what produces correct depth
        // ordering with no z-fighting, no z-index and no manual sorting:
        //   0.00  a neutral mannequin (so a near-transparent texture is still readable)
        //   0.25  layer 1 (helmet/chestplate/boots)
        //   0.55  layer 2 (leggings)
        this._mannequin = this._buildTree(Object.keys(BOXES), 0, skeleton);
        this._layer1 = this._buildTree(Object.keys(BOXES), 0.25, skeleton);
        this._layer2 = this._buildTree(LEGGINGS_BOXES, 0.55, skeleton);

        this._paintMannequin();
    }

    _buildTree(boxNames, inflate, parent) {
        const tree = {};
        boxNames.forEach((name) => {
            const box = buildBox(BOXES[name], inflate);
            const [ax, ay, az] = BOXES[name].anchor;
            box.element.style.transform =
                `translate3d(${ax * SCALE}px, ${ay * SCALE}px, ${az * SCALE}px)`;
            parent.appendChild(box.element);
            tree[name] = box;
        });
        return tree;
    }

    // A flat neutral-grey mannequin -- deliberately not any vanilla skin (a licensing matter,
    // the same reason no Minecraft asset is bundled).
    _paintMannequin() {
        Object.values(this._mannequin).forEach((box) => {
            FACES.forEach((face) => {
                const { canvas } = box.faces[face];
                const ctx = canvas.getContext('2d');
                ctx.fillStyle = '#3c4049';
                ctx.fillRect(0, 0, canvas.width, canvas.height);
            });
        });
    }

    /** Loads the OTHER layer's texture, purely so the preview matches the game. */
    setSiblingImage(rgba, width, height) {
        this._siblingImage = rgba ? { image: toImageData(rgba, width, height), width, height } : null;
        this._paintTree(this._editingLeggings ? this._layer1 : this._layer2,
            this._siblingImage, null);
    }

    update(composite, width, height, dirtyRect) {
        const target = this._editingLeggings ? this._layer2 : this._layer1;
        this._paintTree(target, { image: toImageData(composite, width, height), width, height }, dirtyRect);
    }

    _paintTree(tree, source, dirtyRect) {
        if (!source) {
            Object.values(tree).forEach((box) => {
                box.element.style.display = 'none';
            });
            return;
        }
        Object.values(tree).forEach((box) => {
            box.element.style.display = '';
            FACES.forEach((face) => {
                const entry = box.faces[face];
                // Only faces whose rect INTERSECTS the changed region are redrawn: a stroke in
                // the "Body Front" region dirties just 1 of ~40 canvases.
                if (dirtyRect && !rectIntersect(dirtyRect, entry.rect)) {
                    return;
                }
                paintFace(entry, source.image, source.width, source.height);
            });
        });
    }

    destroy() {}
}

// --- item: the 2D inventory frame -----------------------------------------

class ItemPreview {
    constructor(container) {
        container.textContent = '';
        container.className = 'flex items-center justify-center gap-6 p-4';

        this._slots = [48, 96].map((size) => {
            const slot = document.createElement('div');
            slot.className = 'if-slot';
            slot.style.width = size + 8 + 'px';
            slot.style.height = size + 8 + 'px';
            const canvas = document.createElement('canvas');
            canvas.className = 'image-pixelated';
            canvas.style.width = size + 'px';
            canvas.style.height = size + 'px';
            slot.appendChild(canvas);
            container.appendChild(slot);
            return canvas;
        });
    }

    update(composite, width, height) {
        const image = toImageData(composite, width, height);
        this._slots.forEach((canvas) => {
            canvas.width = width;
            canvas.height = height;
            canvas.getContext('2d').putImageData(image, 0, 0);
        });
    }

    destroy() {}
}
