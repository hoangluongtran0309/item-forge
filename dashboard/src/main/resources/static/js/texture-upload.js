// A classic script, not a module -- the Studio relies on window.ItemForgeTexture.preview
// to show the freshly drawn image before it clicks the upload button on the user's behalf.
const ItemForgeTexture = {
    preview(file, imgId) {
        if (!file) {
            return;
        }
        const reader = new FileReader();
        reader.onload = (e) => {
            const img = document.getElementById(imgId);
            if (!img) {
                return;
            }
            img.src = e.target.result;
            img.style.display = 'inline-block';
        };
        reader.readAsDataURL(file);
    },

    // Push a File into a real <input type=file>'s buffer. This is the same trick the Studio
    // uses when saving: once the file is in the input, the existing hx-post/CSRF/spinner
    // wiring handles everything else.
    assignFile(file, inputId, imgId) {
        const input = document.getElementById(inputId);
        if (!input || !file) {
            return;
        }
        const dataTransfer = new DataTransfer();
        dataTransfer.items.add(file);
        input.files = dataTransfer.files;
        this.preview(file, imgId);
    }
};

// A delegated listener for every drop zone and file input in the texture panel. It uses
// data-* rather than inline ondrop=/onchange=: shorter markup, it survives htmx swaps, and
// it depends on no global.
document.addEventListener('dragover', function (event) {
    const zone = event.target.closest && event.target.closest('[data-texture-drop]');
    if (!zone) {
        return;
    }
    event.preventDefault();
    zone.classList.add('border-primary', 'bg-primary/5');
});

document.addEventListener('dragleave', function (event) {
    const zone = event.target.closest && event.target.closest('[data-texture-drop]');
    if (zone) {
        zone.classList.remove('border-primary', 'bg-primary/5');
    }
});

document.addEventListener('drop', function (event) {
    const zone = event.target.closest && event.target.closest('[data-texture-drop]');
    if (!zone) {
        return;
    }
    event.preventDefault();
    zone.classList.remove('border-primary', 'bg-primary/5');
    const file = event.dataTransfer && event.dataTransfer.files[0];
    ItemForgeTexture.assignFile(file, zone.dataset.inputId, zone.dataset.previewImgId);
});

document.addEventListener('change', function (event) {
    const input = event.target;
    if (!(input instanceof HTMLInputElement) || !input.dataset.textureFile) {
        return;
    }
    ItemForgeTexture.preview(input.files[0], input.dataset.previewImgId);
});
