import type { RouteRecordRaw } from 'vue-router'

export const systemRoutes: RouteRecordRaw[] = [
  {
    path: 'system/users',
    name: 'system-users',
    component: () => import('@/views/system/UserListView.vue'),
    meta: {
      title: '用户管理',
      requiresAuth: true,
      roles: ['admin'],
      permissions: ['system:user:manage'],
    },
  },
  {
    path: 'system/roles',
    name: 'system-roles',
    component: () => import('@/views/system/RoleListView.vue'),
    meta: {
      title: '角色管理',
      requiresAuth: true,
      roles: ['admin'],
      permissions: ['system:role:manage'],
    },
  },
  {
    path: 'system/permissions',
    name: 'system-permissions',
    component: () => import('@/views/system/PermissionListView.vue'),
    meta: {
      title: '权限字典',
      requiresAuth: true,
      roles: ['admin'],
      permissions: ['system:role:manage'],
    },
  },
]
