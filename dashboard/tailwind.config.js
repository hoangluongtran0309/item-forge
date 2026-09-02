/** @type {import('tailwindcss').Config} */
const defaultTheme = require('tailwindcss/defaultTheme')

// Tokens shared by both themes - kept in one place so they cannot drift apart
// when edited.
const sharedThemeVars = {
  '--rounded-box': '0.5rem',
  '--rounded-btn': '0.375rem',
  '--rounded-badge': '0.25rem',
  '--border-btn': '1px',
  '--tab-radius': '0.375rem',
}

module.exports = {
  // static/js MUST be in content: the Studio builds class names from strings in
  // JS (active tool, layer rows, color swatches), and they get purged if these
  // files are not scanned.
  content: [
    './src/main/resources/templates/**/*.html',
    './src/main/resources/static/js/**/*.js',
  ],
  theme: {
    extend: {
      fontFamily: {
        sans: ['Inter', ...defaultTheme.fontFamily.sans],
        display: ['"Space Grotesk"', ...defaultTheme.fontFamily.sans],
        mono: ['"JetBrains Mono"', ...defaultTheme.fontFamily.mono],
      },
      boxShadow: {
        elevated: '0 1px 2px rgb(0 0 0 / 0.08), 0 8px 24px -8px rgb(0 0 0 / 0.35)',
        'inset-slot': 'inset 0 2px 4px rgb(0 0 0 / 0.25)',
      },
    },
  },
  plugins: [require('daisyui'),require('@tailwindcss/forms')({ strategy: 'class' }),require('@tailwindcss/typography')],
  daisyui: {
    themes: [
      {
        'itemforge-dark': {
          'base-100': '#1E2129',
          'base-200': '#14161C',
          'base-300': '#2B2F3A',
          'base-content': '#E8E6DF',
          primary: '#C9A227',
          'primary-content': '#1A1506',
          secondary: '#5B8C5A',
          'secondary-content': '#0D1A0C',
          accent: '#4C8BB4',
          'accent-content': '#06121A',
          neutral: '#2B2F3A',
          'neutral-content': '#E8E6DF',
          info: '#4C8BB4',
          'info-content': '#06121A',
          success: '#4CAF6D',
          'success-content': '#08210F',
          warning: '#D9A441',
          'warning-content': '#241A02',
          error: '#D9534F',
          'error-content': '#250705',
          ...sharedThemeVars,
        },
      },
      {
        'itemforge-light': {
          'base-100': '#FFFFFF',
          'base-200': '#EEF1F4',
          'base-300': '#DCE1E6',
          'base-content': '#1E2129',
          primary: '#A6791C',
          'primary-content': '#FFFFFF',
          secondary: '#3F7A3E',
          'secondary-content': '#FFFFFF',
          accent: '#2E6690',
          'accent-content': '#FFFFFF',
          // The light theme used to use #1E2129 (near black) for neutral, which contrasts
          // too harshly against a white base-100 -- toned down to a neutral grey.
          neutral: '#4A5260',
          'neutral-content': '#FFFFFF',
          info: '#2E6690',
          'info-content': '#FFFFFF',
          success: '#2F8F55',
          'success-content': '#FFFFFF',
          warning: '#B9791E',
          'warning-content': '#FFFFFF',
          error: '#C0392B',
          'error-content': '#FFFFFF',
          ...sharedThemeVars,
        },
      },
    ],
    darkTheme: 'itemforge-dark',
  },
}
