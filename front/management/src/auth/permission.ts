export interface AccessSubject {
  roles: readonly string[]
  permissions: readonly string[]
}

function hasAny(granted: readonly string[], required?: readonly string[]): boolean {
  return !required?.length || required.some((item) => granted.includes(item))
}

/** 同一类别内满足任意一项；同时配置角色和权限时，两类条件都必须满足。 */
export function hasAccess(
  subject: AccessSubject,
  requirements: { roles?: readonly string[]; permissions?: readonly string[] },
): boolean {
  return (
    hasAny(subject.roles, requirements.roles) &&
    hasAny(subject.permissions, requirements.permissions)
  )
}

export function hasAnyPermission(
  granted: readonly string[],
  required: string | readonly string[],
): boolean {
  return hasAny(granted, typeof required === 'string' ? [required] : required)
}
