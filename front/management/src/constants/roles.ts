export interface RoleMeta {
  label: string
  icon: string
  description: string
  accent: string
}

const ROLE_META: Record<string, RoleMeta> = {
  student: {
    label: '学生',
    icon: '🎒',
    description: '练习、成长档案与数学闯关',
    accent: '#ff8a3d',
  },
  teacher: {
    label: '教师',
    icon: '📐',
    description: '班级、作业与学习反馈',
    accent: '#4dabf7',
  },
  admin: {
    label: '管理员',
    icon: '🛡️',
    description: '用户、角色与平台权限',
    accent: '#7c6bf2',
  },
}

export function roleMeta(role: string): RoleMeta {
  return (
    ROLE_META[role] ?? {
      label: role,
      icon: '👤',
      description: '使用该角色对应的功能',
      accent: '#7b8289',
    }
  )
}

export function roleLabel(role: string): string {
  return roleMeta(role).label
}
