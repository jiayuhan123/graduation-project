import { pageSessionsApi } from '../../../../api/game'
import type { GameSessionSummary } from '../../../../types/game'

Page({
  data: {
    loading: true,
    records: [] as GameSessionSummary[],
  },

  onShow() {
    void this.loadRecords()
  },

  async loadRecords() {
    this.setData({ loading: true })
    try {
      const page = await pageSessionsApi({ page: 1, size: 50 })
      this.setData({ records: page.records.filter((item) => item.mode === 'PK') })
    } catch (error) {
      await wx.showToast({
        title: error instanceof Error ? error.message : '对局记录加载失败',
        icon: 'none',
      })
    } finally {
      this.setData({ loading: false })
    }
  },

  openSession(event: WechatMiniprogram.BaseEvent) {
    const sessionId = String(event.currentTarget.dataset.id ?? '')
    if (sessionId)
      void wx.navigateTo({ url: `/packages/game/pages/play/index?sessionId=${sessionId}` })
  },
})
