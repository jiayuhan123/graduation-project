import { pagePhotoTasksApi } from '../../../../api/learning'
import type { PhotoTask } from '../../../../types/learning'

Page({
  data: { loading: true, records: [] as PhotoTask[] },
  onShow() {
    void this.load()
  },
  async load() {
    this.setData({ loading: true })
    try {
      const page = await pagePhotoTasksApi({ page: 1, size: 50 })
      this.setData({ loading: false, records: page.records })
    } catch (error) {
      this.setData({ loading: false })
      await wx.showToast({
        title: error instanceof Error ? error.message : '历史记录加载失败',
        icon: 'none',
      })
    }
  },
})
