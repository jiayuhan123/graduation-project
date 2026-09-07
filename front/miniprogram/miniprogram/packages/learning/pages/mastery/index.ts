import { listMasteryApi } from '../../../../api/learning'
import type { Mastery } from '../../../../types/learning'

interface DisplayMastery extends Mastery {
  masteryPercent: number
  accuracyPercent: number
  reviewText: string
}

Page({
  data: { loading: true, records: [] as DisplayMastery[] },
  onShow() {
    void this.load()
  },
  onPullDownRefresh() {
    void this.load().finally(() => wx.stopPullDownRefresh())
  },
  async load() {
    this.setData({ loading: true })
    try {
      const records = (await listMasteryApi()).map((item) => ({
        ...item,
        masteryPercent: Math.round(item.masteryScore * 100),
        accuracyPercent: Math.round(item.accuracyRate * 100),
        reviewText: item.nextReviewAt
          ? new Date(item.nextReviewAt).toLocaleDateString()
          : '继续练习后生成',
      }))
      this.setData({ loading: false, records })
    } catch (error) {
      this.setData({ loading: false })
      await wx.showToast({
        title: error instanceof Error ? error.message : '掌握度加载失败',
        icon: 'none',
      })
    }
  },
})
