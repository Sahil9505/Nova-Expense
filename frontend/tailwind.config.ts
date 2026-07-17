import type { Config } from 'tailwindcss';

export default {
  darkMode: 'class',
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        background: 'rgb(var(--background) / <alpha-value>)',
        surface: 'rgb(var(--surface) / <alpha-value>)',
        'surface-2': 'rgb(var(--surface-2) / <alpha-value>)',
        foreground: 'rgb(var(--foreground) / <alpha-value>)',
        muted: 'rgb(var(--muted) / <alpha-value>)',
        'muted-foreground': 'rgb(var(--muted-foreground) / <alpha-value>)',
        border: 'rgb(var(--border) / <alpha-value>)',
        primary: 'rgb(var(--primary) / <alpha-value>)',
        'primary-foreground': 'rgb(var(--primary-foreground) / <alpha-value>)',
        secondary: 'rgb(var(--secondary) / <alpha-value>)',
        accent: 'rgb(var(--accent) / <alpha-value>)',
        success: 'rgb(var(--success) / <alpha-value>)',
        warning: 'rgb(var(--warning) / <alpha-value>)',
        danger: 'rgb(var(--danger) / <alpha-value>)',
        // Atmosphere glow accents (driven by theme CSS variables).
        glow: {
          blue: 'rgb(var(--glow-blue) / <alpha-value>)',
          purple: 'rgb(var(--glow-purple) / <alpha-value>)',
          cyan: 'rgb(var(--glow-cyan) / <alpha-value>)',
        },
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', '-apple-system', 'Segoe UI', 'sans-serif'],
      },
      borderRadius: {
        lg: '0.75rem',
        md: '0.5rem',
        sm: '0.375rem',
      },
      boxShadow: {
        card: '0 1px 2px 0 rgb(0 0 0 / 0.04), 0 8px 24px -12px rgb(15 23 42 / 0.25)',
        glow: '0 0 0 1px rgb(var(--primary) / 0.25), 0 8px 30px -8px rgb(var(--primary) / 0.35)',
        // Glass surfaces read their shadow from CSS variables so they stay
        // theme-aware; the classes in index.css consume these.
        glass: 'var(--glass-shadow)',
        'glass-strong': 'var(--glass-strong-shadow)',
        'glow-primary': '0 8px 24px -8px rgb(var(--primary) / 0.5)',
        'glow-success': '0 8px 24px -8px rgb(var(--success) / 0.5)',
        'glow-danger': '0 8px 24px -8px rgb(var(--danger) / 0.5)',
        'glow-warning': '0 8px 24px -8px rgb(var(--warning) / 0.5)',
        'card-hover': '0 18px 44px -20px rgb(var(--primary) / 0.45)',
      },
      backdropBlur: {
        glass: '22px',
      },
      transitionTimingFunction: {
        premium: 'cubic-bezier(0.22, 1, 0.36, 1)',
      },
      transitionDuration: {
        150: '150ms',
        200: '200ms',
        300: '300ms',
        500: '500ms',
      },
      keyframes: {
        'fade-in': {
          from: { opacity: '0', transform: 'translateY(4px)' },
          to: { opacity: '1', transform: 'translateY(0)' },
        },
        shimmer: {
          '100%': { transform: 'translateX(100%)' },
        },
      },
      animation: {
        'fade-in': 'fade-in 0.35s ease-out both',
        'fade-in-up': 'nova-fade-in-up 0.4s cubic-bezier(0.22, 1, 0.36, 1) both',
        'scale-in': 'nova-scale-in 0.3s cubic-bezier(0.22, 1, 0.36, 1) both',
        'pulse-soft': 'nova-pulse-soft 2.4s ease-in-out infinite',
      },
    },
  },
  plugins: [],
} satisfies Config;
