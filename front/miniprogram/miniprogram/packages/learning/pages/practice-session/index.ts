import {
  completePracticeTaskApi,
  startPracticeTaskApi,
  submitPracticeAnswerApi,
} from '../../../../api/learning'
import type { AnswerFeedback, PracticeDetail, PracticeQuestion } from '../../../../types/learning'

function jsonValue(value: string | null | undefined): string {
  if (!value) return ''
  try {
    const parsed = JSON.parse(value) as { value?: unknown }
    if (parsed.value === undefined) return value
    if (typeof parsed.value === 'string') return parsed.value
    if (typeof parsed.value === 'number' || typeof parsed.value === 'boolean') {
      return String(parsed.value)
    }
    return JSON.stringify(parsed.value) ?? ''
  } catch {
    return value
  }
}

interface DisplayQuestion extends PracticeQuestion {
  savedAnswer: string
  standardAnswerText: string
}

function decorateQuestion(question: PracticeQuestion): DisplayQuestion {
  return {
    ...question,
    savedAnswer: jsonValue(question.latestAnswer?.studentAnswer),
    standardAnswerText: jsonValue(question.standardAnswer),
  }
}

Page({
  data: {
    loading: true,
    submitting: false,
    completing: false,
    taskId: '',
    detail: null as PracticeDetail | null,
    questions: [] as DisplayQuestion[],
    currentIndex: 0,
    currentQuestion: null as DisplayQuestion | null,
    answerValue: '',
    feedback: null as AnswerFeedback | null,
    feedbackAnswer: '',
    isAssignment: false,
    isCompleted: false,
    progressText: '',
    startedQuestionAt: 0,
  },

  onLoad(options: Record<string, string | undefined>) {
    const taskId = options.taskId ?? ''
    if (!taskId) {
      void wx.showToast({ title: '缺少练习任务编号', icon: 'none' })
      return
    }
    this.setData({ taskId })
    void this.loadTask(taskId)
  },

  async loadTask(taskId: string) {
    this.setData({ loading: true })
    try {
      const detail = await startPracticeTaskApi(taskId)
      const questions = detail.questions.map(decorateQuestion)
      const firstPending = questions.findIndex((item) => !item.latestAnswer)
      this.applyDetail(detail, questions, firstPending < 0 ? 0 : firstPending)
    } catch (error) {
      this.setData({ loading: false })
      await this.showError(error, '练习加载失败')
    }
  },

  applyDetail(detail: PracticeDetail, questions: DisplayQuestion[], index: number) {
    const currentIndex = Math.max(0, Math.min(index, Math.max(0, questions.length - 1)))
    const currentQuestion = questions[currentIndex] ?? null
    this.setData({
      loading: false,
      detail,
      questions,
      currentIndex,
      currentQuestion,
      answerValue: currentQuestion?.savedAnswer ?? '',
      feedback: null,
      feedbackAnswer: '',
      isAssignment: detail.task.sourceType === 'ASSIGNMENT',
      isCompleted: detail.task.status === 'COMPLETED',
      progressText: questions.length ? `${currentIndex + 1} / ${questions.length}` : '0 / 0',
      startedQuestionAt: Date.now(),
    })
  },

  onAnswerInput(event: WechatMiniprogram.Input) {
    this.setData({ answerValue: event.detail.value })
  },

  async submitAnswer() {
    const question = this.data.currentQuestion
    const answer = this.data.answerValue.trim()
    if (!question || !answer) {
      await wx.showToast({ title: '请先填写答案', icon: 'none' })
      return
    }
    this.setData({ submitting: true })
    try {
      const feedback = await submitPracticeAnswerApi(this.data.taskId, question.id, {
        answer,
        responseMs: Math.max(0, Date.now() - this.data.startedQuestionAt),
      })
      const questions = [...this.data.questions]
      questions[this.data.currentIndex] = {
        ...question,
        latestAnswer: feedback.answer,
        savedAnswer: answer,
        solutionVisible: feedback.solutionVisible,
        standardAnswer: feedback.standardAnswer,
        standardAnswerText: jsonValue(feedback.standardAnswer),
        analysis: feedback.analysis,
      }
      this.setData({
        questions,
        currentQuestion: questions[this.data.currentIndex],
        feedback,
        feedbackAnswer: jsonValue(feedback.standardAnswer),
      })
      if (!feedback.solutionVisible) {
        await wx.showToast({ title: '答案已保存，提交作业后统一反馈', icon: 'none' })
      }
    } catch (error) {
      await this.showError(error, '答案提交失败')
    } finally {
      this.setData({ submitting: false })
    }
  },

  retryQuestion() {
    this.setData({
      answerValue: '',
      feedback: null,
      feedbackAnswer: '',
      startedQuestionAt: Date.now(),
    })
  },

  previousQuestion() {
    this.switchQuestion(this.data.currentIndex - 1)
  },

  nextQuestion() {
    this.switchQuestion(this.data.currentIndex + 1)
  },

  switchQuestion(index: number) {
    const question = this.data.questions[index]
    if (!question) return
    this.setData({
      currentIndex: index,
      currentQuestion: question,
      answerValue: question.savedAnswer,
      feedback: null,
      feedbackAnswer: question.standardAnswerText,
      progressText: `${index + 1} / ${this.data.questions.length}`,
      startedQuestionAt: Date.now(),
    })
  },

  async completePractice() {
    const unanswered = this.data.questions.filter((item) => !item.latestAnswer).length
    if (unanswered) {
      await wx.showToast({ title: `还有 ${unanswered} 道题未作答`, icon: 'none' })
      const index = this.data.questions.findIndex((item) => !item.latestAnswer)
      if (index >= 0) this.switchQuestion(index)
      return
    }
    const confirmation = await wx.showModal({
      title: this.data.isAssignment ? '提交作业' : '完成练习',
      content: this.data.isAssignment
        ? '提交后将统一判题，且不能再修改答案。'
        : '确定结束本次练习吗？',
    })
    if (!confirmation.confirm) return
    this.setData({ completing: true })
    try {
      const detail = await completePracticeTaskApi(this.data.taskId)
      const questions = detail.questions.map(decorateQuestion)
      this.applyDetail(detail, questions, this.data.currentIndex)
      await wx.showModal({
        title: '本次练习已完成',
        content: '作答结果已计入错题、掌握度和个性化推荐。',
        showCancel: false,
        confirmText: '查看结果',
      })
    } catch (error) {
      await this.showError(error, '练习提交失败')
    } finally {
      this.setData({ completing: false })
    }
  },

  async showError(error: unknown, fallback: string) {
    await wx.showToast({ title: error instanceof Error ? error.message : fallback, icon: 'none' })
  },
})
