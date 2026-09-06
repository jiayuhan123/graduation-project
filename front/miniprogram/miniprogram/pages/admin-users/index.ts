import { listAdminUsersApi } from '../../api/admin-user'
import { directAvatarDisplayUrl } from '../../services/avatar'
import type { AdminUserSummary } from '../../types/admin-user'

const ROLE_FILTERS = [
  { label: '全部角色', code: '' },
  { label: '学生', code: 'student' },
  { label: '教师', code: 'teacher' },
  { label: '管理员', code: 'admin' },
]

interface UserCard extends AdminUserSummary {
  displayName: string
  avatarDisplayUrl: string
  avatarText: string
  roleText: string
}

function toUserCard(user: AdminUserSummary): UserCard {
  const displayName = user.nickname || user.realName || user.username
  return {
    ...user,
    displayName,
    avatarDisplayUrl: directAvatarDisplayUrl(user.avatarUrl),
    avatarText: displayName.slice(0, 1),
    roleText: user.roles.map((role) => role.name).join('、') || '未分配角色',
  }
}

Page({
  data: {
    loading: true,
    loadingMore: false,
    keyword: '',
    roleFilters: ROLE_FILTERS,
    roleIndex: 0,
    users: [] as UserCard[],
    page: 1,
    pages: 1,
    total: 0,
  },

  onLoad() {
    void this.loadUsers(true)
  },

  onPullDownRefresh() {
    void this.loadUsers(true).finally(() => wx.stopPullDownRefresh())
  },

  onKeywordInput(event: WechatMiniprogram.Input) {
    this.setData({ keyword: event.detail.value })
  },

  onSearch() {
    void this.loadUsers(true)
  },

  onRoleChange(event: WechatMiniprogram.PickerChange) {
    this.setData({ roleIndex: Number(event.detail.value) })
    void this.loadUsers(true)
  },

  async loadUsers(reset: boolean) {
    const nextPage = reset ? 1 : this.data.page + 1
    if (!reset && nextPage > this.data.pages) return
    this.setData(reset ? { loading: true } : { loadingMore: true })
    try {
      const roleCode = ROLE_FILTERS[this.data.roleIndex]?.code
      const result = await listAdminUsersApi({
        page: nextPage,
        size: 10,
        keyword: this.data.keyword.trim() || undefined,
        roleCode: roleCode || undefined,
      })
      const records = result.records.map(toUserCard)
      this.setData({
        users: reset ? records : [...this.data.users, ...records],
        page: result.page,
        pages: result.pages,
        total: result.total,
      })
    } catch (error) {
      await wx.showToast({
        title: error instanceof Error ? error.message : '用户列表加载失败',
        icon: 'none',
      })
    } finally {
      this.setData({ loading: false, loadingMore: false })
    }
  },

  loadMore() {
    void this.loadUsers(false)
  },
})
