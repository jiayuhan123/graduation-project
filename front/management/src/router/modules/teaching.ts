import type { RouteRecordRaw } from 'vue-router'

export const teachingRoutes: RouteRecordRaw[] = [
  {
    path: 'teaching/classes',
    name: 'teaching-classes',
    component: () => import('@/views/teaching/ClassListView.vue'),
    meta: { title: '我的班级', requiresAuth: true, roles: ['teacher', 'student'] },
  },
  {
    path: 'teaching/assignments',
    name: 'teaching-assignments',
    component: () => import('@/views/teaching/AssignmentListView.vue'),
    meta: { title: '作业中心', requiresAuth: true, roles: ['teacher', 'student'] },
  },
  {
    path: 'teaching/submissions',
    name: 'teaching-submissions',
    component: () => import('@/views/teaching/SubmissionListView.vue'),
    meta: { title: '提交记录', requiresAuth: true, roles: ['teacher', 'student'] },
  },
]
