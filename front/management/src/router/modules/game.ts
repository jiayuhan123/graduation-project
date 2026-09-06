import type { RouteRecordRaw } from 'vue-router'

export const gameRoutes: RouteRecordRaw[] = [
  {
    path: 'game/levels',
    name: 'game-levels',
    component: () => import('@/views/game/LevelListView.vue'),
    meta: { title: '关卡', requiresAuth: true },
  },
  {
    path: 'game/sessions',
    name: 'game-sessions',
    component: () => import('@/views/game/SessionListView.vue'),
    meta: { title: '对局', requiresAuth: true },
  },
  {
    path: 'game/rewards',
    name: 'game-rewards',
    component: () => import('@/views/game/RewardListView.vue'),
    meta: { title: '奖励记录', requiresAuth: true },
  },
]
