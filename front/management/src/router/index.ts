import { createRouter, createWebHistory } from 'vue-router'
import { setupRouterGuards } from '@/router/guards'
import { routes } from '@/router/routes'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
  scrollBehavior: () => ({ top: 0 }),
})

setupRouterGuards(router)

export default router
