import { pageRewardsApi } from '../../../../api/game'
import type { Reward } from '../../../../types/game'

Page({
  data: { loading: true, records: [] as Reward[] },
  onLoad() {
    void this.loadRewards()
  },
  async loadRewards() {
    try {
      const page = await pageRewardsApi({ page: 1, size: 100 })
      this.setData({ records: page.records })
    } catch (error) {
      await wx.showToast({
        title: error instanceof Error ? error.message : '奖励记录加载失败',
        icon: 'none',
      })
    } finally {
      this.setData({ loading: false })
    }
  },
})
