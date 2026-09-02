// Minecraft's "classic" UV layout for a 64x32 texture (shared by player skins and armor
// layers). Derived from standard knowledge of the Minecraft format, NOT extracted from any
// sample file in this repository.
//
// The data is organised BY BOX rather than as a flat array of rects, so the 2D UV guide
// overlay and the 3D preview use EXACTLY ONE source of truth. `size` is [width, height,
// depth] in model pixels; `anchor` is the box's position within the skeleton, in the same
// units.
//
// The 64x32 layout has NO dedicated region for the LEFT arm and leg -- the game mirrors them
// from the right ones at render time. So the left boxes reuse the right boxes' rects,
// flipped horizontally; see mirrorOf() below.

const RIGHT_ARM = {
    size: [4, 12, 4],
    anchor: [-6, 0, 0],
    uv: {
        top: { x: 44, y: 16, w: 4, h: 4 },
        bottom: { x: 48, y: 16, w: 4, h: 4 },
        right: { x: 40, y: 20, w: 4, h: 12 },
        front: { x: 44, y: 20, w: 4, h: 12 },
        left: { x: 48, y: 20, w: 4, h: 12 },
        back: { x: 52, y: 20, w: 4, h: 12 },
    },
};

const RIGHT_LEG = {
    size: [4, 12, 4],
    anchor: [-2, 12, 0],
    uv: {
        top: { x: 4, y: 16, w: 4, h: 4 },
        bottom: { x: 8, y: 16, w: 4, h: 4 },
        right: { x: 0, y: 20, w: 4, h: 12 },
        front: { x: 4, y: 20, w: 4, h: 12 },
        left: { x: 8, y: 20, w: 4, h: 12 },
        back: { x: 12, y: 20, w: 4, h: 12 },
    },
};

function mirrorOf(box, anchor) {
    return {
        size: box.size.slice(),
        anchor,
        mirrored: true,
        // Swap left/right: after mirroring, the left limb's "outer" face is the right limb's
        // "inner" face.
        uv: {
            top: box.uv.top,
            bottom: box.uv.bottom,
            right: box.uv.left,
            front: box.uv.front,
            left: box.uv.right,
            back: box.uv.back,
        },
    };
}

export const BOXES = {
    head: {
        size: [8, 8, 8],
        anchor: [0, -8, 0],
        uv: {
            top: { x: 8, y: 0, w: 8, h: 8 },
            bottom: { x: 16, y: 0, w: 8, h: 8 },
            right: { x: 0, y: 8, w: 8, h: 8 },
            front: { x: 8, y: 8, w: 8, h: 8 },
            left: { x: 16, y: 8, w: 8, h: 8 },
            back: { x: 24, y: 8, w: 8, h: 8 },
        },
    },
    body: {
        size: [8, 12, 4],
        anchor: [0, 0, 0],
        uv: {
            top: { x: 20, y: 16, w: 8, h: 4 },
            bottom: { x: 28, y: 16, w: 8, h: 4 },
            right: { x: 16, y: 20, w: 4, h: 12 },
            front: { x: 20, y: 20, w: 8, h: 12 },
            left: { x: 28, y: 20, w: 4, h: 12 },
            back: { x: 32, y: 20, w: 8, h: 12 },
        },
    },
    armR: RIGHT_ARM,
    armL: mirrorOf(RIGHT_ARM, [6, 0, 0]),
    legR: RIGHT_LEG,
    legL: mirrorOf(RIGHT_LEG, [2, 12, 0]),
};

const BOX_LABELS = {
    head: 'Head',
    body: 'Body',
    armR: 'R.Arm',
    armL: 'L.Arm',
    legR: 'R.Leg',
    legL: 'L.Leg',
};

const FACE_LABELS = {
    top: 'Top',
    bottom: 'Bottom',
    right: 'Right',
    front: 'Front',
    left: 'Left',
    back: 'Back',
};

// Layer 2 (leggings) covers only the body and both legs -- matching how the game stacks them.
export const LEGGINGS_BOXES = ['body', 'legR', 'legL'];

// The guide cells for the 2D overlay. Taken only from the NON-mirrored boxes: the left
// boxes reuse the right boxes' exact rects, so including them would just draw over the
// same area twice.
function buildGuideRects() {
    const rects = [];
    ['head', 'body', 'armR', 'legR'].forEach((boxName) => {
        const box = BOXES[boxName];
        ['top', 'bottom', 'right', 'front', 'left', 'back'].forEach((face) => {
            const uv = box.uv[face];
            rects.push({
                box: boxName,
                face,
                label: BOX_LABELS[boxName] + ' ' + FACE_LABELS[face],
                x: uv.x,
                y: uv.y,
                w: uv.w,
                h: uv.h,
            });
        });
    });
    return rects;
}

export const GUIDE_RECTS = buildGuideRects();

export function guideRectsFor(kind) {
    if (kind === 'humanoid') {
        return GUIDE_RECTS;
    }
    if (kind === 'humanoid_leggings') {
        return GUIDE_RECTS.filter((rect) => LEGGINGS_BOXES.includes(rect.box));
    }
    return [];
}

export function guideRectAt(kind, x, y) {
    return guideRectsFor(kind).find(
        (rect) => x >= rect.x && y >= rect.y && x < rect.x + rect.w && y < rect.y + rect.h,
    ) || null;
}
