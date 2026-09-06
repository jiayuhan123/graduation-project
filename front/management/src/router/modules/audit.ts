import type { RouteRecordRaw } from 'vue-router'

export const auditRoutes: RouteRecordRaw[] = [
  {
    path: 'audit/logs',
    name: 'audit-logs',
    component: () => import('@/views/audit/AuditLogListView.vue'),
    meta: { title: '审计日志', requiresAuth: true },
  },
]
