import { createStudio } from './studio.js';
import { createModalHost, createPageHost } from './host.js';

// The Studio's ONLY entry point. It is loaded as <script type="module">, which means:
//   - it runs after the DOM has been parsed (modules are deferred), so no DOMContentLoaded
//     is needed;
//   - it creates NO global, so inline onclick="..." attributes cannot reach it. That is
//     precisely why the trigger points use data-studio-* plus one delegated listener,
//     rather than the long onclick strings the previous editor used.
//
// Note: NEVER put a <script type="module"> inside a region htmx swaps -- htmx re-executes
// classic <script> tags in the new content, but a module is fetched only once per URL and
// therefore will not run again.

let modalStudio = null;

function readTriggerConfig(button) {
    const data = button.dataset;
    return {
        width: Number(data.studioWidth) || 16,
        height: Number(data.studioHeight) || 16,
        loadUrl: data.studioLoadUrl || null,
        siblingLoadUrl: data.studioSiblingLoadUrl || null,
        uvOverlay: data.studioUvOverlay || null,
        previewKind: data.studioPreviewKind || 'item',
        title: data.studioTitle || 'Draw texture',
        subtitle: data.studioSubtitle || '',
        inputId: data.studioInputId,
        uploadBtnId: data.studioUploadBtnId,
        previewImgId: data.studioPreviewImgId,
        statusId: data.studioStatusId,
        draftKey: data.studioDraftKey || null,
        downloadName: data.studioDownloadName || 'texture',
        closeOnSave: data.studioCloseOnSave === 'true',
    };
}

function openModal(config) {
    const modal = document.getElementById('studio-modal');
    if (!modal) {
        console.warn('[studio] studio-modal fragment is not on this page');
        return;
    }
    const root = modal.querySelector('[data-studio-root]');

    // The modal is shared by every trigger on the page, so the previous instance has to be
    // destroyed before mounting a new one -- otherwise its listeners and
    // requestAnimationFrame loop would live forever.
    if (modalStudio) {
        modalStudio.destroy();
        modalStudio = null;
    }

    root.querySelector('[data-studio-title]').textContent = config.title;
    const subtitle = root.querySelector('[data-studio-subtitle]');
    subtitle.textContent = config.subtitle;
    subtitle.hidden = config.subtitle === '';

    const reload = root.querySelector('[data-studio-reload]');
    if (reload) {
        reload.hidden = !config.loadUrl;
    }

    // Esc: a dialog closes itself, so it has to be intercepted while there are unsaved
    // changes. Bound ONCE per dialog (not on every open) -- otherwise the listeners would
    // pile up with each click of "Draw it yourself".
    if (!modal.dataset.cancelGuardInstalled) {
        modal.dataset.cancelGuardInstalled = 'true';
        modal.addEventListener('cancel', (event) => {
            if (modalStudio && modalStudio.doc.isDirty()) {
                event.preventDefault();
                modalStudio.requestClose();
            }
        });
    }

    modal.showModal();
    // Mount AFTER the dialog opens: the viewport needs real dimensions to compute a
    // fit-to-frame zoom, and before showModal() getBoundingClientRect() returns 0.
    modalStudio = createStudio(root, createModalHost(config));
}

document.addEventListener('click', (event) => {
    const trigger = event.target.closest('[data-studio-open]');
    if (!trigger) {
        return;
    }
    event.preventDefault();
    openModal(readTriggerConfig(trigger));
});

// The /studio page: mount as soon as #studio-page-root exists, no trigger needed.
const pageRoot = document.querySelector('[data-studio-page]');
if (pageRoot) {
    const data = pageRoot.dataset;
    const root = pageRoot.querySelector('[data-studio-root]');

    root.querySelector('[data-studio-title]').textContent = data.studioTitle || 'Texture Studio';
    const pageSubtitle = root.querySelector('[data-studio-subtitle]');
    pageSubtitle.textContent = data.studioSubtitle || '';
    pageSubtitle.hidden = !data.studioSubtitle;
    const pageReload = root.querySelector('[data-studio-reload]');
    if (pageReload) {
        pageReload.hidden = !data.studioLoadUrl;
    }
    // With no upload destination (scratch mode) the Save button is hidden, so it does not
    // promise an action that would fail -- only Download PNG remains.
    const pageSave = root.querySelector('[data-studio-save]');
    if (pageSave) {
        pageSave.hidden = !data.studioUploadUrl;
    }

    createStudio(root, createPageHost({
        width: Number(data.studioWidth) || 16,
        height: Number(data.studioHeight) || 16,
        loadUrl: data.studioLoadUrl || null,
        siblingLoadUrl: data.studioSiblingLoadUrl || null,
        uploadUrl: data.studioUploadUrl || null,
        uvOverlay: data.studioUvOverlay || null,
        previewKind: data.studioPreviewKind || 'item',
        backUrl: data.studioBackUrl || '/',
        draftKey: data.studioDraftKey || 'scratch',
        downloadName: data.studioDownloadName || 'texture',
    }));
}

// The single bridge to a global, so non-module code (a fragment htmx swapped in, for
// instance) can open the Studio if it needs to.
window.ItemForgeStudio = { openModal };
