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
    title: '成长空间',
    items: [
      { title: '首页', path: '/dashboard', icon: '⌂' },
      { title: '个人资料', path: '/account/profile', icon: '☺' },
      { title: '账号安全', path: '/account/security', icon: '◇' },
    ],
  },
  {
    title: '学生空间',
    items: [
      { title: '学生资料', path: '/account/student', icon: '🎒', roles: ['student'] },
      { title: '我的班级', path: '/teaching/classes', icon: '♧', roles: ['student'] },
      { title: '我的作业', path: '/teaching/assignments', icon: '✎', roles: ['student'] },
      { title: '提交记录', path: '/teaching/submissions', icon: '✓', roles: ['student'] },
    ],
  },
  {
    title: '教师空间',
    items: [
      { title: '教师资料', path: '/account/teacher', icon: '📐', roles: ['teacher'] },
      {
        title: '班级管理',
        path: '/teaching/classes',
        icon: '♧',
        roles: ['teacher'],
        permissions: ['teaching:class:manage'],
      },
      {
        title: '作业管理',
        path: '/teaching/assignments',
        icon: '✎',
        roles: ['teacher'],
        permissions: ['teaching:assignment:manage'],
      },
      {
        title: '学情提交',
        path: '/teaching/submissions',
        icon: '✓',
        roles: ['teacher'],
        permissions: ['teaching:assignment:manage'],
      },
    ],
  },
  {
    title: '平台管理',
    items: [
      {
        title: '内容模块',
        path: '/content/modules',
        icon: '▦',
        permissions: ['content:manage'],
      },
      {
        title: '知识点',
        path: '/content/knowledge-points',
        icon: '⌁',
        permissions: ['content:manage'],
      },
      {
        title: '题库管理',
        path: '/content/questions',
        icon: '✎',
        permissions: ['content:manage'],
      },
      {
        title: '内容审核',
        path: '/content/reviews',
        icon: '✓',
        permissions: ['content:manage'],
      },
      {
        title: '错因字典',
        path: '/content/error-types',
        icon: '◎',
        permissions: ['content:manage'],
      },
      {
        title: '用户目录',
        path: '/system/users',
        icon: '♧',
        roles: ['admin'],
        permissions: ['system:user:manage'],
      },
      {
        title: '角色管理',
        path: '/system/roles',
        icon: '♢',
        roles: ['admin'],
        permissions: ['system:role:manage'],
      },
      {
        title: '权限字典',
        path: '/system/permissions',
        icon: '✦',
        roles: ['admin'],
        permissions: ['system:role:manage'],
      },
    ],
  },
]
