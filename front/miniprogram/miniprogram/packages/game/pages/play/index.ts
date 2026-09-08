import {
  finishGameSessionApi,
  getSessionApi,
  startGameSessionApi,
  submitGameAnswerApi,
} from '../../../../api/game'
import { getSession } from '../../../../services/session'
import type { SessionDetail, SessionMember, SessionQuestion } from '../../../../types/game'

let refreshTimer: ReturnType<typeof setInterval> | undefined
let countdownTimer: ReturnType<typeof setInterval> | undefined
let questionStartedAt = Date.now()
let finishing = false

function errorMessage(error: unknown): string {
  return error instanceof Error ? error.message : '对局操作失败'
}

function resultLabel(member: SessionMember | undefined): string {
  if (!member?.result || member.result === 'PENDING') return '等待结算'
  return (
    { WIN: '胜利', LOSE: '惜败', DRAW: '平局', CANCELLED: '已取消' }[member.result] ?? member.result
  )
}

Page({
  data: {
    sessionId: '',
    loading: true,
    submitting: false,
    detail: null as SessionDetail | null,
    currentQuestion: null as SessionQuestion | null,
    currentIndex: 0,
    answerValue: '',
    remainingSeconds: 0,
    myMember: null as SessionMember | null,
    opponentMember: null as SessionMember | null,
    myProgress: 0,
    opponentProgress: 0,
    completed: false,
    resultText: '',
    feedbackText: '',
    feedbackCorrect: false,
  },

  onLoad(options: Record<string, string | undefined>) {
    const sessionId = options.sessionId ?? ''
    if (!sessionId) {
      void wx.showToast({ title: '缺少对局编号', icon: 'none' })
      return
    }
    this.setData({ sessionId })
    void this.initialize()
  },

  onUnload() {
    this.stopTimers()
  },

  async initialize() {
    this.setData({ loading: true })
    try {
      let detail = await getSessionApi(this.data.sessionId)
      if (detail.session.status === 'READY') detail = await startGameSessionApi(this.data.sessionId)
      this.applyDetail(detail)
      if (detail.session.status === 'IN_PROGRESS') this.startTimers()
    } catch (error) {
      await wx.showModal({ title: '无法进入对局', content: errorMessage(error), showCancel: false })
    } finally {
      this.setData({ loading: false })
    }
  },

  applyDetail(detail: SessionDetail) {
    const userId = getSession()?.userId
    const myMember = detail.members.find((item) => item.studentId === userId) ?? null
    const opponentMember = detail.members.find((item) => item.studentId !== userId) ?? null
    const currentIndex = Math.min(myMember?.answeredCount ?? 0, detail.myQuestions.length - 1)
    const currentQuestion =
      detail.myQuestions.find((item) => item.gradingStatus !== 'GRADED') ?? null
    const questionCount = Math.max(1, detail.session.questionCountSnapshot)
    const completed = detail.session.status === 'COMPLETED' || detail.session.status === 'CANCELLED'
    this.setData({
      detail,
      myMember,
      opponentMember,
      currentQuestion,
      currentIndex: currentQuestion ? currentQuestion.sequenceNo - 1 : currentIndex,
      myProgress: Math.round(((myMember?.answeredCount ?? 0) / questionCount) * 100),
      opponentProgress: Math.round(((opponentMember?.answeredCount ?? 0) / questionCount) * 100),
      completed,
      resultText: completed ? resultLabel(myMember ?? undefined) : '',
    })
    if (currentQuestion) questionStartedAt = Date.now()
    if (completed) this.stopTimers()
    this.updateCountdown()
  },

  startTimers() {
    this.stopTimers()
    refreshTimer = setInterval(() => void this.refreshSession(), 1500)
    countdownTimer = setInterval(() => this.updateCountdown(), 1000)
  },

  stopTimers() {
    if (refreshTimer !== undefined) clearInterval(refreshTimer)
    if (countdownTimer !== undefined) clearInterval(countdownTimer)
    refreshTimer = undefined
    countdownTimer = undefined
  },

  async refreshSession() {
    try {
      this.applyDetail(await getSessionApi(this.data.sessionId))
    } catch {
      // 临时网络波动不打断作答，下一轮自动恢复。
    }
  },

  updateCountdown() {
    const expiresAt = this.data.detail?.session.expiresAt
    if (!expiresAt || this.data.completed) return
    const remainingSeconds = Math.max(
      0,
      Math.ceil((new Date(expiresAt).getTime() - Date.now()) / 1000),
    )
    this.setData({ remainingSeconds })
    if (remainingSeconds === 0 && !finishing) void this.finishExpiredSession()
  },

  onAnswerInput(event: WechatMiniprogram.Input) {
    this.setData({ answerValue: event.detail.value })
  },

  async submitAnswer() {
    const question = this.data.currentQuestion
    const answer = this.data.answerValue.trim()
    if (!question || !answer || this.data.submitting || this.data.completed) {
      if (!answer) await wx.showToast({ title: '请输入答案', icon: 'none' })
      return
    }
    this.setData({ submitting: true, feedbackText: '' })
    try {
      const feedback = await submitGameAnswerApi(this.data.sessionId, question.id, {
        requestId: `${Date.now()}-${Math.random().toString(36).slice(2, 12)}`,
        answer,
        responseMs: Math.max(0, Date.now() - questionStartedAt),
      })
      this.setData({
        answerValue: '',
        feedbackCorrect: Boolean(feedback.correct),
        feedbackText: feedback.accepted
          ? feedback.correct
            ? `回答正确，当前连击 ${feedback.combo}`
            : '答案不正确，结果已进入错题与巩固记录'
          : '本题未被接收，对局可能已经超时',
      })
      await this.refreshSession()
    } catch (error) {
      await wx.showToast({ title: errorMessage(error), icon: 'none' })
    } finally {
      this.setData({ submitting: false })
    }
  },

  async finishExpiredSession() {
    finishing = true
    try {
      this.applyDetail(await finishGameSessionApi(this.data.sessionId))
    } catch {
      await this.refreshSession()
    } finally {
      finishing = false
    }
  },

  openHistory() {
    void wx.redirectTo({ url: '/packages/game/pages/sessions/index' })
  },

  playAgain() {
    void wx.redirectTo({ url: '/packages/game/pages/levels/index' })
  },
})
