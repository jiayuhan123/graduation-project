import { getCurrentUserApi } from '../../api/account'
import { logoutApi } from '../../api/auth'
import { roleLabel, roleOptions } from '../../constants/roles'
import { resolveAvatarDisplayUrl } from '../../services/avatar'
import { clearSession, getSession, setActiveRole } from '../../services/session'
import type { UserProfile } from '../../types/account'

function formatDate(value: string | null): string {
  if (!value) return '暂无记录'
  return value.replace('T', ' ').slice(0, 16)
}

function studentSummary(profile: UserProfile): string {
  if (!profile.studentProfile) return '资料待完善'
  const { gradeLevel, semester } = profile.studentProfile
  return `${gradeLevel} 年级 · ${semester === 1 ? '上学期' : '下学期'}`
}

function teacherSummary(profile: UserProfile): string {
  return profile.teacherProfile?.title || '资料待完善'
}

Page({
  data: {
    loading: true,
    profile: null as UserProfile | null,
    displayName: '用户',
    avatarUrl: '',
    avatarText: '用',
    activeRole: '',
    activeRoleLabel: '',
    roleOptions: [] as ReturnType<typeof roleOptions>,
    studentSummary: '',
    teacherSummary: '',
    joinedAt: '',
    lastLoginAt: '',
    loggingOut: false,
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
        profile,
        displayName,
        avatarUrl,
        avatarText: displayName.slice(0, 1),
        activeRole: selectedRole,
        activeRoleLabel: roleLabel(selectedRole),
        roleOptions: roleOptions(profile.roles),
        studentSummary: studentSummary(profile),
        teacherSummary: teacherSummary(profile),
        joinedAt: formatDate(profile.createdAt),
        lastLoginAt: formatDate(profile.lastLoginAt),
      })
    } catch (error) {
      this.setData({ loading: false })
      await wx.showToast({
        title: error instanceof Error ? error.message : '个人资料加载失败',
        icon: 'none',
      })
    }
  },

  switchRole(event: WechatMiniprogram.BaseEvent) {
    const role = String(event.currentTarget.dataset.role ?? '')
    if (!role || role === this.data.activeRole) return
    const session = setActiveRole(role)
    if (!session) return
    this.setData({ activeRole: role, activeRoleLabel: roleLabel(role) })
  },

  openEditProfile() {
    void wx.navigateTo({ url: '/pages/profile-edit/index' })
  },

  openStudentProfile() {
    void wx.navigateTo({ url: '/pages/student-profile/index' })
  },

  openTeacherProfile() {
    void wx.navigateTo({ url: '/pages/teacher-profile/index' })
  },

  openSecurity() {
    void wx.navigateTo({ url: '/pages/security/index' })
  },

  async handleLogout() {
    if (this.data.loggingOut) return
    const result = await wx.showModal({
      title: '退出登录',
      content: '确定退出当前账号吗？',
      confirmColor: '#ff8a3d',
    })
    if (!result.confirm) return

    this.setData({ loggingOut: true })
    try {
      await logoutApi()
    } catch {
      // 本地会话仍需清理，避免失效令牌滞留。
    } finally {
      clearSession()
      this.setData({ loggingOut: false })
      await wx.reLaunch({ url: '/pages/login/index' })
    }
  },
})
