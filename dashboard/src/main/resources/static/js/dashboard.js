// Plain <select onchange> toggles for the type-dependent parts of a form -- deliberately
// NOT htmx, because this is purely client-side show/hide and every field stays in the DOM
// whether visible or not (a `hidden` field is still submitted with the form).

function toggleAbilityFields(select) {
    var row = select.closest('.ability-row');
    var potionFields = row.querySelector('.potion-fields');
    var damageFields = row.querySelector('.damage-fields');
    var isPotion = select.value === 'POTION_EFFECT';
    potionFields.hidden = !isPotion;
    damageFields.hidden = isPotion;
}

// Ingredient rows (key + item id) are shared by both recipe kinds -- the key column is
// simply ignored by the mapper for SHAPELESS -- so only the shape grid rows need showing or
// hiding based on the selected kind.
function toggleRecipeType(select) {
    var shapeLinesSection = document.getElementById('shape-lines-section');
    shapeLinesSection.hidden = select.value !== 'SHAPED';
}

// htmx-only actions (delete, add/remove-row) signal success through the
// `HX-Trigger: {"toast": "..."}` response header rather than a redirect, because there is
// no full page load to attach a flash attribute to. The full-page create/update redirects
// render their toast inline through ${toastMessage} instead of going through this listener.
// The recipe's 3x3 shape grid. The grid cells are pure UI; the real values are submitted
// through the three hidden shapeLines[0..2] inputs, so RecipeForm/RecipeFormMapper and their
// tests need no changes.
//
// The important assembly rule: ALL non-empty rows must be the SAME length, because Bukkit
// rejects a ShapedRecipe with ragged rows. So the width is the furthest-used column across
// the WHOLE grid, and every row is padded to that width with spaces -- trimming trailing
// spaces row by row would break shapes like [" X ", "XXX", " X "].
(function () {
    function cells() {
        return Array.prototype.slice.call(document.querySelectorAll('[data-shape-cell]'));
    }

    function syncGridToLines() {
        const grid = cells();
        if (grid.length !== 9) {
            return;
        }
        const rows = [0, 1, 2].map(function (row) {
            return [0, 1, 2].map(function (col) {
                const value = grid[row * 3 + col].value;
                return value === '' ? ' ' : value;
            });
        });

        let width = 0;
        let lastRow = -1;
        rows.forEach(function (row, rowIndex) {
            row.forEach(function (ch, colIndex) {
                if (ch !== ' ') {
                    width = Math.max(width, colIndex + 1);
                    lastRow = Math.max(lastRow, rowIndex);
                }
            });
        });

        [0, 1, 2].forEach(function (rowIndex) {
            const hidden = document.querySelector('[data-shape-line="' + rowIndex + '"]');
            if (!hidden) {
                return;
            }
            // Rows after the last non-empty one are submitted as empty strings --
            // RecipeFormMapper filters empty lines out.
            hidden.value = rowIndex > lastRow ? '' : rows[rowIndex].slice(0, width).join('');
        });
    }

    function syncLinesToGrid() {
        const grid = cells();
        if (grid.length !== 9) {
            return;
        }
        [0, 1, 2].forEach(function (rowIndex) {
            const hidden = document.querySelector('[data-shape-line="' + rowIndex + '"]');
            const line = hidden ? hidden.value : '';
            [0, 1, 2].forEach(function (colIndex) {
                const ch = line.charAt(colIndex);
                grid[rowIndex * 3 + colIndex].value = ch === ' ' ? '' : ch;
            });
        });
    }

    document.addEventListener('DOMContentLoaded', syncLinesToGrid);
    document.addEventListener('input', function (event) {
        if (event.target.matches && event.target.matches('[data-shape-cell]')) {
            syncGridToLines();
        }
    });
})();

// Client-side grid/table filtering. The input names its target with
// data-filter-target="#grid-id", and each row already carries a pre-lowercased string in
// data-filter-text (rendered by the server), so filtering is just string comparison -- no
// request needed, and every row's htmx wiring is left completely intact.
document.addEventListener('input', function (event) {
    var input = event.target;
    if (!(input instanceof HTMLInputElement) || !input.dataset.filterTarget) {
        return;
    }
    var container = document.querySelector(input.dataset.filterTarget);
    if (!container) {
        return;
    }
    var needle = input.value.trim().toLowerCase();
    var rows = container.querySelectorAll('[data-filter-text]');
    var visible = 0;
    rows.forEach(function (row) {
        var matches = needle === '' || row.dataset.filterText.indexOf(needle) !== -1;
        row.hidden = !matches;
        if (matches) {
            visible += 1;
        }
    });

    var emptyState = document.getElementById('filter-empty-state');
    if (emptyState) {
        emptyState.hidden = visible !== 0 || rows.length === 0;
    }
});

// Texture thumbnails: the GET endpoint returns 404 when an entity has no texture yet, so
// fall back to the placeholder letter. This has to listen in the CAPTURE phase on document
// -- an <img> 'error' event does not bubble, so an ordinary document listener would never
// fire.
document.addEventListener('error', function (event) {
    var img = event.target;
    if (!(img instanceof HTMLImageElement) || !img.dataset.thumbFallback) {
        return;
    }
    var slot = img.closest('.if-slot');
    img.remove();
    var fallback = slot ? slot.querySelector('.thumb-fallback') : null;
    if (fallback) {
        fallback.classList.remove('hidden');
    }
}, true);

// Attached to `document`, NOT `document.body`: this file is loaded by a blocking <script>
// in <head>, so at run time `document.body` is still null and addEventListener would throw
// a TypeError, silently losing every toast event. htmx events bubble all the way to
// document, so a listener here still catches them.
document.addEventListener('toast', function (event) {
    var container = document.getElementById('toast-container');
    if (!container) {
        return;
    }
    var message = event.detail && event.detail.value ? event.detail.value : String(event.detail);
    var alertEl = document.createElement('div');
    alertEl.className = 'alert alert-success shadow-lg';
    alertEl.textContent = message;
    container.appendChild(alertEl);
    setTimeout(function () {
        alertEl.remove();
    }, 4000);
});
