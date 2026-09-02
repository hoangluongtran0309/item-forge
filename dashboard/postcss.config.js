const postcssConfig = {
    plugins: [require('postcss-import'),
        // Fontsource CSS references its .woff2 files with relative url()s that only
        // resolve inside node_modules; postcss-url copies those files next to the
        // compiled CSS and rewrites the url()s to point at the copy.
        require('postcss-url')({url: 'copy', useHash: true, assetsPath: 'fonts'}),
        require('autoprefixer'),
        require('tailwindcss')],
};

// If we are in production mode, then add cssnano
if (process.env.NODE_ENV === 'production') {
    postcssConfig.plugins.push(
        require('cssnano')({
            // use the safe preset so that it doesn't
            // mutate or remove code from our css
            preset: 'default',
        })
    );
}

module.exports = postcssConfig;
