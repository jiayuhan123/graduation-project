import { pageErrorsApi } from '../../../../api/learning'
import type { ErrorRecord } from '../../../../types/learning'
import { withKnowledgeLabels } from '../../../../services/knowledge-labels'

interface DisplayError extends ErrorRecord {
  statusLabel: string
  knowledgePointName: string
}

Page({
  data: {
    loading: true,
    status: 'OPEN',
    records: [] as DisplayError[],
  },
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
      if (revision !== this.loadRevision) return
      const records = page.records.map((item) => ({
        ...item,
        knowledgePointName: '正在加载知识点…',
        statusLabel: labels[item.status] ?? '待更新',
      }))
      this.setData({ loading: false, records })
      const labeled = await withKnowledgeLabels(records)
      if (revision !== this.loadRevision) return
      this.setData({
        records: labeled,
      })
    } catch (error) {
      if (revision !== this.loadRevision) return
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
