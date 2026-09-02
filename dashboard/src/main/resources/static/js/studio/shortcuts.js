import { TOOLS, ACTIONS } from './tools.js';

// An Aseprite-style keymap. A single listener, attached to document when the workspace
// mounts and removed when it unmounts.
//
// The one mandatory guard: never swallow a key while the cursor is inside an input --
// without it, typing "b" into a layer name field would switch tools.
function typingInside(target) {
    return !!(target && target.closest && target.closest('input, textarea, select, [contenteditable="true"]'));
}

export function installShortcuts(studio) {
    const onKeyDown = (event) => {
        if (typingInside(event.target)) {
            return;
        }

        const mod = event.ctrlKey || event.metaKey;

        if (mod && event.key.toLowerCase() === 'z') {
            event.preventDefault();
            if (event.shiftKey) {
                studio.redo();
            } else {
                studio.undo();
            }
            return;
        }
        if (mod && event.key.toLowerCase() === 'y') {
            event.preventDefault();
            studio.redo();
            return;
        }
        if (mod && event.key.toLowerCase() === 's') {
            event.preventDefault();
            studio.save();
            return;
        }
        if (mod && event.key.toLowerCase() === 'a') {
            event.preventDefault();
            studio.selectAll();
            return;
        }
        if (mod) {
            return;
        }

        switch (event.key) {
            case 'Escape':
                studio.deselect();
                return;
            case 'Delete':
            case 'Backspace':
                event.preventDefault();
                ACTIONS.clearSelection(studio.toolContext());
                return;
            case '[':
                studio.setBrushSize(studio.brushSize - 1);
                return;
            case ']':
                studio.setBrushSize(studio.brushSize + 1);
                return;
            case '+':
            case '=':
                studio.viewport.setZoom(studio.viewport.zoom + 1, null);
                return;
            case '-':
                studio.viewport.setZoom(studio.viewport.zoom - 1, null);
                return;
            case 'g':
            case 'G':
                // G is Fill (bucket) as in Aseprite; toggling the grid is Shift+G.
                if (event.shiftKey) {
                    studio.toggleGrid();
                    return;
                }
                break;
            case 'h':
            case 'H':
                if (event.shiftKey) {
                    ACTIONS.flipHorizontal(studio.toolContext());
                    return;
                }
                break;
            case 'v':
            case 'V':
                if (event.shiftKey) {
                    ACTIONS.flipVertical(studio.toolContext());
                    return;
                }
                break;
            case ' ':
                // Space is held to pan: suppress page scrolling while it is down.
                event.preventDefault();
                studio.setSpaceHeld(true);
                return;
            default:
                break;
        }

        // Shift+U = ellipse, U = rectangle: one shape group, as in the reference editor.
        if (event.key === 'U') {
            studio.setTool('ellipse');
            return;
        }

        const tool = TOOLS.find((t) => t.shortcut === event.key.toLowerCase());
        if (tool) {
            studio.setTool(tool.id);
        }
    };

    const onKeyUp = (event) => {
        if (event.key === ' ') {
            studio.setSpaceHeld(false);
        }
    };

    document.addEventListener('keydown', onKeyDown);
    document.addEventListener('keyup', onKeyUp);

    return () => {
        document.removeEventListener('keydown', onKeyDown);
        document.removeEventListener('keyup', onKeyUp);
    };
}
