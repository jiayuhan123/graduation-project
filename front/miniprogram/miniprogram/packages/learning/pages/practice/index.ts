import { getCurrentUserApi } from '../../../../api/account'
import { listKnowledgePointsApi, pageContentModulesApi } from '../../../../api/content'
import { createPracticeTaskApi } from '../../../../api/learning'
import { STORAGE_KEYS } from '../../../../constants/storage'
import { getStorage, setStorage } from '../../../../services/storage'
import type { ContentModule, KnowledgePoint } from '../../../../types/content'

interface KnowledgePointCard extends KnowledgePoint {
  indent: number
}

interface PracticePreference {
  gradeLevel: number
  semester: number
  moduleId: string
  knowledgePointId: string
  difficulty: number
  questionCount: number
  mode: 'NORMAL' | 'TIMED'
}

function defaultPracticeMode(): 'NORMAL' | 'TIMED' {
  return 'NORMAL'
}

function orderKnowledgePoints(points: KnowledgePoint[]): KnowledgePointCard[] {
  const children = new Map<string | null, KnowledgePoint[]>()
  points.forEach((point) => {
    const siblings = children.get(point.parentId) ?? []
    siblings.push(point)
    children.set(point.parentId, siblings)
  })
  children.forEach((siblings) =>
    siblings.sort(
      (left, right) => left.sortOrder - right.sortOrder || left.code.localeCompare(right.code),
    ),
  )

  const ordered: KnowledgePointCard[] = []
  const visited = new Set<string>()
  const append = (parentId: string | null, level: number) => {
    for (const point of children.get(parentId) ?? []) {
      if (visited.has(point.id)) continue
      visited.add(point.id)
      ordered.push({ ...point, indent: Math.min(level, 3) * 28 })
      append(point.id, level + 1)
    }
  }
  append(null, 0)
  points.forEach((point) => {
    if (!visited.has(point.id)) ordered.push({ ...point, indent: 0 })
  })
  return ordered
}

Page({
  data: {
    loading: true,
    loadingPoints: false,
    gradeLevel: 1,
    semester: 1,
    modules: [] as ContentModule[],
    knowledgePoints: [] as KnowledgePointCard[],
    selectedModuleId: '',
    selectedKnowledgePointId: '',
    difficulty: 3,
    difficultyOptions: [1, 2, 3, 4, 5],
    questionCount: 10,
    questionCountOptions: [5, 10, 15, 20],
    mode: defaultPracticeMode(),
    submitting: false,
  },

  onLoad() {
    void this.loadCatalog()
  },

  async loadCatalog() {
    this.setData({ loading: true })
    try {
      const profile = await getCurrentUserApi()
      if (!profile.studentProfile) throw new Error('请先完善学生年级和学期信息')

      const { gradeLevel, semester } = profile.studentProfile
      const preference = getStorage<PracticePreference>(STORAGE_KEYS.practicePreference)
      const page = await pageContentModulesApi({ page: 1, size: 100, gradeLevel, semester })
      const preferredModuleId =
        preference?.gradeLevel === gradeLevel &&
        preference.semester === semester &&
        page.records.some((module) => module.id === preference.moduleId)
          ? preference.moduleId
          : (page.records[0]?.id ?? '')

      this.setData({
        loading: false,
        gradeLevel,
        semester,
        modules: page.records,
        selectedModuleId: preferredModuleId,
        selectedKnowledgePointId: '',
        difficulty: preference?.difficulty ?? 3,
        questionCount: preference?.questionCount ?? 10,
        mode: preference?.mode ?? 'NORMAL',
      })
      if (preferredModuleId) await this.loadKnowledgePoints(preferredModuleId, preference)
    } catch (error) {
      this.setData({ loading: false })
      await wx.showToast({
        title: error instanceof Error ? error.message : '内容目录加载失败',
        icon: 'none',
      })
    }
  },

  async loadKnowledgePoints(moduleId: string, preference?: PracticePreference | null) {
    this.setData({ loadingPoints: true, knowledgePoints: [], selectedKnowledgePointId: '' })
    try {
      const points = orderKnowledgePoints(await listKnowledgePointsApi({ moduleId }))
      const preferredPointId =
        preference?.moduleId === moduleId &&
        points.some((point) => point.id === preference.knowledgePointId)
          ? preference.knowledgePointId
          : ''
      this.setData({
        loadingPoints: false,
        knowledgePoints: points,
        selectedKnowledgePointId: preferredPointId,
      })
    } catch (error) {
      this.setData({ loadingPoints: false })
      await wx.showToast({
        title: error instanceof Error ? error.message : '知识点加载失败',
        icon: 'none',
      })
    }
  },

  selectModule(event: WechatMiniprogram.BaseEvent) {
    const moduleId = String(event.currentTarget.dataset.id ?? '')
    if (!moduleId || moduleId === this.data.selectedModuleId) return
    this.setData({ selectedModuleId: moduleId })
    void this.loadKnowledgePoints(moduleId)
  },

  selectKnowledgePoint(event: WechatMiniprogram.BaseEvent) {
    const knowledgePointId = String(event.currentTarget.dataset.id ?? '')
    if (knowledgePointId) this.setData({ selectedKnowledgePointId: knowledgePointId })
  },

  selectDifficulty(event: WechatMiniprogram.BaseEvent) {
    const difficulty = Number(event.currentTarget.dataset.value)
    if (difficulty >= 1 && difficulty <= 5) this.setData({ difficulty })
  },

  selectQuestionCount(event: WechatMiniprogram.BaseEvent) {
    const questionCount = Number(event.currentTarget.dataset.value)
    if ([5, 10, 15, 20].includes(questionCount)) this.setData({ questionCount })
  },

  selectMode(event: WechatMiniprogram.BaseEvent) {
    const mode = String(event.currentTarget.dataset.value)
    if (mode === 'NORMAL' || mode === 'TIMED') this.setData({ mode })
  },

  async startPractice() {
    const {
      gradeLevel,
      semester,
      selectedModuleId,
      selectedKnowledgePointId,
      difficulty,
      questionCount,
      mode,
    } = this.data
    if (!selectedModuleId || !selectedKnowledgePointId) {
      await wx.showToast({ title: '请先选择一个知识点', icon: 'none' })
      return
    }

    setStorage<PracticePreference>(STORAGE_KEYS.practicePreference, {
      gradeLevel,
      semester,
      moduleId: selectedModuleId,
      knowledgePointId: selectedKnowledgePointId,
      difficulty,
      questionCount,
      mode,
    })
    this.setData({ submitting: true })
    try {
      const detail = await createPracticeTaskApi({
        knowledgePointId: selectedKnowledgePointId,
        difficulty,
        questionCount,
        mode,
        timeLimitSeconds: mode === 'TIMED' ? Math.max(300, questionCount * 60) : undefined,
        targetAccuracy: 0.8,
      })
      await wx.navigateTo({
        url: `/packages/learning/pages/practice-session/index?taskId=${detail.task.id}`,
      })
    } catch (error) {
      await wx.showToast({
        title: error instanceof Error ? error.message : '练习创建失败',
        icon: 'none',
      })
    } finally {
      this.setData({ submitting: false })
    }
  },
})
