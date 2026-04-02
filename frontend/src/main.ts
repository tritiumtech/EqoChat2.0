import { createSSRApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import { i18n } from './locale/i18n'
import uviewPlus from '@/uni_modules/uview-plus'
import './styles/global-safe-area.css'

export function createApp() {
  const app = createSSRApp(App)
  const pinia = createPinia()
  
  app.use(pinia)
  app.use(i18n)
  app.use(uviewPlus)
  
  return {
    app,
    pinia
  }
}
