import { getCurrentUserApi } from '../../api/account'
import { roleLabel, roleOptions } from '../../constants/roles'
import { resolveAvatarDisplayUrl } from '../../services/avatar'
import { getSession, setActiveRole } from '../../services/session'
import type { UserProfile } from '../../types/account'

function identityDescription(profile: UserProfile, role: string): string {
  if (role === 'student' && profile.studentProfile) {
    const { gradeLevel, semester, studentNo } = profile.studentProfile
    return `${gradeLevel} 年级 · ${semester === 1 ? '上学期' : '下学期'}${studentNo ? ` · ${studentNo}` : ''}`
  }
  if (role === 'teacher' && profile.teacherProfile) {
    const { title, teacherNo } = profile.teacherProfile
    return [title, teacherNo].filter(Boolean).join(' · ') || '教师资料待完善'
  }
  if (role === 'admin') return `拥有 ${profile.permissions.length} 项账号权限`
  return '当前角色资料已连接'
}

Page({
  data: {
    loading: true,
    displayName: '用户',
    username: '',
    avatarUrl: '',
    avatarText: '用',
    roleOptions: [] as ReturnType<typeof roleOptions>,
    activeRole: '',
    activeRoleLabel: '',
    activeRoleDescription: '',
    identityLine: '',
    roleCount: 0,
    permissionCount: 0,
    wechatBound: false,
    isStudentRole: false,
    isTeacherRole: false,
    hasStudentRole: false,
    hasTeacherRole: false,
    isAdminRole: false,
    canManageUsers: false,
    canManageRoles: false,
  },

  onShow() {
    const session = getSession()
    if (!session) {
      void wx.reLaunch({ url: '/pages/login/index' })
      return
    }
    void this.loadProfile(session.activeRole)
  },

  async loadProfile(activeRole: string) {
    this.setData({ loading: true })
    try {
      const profile = await getCurrentUserApi()
      const selectedRole = profile.roles.includes(activeRole)
        ? activeRole
        : (profile.roles[0] ?? 'student')
      setActiveRole(selectedRole)
      const displayName = profile.nickname || profile.realName || profile.username
      const avatarUrl = await resolveAvatarDisplayUrl(profile.avatarUrl).catch(() => '')
      this.setData({
        loading: false,
        displayName,
        username: profile.username,
        avatarUrl,
        avatarText: displayName.slice(0, 1),
        roleOptions: roleOptions(profile.roles),
        activeRole: selectedRole,
        activeRoleLabel: roleLabel(selectedRole),
        activeRoleDescription:
          roleOptions(profile.roles).find((role) => role.code === selectedRole)?.description ?? '',
        identityLine: identityDescription(profile, selectedRole),
        roleCount: profile.roles.length,
        permissionCount: profile.permissions.length,
        wechatBound: profile.wechatBound,
        isStudentRole: selectedRole === 'student',
        isTeacherRole: selectedRole === 'teacher',
        hasStudentRole: profile.roles.includes('student'),
        hasTeacherRole: profile.roles.includes('teacher'),
        isAdminRole: selectedRole === 'admin',
        canManageUsers: profile.permissions.includes('system:user:manage'),
        canManageRoles: profile.permissions.includes('system:role:manage'),
      })
    } catch (error) {
      this.setData({ loading: false })
      await wx.showToast({
        title: error instanceof Error ? error.message : '账号资料加载失败',
        icon: 'none',
      })
    }
  },

  switchRole(event: WechatMiniprogram.BaseEvent) {
    const role = String(event.currentTarget.dataset.role ?? '')
    if (!role || role === this.data.activeRole) return
    const session = setActiveRole(role)
    if (!session) return
    void this.loadProfile(role)
  },

  openProfile() {
    void wx.switchTab({ url: '/pages/profile/index' })
  },

  openStudentProfile() {
    void wx.navigateTo({ url: '/pages/student-profile/index' })
  },

  openPractice() {
    void wx.navigateTo({ url: '/packages/learning/pages/practice/index' })
  },

  openTeachingClasses() {
    void wx.navigateTo({ url: '/packages/teaching/pages/classes/index' })
  },

  openTeachingAssignments() {
    void wx.navigateTo({ url: '/packages/teaching/pages/assignments/index' })
  },

  openTeachingSubmissions() {
    void wx.navigateTo({ url: '/packages/teaching/pages/submissions/index' })
  },

  openTeacherProfile() {
    void wx.navigateTo({ url: '/pages/teacher-profile/index' })
  },

  openSecurity() {
    void wx.navigateTo({ url: '/pages/security/index' })
  },

  openAdminUsers() {
    void wx.navigateTo({ url: '/pages/admin-users/index' })
  },

  openAdminRoles() {
    void wx.navigateTo({ url: '/pages/admin-roles/index' })
  },
})
