// The reference texture picker. The dashboard serves this list from the library an admin
// imported themselves (see ReferenceLibraryService) -- no Mojang asset is bundled.
//
// Picking a texture downloads that PNG and pushes it through the Studio's normal import
// path, so it becomes a new layer and ONE undo step. Nothing is sent to the server.

const SEARCH_DEBOUNCE_MS = 250;

export function openReferencePicker(onPick) {
    const dialog = document.getElementById('studio-reference');
    if (!dialog) {
        return false;
    }
    const query = dialog.querySelector('[data-reference-query]');
    const results = dialog.querySelector('[data-reference-results]');

    let timer = null;
    const refresh = async () => {
        try {
            const response = await fetch('/reference/api/search?q=' + encodeURIComponent(query.value || ''));
            if (!response.ok) {
                renderMessage(results, 'Could not read the reference library.');
                return;
            }
            render(results, await response.json(), async (texture) => {
                dialog.close();
                const url = '/reference/file/' + texture.pack + '/' + texture.path;
                const file = await fetchAsFile(url, texture.name);
                if (file) {
                    onPick(file);
                }
            });
        } catch (e) {
            renderMessage(results, 'Could not read the reference library: ' + e.message);
        }
    };

    // Bound once per dialog: openReferencePicker can be called many times.
    if (!dialog.dataset.wired) {
        dialog.dataset.wired = 'true';
        query.addEventListener('input', () => {
            if (timer !== null) {
                clearTimeout(timer);
            }
            timer = setTimeout(refresh, SEARCH_DEBOUNCE_MS);
        });
    }
    dialog.__refresh = refresh;

    dialog.showModal();
    refresh();
    return true;
}

function render(container, textures, onPick) {
    container.textContent = '';
    if (textures.length === 0) {
        renderMessage(container, 'Nothing matches. Import a resource pack from the Reference Library page first.');
        return;
    }
    textures.forEach((texture) => {
        const button = document.createElement('button');
        button.type = 'button';
        button.className = 'flex flex-col items-center gap-1 rounded border border-base-300 p-2 hover:border-primary';
        button.title = texture.pack + '/' + texture.path;

        const img = document.createElement('img');
        img.className = 'if-slot h-12 w-12';
        img.src = '/reference/file/' + texture.pack + '/' + texture.path;
        img.alt = '';

        const label = document.createElement('span');
        label.className = 'itemforge-tech-label w-full truncate';
        label.textContent = texture.name;

        button.append(img, label);
        button.addEventListener('click', () => onPick(texture));
        container.appendChild(button);
    });
}

function renderMessage(container, message) {
    container.textContent = '';
    const p = document.createElement('p');
    p.className = 'col-span-full py-8 text-center text-sm text-base-content/60';
    p.textContent = message;
    container.appendChild(p);
}

async function fetchAsFile(url, name) {
    try {
        const response = await fetch(url);
        if (!response.ok) {
            return null;
        }
        const blob = await response.blob();
        return new File([blob], name, { type: 'image/png' });
    } catch (e) {
        return null;
    }
}
