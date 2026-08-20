/** 只允许站内绝对路径，阻止协议相对地址和登录循环跳转。 */
export function safeRedirect(value: unknown, fallback = '/'): string {
  if (typeof value !== 'string') return fallback
  const target = value.trim()
  if (!target.startsWith('/') || target.startsWith('//') || target.startsWith('/login')) {
    return fallback
  }
  return target
}
