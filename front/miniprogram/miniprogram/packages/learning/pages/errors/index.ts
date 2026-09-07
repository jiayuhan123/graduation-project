import { pageErrorsApi } from '../../../../api/learning'
import type { ErrorRecord } from '../../../../types/learning'

interface DisplayError extends ErrorRecord {
  statusLabel: string
}

Page({
  data: {
    loading: true,
    status: 'OPEN',
    records: [] as DisplayError[],
  },

  onShow() {
    void this.load()
  },

  onPullDownRefresh() {
    void this.load().finally(() => wx.stopPullDownRefresh())
  },

  async load() {
    this.setData({ loading: true })
    try {
      const page = await pageErrorsApi({
        page: 1,
        size: 100,
        status: this.data.status || undefined,
      })
      const labels: Record<string, string> = {
        OPEN: '待巩固',
        RETRAINING: '巩固中',
        RESOLVED: '已掌握',
      }
      this.setData({
        loading: false,
        records: page.records.map((item) => ({
          ...item,
          statusLabel: labels[item.status] ?? item.status,
        })),
      })
    } catch (error) {
      this.setData({ loading: false })
      await wx.showToast({
        title: error instanceof Error ? error.message : '错题加载失败',
        icon: 'none',
      })
    }
  },

  selectStatus(event: WechatMiniprogram.BaseEvent) {
    this.setData({ status: String(event.currentTarget.dataset.status ?? '') })
    void this.load()
  },
})
