export interface RoleMeta {
  label: string
  icon: string
  description: string
  accent: string
}

const ROLE_META: Record<string, RoleMeta> = {
  student: {
    label: '学生',
    icon: 'book',
    description: '练习、作业与学习档案',
    accent: '#3567c8',
  },
  teacher: {
    label: '教师',
    icon: 'edit',
    description: '班级、作业与学习反馈',
    accent: '#4b80cf',
  },
  admin: {
    label: '管理员',
    icon: 'shield',
    description: '用户、角色与平台权限',
    accent: '#315387',
  },
}

export function roleMeta(role: string): RoleMeta {
  return (
    ROLE_META[role] ?? {
      label: role,
      icon: 'user',
      description: '使用该角色对应的功能',
      accent: '#7b8289',
    }
  )
}

export function roleLabel(role: string): string {
  return roleMeta(role).label
}
