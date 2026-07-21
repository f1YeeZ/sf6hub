import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import basicSsl from '@vitejs/plugin-basic-ssl'

export default defineConfig({
  plugins: [
    vue(),
    basicSsl()
  ],
  build: {
    chunkSizeWarningLimit: 900,
    rolldownOptions: {
      checks: {
        invalidAnnotation: false,
      },
    },
  },
  server: {
    https: false,
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
  resolve: {
    alias: {
      '@': '/src'
    }
  }
})
