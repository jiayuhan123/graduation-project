import {
  changeAssignmentStatusApi,
  createAssignmentApi,
  getAssignmentApi,
  getStudentAssignmentApi,
  getTeachingClassApi,
  pageCreatedAssignmentsApi,
  pageQuestionCandidatesApi,
  pageStudentAssignmentsApi,
  pageTeachingClassesApi,
  publishAssignmentApi,
} from '../../../../api/teaching'
import { getSession } from '../../../../services/session'
import type {
  AssignmentDetail,
  AssignmentSummary,
  ClassMembership,
  ClassStudent,
  QuestionCandidate,
  StudentAssignment,
} from '../../../../types/teaching'

interface QuestionCard extends QuestionCandidate {
  selected: boolean
  layerCode: string
  score: number
}

interface PublishStudent extends ClassStudent {
  layerIndex: number
}

function defaultAssignmentType(): 'UNIFIED' | 'LAYERED' {
  return 'UNIFIED'
}

function dateAfter(days: number): string {
  const date = new Date(Date.now() + days * 86_400_000)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

Page({
  data: {
    loading: true,
    submitting: false,
    isTeacher: false,
    assignments: [] as AssignmentSummary[],
    studentAssignments: [] as StudentAssignment[],
    classes: [] as ClassMembership[],
    candidates: [] as QuestionCard[],
    detail: null as AssignmentDetail | null,
    showCreate: false,
    classIndex: 0,
    title: '',
    description: '',
    assignmentType: defaultAssignmentType(),
    assignmentTypeOptions: ['统一作业', '分层作业'],
    startDate: dateAfter(0),
    startTime: '08:00',
    dueDate: dateAfter(1),
    dueTime: '20:00',
    selectedQuestionIds: [] as string[],
    publishTargetId: '',
    publishTargetVersion: 0,
    layerOptions: [] as string[],
    publishStudents: [] as PublishStudent[],
  },

  onShow() {
    const isTeacher = getSession()?.activeRole === 'teacher'
    this.setData({ isTeacher, detail: null, showCreate: false })
    void this.load()
  },

  onPullDownRefresh() {
    void this.load().finally(() => wx.stopPullDownRefresh())
  },

  async load() {
    this.setData({ loading: true })
    try {
      if (this.data.isTeacher) {
        const [assignmentPage, classPage, questionPage] = await Promise.all([
          pageCreatedAssignmentsApi({ page: 1, size: 100 }),
          pageTeachingClassesApi({ page: 1, size: 100 }),
          pageQuestionCandidatesApi({ page: 1, size: 100 }),
        ])
        this.setData({
          loading: false,
          assignments: assignmentPage.records,
          studentAssignments: [],
          classes: classPage.records.filter((item) => item.classStatus === 'ACTIVE'),
          candidates: questionPage.records.map((item) => ({
            ...item,
            selected: false,
            layerCode: 'ALL',
            score: 10,
          })),
        })
      } else {
        const page = await pageStudentAssignmentsApi({ page: 1, size: 100 })
        this.setData({ loading: false, assignments: [], studentAssignments: page.records })
      }
    } catch (error) {
      this.setData({ loading: false })
      await this.showError(error, '作业加载失败')
    }
  },

  toggleCreate() {
    this.setData({ showCreate: !this.data.showCreate, detail: null })
  },

  onInput(event: WechatMiniprogram.Input<WechatMiniprogram.IAnyObject, { field: string }>) {
    const field = event.currentTarget.dataset.field
    if (field) this.setData({ [field]: event.detail.value })
  },

  selectClass(event: WechatMiniprogram.PickerChange) {
    this.setData({ classIndex: Number(event.detail.value) })
  },
  selectAssignmentType(event: WechatMiniprogram.PickerChange) {
    const assignmentType = Number(event.detail.value) === 1 ? 'LAYERED' : 'UNIFIED'
    this.setData({
      assignmentType,
      candidates: this.data.candidates.map((item) => ({
        ...item,
        layerCode:
          assignmentType === 'UNIFIED' ? 'ALL' : item.layerCode === 'ALL' ? 'A' : item.layerCode,
      })),
    })
  },
  selectDate(
    event: WechatMiniprogram.PickerChange<WechatMiniprogram.IAnyObject, { field: string }>,
  ) {
    const field = event.currentTarget.dataset.field
    if (field) this.setData({ [field]: String(event.detail.value) })
  },

  toggleQuestion(event: WechatMiniprogram.BaseEvent) {
    const id = String(event.currentTarget.dataset.id ?? '')
    const selected = this.data.selectedQuestionIds
    const selectedQuestionIds = selected.includes(id)
      ? selected.filter((item) => item !== id)
      : [...selected, id]
    this.setData({
      selectedQuestionIds,
      candidates: this.data.candidates.map((item) =>
        item.id === id
          ? {
              ...item,
              selected: !item.selected,
              layerCode: this.data.assignmentType === 'UNIFIED' ? 'ALL' : item.layerCode,
            }
          : item,
      ),
    })
  },

  onQuestionInput(
    event: WechatMiniprogram.Input<WechatMiniprogram.IAnyObject, { id: string; field: string }>,
  ) {
    const id = event.currentTarget.dataset.id
    const field = event.currentTarget.dataset.field
    if (!id || (field !== 'layerCode' && field !== 'score')) return
    const value = field === 'score' ? Number(event.detail.value) : event.detail.value.toUpperCase()
    this.setData({
      candidates: this.data.candidates.map((item) =>
        item.id === id ? { ...item, [field]: value } : item,
      ),
    })
  },

  async createAssignment() {
    const selectedClass = this.data.classes[this.data.classIndex]
    const selectedQuestions = this.data.candidates.filter((item) => item.selected)
    if (!selectedClass || !this.data.title.trim() || !selectedQuestions.length) {
      await wx.showToast({ title: '请填写标题并至少选择一道题', icon: 'none' })
      return
    }
    const startAt = `${this.data.startDate}T${this.data.startTime}:00+08:00`
    const dueAt = `${this.data.dueDate}T${this.data.dueTime}:00+08:00`
    if (new Date(dueAt) <= new Date(startAt)) {
      await wx.showToast({ title: '截止时间必须晚于开始时间', icon: 'none' })
      return
    }
    const layers = [
      ...new Set(
        selectedQuestions.map((item) =>
          this.data.assignmentType === 'UNIFIED' ? 'ALL' : item.layerCode.trim().toUpperCase(),
        ),
      ),
    ]
    if (
      this.data.assignmentType === 'LAYERED' &&
      (layers.length < 2 || layers.includes('ALL') || layers.includes(''))
    ) {
      await wx.showToast({ title: '分层作业至少需要两个非 ALL 层级', icon: 'none' })
      return
    }
    const totals = layers.map((layer) =>
      selectedQuestions
        .filter(
          (item) =>
            (this.data.assignmentType === 'UNIFIED'
              ? 'ALL'
              : item.layerCode.trim().toUpperCase()) === layer,
        )
        .reduce((sum, item) => sum + item.score, 0),
    )
    if (
      selectedQuestions.some((item) => !Number.isFinite(item.score) || item.score <= 0) ||
      totals.some((value) => Math.abs(value - (totals[0] ?? 0)) > 0.001)
    ) {
      await wx.showToast({ title: '题目分值需大于 0，且各层总分相同', icon: 'none' })
      return
    }
    const layerSequences: Record<string, number> = {}
    this.setData({ submitting: true })
    try {
      await createAssignmentApi({
        classId: selectedClass.classId,
        title: this.data.title.trim(),
        description: this.data.description.trim() || undefined,
        assignmentType: this.data.assignmentType,
        startAt,
        dueAt,
        questions: selectedQuestions.map((question) => {
          const layerCode =
            this.data.assignmentType === 'UNIFIED' ? 'ALL' : question.layerCode.trim().toUpperCase()
          layerSequences[layerCode] = (layerSequences[layerCode] ?? 0) + 1
          return {
            questionId: question.id,
            layerCode,
            sequenceNo: layerSequences[layerCode],
            score: question.score,
          }
        }),
      })
      this.setData({
        showCreate: false,
        title: '',
        description: '',
        assignmentType: 'UNIFIED',
        selectedQuestionIds: [],
        candidates: this.data.candidates.map((item) => ({
          ...item,
          selected: false,
          layerCode: 'ALL',
          score: 10,
        })),
      })
      await wx.showToast({ title: '草稿已创建', icon: 'success' })
      await this.load()
    } catch (error) {
      await this.showError(error, '创建作业失败')
    } finally {
      this.setData({ submitting: false })
    }
  },

  async openDetail(event: WechatMiniprogram.BaseEvent) {
    const id = String(event.currentTarget.dataset.id ?? '')
    if (!id) return
    try {
      this.setData({
        detail: this.data.isTeacher
          ? await getAssignmentApi(id)
          : await getStudentAssignmentApi(id),
        showCreate: false,
      })
    } catch (error) {
      await this.showError(error, '作业详情加载失败')
    }
  },

  closeDetail() {
    this.setData({ detail: null })
  },

  async publish(event: WechatMiniprogram.BaseEvent) {
    const id = String(event.currentTarget.dataset.id ?? '')
    const version = Number(event.currentTarget.dataset.version)
    const assignmentType = String(event.currentTarget.dataset.type)
    try {
      if (assignmentType === 'LAYERED') {
        const assignment = this.data.assignments.find((item) => item.id === id)
        if (!assignment) return
        const [detail, classDetail] = await Promise.all([
          getAssignmentApi(id),
          getTeachingClassApi(assignment.classId),
        ])
        const layerOptions = [...new Set(detail.questions.map((item) => item.layerCode))].sort()
        const publishStudents = classDetail.students
          .filter((item) => item.status === 'ACTIVE')
          .map((item) => ({ ...item, layerIndex: 0 }))
        this.setData({
          publishTargetId: id,
          publishTargetVersion: version,
          layerOptions,
          publishStudents,
        })
        return
      }
      await publishAssignmentApi(id, version)
      await wx.showToast({ title: '发布成功', icon: 'success' })
      await this.load()
    } catch (error) {
      await this.showError(error, '发布失败')
    }
  },

  selectStudentLayer(
    event: WechatMiniprogram.PickerChange<WechatMiniprogram.IAnyObject, { index: number }>,
  ) {
    const index = Number(event.currentTarget.dataset.index)
    const publishStudents = [...this.data.publishStudents]
    const student = publishStudents[index]
    if (!student) return
    publishStudents[index] = { ...student, layerIndex: Number(event.detail.value) }
    this.setData({ publishStudents })
  },

  cancelLayerPublish() {
    this.setData({
      publishTargetId: '',
      publishTargetVersion: 0,
      layerOptions: [],
      publishStudents: [],
    })
  },

  async publishLayered() {
    if (!this.data.publishTargetId || !this.data.publishStudents.length) {
      await wx.showToast({ title: '班级暂无在班学生，不能发布', icon: 'none' })
      return
    }
    try {
      await publishAssignmentApi(
        this.data.publishTargetId,
        this.data.publishTargetVersion,
        this.data.publishStudents.map((student) => ({
          studentId: student.studentId,
          layerCode: this.data.layerOptions[student.layerIndex] ?? this.data.layerOptions[0] ?? 'A',
        })),
      )
      this.cancelLayerPublish()
      await wx.showToast({ title: '分层作业已发布', icon: 'success' })
      await this.load()
    } catch (error) {
      await this.showError(error, '发布失败')
    }
  },

  async changeStatus(event: WechatMiniprogram.BaseEvent) {
    const id = String(event.currentTarget.dataset.id ?? '')
    const version = Number(event.currentTarget.dataset.version)
    const action = String(event.currentTarget.dataset.action) as 'CLOSE' | 'CANCEL'
    const confirm = await wx.showModal({
      title: action === 'CLOSE' ? '结束作业' : '取消作业',
      content: '该操作会立即影响学生端状态，确定继续吗？',
    })
    if (!confirm.confirm) return
    try {
      await changeAssignmentStatusApi(id, action, version)
      await wx.showToast({ title: '操作成功', icon: 'success' })
      await this.load()
    } catch (error) {
      await this.showError(error, '操作失败')
    }
  },

  async showError(error: unknown, fallback: string) {
    await wx.showToast({ title: error instanceof Error ? error.message : fallback, icon: 'none' })
  },
})
