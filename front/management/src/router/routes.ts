import type { RouteRecordRaw } from 'vue-router'
import { accountRoutes } from '@/router/modules/account'
import { contentRoutes } from '@/router/modules/content'
import { systemRoutes } from '@/router/modules/system'
import { teachingRoutes } from '@/router/modules/teaching'
import { learningRoutes } from '@/router/modules/learning'
import { auditRoutes } from '@/router/modules/audit'

export const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/login/LoginView.vue'),
    meta: { title: '登录', requiresAuth: false, hidden: true },
  },
  {
    path: '/login/wechat/callback',
    name: 'wechat-login-callback',
    component: () => import('@/views/login/WechatCallbackView.vue'),
    meta: { title: '微信登录', requiresAuth: false, hidden: true },
  },
  {
    path: '/403',
    name: 'forbidden',
    component: () => import('@/views/error/ForbiddenView.vue'),
    meta: { title: '无权访问', requiresAuth: true, hidden: true },
  },
  {
    path: '/',
    component: () => import('@/layouts/DefaultLayout.vue'),
    redirect: '/dashboard',
    meta: { title: '首页', requiresAuth: true },
    children: [
      {
        path: 'dashboard',
        name: 'dashboard',
        component: () => import('@/views/dashboard/DashboardView.vue'),
        meta: { title: '工作台', requiresAuth: true },
      },
      ...accountRoutes,
      ...teachingRoutes,
      ...learningRoutes,
      ...contentRoutes,
      ...systemRoutes,
      ...auditRoutes,
    ],
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'not-found',
    component: () => import('@/views/error/NotFoundView.vue'),
    meta: { title: '页面不存在', requiresAuth: false, hidden: true },
  },
]
