import { listMasteryApi } from '../../../../api/learning'
import type { Mastery } from '../../../../types/learning'
import { withKnowledgeLabels } from '../../../../services/knowledge-labels'

interface DisplayMastery extends Mastery {
  knowledgePointName: string
  masteryPercent: number
  accuracyPercent: number
  reviewText: string
}

Page({
  data: { loading: true, records: [] as DisplayMastery[] },
  loadRevision: 0,
  onShow() {
    void this.load()
  },
  onPullDownRefresh() {
    void this.load().finally(() => wx.stopPullDownRefresh())
  },
  async load() {
    const revision = ++this.loadRevision
    this.setData({ loading: true })
    try {
      const records = (await listMasteryApi()).map((item) => ({
        ...item,
        knowledgePointName: '正在加载知识点…',
        masteryPercent: Math.round(item.masteryScore * 100),
        accuracyPercent: Math.round(item.accuracyRate * 100),
        reviewText: item.nextReviewAt
          ? new Date(item.nextReviewAt).toLocaleDateString()
          : '继续练习后生成',
      }))
      if (revision !== this.loadRevision) return
      this.setData({ loading: false, records })
      const labeled = await withKnowledgeLabels(records)
      if (revision === this.loadRevision) this.setData({ records: labeled })
    } catch (error) {
      if (revision !== this.loadRevision) return
      this.setData({ loading: false })
      await wx.showToast({
        title: error instanceof Error ? error.message : '掌握度加载失败',
        icon: 'none',
      })
    }
  },
})
