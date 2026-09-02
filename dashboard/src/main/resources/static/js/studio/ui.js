import { TOOLS, ACTIONS } from './tools.js';
import { openReferencePicker } from './reference.js';
import { createColorWheel } from './color-wheel.js';

// The Studio's panels. Every Tailwind class here is written out IN FULL as a string
// literal, never assembled from pieces: Tailwind scans static/js with a regex looking for
// class candidates, and 'btn-' + variant would be purged from the built CSS.

const TOOL_ICONS = {
    pencil: '&#9998;',
    eraser: '&#9003;',
    bucket: '&#9832;',
    eyedropper: '&#9684;',
    line: '&#8726;',
    rect: '&#9645;',
    ellipse: '&#9711;',
    select: '&#8863;',
    move: '&#10021;',
};

export function mountPanels(root, studio) {
    const toolRail = root.querySelector('[data-studio-toolrail]');
    const toolOptions = root.querySelector('[data-studio-tooloptions]');
    const layersPanel = root.querySelector('[data-studio-layers]');
    const colourPanel = root.querySelector('[data-studio-colour]');
    const statusBar = root.querySelector('[data-studio-status]');

    buildToolRail(toolRail, studio);
    buildToolOptions(toolOptions, studio);
    wireHeaderButtons(root, studio);
    wirePreviewCollapse(root);

    // The color wheel is built EXACTLY ONCE and only setColor()'d afterwards. It owns h/s/v
    // as state, so rebuilding it on every color change would lose the hue whenever the
    // current color is black or grey -- see the note in color-wheel.js.
    const wheel = createColorWheel(colourPanel, {
        size: 208,
        onChange: (rgba) => studio.setPrimary(rgba),
    });

    const api = {
        refreshToolbar() {
            refreshToolbar(root, studio);
        },
        refreshLayers() {
            renderLayers(layersPanel, studio);
        },
        refreshColour() {
            wheel.setColor(studio.primary);
        },
        refreshHistory() {
            refreshHistory(root, studio);
        },
        refreshPosition(p) {
            const label = statusBar.querySelector('[data-studio-position]');
            label.textContent = `${p.x}, ${p.y}`;
        },
        refreshAll() {
            api.refreshToolbar();
            api.refreshLayers();
            api.refreshColour();
            api.refreshHistory();
            const size = statusBar.querySelector('[data-studio-size]');
            size.textContent = `${studio.doc.width} x ${studio.doc.height}`;
        },
        destroy() {
            wheel.destroy();
        },
    };

    api.refreshAll();
    return api;
}

const PREVIEW_COLLAPSED_KEY = 'itemforge-studio-preview-collapsed';

// Preview is the only collapsible thing in the sidebar: it takes the most room and not
// everyone needs it (for an item icon the inventory frame is enough). Color and Layers stay
// visible always -- both sit in the continuous working loop.
function wirePreviewCollapse(root) {
    const toggle = root.querySelector('[data-studio-preview-toggle]');
    const body = root.querySelector('[data-studio-preview-body]');
    const caret = root.querySelector('[data-studio-preview-caret]');
    if (!toggle || !body) {
        return;
    }

    const apply = (collapsed) => {
        body.hidden = collapsed;
        caret.textContent = collapsed ? '▶' : '▼';
        toggle.setAttribute('aria-expanded', collapsed ? 'false' : 'true');
    };

    let collapsed = false;
    try {
        collapsed = localStorage.getItem(PREVIEW_COLLAPSED_KEY) === 'true';
    } catch (e) {
        // localStorage is unavailable -- default to expanded.
    }
    apply(collapsed);

    toggle.addEventListener('click', () => {
        collapsed = !collapsed;
        apply(collapsed);
        try {
            localStorage.setItem(PREVIEW_COLLAPSED_KEY, String(collapsed));
        } catch (e) {
            // If it cannot be saved, so be it -- this is not a fatal error.
        }
    });
}

// --- tool rail + per-tool options -----------------------------------------

function buildToolRail(rail, studio) {
    TOOLS.forEach((tool) => {
        const button = document.createElement('button');
        button.type = 'button';
        button.className = 'if-toolbtn';
        button.dataset.tool = tool.id;
        button.title = `${tool.label} (${tool.shortcut.toUpperCase()})`;
        button.setAttribute('aria-label', tool.label);
        button.innerHTML = TOOL_ICONS[tool.id] || '?';
        button.addEventListener('click', () => studio.setTool(tool.id));
        rail.appendChild(button);
    });
}

