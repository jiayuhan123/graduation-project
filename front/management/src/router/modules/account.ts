import type { RouteRecordRaw } from 'vue-router'

export const accountRoutes: RouteRecordRaw[] = [
  {
    path: 'account/profile',
    name: 'account-profile',
    component: () => import('@/views/account/ProfileView.vue'),
    meta: { title: '个人资料', requiresAuth: true },
  },
  {
    path: 'account/student',
    name: 'account-student',
    component: () => import('@/views/account/StudentProfileView.vue'),
    meta: { title: '学生资料', requiresAuth: true, roles: ['student'] },
  },
  {
    path: 'account/teacher',
    name: 'account-teacher',
    component: () => import('@/views/account/TeacherProfileView.vue'),
    meta: { title: '教师资料', requiresAuth: true, roles: ['teacher'] },
  },
  {
    path: 'account/security',
    name: 'account-security',
    component: () => import('@/views/account/SecurityView.vue'),
    meta: { title: '账号安全', requiresAuth: true },
  },
]
