import {
  acceptRecommendationApi,
  dismissRecommendationApi,
  pageRecommendationsApi,
} from '../../../../api/learning'
import type { RecommendationItem } from '../../../../types/learning'

Page({
  data: { loading: true, operatingId: '', records: [] as RecommendationItem[] },
  onShow() {
    void this.load()
  },
  onPullDownRefresh() {
    void this.load().finally(() => wx.stopPullDownRefresh())
  },
  async load() {
    this.setData({ loading: true })
    try {
      const page = await pageRecommendationsApi({ page: 1, size: 100, status: 'PENDING' })
      this.setData({ loading: false, records: page.records })
    } catch (error) {
      this.setData({ loading: false })
      await this.showError(error, '推荐加载失败')
    }
  },
  async accept(event: WechatMiniprogram.BaseEvent) {
    const id = String(event.currentTarget.dataset.id ?? '')
    if (!id) return
    this.setData({ operatingId: id })
    try {
      const detail = await acceptRecommendationApi(id)
      await wx.navigateTo({
        url: `/packages/learning/pages/practice-session/index?taskId=${detail.task.id}`,
      })
    } catch (error) {
      await this.showError(error, '巩固练习创建失败')
    } finally {
      this.setData({ operatingId: '' })
    }
  },
  async dismiss(event: WechatMiniprogram.BaseEvent) {
    const id = String(event.currentTarget.dataset.id ?? '')
    if (!id) return
    const confirmation = await wx.showModal({ title: '忽略推荐', content: '确定暂时不做这组巩固题吗？' })
    if (!confirmation.confirm) return
    this.setData({ operatingId: id })
    try {
      await dismissRecommendationApi(id)
      await this.load()
    } catch (error) {
      await this.showError(error, '推荐处理失败')
    } finally {
      this.setData({ operatingId: '' })
    }
  },
  async showError(error: unknown, fallback: string) {
    await wx.showToast({ title: error instanceof Error ? error.message : fallback, icon: 'none' })
  },
})
