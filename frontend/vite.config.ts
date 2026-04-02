import path from 'path'
import { defineConfig, loadEnv } from 'vite'
import uni from '@dcloudio/vite-plugin-uni'

export default ({ mode }: { mode: string }) => {
  const env = loadEnv(mode, path.resolve(process.cwd(), 'env'))
  const port = Number(env.VITE_APP_PORT || '3000')
  const useProxy = env.VITE_APP_PROXY === 'true'
  const proxyPrefix = env.VITE_APP_PROXY_PREFIX || '/api'
  const proxyTarget = env.VITE_PROXY_TARGET || env.VITE_API_BASE_URL || 'http://localhost:8080'


  const {
    VITE_APP_PORT,
    VITE_SERVER_BASEURL,
    VITE_DELETE_CONSOLE,
    VITE_SHOW_SOURCEMAP,
    VITE_APP_PROXY,
    VITE_APP_PROXY_PREFIX,
  } = env
  console.log('环境变量 env -> ', env)

  return defineConfig({
    envDir: './env',
    plugins: [uni()],
    css: {
      preprocessorOptions: {
        scss: {
          // uview-plus 目前仍使用 legacy API 与 @import，先静默弃用警告
          silenceDeprecations: ['legacy-js-api', 'import'],
          quietDeps: true,
        },
      },
    },
    resolve: {
      alias: {
        '@': path.resolve(__dirname, 'src'),
      },
    },
    server: {
      host: '0.0.0.0',
      port,
      proxy: useProxy
        ? {
            [proxyPrefix]: {
              target: proxyTarget,
              changeOrigin: true,
              configure: (proxy) => {
                proxy.on('proxyReq', (proxyReq, req) => {
                  const auth = req.headers['authorization']
                  if (auth) {
                    proxyReq.setHeader('authorization', auth)
                  }
                })
              }
            },
          }
        : undefined,
    },
  })
}
