import { describe, expect, it } from 'vitest'
import { getVisibleNavigationGroups } from '@/router/navigation'

const pathsFor = (roles: string[], permissions: string[] = []) =>
  getVisibleNavigationGroups({ roles, permissions }).flatMap((group) =>
    group.items.map((item) => item.path),
  )

describe('工作空间入口', () => {
  it('学生可以进入学习和班级入口，不能看到管理入口', () => {
    const paths = pathsFor(['student'])
    expect(paths).toContain('/learning/practice')
    expect(paths).toContain('/teaching/assignments')
    expect(paths).toContain('/account/student')
    expect(paths).not.toContain('/account/teacher')
    expect(paths).not.toContain('/system/users')
  })

  it('教师快捷入口与导航都遵守已授予的权限', () => {
    const paths = pathsFor(['teacher'], ['teaching:class:manage'])
    expect(paths).toContain('/teaching/classes')
    expect(paths).not.toContain('/teaching/assignments')
    expect(paths).not.toContain('/learning/class-dashboard')
    expect(paths).not.toContain('/learning/photo-reviews')
  })

  it('切换到教师身份后，不保留学生专属入口', () => {
    const paths = pathsFor(['teacher'], ['teaching:assignment:manage'])
    expect(paths).toContain('/teaching/assignments')
    expect(paths).toContain('/account/teacher')
    expect(paths).not.toContain('/learning/practice')
    expect(paths).not.toContain('/account/student')
  })

  it('教学 Agent 只对具有学情权限的教师开放', () => {
    expect(pathsFor(['teacher'], ['teaching:report:query'])).toContain('/ai/teaching-agent')
    expect(pathsFor(['teacher'], ['teaching:class:manage'])).not.toContain('/ai/teaching-agent')
  })

  it('Agent 知识库只向具有内容管理权限的管理员开放', () => {
    expect(pathsFor(['admin'], ['content:manage'])).toContain('/ai/knowledge-base')
    expect(pathsFor(['admin'], ['teaching:report:query'])).not.toContain('/ai/knowledge-base')
    expect(pathsFor(['teacher'], ['content:manage'])).not.toContain('/ai/knowledge-base')
  })

  it('保留按权限开放的内容管理，并将账号设置放在末尾', () => {
    const groups = getVisibleNavigationGroups({
      roles: ['teacher'],
      permissions: ['content:manage'],
    })
    expect(groups.find((group) => group.title === '教学内容')?.items).toEqual(
      expect.arrayContaining([expect.objectContaining({ path: '/content/questions' })]),
    )
    expect(groups.at(-1)?.title).toBe('我的账号')
    expect(groups.every((group) => group.items.length > 0)).toBe(true)
    expect(pathsFor(['teacher'], ['content:manage'])).not.toContain('/system/users')
  })
})