function buildToolOptions(wrap, studio) {
    const brush = document.createElement('label');
    brush.className = 'flex items-center gap-2 text-xs';
    brush.innerHTML = '<span class="itemforge-tech-label">Brush</span>';
    const brushInput = document.createElement('input');
    brushInput.type = 'range';
    brushInput.min = '1';
    brushInput.max = '8';
    brushInput.value = String(studio.brushSize);
    brushInput.className = 'range range-xs w-20';
    brushInput.addEventListener('input', () => studio.setBrushSize(Number(brushInput.value)));
    const brushValue = document.createElement('span');
    brushValue.className = 'font-mono text-xs w-4';
    brushValue.dataset.studioBrushValue = 'true';
    brushValue.textContent = String(studio.brushSize);
    brush.append(brushInput, brushValue);

    wrap.appendChild(brush);
    wrap.appendChild(divider());
    wrap.appendChild(toggleButton('Mirror X', 'mirrorX', () => studio.setMirror('x', !studio.mirrorX)));
    wrap.appendChild(toggleButton('Mirror Y', 'mirrorY', () => studio.setMirror('y', !studio.mirrorY)));
    wrap.appendChild(toggleButton('Filled', 'filled', () => {
        studio.filledShapes = !studio.filledShapes;
        refreshToolbar(wrap.closest('[data-studio-root]'), studio);
    }));
    wrap.appendChild(toggleButton('Grid', 'grid', () => studio.toggleGrid()));
    wrap.appendChild(divider());
    wrap.appendChild(actionButton('Flip H', () => ACTIONS.flipHorizontal(studio.toolContext())));
    wrap.appendChild(actionButton('Flip V', () => ACTIONS.flipVertical(studio.toolContext())));
}

function divider() {
    const line = document.createElement('span');
    line.className = 'h-4 w-px bg-base-300';
    return line;
}

function toggleButton(label, key, onClick) {
    const button = document.createElement('button');
    button.type = 'button';
    button.className = 'btn btn-ghost btn-xs';
    button.dataset.toggle = key;
    button.textContent = label;
    button.addEventListener('click', onClick);
    return button;
}

function actionButton(label, onClick) {
    const button = document.createElement('button');
    button.type = 'button';
    button.className = 'btn btn-ghost btn-xs';
    button.textContent = label;
    button.addEventListener('click', onClick);
    return button;
}

// Takes `root` (not just the toolbar) because the tools and their options now live in two
// different blocks: the vertical rail on the left and the row below the canvas.
function refreshToolbar(root, studio) {
    if (!root) {
        return;
    }
    root.querySelectorAll('[data-tool]').forEach((button) => {
        button.classList.toggle('if-toolbtn-active', button.dataset.tool === studio.activeToolId);
    });
    const states = {
        mirrorX: studio.mirrorX,
        mirrorY: studio.mirrorY,
        filled: studio.filledShapes,
        grid: studio.viewport.showGrid,
    };
    root.querySelectorAll('[data-toggle]').forEach((button) => {
        const on = states[button.dataset.toggle];
        button.classList.toggle('btn-active', !!on);
        button.setAttribute('aria-pressed', on ? 'true' : 'false');
    });
    const brushValue = root.querySelector('[data-studio-brush-value]');
    if (brushValue) {
        brushValue.textContent = String(studio.brushSize);
    }
    const zoom = root.querySelector('[data-studio-zoom]');
    if (zoom) {
        zoom.textContent = studio.viewport.zoom + 'x';
    }
}

function refreshHistory(root, studio) {
    const undo = root.querySelector('[data-studio-undo]');
    const redo = root.querySelector('[data-studio-redo]');
    if (undo) {
        undo.disabled = !studio.doc.canUndo();
    }
    if (redo) {
        redo.disabled = !studio.doc.canRedo();
    }
}

// --- header / save state -------------------------------------------------

