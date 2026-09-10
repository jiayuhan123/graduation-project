export interface RoleOption {
  code: string
  label: string
  icon: string
  description: string
}

const CORE_ROLE_META: Record<string, Omit<RoleOption, 'code'>> = {
  student: {
    label: '学生',
    icon: 'education',
    description: '管理年级、学期和学习身份',
  },
  teacher: {
    label: '教师',
    icon: 'edit-1',
    description: '管理教师资料和教学身份',
  },
  admin: {
    label: '管理员',
    icon: 'secured',
    description: '查看账号权限和平台身份',
  },
}

export function roleLabel(roleCode: string): string {
  return CORE_ROLE_META[roleCode]?.label ?? roleCode
}

export function roleOptions(roleCodes: string[]): RoleOption[] {
  return roleCodes.map((code) => ({
    code,
    label: roleLabel(code),
    icon: CORE_ROLE_META[code]?.icon ?? 'user',
    description: CORE_ROLE_META[code]?.description ?? '使用该角色对应的功能',
  }))
}

export function preferredRole(roleCodes: string[], candidate?: string): string {
  if (candidate && roleCodes.includes(candidate)) return candidate
  return roleCodes.includes('student') ? 'student' : (roleCodes[0] ?? 'student')
}
