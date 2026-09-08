import type { RouteRecordRaw } from 'vue-router'

export const learningRoutes: RouteRecordRaw[] = [
  {
    path: 'learning/practice',
    name: 'learning-practice',
    component: () => import('@/views/learning/LearningPracticeView.vue'),
    meta: { title: '个性练习', requiresAuth: true, roles: ['student'] },
  },
  {
    path: 'learning/class-dashboard',
    name: 'learning-class-dashboard',
    component: () => import('@/views/learning/ClassDashboardView.vue'),
    meta: {
      title: '班级学情',
      requiresAuth: true,
      roles: ['teacher'],
      permissions: ['teaching:report:query'],
    },
  },
  {
    path: 'learning/records',
    name: 'learning-records',
    component: () => import('@/views/learning/LearningRecordListView.vue'),
    meta: { title: '学习档案', requiresAuth: true, roles: ['student', 'teacher', 'admin'] },
  },
  {
    path: 'learning/recommendations',
    name: 'learning-recommendations',
    component: () => import('@/views/learning/RecommendationListView.vue'),
    meta: { title: '推荐记录', requiresAuth: true, roles: ['student', 'teacher', 'admin'] },
  },
  {
    path: 'learning/photo-reviews',
    name: 'learning-photo-reviews',
    component: () => import('@/views/learning/PhotoReviewListView.vue'),
    meta: { title: '拍照复核', requiresAuth: true, roles: ['student', 'teacher', 'admin'] },
  },
]
