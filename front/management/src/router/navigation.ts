import { hasAccess, type AccessSubject } from '@/auth/permission'

export interface NavigationItem {
  title: string
  path: string
  icon: string
  roles?: readonly string[]
  permissions?: readonly string[]
}

export interface NavigationGroup {
  title: string
  items: readonly NavigationItem[]
}

/** 单一 Web 端导航；未完成微服务的入口暂不注册。 */
export const navigationGroups: readonly NavigationGroup[] = [
  {
    title: '工作台',
    items: [{ title: '工作台', path: '/dashboard', icon: 'home' }],
  },
  {
    title: '学习与班级',
    items: [
      { title: '我的班级', path: '/teaching/classes', icon: 'users', roles: ['student'] },
      { title: '我的作业', path: '/teaching/assignments', icon: 'edit', roles: ['student'] },
      { title: '提交记录', path: '/teaching/submissions', icon: 'check', roles: ['student'] },
      { title: '个性练习', path: '/learning/practice', icon: 'target', roles: ['student'] },
      { title: '学习档案', path: '/learning/records', icon: 'chart', roles: ['student'] },
      { title: '巩固推荐', path: '/learning/recommendations', icon: 'target', roles: ['student'] },
      { title: '拍照批改', path: '/learning/photo-reviews', icon: 'camera', roles: ['student'] },
    ],
  },
  {
    title: '教学工作',
    items: [
      {
        title: '班级管理',
        path: '/teaching/classes',
        icon: 'users',
        roles: ['teacher'],
        permissions: ['teaching:class:manage'],
      },
      {
        title: '作业管理',
        path: '/teaching/assignments',
        icon: 'edit',
        roles: ['teacher'],
        permissions: ['teaching:assignment:manage'],
      },
      {
        title: '学情提交',
        path: '/teaching/submissions',
        icon: 'check',
        roles: ['teacher'],
        permissions: ['teaching:assignment:manage'],
      },
      {
        title: '班级学情',
        path: '/learning/class-dashboard',
        icon: 'chart',
        roles: ['teacher'],
        permissions: ['teaching:report:query'],
      },
      {
        title: '学生学情',
        path: '/learning/records',
        icon: 'chart',
        roles: ['teacher'],
        permissions: ['teaching:report:query'],
      },
      {
        title: '巩固建议',
        path: '/learning/recommendations',
        icon: 'target',
        roles: ['teacher'],
        permissions: ['teaching:report:query'],
      },
      {
        title: '拍照复核',
        path: '/learning/photo-reviews',
        icon: 'camera',
        roles: ['teacher'],
        permissions: ['learning:marking:review'],
      },
    ],
  },
  {
    title: '平台管理',
    items: [
      {
        title: '运营总览',
        path: '/system/operations',
        icon: 'document',
        roles: ['admin'],
        permissions: ['teaching:report:query'],
      },
      {
        title: '内容模块',
        path: '/content/modules',
        icon: 'grid',
        permissions: ['content:manage'],
      },
      {
        title: '知识点',
        path: '/content/knowledge-points',
        icon: 'chart',
        permissions: ['content:manage'],
      },
      {
        title: '题库管理',
        path: '/content/questions',
        icon: 'edit',
        permissions: ['content:manage'],
      },
      {
        title: '内容审核',
        path: '/content/reviews',
        icon: 'check',
        permissions: ['content:manage'],
      },
      {
        title: '题目反馈',
        path: '/content/question-feedback',
        icon: 'flag',
        roles: ['admin'],
        permissions: ['content:manage'],
      },
      {
        title: '错因字典',
        path: '/content/error-types',
        icon: 'target',
        permissions: ['content:manage'],
      },
      {
        title: '用户目录',
        path: '/system/users',
        icon: 'users',
        roles: ['admin'],
        permissions: ['system:user:manage'],
      },
      {
        title: '学生学情',
        path: '/learning/records',
        icon: 'chart',
        roles: ['admin'],
        permissions: ['teaching:report:query'],
      },
      {
        title: '推荐记录',
        path: '/learning/recommendations',
        icon: 'target',
        roles: ['admin'],
        permissions: ['teaching:report:query'],
      },
      {
        title: '拍照批改',
        path: '/learning/photo-reviews',
        icon: 'camera',
        roles: ['admin'],
      },
      {
        title: '角色管理',
        path: '/system/roles',
        icon: 'shield',
        roles: ['admin'],
        permissions: ['system:role:manage'],
      },
      {
        title: '权限字典',
        path: '/system/permissions',
        icon: 'target',
        roles: ['admin'],
        permissions: ['system:role:manage'],
      },
      {
        title: '审计日志',
        path: '/audit/logs',
        icon: 'document',
        roles: ['admin'],
        permissions: ['audit:query'],
      },
    ],
  },
  {
    title: '我的账号',
    items: [
      { title: '个人资料', path: '/account/profile', icon: 'user' },
      { title: '学生资料', path: '/account/student', icon: 'book', roles: ['student'] },
      { title: '教师资料', path: '/account/teacher', icon: 'edit', roles: ['teacher'] },
      { title: '账号安全', path: '/account/security', icon: 'lock' },
    ],
  },
]

/** 导航和工作台共用可访问入口，避免快捷入口与侧栏权限不一致。 */
export function getVisibleNavigationGroups(subject: AccessSubject): NavigationGroup[] {
  return navigationGroups.flatMap((group) => {
    const items = group.items.filter((item) => hasAccess(subject, item))
    if (group.title !== '平台管理') return items.length ? [{ ...group, items }] : []
    return [
      { title: '教学内容', items: items.filter((item) => item.path.startsWith('/content/')) },
      { title: '平台管理', items: items.filter((item) => !item.path.startsWith('/content/')) },
    ].filter((section) => section.items.length)
  })
}
