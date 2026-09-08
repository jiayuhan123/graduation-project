import { getCurrentUserApi } from '../../../../api/account'
import { listKnowledgePointsApi, pageContentModulesApi } from '../../../../api/content'
import {
  cancelGameSessionApi,
  cancelRandomMatchApi,
  createPkRoomApi,
  getSessionApi,
  joinPkRoomApi,
  requestRandomMatchApi,
} from '../../../../api/game'
import type { ContentModule, KnowledgePoint } from '../../../../types/content'
import type { PkRuleInput } from '../../../../types/game'

let pollTimer: ReturnType<typeof setInterval> | undefined

function errorMessage(error: unknown): string {
  return error instanceof Error ? error.message : '操作失败，请稍后重试'
}

Page({
  data: {
    loading: true,
    loadingPoints: false,
    submitting: false,
    matching: false,
    waitingRoom: false,
    gradeLevel: 1,
    modules: [] as ContentModule[],
    moduleNames: [] as string[],
    moduleIndex: 0,
    knowledgePoints: [] as KnowledgePoint[],
    knowledgePointNames: [] as string[],
    knowledgePointIndex: 0,
    difficulty: 3,
    difficultyOptions: [1, 2, 3, 4, 5],
    difficultyIndex: 2,
    questionCount: 5,
    questionCountOptions: [1, 3, 5, 10],
    questionCountIndex: 2,
    timeLimitSeconds: 180,
    timeOptions: [60, 120, 180, 300],
    timeIndex: 2,
    inviteCode: '',
    roomInviteCode: '',
    waitingSessionId: '',
    matchTicketId: '',
    waitingText: '',
  },

  onLoad() {
    void this.loadCatalog()
  },

  onUnload() {
    this.stopPolling()
  },

  async loadCatalog() {
    this.setData({ loading: true })
    try {
      const profile = await getCurrentUserApi()
      if (!profile.studentProfile) throw new Error('请先完善学生年级和学期信息')
      const { gradeLevel, semester } = profile.studentProfile
      const page = await pageContentModulesApi({ page: 1, size: 100, gradeLevel, semester })
      this.setData({
        loading: false,
        gradeLevel,
        modules: page.records,
        moduleNames: page.records.map((item) => item.name),
        moduleIndex: 0,
      })
      if (page.records[0]) await this.loadKnowledgePoints(page.records[0].id)
    } catch (error) {
      this.setData({ loading: false })
      await wx.showToast({ title: errorMessage(error), icon: 'none' })
    }
  },

  async loadKnowledgePoints(moduleId: string) {
    this.setData({ loadingPoints: true })
    try {
      const points = await listKnowledgePointsApi({ moduleId })
      this.setData({
        loadingPoints: false,
        knowledgePoints: points,
        knowledgePointNames: points.map((item) => item.name),
        knowledgePointIndex: 0,
      })
    } catch (error) {
      this.setData({ loadingPoints: false })
      await wx.showToast({ title: errorMessage(error), icon: 'none' })
    }
  },

  onModuleChange(event: WechatMiniprogram.PickerChange) {
    const moduleIndex = Number(event.detail.value)
    const selected = this.data.modules[moduleIndex]
    this.setData({ moduleIndex })
    if (selected) void this.loadKnowledgePoints(selected.id)
  },

  onKnowledgePointChange(event: WechatMiniprogram.PickerChange) {
    this.setData({ knowledgePointIndex: Number(event.detail.value) })
  },

  onDifficultyChange(event: WechatMiniprogram.PickerChange) {
    const difficultyIndex = Number(event.detail.value)
    this.setData({ difficultyIndex, difficulty: this.data.difficultyOptions[difficultyIndex] })
  },

  onQuestionCountChange(event: WechatMiniprogram.PickerChange) {
    const questionCountIndex = Number(event.detail.value)
    this.setData({
      questionCountIndex,
      questionCount: this.data.questionCountOptions[questionCountIndex],
    })
  },

  onTimeChange(event: WechatMiniprogram.PickerChange) {
    const timeIndex = Number(event.detail.value)
    this.setData({ timeIndex, timeLimitSeconds: this.data.timeOptions[timeIndex] })
  },

  onInviteCodeInput(event: WechatMiniprogram.Input) {
    this.setData({ inviteCode: event.detail.value.toUpperCase().replace(/[^A-Z0-9]/g, '') })
  },

  selectedRule(): PkRuleInput | null {
    const point = this.data.knowledgePoints[this.data.knowledgePointIndex]
    if (!point) return null
    return {
      gradeLevel: this.data.gradeLevel,
      knowledgePointId: point.id,
      difficulty: this.data.difficulty,
      questionCount: this.data.questionCount,
      timeLimitSeconds: this.data.timeLimitSeconds,
    }
  },

  async createRoom() {
    const rule = this.selectedRule()
    if (!rule || this.data.submitting) {
      if (!rule) await wx.showToast({ title: '请选择知识点', icon: 'none' })
      return
    }
    this.setData({ submitting: true })
    try {
      const detail = await createPkRoomApi(rule)
      this.setData({
        waitingRoom: true,
        waitingSessionId: detail.session.id,
        roomInviteCode: detail.session.inviteCode ?? '',
        waitingText: '等待对手输入邀请码加入',
      })
      this.startRoomPolling(detail.session.id)
    } catch (error) {
      await wx.showToast({ title: errorMessage(error), icon: 'none' })
    } finally {
      this.setData({ submitting: false })
    }
  },

  async joinRoom() {
    const inviteCode = this.data.inviteCode.trim()
    if (!/^[A-Z0-9]{6,12}$/.test(inviteCode) || this.data.submitting) {
      await wx.showToast({ title: '请输入 6–12 位邀请码', icon: 'none' })
      return
    }
    this.setData({ submitting: true })
    try {
      const detail = await joinPkRoomApi(inviteCode)
      await this.enterSession(detail.session.id)
    } catch (error) {
      await wx.showToast({ title: errorMessage(error), icon: 'none' })
    } finally {
      this.setData({ submitting: false })
    }
  },

  async randomMatch() {
    const rule = this.selectedRule()
    if (!rule || this.data.submitting) {
      if (!rule) await wx.showToast({ title: '请选择知识点', icon: 'none' })
      return
    }
    this.setData({ submitting: true })
    try {
      const status = await requestRandomMatchApi(rule)
      if (status.sessionId) {
        await this.enterSession(status.sessionId)
        return
      }
      this.setData({
        matching: true,
        matchTicketId: status.ticketId ?? '',
        waitingText: '正在寻找相同年级、知识点和难度的对手',
      })
      this.startMatchPolling(rule)
    } catch (error) {
      await wx.showToast({ title: errorMessage(error), icon: 'none' })
    } finally {
      this.setData({ submitting: false })
    }
  },

  startMatchPolling(rule: PkRuleInput) {
    this.stopPolling()
    pollTimer = setInterval(() => {
      void requestRandomMatchApi(rule)
        .then((status) => {
          if (status.sessionId) void this.enterSession(status.sessionId)
        })
        .catch(() => undefined)
    }, 2000)
  },

  startRoomPolling(sessionId: string) {
    this.stopPolling()
    pollTimer = setInterval(() => {
      void getSessionApi(sessionId)
        .then((detail) => {
          if (detail.session.status === 'READY' || detail.session.status === 'IN_PROGRESS') {
            void this.enterSession(sessionId)
          }
        })
        .catch(() => undefined)
    }, 2000)
  },

  stopPolling() {
    if (pollTimer !== undefined) clearInterval(pollTimer)
    pollTimer = undefined
  },

  async cancelWaiting() {
    this.stopPolling()
    try {
      if (this.data.matching && this.data.matchTicketId) {
        await cancelRandomMatchApi(this.data.matchTicketId)
      } else if (this.data.waitingRoom && this.data.waitingSessionId) {
        await cancelGameSessionApi(this.data.waitingSessionId)
      }
    } catch (error) {
      await wx.showToast({ title: errorMessage(error), icon: 'none' })
    } finally {
      this.setData({
        matching: false,
        waitingRoom: false,
        matchTicketId: '',
        waitingSessionId: '',
        roomInviteCode: '',
        waitingText: '',
      })
    }
  },

  async copyInviteCode() {
    if (this.data.roomInviteCode) await wx.setClipboardData({ data: this.data.roomInviteCode })
  },

  async enterSession(sessionId: string) {
    this.stopPolling()
    this.setData({ matching: false, waitingRoom: false })
    await wx.redirectTo({ url: `/packages/game/pages/play/index?sessionId=${sessionId}` })
  },

  openHistory() {
    void wx.navigateTo({ url: '/packages/game/pages/sessions/index' })
  },

  openProgress() {
    void wx.navigateTo({ url: '/packages/game/pages/progress/index' })
  },
})