function wireHeaderButtons(root, studio) {
    const on = (selector, handler, event = 'click') => {
        const el = root.querySelector(selector);
        if (el) {
            el.addEventListener(event, handler);
        }
    };

    on('[data-studio-undo]', () => studio.undo());
    on('[data-studio-redo]', () => studio.redo());
    on('[data-studio-save]', () => studio.save());
    on('[data-studio-close]', () => studio.requestClose());
    on('[data-studio-download]', () => studio.downloadPng());
    on('[data-studio-fit]', () => {
        studio.viewport.fitToWrapper();
        refreshToolbar(root, studio);
    });
    on('[data-studio-reload]', async () => {
        if (!studio.config.loadUrl) {
            return;
        }
        const ok = await studio.loadFromUrl(studio.config.loadUrl);
        if (!ok) {
            showToast(root, 'No texture saved on the server yet.', 'info');
        }
    });

    on('[data-studio-reference]', () => {
        const opened = openReferencePicker((file) => studio.importFile(file));
        if (!opened) {
            showToast(root, 'The reference picker is not available on this page.', 'info');
        }
    });

    const importInput = root.querySelector('[data-studio-import]');
    if (importInput) {
        importInput.addEventListener('change', () => {
            if (importInput.files && importInput.files[0]) {
                studio.importFile(importInput.files[0]);
                importInput.value = '';
            }
        });
    }

    // The canvas is a drop zone too: drag any PNG onto it to use as a starting point.
    const viewport = root.querySelector('[data-studio-viewport]');
    viewport.addEventListener('dragover', (event) => event.preventDefault());
    viewport.addEventListener('drop', (event) => {
        event.preventDefault();
        const file = event.dataTransfer.files && event.dataTransfer.files[0];
        if (file) {
            studio.importFile(file);
        }
    });
}

export function setSaveState(root, isDirty, saving = false) {
    const save = root.querySelector('[data-studio-save]');
    const badge = root.querySelector('[data-studio-dirty]');
    if (save) {
        save.disabled = saving;
        save.classList.toggle('loading', saving);
    }
    if (badge) {
        badge.hidden = !isDirty;
    }
}

// --- layers --------------------------------------------------------------

function renderLayers(panel, studio) {
    if (!panel) {
        return;
    }
    panel.textContent = '';

    const header = document.createElement('div');
    header.className = 'flex shrink-0 items-center gap-1 pb-2';
    const title = document.createElement('span');
    title.className = 'itemforge-tech-label flex-1 uppercase';
    title.textContent = 'Layers';
    header.appendChild(title);

    const add = actionButton('+', () => studio.doc.addLayer());
    add.title = 'Add a layer above the current one';
    header.appendChild(add);
    panel.appendChild(header);

    // Layer index 0 is the BOTTOM one, but users expect the topmost layer at the top of the
    // list -- so it is rendered in reverse.
    const list = document.createElement('ul');
    list.className = 'flex min-h-0 flex-1 flex-col gap-1 overflow-y-auto';
    studio.doc.layers.slice().reverse().forEach((layer) => {
        list.appendChild(layerRow(layer, studio));
    });
    panel.appendChild(list);
}

function layerRow(layer, studio) {
    const isActive = studio.doc.activeLayer().id === layer.id;
    const row = document.createElement('li');
    row.className = isActive
        ? 'flex flex-col gap-1 rounded border border-primary/60 bg-primary/10 p-2'
        : 'flex flex-col gap-1 rounded border border-base-300 bg-base-200 p-2';

    const top = document.createElement('div');
    top.className = 'flex items-center gap-2';

    const visible = document.createElement('input');
    visible.type = 'checkbox';
    visible.checked = layer.visible;
    visible.className = 'checkbox checkbox-xs shrink-0';
    visible.title = 'Toggle visibility';
    visible.addEventListener('change', () => studio.doc.setLayerVisible(layer.id, visible.checked));

    const name = document.createElement('button');
    name.type = 'button';
    name.className = 'min-w-0 flex-1 truncate text-left text-sm';
    name.textContent = layer.name;
    name.title = 'Select this layer';
    name.addEventListener('click', () => {
        studio.doc.activeIndex = studio.doc.layers.findIndex((l) => l.id === layer.id);
        renderLayers(studio.root.querySelector('[data-studio-layers]'), studio);
    });

    top.append(visible, name, layerMenu(layer, studio));
    row.appendChild(top);

    // Opacity is shown only on the SELECTED layer: in a 320px sidebar, four stacked sliders
    // produce more noise than information, and changing a layer's opacity means selecting it
    // first anyway.
    if (isActive) {
        const opacity = document.createElement('input');
        opacity.type = 'range';
        opacity.min = '0';
        opacity.max = '100';
        opacity.value = String(Math.round(layer.opacity * 100));
        opacity.className = 'range range-xs';
        opacity.title = 'Layer opacity';
        opacity.addEventListener('input', () => studio.doc.setLayerOpacity(layer.id, Number(opacity.value) / 100));
        row.appendChild(opacity);
    }

    return row;
}

