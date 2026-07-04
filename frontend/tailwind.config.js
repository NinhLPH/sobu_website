const colorToken = (name) => ({opacityValue}) => {
  if (opacityValue === undefined) {
    return `rgb(var(${name}))`;
  }
  return `rgb(var(${name}) / ${opacityValue})`;
};

/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        primary: colorToken('--color-primary'),
        'on-primary': colorToken('--color-on-primary'),
        'primary-container': colorToken('--color-primary-container'),
        'on-primary-container': colorToken('--color-on-primary-container'),
        secondary: colorToken('--color-secondary'),
        'on-secondary': colorToken('--color-on-secondary'),
        'secondary-container': colorToken('--color-secondary-container'),
        'on-secondary-container': colorToken('--color-on-secondary-container'),
        tertiary: colorToken('--color-tertiary'),
        'on-tertiary': colorToken('--color-on-tertiary'),
        'tertiary-container': colorToken('--color-tertiary-container'),
        'on-tertiary-container': colorToken('--color-on-tertiary-container'),
        error: colorToken('--color-error'),
        'on-error': colorToken('--color-on-error'),
        'error-container': colorToken('--color-error-container'),
        'on-error-container': colorToken('--color-on-error-container'),
        background: colorToken('--color-background'),
        'on-background': colorToken('--color-on-background'),
        surface: colorToken('--color-surface'),
        'on-surface': colorToken('--color-on-surface'),
        'surface-variant': colorToken('--color-surface-variant'),
        'on-surface-variant': colorToken('--color-on-surface-variant'),
        outline: colorToken('--color-outline'),
        'outline-variant': colorToken('--color-outline-variant'),
        'surface-container-lowest': colorToken('--color-surface-container-lowest'),
        'surface-container-low': colorToken('--color-surface-container-low'),
        'surface-container': colorToken('--color-surface-container'),
        'surface-container-high': colorToken('--color-surface-container-high'),
        'surface-container-highest': colorToken('--color-surface-container-highest'),
        'inverse-surface': colorToken('--color-inverse-surface'),
        'inverse-on-surface': colorToken('--color-inverse-on-surface'),
        'inverse-primary': colorToken('--color-inverse-primary'),
      },
      fontFamily: {
        sans: ['"Montserrat"', 'sans-serif'],
      },
      borderRadius: {
        'lg': '2rem', 
        'md': '1.5rem', 
      },
      letterSpacing: {
        tightest: '-0.02em',
        blueprint: '0.05em', 
      },
      backdropBlur: {
        'xs': '20px',
      }
    },
  },
  plugins: [],
}

