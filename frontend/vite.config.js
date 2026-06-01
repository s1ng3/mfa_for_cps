import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// The frontend talks to the Spring Boot gateway on :8080. We use a dev proxy so the browser
// sees same-origin /api calls (simplifies CORS during development).
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
});
