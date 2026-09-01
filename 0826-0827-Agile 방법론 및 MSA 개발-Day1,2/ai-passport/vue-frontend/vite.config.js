import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

const authProxyTarget = process.env.VITE_DEV_PROXY_TARGET || 'http://localhost:8080'
const authServerTarget = process.env.VITE_DEV_AUTH_TARGET || 'http://localhost:9000'

function keepAuthRedirectsOnFrontend(proxy) {
  const targetOrigin = new URL(authProxyTarget).origin

  proxy.on('proxyRes', (proxyResponse) => {
    const location = proxyResponse.headers.location
    if (!location) return

    const redirectUrl = new URL(location, targetOrigin)
    if (redirectUrl.origin !== targetOrigin) return

    let frontendPath = `${redirectUrl.pathname}${redirectUrl.search}${redirectUrl.hash}`
    if (redirectUrl.pathname === '/login') {
      frontendPath = frontendPath.replace(/^\/login/, '/auth-login')
    }

    proxyResponse.headers.location = frontendPath
  })
}

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src')
    }
  },
  server: {
    host: 'localhost',
    port: 3000,
    strictPort: true,
    proxy: {
      '/api': {
        target: authProxyTarget,
        changeOrigin: true,
        secure: false
      },
      '/oauth2': {
        target: authProxyTarget,
        changeOrigin: true,
        secure: false,
        configure: keepAuthRedirectsOnFrontend
      },
      '/auth-login': {
        target: authProxyTarget,
        changeOrigin: true,
        secure: false,
        rewrite: (path) => path.replace(/^\/auth-login/, '/login'),
        configure: keepAuthRedirectsOnFrontend
      },
      '/auth-logout': {
        target: authServerTarget,
        changeOrigin: true,
        secure: false,
        rewrite: (path) => path.replace(/^\/auth-logout/, '/logout')
      },
      '/logout': {
        target: authProxyTarget,
        changeOrigin: true,
        secure: false
      },
      '/userinfo': {
        target: authProxyTarget,
        changeOrigin: true,
        secure: false
      }
    }
  }
})
