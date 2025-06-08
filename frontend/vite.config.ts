import {defineConfig} from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 3000,
    proxy: {
      '/api': 'http://localhost:8080',
      '/api/reb-data': {
        target: 'https://www.reb.or.kr',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api\/reb-data/, '/r-one/openapi/SttsApiTbl.do'),
        secure: true
      }
    }
  },
  css: {
    preprocessorOptions: {
      scss: {
        api: "modern-compiler"
      }
    }
  },
  resolve: {
    alias: {
      "@": "/src"
    }
  }
})

