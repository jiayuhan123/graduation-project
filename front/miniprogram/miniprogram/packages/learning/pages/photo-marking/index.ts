import {
  confirmPhotoItemApi,
  createPhotoMarkingApi,
  getPhotoTaskApi,
  pagePhotoTasksApi,
} from '../../../../api/learning'
import { uploadPrivateFileApi } from '../../../../api/file'
import type { PhotoDetail, PhotoItem, PhotoTask } from '../../../../types/learning'

type PhotoTaskView = PhotoTask & { statusText: string; confidenceText: string }
type PhotoItemView = PhotoItem & { answerText: string; confidenceText: string; statusText: string }

const statusLabels: Record<string, string> = {
  UPLOADED: '等待识别',
  PROCESSING: '识别中',
  REVIEW_REQUIRED: '需要确认',
  COMPLETED: '已完成',
  FAILED: '识别失败',
  CONFIRMED: '已确认',
  REJECTED: '已忽略',
}

let pollTimer: ReturnType<typeof setInterval> | null = null

function readAnswer(value: string | null): string {
  if (!value) return ''
  try {
    const parsed = JSON.parse(value) as { value?: unknown }
    return typeof parsed.value === 'string' ? parsed.value : value
  } catch {
    return value
  }
}

function toTaskView(task: PhotoTask): PhotoTaskView {
  return {
    ...task,
    statusText: statusLabels[task.status] ?? task.status,
    confidenceText:
      task.overallConfidence === null ? '--' : `${Math.round(task.overallConfidence * 100)}%`,
  }
}

function toDetailView(detail: PhotoDetail): { task: PhotoTaskView; items: PhotoItemView[] } {
  return {
    task: toTaskView(detail.task),
    items: detail.items.map((item) => ({
      ...item,
      answerText: readAnswer(item.recognizedAnswer),
      confidenceText:
        item.recognitionConfidence === null
          ? '--'
          : `${Math.round(item.recognitionConfidence * 100)}%`,
      statusText: statusLabels[item.status] ?? item.status,
    })),
  }
}

Page({
  data: {
    loading: true,
    submitting: false,
    reviewing: false,
    pendingImagePath: '',
    records: [] as PhotoTaskView[],
    detail: null as ReturnType<typeof toDetailView> | null,
    selectedItemId: '',
    correctedAnswer: '',
    matchedQuestionId: '',
  },

  onShow() {
    void this.load()
    this.startPolling()
  },

  onHide() {
    this.stopPolling()
  },

  onUnload() {
    this.stopPolling()
  },

  startPolling() {
    this.stopPolling()
    pollTimer = setInterval(() => {
      const task = this.data.detail?.task
      if (task && (task.status === 'UPLOADED' || task.status === 'PROCESSING')) {
        void this.openTaskById(task.id, false)
      }
    }, 3000)
  },

  stopPolling() {
    if (pollTimer) clearInterval(pollTimer)
    pollTimer = null
  },

  async load() {
    this.setData({ loading: true })
    try {
      const page = await pagePhotoTasksApi({ page: 1, size: 50 })
      this.setData({ loading: false, records: page.records.map(toTaskView) })
    } catch (error) {
      this.setData({ loading: false })
      await wx.showToast({
        title: error instanceof Error ? error.message : '历史记录加载失败',
        icon: 'none',
      })
    }
  },

  async chooseImage() {
    try {
      const result = await wx.chooseMedia({
        count: 1,
        mediaType: ['image'],
        sourceType: ['album', 'camera'],
      })
      const path = result.tempFiles[0]?.tempFilePath
      if (path) this.setData({ pendingImagePath: path })
    } catch {
      // 用户主动取消选择，无需提示。
    }
  },

  async submitImage() {
    if (!this.data.pendingImagePath || this.data.submitting) return
    this.setData({ submitting: true })
    try {
      const uploaded = await uploadPrivateFileApi(this.data.pendingImagePath)
      const detail = await createPhotoMarkingApi({ originalFileId: uploaded.id, pageCount: 1 })
      this.setData({
        pendingImagePath: '',
        detail: toDetailView(detail),
        selectedItemId: '',
      })
      await this.load()
      await wx.showToast({ title: '已提交识别', icon: 'success' })
    } catch (error) {
      await wx.showToast({
        title: error instanceof Error ? error.message : '提交失败',
        icon: 'none',
      })
    } finally {
      this.setData({ submitting: false })
    }
  },

  openTask(event: WechatMiniprogram.TouchEvent) {
    const taskId = String(event.currentTarget.dataset.id ?? '')
    if (taskId) void this.openTaskById(taskId, true)
  },

  async openTaskById(taskId: string, showLoading: boolean) {
    if (showLoading) this.setData({ loading: true })
    try {
      const detail = await getPhotoTaskApi(taskId)
      this.setData({
        loading: false,
        detail: toDetailView(detail),
        selectedItemId: '',
        correctedAnswer: '',
        matchedQuestionId: '',
      })
      if (detail.task.status === 'COMPLETED' || detail.task.status === 'FAILED') {
        await this.load()
      }
    } catch (error) {
      this.setData({ loading: false })
      if (showLoading) {
        await wx.showToast({
          title: error instanceof Error ? error.message : '任务加载失败',
          icon: 'none',
        })
      }
    }
  },

  closeDetail() {
    this.setData({ detail: null, selectedItemId: '' })
  },

  selectItem(event: WechatMiniprogram.TouchEvent) {
    const itemId = String(event.currentTarget.dataset.id ?? '')
    const item = this.data.detail?.items.find((candidate) => candidate.id === itemId)
    if (!item || item.status !== 'REVIEW_REQUIRED') return
    this.setData({
      selectedItemId: item.id,
      correctedAnswer: item.answerText,
      matchedQuestionId: item.matchedQuestionId ?? '',
    })
  },

  onAnswerInput(event: WechatMiniprogram.Input) {
    this.setData({ correctedAnswer: event.detail.value })
  },

  onQuestionIdInput(event: WechatMiniprogram.Input) {
    this.setData({ matchedQuestionId: event.detail.value.trim() })
  },

  async reviewItem(event: WechatMiniprogram.TouchEvent) {
    if (this.data.reviewing || !this.data.detail || !this.data.selectedItemId) return
    const decision = String(event.currentTarget.dataset.decision) as
      'APPROVE' | 'CORRECT' | 'REJECT'
    if (decision === 'CORRECT' && !this.data.correctedAnswer.trim()) {
      await wx.showToast({ title: '请填写正确的识别答案', icon: 'none' })
      return
    }
    this.setData({ reviewing: true })
    try {
      const detail = await confirmPhotoItemApi(this.data.detail.task.id, this.data.selectedItemId, {
        decision,
        recognizedAnswer: decision === 'CORRECT' ? this.data.correctedAnswer.trim() : undefined,
        matchedQuestionId:
          decision === 'CORRECT' && this.data.matchedQuestionId
            ? this.data.matchedQuestionId
            : undefined,
      })
      this.setData({
        detail: toDetailView(detail),
        selectedItemId: '',
        correctedAnswer: '',
        matchedQuestionId: '',
      })
      await this.load()
      await wx.showToast({ title: '处理完成', icon: 'success' })
    } catch (error) {
      await wx.showToast({
        title: error instanceof Error ? error.message : '处理失败',
        icon: 'none',
      })
    } finally {
      this.setData({ reviewing: false })
    }
  },
})
