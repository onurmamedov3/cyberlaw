import {defineConfig} from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    port: 5173,
    // during development the Spring Boot backend runs on :8080
    proxy: {
      '/api': { target: 'http://localhost:8080', changeOrigin: true },
    },
  },
  build: {
    // `npm run build` puts the app inside the backend, so one jar serves everything
    outDir: '../backend/src/main/resources/static',
    emptyOutDir: true,
  },
})
