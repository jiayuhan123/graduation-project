import { getPlayerProgressApi, pageRewardsApi } from '../../../../api/game'
import type { PlayerProgress, Reward } from '../../../../types/game'

Page({
  data: {
    loading: true,
    progress: null as PlayerProgress | null,
    rewards: [] as Reward[],
    winRate: 0,
  },

  onShow() {
    void this.loadProgress()
  },

  async loadProgress() {
    this.setData({ loading: true })
    try {
      const [progress, rewards] = await Promise.all([
        getPlayerProgressApi(),
        pageRewardsApi({ page: 1, size: 5 }),
      ])
      const decided = progress.pkWins + progress.pkLosses + progress.pkDraws
      this.setData({
        progress,
        rewards: rewards.records,
        winRate: decided ? Math.round((progress.pkWins / decided) * 100) : 0,
      })
    } catch (error) {
      await wx.showToast({
        title: error instanceof Error ? error.message : '成长数据加载失败',
        icon: 'none',
      })
    } finally {
      this.setData({ loading: false })
    }
  },

  openRewards() {
    void wx.navigateTo({ url: '/packages/game/pages/rewards/index' })
  },
})
