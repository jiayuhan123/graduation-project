import { fileURLToPath, URL } from 'node:url'

import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

// https://vite.dev/config/
export default defineConfig(({ command, mode }) => {
  const env = loadEnv(mode, process.cwd(), '')

  return {
    plugins: [vue(), ...(command === 'serve' ? [vueDevTools()] : [])],
    server: {
      port: 5173,
      proxy: {
        // 开发环境代理：/api 请求直接转发到服务器模块化单体
        '/api': {
          target: env.DEV_API_PROXY_TARGET || 'http://122.51.234.113:10000',
          changeOrigin: true,
        },
      },
    },
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url)),
      },
    },
  }
})
