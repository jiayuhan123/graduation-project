import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/login/LoginView.vue'),
      meta: { title: '登录', requiresAuth: false },
    },
    {
      path: '/',
      component: () => import('@/layouts/DefaultLayout.vue'),
      redirect: '/dashboard',
      children: [
        {
          path: 'dashboard',
          name: 'dashboard',
          component: () => import('@/views/dashboard/DashboardView.vue'),
          meta: { title: '控制台', requiresAuth: true },
        },
      ],
    },
  ],
})

/** 全局路由守卫：登录态校验 + 页面标题 */
router.beforeEach((to) => {
  const userStore = useUserStore()
  document.title = `${(to.meta.title as string) ?? ''} - 文明祭扫管理后台`

  if (to.meta.requiresAuth !== false && !userStore.isLoggedIn) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  // 已登录访问登录页时重定向到首页
  if (to.name === 'login' && userStore.isLoggedIn) {
    return { name: 'dashboard' }
  }
})

export default router