// The less-used actions are collected into a menu so each row holds only: visibility, name,
// and one button. A smaller layer list is much faster to read.
function layerMenu(layer, studio) {
    const wrap = document.createElement('div');
    wrap.className = 'dropdown dropdown-end shrink-0';

    const trigger = document.createElement('div');
    trigger.tabIndex = 0;
    trigger.setAttribute('role', 'button');
    trigger.className = 'btn btn-ghost btn-xs px-1';
    trigger.textContent = '\u22ee';
    trigger.title = 'Layer actions';

    const menu = document.createElement('ul');
    menu.tabIndex = 0;
    menu.className = 'menu dropdown-content z-50 w-44 rounded-box border border-base-300 '
        + 'bg-base-100 p-1 shadow-elevated';

    const isTop = studio.doc.layers[studio.doc.layers.length - 1].id === layer.id;
    const isBottom = studio.doc.layers[0].id === layer.id;

    menu.appendChild(menuItem('Move up', () => studio.doc.moveLayer(layer.id, 1), isTop));
    menu.appendChild(menuItem('Move down', () => studio.doc.moveLayer(layer.id, -1), isBottom));
    // Merge down folds into the layer directly below and is EXACTLY ONE undo step, so the
    // bottom layer has nothing to merge into.
    menu.appendChild(menuItem('Merge down', () => {
        studio.doc.activeIndex = studio.doc.layers.findIndex((l) => l.id === layer.id);
        studio.doc.mergeDown();
    }, isBottom));
    menu.appendChild(menuItem('Rename\u2026', () => {
        const next = window.prompt('Layer name', layer.name);
        if (next !== null && next.trim() !== '') {
            studio.doc.renameLayer(layer.id, next.trim());
        }
    }, false));
    menu.appendChild(menuItem('Delete', () => studio.doc.removeLayer(layer.id),
        studio.doc.layers.length <= 1, true));

    wrap.append(trigger, menu);
    return wrap;
}

function menuItem(label, onClick, disabled, danger) {
    const li = document.createElement('li');
    const button = document.createElement('button');
    button.type = 'button';
    button.textContent = label;
    button.disabled = !!disabled;
    if (danger) {
        button.className = 'text-error';
    }
    button.addEventListener('click', () => {
        // Close the dropdown: daisyUI opens and closes it via :focus, so focus has to be dropped.
        if (document.activeElement instanceof HTMLElement) {
            document.activeElement.blur();
        }
        onClick();
    });
    li.appendChild(button);
    return li;
}

// --- dialogs / toasts ----------------------------------------------------

export function showToast(root, message, kind = 'info') {
    const container = root.querySelector('[data-studio-toasts]') || document.getElementById('toast-container');
    if (!container) {
        return;
    }
    const alert = document.createElement('div');
    const classes = {
        success: 'alert alert-success shadow-lg',
        error: 'alert alert-error shadow-lg',
        info: 'alert alert-info shadow-lg',
    };
    alert.className = classes[kind] || classes.info;
    alert.textContent = message;
    container.appendChild(alert);
    setTimeout(() => alert.remove(), 5000);
}

/**
 * A replacement for window.confirm: it uses daisyUI's <dialog> to match the rest of the
 * app, and because the native confirm() blocks the whole event loop.
 */
export function confirmDialog(title, body) {
    return new Promise((resolve) => {
        const dialog = document.getElementById('studio-confirm');
        if (!dialog) {
            resolve(window.confirm(title));
            return;
        }
        dialog.querySelector('[data-confirm-title]').textContent = title;
        dialog.querySelector('[data-confirm-body]').textContent = body || '';

        const finish = (answer) => {
            dialog.removeEventListener('close', onClose);
            resolve(answer);
        };
        const onClose = () => finish(dialog.returnValue === 'confirm');
        dialog.addEventListener('close', onClose);
        dialog.showModal();
    });
}
