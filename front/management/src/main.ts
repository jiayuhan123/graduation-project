import { createApp } from 'vue'
import 'element-plus/es/components/message/style/css'

import App from './App.vue'
import { setupDirectives } from './directives'
import router from './router'
import pinia from './stores'
import './styles/index.scss'
import { useUserStore } from '@/stores/user'
import { reportError } from '@/utils/error-reporter'
import { setUnauthorizedHandler } from '@/utils/request'

const app = createApp(App)

app.use(pinia)
app.use(router)
setupDirectives(app)

setUnauthorizedHandler(async () => {
  const userStore = useUserStore(pinia)
  const currentRoute = router.currentRoute.value
  userStore.clearSession()
  if (currentRoute.name !== 'login') {
    await router.replace({
      name: 'login',
      query: { redirect: currentRoute.fullPath },
    })
  }
})

app.config.errorHandler = (error, _instance, info) => {
  reportError(error, { source: 'vue', info, route: router.currentRoute.value.fullPath })
}

window.addEventListener('error', (event) => {
  reportError(event.error ?? event.message, {
    source: 'window',
    route: router.currentRoute.value.fullPath,
  })
})

window.addEventListener('unhandledrejection', (event) => {
  reportError(event.reason, { source: 'promise', route: router.currentRoute.value.fullPath })
})

app.mount('#app')
