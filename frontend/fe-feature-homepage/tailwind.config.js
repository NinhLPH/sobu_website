/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        primary: '#00618e',
        'primary-container': '#4bbafe',
        surface: '#f3f6ff',
        'surface-container': '#dae9ff',
        'surface-container-low': '#f3f6ff', 
        'surface-container-lowest': '#ffffff',
        tertiary: '#5a4bb4',
        'tertiary-container': '#b1a6ff',
        outline: '#5b799b',
        'on-surface': '#0e304e',
      },
      fontFamily: {
        sans: ['"Be Vietnam Pro"', 'sans-serif'],
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

