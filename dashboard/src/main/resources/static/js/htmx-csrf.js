document.addEventListener('htmx:configRequest', function (event) {
    var tokenMeta = document.querySelector('meta[name="_csrf"]');
    var headerMeta = document.querySelector('meta[name="_csrf_header"]');
    if (tokenMeta && headerMeta) {
        event.detail.headers[headerMeta.content] = tokenMeta.content;
    }
});
