import { listRolesApi } from '../../api/admin-user'
import type { RoleDetail } from '../../types/admin-user'

Page({
  data: {
    loading: true,
    roles: [] as RoleDetail[],
  },

  onLoad() {
    void this.loadRoles()
  },

  async loadRoles() {
    this.setData({ loading: true })
    try {
      this.setData({ roles: await listRolesApi() })
    } catch (error) {
      await wx.showToast({
        title: error instanceof Error ? error.message : '角色数据加载失败',
        icon: 'none',
      })
    } finally {
      this.setData({ loading: false })
    }
  },
})
