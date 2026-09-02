// A blocking inline script in <head> already applies the stored theme before the first
// paint (avoiding FOUC) -- this file only wires up the visible sun/moon toggle to match
// that state and to persist later changes.
//
// The app shell renders two copies of the control (the mobile header and the sidebar
// footer) and only one is visible at a time, so ALL of them have to be kept in sync
// rather than a single getElementById.
document.addEventListener('DOMContentLoaded', function () {
    var toggles = document.querySelectorAll('.theme-toggle');
    if (toggles.length === 0) {
        return;
    }

    function syncCheckedState(isDark) {
        toggles.forEach(function (el) {
            el.checked = isDark;
        });
    }

    syncCheckedState(document.documentElement.getAttribute('data-theme') === 'itemforge-dark');

    toggles.forEach(function (toggle) {
        toggle.addEventListener('change', function () {
            var theme = toggle.checked ? 'itemforge-dark' : 'itemforge-light';
            document.documentElement.setAttribute('data-theme', theme);
            syncCheckedState(toggle.checked);
            try {
                localStorage.setItem('itemforge-theme', theme);
            } catch (e) {
                // localStorage is unavailable (private browsing, for instance) -- the theme just will not persist
            }
        });
    });
});
