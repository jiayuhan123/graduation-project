import type { RouteRecordRaw } from 'vue-router'

export const contentRoutes: RouteRecordRaw[] = [
  {
    path: 'ai/knowledge-base',
    name: 'ai-knowledge-base',
    component: () => import('@/views/ai/AgentKnowledgeView.vue'),
    meta: {
      title: 'Agent 知识库',
      requiresAuth: true,
      roles: ['admin'],
      permissions: ['content:manage'],
    },
  },
  {
    path: 'content/modules',
    name: 'content-modules',
    component: () => import('@/views/content/ModuleListView.vue'),
    meta: { title: '内容模块', requiresAuth: true, permissions: ['content:manage'] },
  },
  {
    path: 'content/knowledge-points',
    name: 'content-knowledge-points',
    component: () => import('@/views/content/KnowledgePointListView.vue'),
    meta: { title: '知识点', requiresAuth: true, permissions: ['content:manage'] },
  },
  {
    path: 'content/questions',
    name: 'content-questions',
    component: () => import('@/views/content/QuestionListView.vue'),
    meta: { title: '题目', requiresAuth: true, permissions: ['content:manage'] },
  },
  {
    path: 'content/error-types',
    name: 'content-error-types',
    component: () => import('@/views/content/ErrorTypeListView.vue'),
    meta: { title: '错因字典', requiresAuth: true, permissions: ['content:manage'] },
  },
  {
    path: 'content/reviews',
    name: 'content-reviews',
    component: () => import('@/views/content/ContentReviewView.vue'),
    meta: { title: '内容审核', requiresAuth: true, permissions: ['content:manage'] },
  },
  {
    path: 'content/question-feedback',
    name: 'content-question-feedback',
    component: () => import('@/views/content/QuestionFeedbackView.vue'),
    meta: { title: '题目反馈', requiresAuth: true, permissions: ['content:manage'] },
  },
]
