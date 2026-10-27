import react from '@vitejs/plugin-react';
import { defineConfig } from 'vitest/config';

/** Dev proxy target when `VITE_API_BASE_URL` is empty (see `.env.example`). */
const DEV_API_ORIGIN = 'http://localhost:8080';

export default defineConfig({
  plugins: [react()],
  test: {
    environment: 'jsdom',
  },
  server: {
    proxy: {
      '/api': {
        target: DEV_API_ORIGIN,
        changeOrigin: true,
      },
    },
  },
});
