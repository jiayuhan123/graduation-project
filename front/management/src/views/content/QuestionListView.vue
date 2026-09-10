<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import {
  Button as TButton,
  Loading as TLoading,
  MessagePlugin,
  Tag as TTag,
} from 'tdesign-vue-next'
import {
  createQuestionApi,
  deleteQuestionApi,
  getQuestionApi,
  listKnowledgePointsApi,
  pageContentModulesApi,
  pageQuestionsApi,
  reviewQuestionApi,
  updateQuestionApi,
} from '@/api/content'
import ContentEditorPanel from '@/components/ContentEditorPanel.vue'
import PageHeader from '@/components/PageHeader.vue'
import type {
  ContentModule,
  KnowledgePoint,
  QuestionInput,
  QuestionMode,
  QuestionStatus,
  QuestionSummary,
  QuestionType,
} from '@/types/content'

interface QuestionForm {
  code: string
  questionMode: QuestionMode
  questionType: QuestionType
  stem: string
  options: string
  standardAnswer: string
  analysis: string
  generationRule: string
  difficulty: number
  estimatedSeconds: number
  selectedPointIds: string[]
  primaryPointId: string
  weights: Record<string, number>
  version: number
}

const questionTypeLabels: Record<QuestionType, string> = {
  ARITHMETIC: '口算',
  FILL_BLANK: '填空',
  VERTICAL: '竖式',
  CHOICE: '选择',
  JUDGEMENT: '判断',
}
const statusLabels: Record<QuestionStatus, string> = {
  DRAFT: '草稿',
  ENABLED: '已发布',
  DISABLED: '已下架',
}

const loading = ref(true)
const saving = ref(false)
const questions = ref<QuestionSummary[]>([])
const modules = ref<ContentModule[]>([])
const points = ref<KnowledgePoint[]>([])
const total = ref(0)
const pages = ref(0)
const editorVisible = ref(false)
const editingId = ref<string | null>(null)
const pointModuleFilter = ref('')
const query = reactive({
  page: 1,
  size: 12,
  keyword: '',
  knowledgePointId: '',
  questionType: '',
  status: '',
})
const form = reactive<QuestionForm>(emptyForm())
const visiblePoints = computed(() =>
  pointModuleFilter.value
    ? points.value.filter((point) => point.moduleId === pointModuleFilter.value)
    : points.value,
)

function emptyForm(): QuestionForm {
  return {
    code: '',
    questionMode: 'FIXED',
    questionType: 'ARITHMETIC',
    stem: '',
    options: '',
    standardAnswer: '{"value":""}',
    analysis: '',
    generationRule: '',
    difficulty: 1,
    estimatedSeconds: 20,
    selectedPointIds: [],
    primaryPointId: '',
    weights: {},
    version: 0,
  }
}

async function loadQuestions() {
  loading.value = true
  try {
    const result = await pageQuestionsApi({
      page: query.page,
      size: query.size,
      keyword: query.keyword.trim() || undefined,
      knowledgePointId: query.knowledgePointId || undefined,
      questionType: (query.questionType || undefined) as QuestionType | undefined,
      status: (query.status || undefined) as QuestionStatus | undefined,
    })
    questions.value = result.records
    total.value = result.total
    pages.value = result.pages
    query.page = result.page
  } finally {
    loading.value = false
  }
}

async function loadCatalog() {
  const [modulePage, knowledgePoints] = await Promise.all([
    pageContentModulesApi({ page: 1, size: 100 }),
    listKnowledgePointsApi(),
  ])
  modules.value = modulePage.records
  points.value = knowledgePoints
}

function search() {
  query.page = 1
  void loadQuestions()
}

function openCreate() {
  editingId.value = null
  pointModuleFilter.value = modules.value[0]?.id || ''
  Object.assign(form, emptyForm())
  editorVisible.value = true
}

async function openEdit(question: QuestionSummary) {
  const detail = await getQuestionApi(question.id)
  editingId.value = detail.id
  const weights = Object.fromEntries(
    detail.knowledgePoints.map((binding) => [binding.knowledgePointId, binding.weight]),
  )
  Object.assign(form, {
    code: detail.code,
    questionMode: detail.questionMode,
    questionType: detail.questionType,
    stem: detail.stem,
    options: detail.options ?? '',
    standardAnswer: detail.standardAnswer,
    analysis: detail.analysis ?? '',
    generationRule: detail.generationRule ?? '',
    difficulty: detail.difficulty,
    estimatedSeconds: detail.estimatedSeconds ?? 20,
    selectedPointIds: detail.knowledgePoints.map((binding) => binding.knowledgePointId),
    primaryPointId:
      detail.knowledgePoints.find((binding) => binding.primary)?.knowledgePointId ?? '',
    weights,
    version: detail.version,
  })
  pointModuleFilter.value =
    points.value.find((point) => point.id === form.primaryPointId)?.moduleId || ''
  editorVisible.value = true
}

function pointSelected(pointId: string): boolean {
  return form.selectedPointIds.includes(pointId)
}

function togglePoint(pointId: string, checked: boolean) {
  if (checked && !pointSelected(pointId)) {
    form.selectedPointIds = [...form.selectedPointIds, pointId]
    form.weights[pointId] = 1
    if (!form.primaryPointId) form.primaryPointId = pointId
    return
  }
  if (!checked) {
    form.selectedPointIds = form.selectedPointIds.filter((id) => id !== pointId)
    delete form.weights[pointId]
    if (form.primaryPointId === pointId) form.primaryPointId = form.selectedPointIds[0] || ''
  }
}

async function save() {
  if (!form.selectedPointIds.length || !form.primaryPointId) {
    void MessagePlugin.warning('请至少绑定一个知识点，并指定主知识点')
    return
  }
  saving.value = true
  try {
    const payload: QuestionInput = {
      questionMode: form.questionMode,
      questionType: form.questionType,
      stem: form.stem.trim(),
      options: form.options.trim() || undefined,
      standardAnswer: form.standardAnswer.trim(),
      analysis: form.analysis.trim() || undefined,
      generationRule: form.generationRule.trim() || undefined,
      difficulty: form.difficulty,
      estimatedSeconds: form.estimatedSeconds || undefined,
      knowledgePoints: form.selectedPointIds.map((knowledgePointId) => ({
        knowledgePointId,
        primary: knowledgePointId === form.primaryPointId,
        weight: form.weights[knowledgePointId] ?? 1,
      })),
    }
    if (editingId.value) {
      payload.version = form.version
      await updateQuestionApi(editingId.value, payload)
    } else {
      payload.code = form.code.trim().toUpperCase()
      await createQuestionApi(payload)
    }
    editorVisible.value = false
    void MessagePlugin.success(editingId.value ? '题目已更新并回到草稿' : '题目草稿已创建')
    await loadQuestions()
  } finally {
    saving.value = false
  }
}

async function disable(question: QuestionSummary) {
  await reviewQuestionApi(question.id, { decision: 'DISABLE', version: question.version })
  void MessagePlugin.success('题目已下架')
  await loadQuestions()
}

async function remove(question: QuestionSummary) {
  if (!window.confirm(`确定删除题目“${question.code}”吗？`)) return
  await deleteQuestionApi(question.id, question.version)
  void MessagePlugin.success('题目已删除')
  await loadQuestions()
}

function changePage(delta: number) {
  const next = query.page + delta
  if (next < 1 || next > pages.value) return
  query.page = next
  void loadQuestions()
}

onMounted(async () => {
  await Promise.all([loadCatalog(), loadQuestions()])
})
</script>

<template>
  <div>
    <PageHeader
      eyebrow="教学内容"
      title="题库管理"
      :description="`固定题与模板题统一维护，当前共 ${total} 道题。`"
    >
      <template #actions
        ><t-button :disabled="points.length === 0" @click="openCreate">新建题目</t-button></template
      >
    </PageHeader>

    <section class="surface-card">
      <div class="content-toolbar">
        <input v-model="query.keyword" placeholder="搜索题目编码或题干" @keyup.enter="search" />
        <select v-model="query.knowledgePointId" @change="search">
          <option value="">全部知识点</option>
          <option v-for="point in points" :key="point.id" :value="point.id">
            {{ point.name }}
          </option>
        </select>
        <select v-model="query.questionType" @change="search">
          <option value="">全部题型</option>
          <option v-for="(label, code) in questionTypeLabels" :key="code" :value="code">
            {{ label }}
          </option>
        </select>
        <select v-model="query.status" @change="search">
          <option value="">全部状态</option>
          <option v-for="(label, code) in statusLabels" :key="code" :value="code">
            {{ label }}
          </option>
        </select>
        <t-button variant="outline" @click="search">查询</t-button>
      </div>

      <div v-if="loading" class="content-loading"><t-loading text="正在加载题库" /></div>
      <div v-else-if="questions.length" class="content-list">
        <article v-for="question in questions" :key="question.id" class="content-row question-row">
          <div class="content-row__main">
            <strong>{{ question.stem }}</strong>
            <span
              >{{ question.code }} ·
              {{ question.questionMode === 'TEMPLATE' ? '模板题' : '固定题' }}</span
            >
          </div>
          <div class="content-row__meta">
            {{ questionTypeLabels[question.questionType] }} · 难度 {{ question.difficulty }}
            <template v-if="question.estimatedSeconds">
              · {{ question.estimatedSeconds }} 秒</template
            >
          </div>
          <div class="content-row__actions">
            <t-tag
              :theme="
                question.status === 'ENABLED'
                  ? 'success'
                  : question.status === 'DRAFT'
                    ? 'warning'
                    : 'default'
              "
              variant="light"
            >
              {{ statusLabels[question.status] }}
            </t-tag>
            <t-button size="small" variant="outline" @click="openEdit(question)">编辑</t-button>
            <t-button
              v-if="question.status === 'ENABLED'"
              size="small"
              variant="text"
              @click="disable(question)"
              >下架</t-button
            >
            <t-button size="small" theme="danger" variant="text" @click="remove(question)"
              >删除</t-button
            >
          </div>
        </article>
      </div>
      <div v-else class="content-empty">
        {{ points.length ? '暂无符合条件的题目。' : '请先创建知识点，再维护题目。' }}
      </div>

      <footer class="content-pagination">
        <span>第 {{ query.page }} / {{ Math.max(pages, 1) }} 页</span>
        <div>
          <t-button
            size="small"
            variant="outline"
            :disabled="query.page <= 1"
            @click="changePage(-1)"
            >上一页</t-button
          >
          <t-button
            size="small"
            variant="outline"
            :disabled="query.page >= pages"
            @click="changePage(1)"
            >下一页</t-button
          >
        </div>
      </footer>
    </section>

    <ContentEditorPanel
      v-if="editorVisible"
      :title="editingId ? '编辑题目' : '新建题目'"
      description="保存修改后统一回到草稿，需在内容审核页发布。"
      :busy="saving"
      @close="editorVisible = false"
      @submit="save"
    >
      <div class="editor-grid">
        <label class="editor-field">
          <span>稳定编码</span>
          <input
            v-model="form.code"
            required
            maxlength="64"
            :disabled="Boolean(editingId)"
            placeholder="Q-G1-ADD-0001"
          />
        </label>
        <label class="editor-field">
          <span>题目模式</span>
          <select v-model="form.questionMode">
            <option value="FIXED">固定题</option>
            <option value="TEMPLATE">模板题</option>
          </select>
        </label>
        <label class="editor-field">
          <span>题型</span>
          <select v-model="form.questionType">
            <option v-for="(label, code) in questionTypeLabels" :key="code" :value="code">
              {{ label }}
            </option>
          </select>
        </label>
        <label class="editor-field"
          ><span>难度</span
          ><input v-model.number="form.difficulty" type="number" min="1" max="5" required
        /></label>
        <label class="editor-field"
          ><span>预计作答秒数</span
          ><input v-model.number="form.estimatedSeconds" type="number" min="1"
        /></label>
        <label class="editor-field editor-field--wide"
          ><span>题干</span><textarea v-model="form.stem" required></textarea>
        </label>
        <label v-if="form.questionType === 'CHOICE'" class="editor-field editor-field--wide"
          ><span>选项 JSON</span
          ><textarea v-model="form.options" required placeholder='["A","B","C","D"]'></textarea>
        </label>
        <label class="editor-field editor-field--wide"
          ><span>标准答案 JSON</span><textarea v-model="form.standardAnswer" required></textarea>
        </label>
        <label v-if="form.questionMode === 'TEMPLATE'" class="editor-field editor-field--wide"
          ><span>生成规则 JSON</span
          ><textarea
            v-model="form.generationRule"
            required
            placeholder='{"left":{"min":1,"max":10}}'
          ></textarea>
        </label>
        <label class="editor-field editor-field--wide"
          ><span>解析</span><textarea v-model="form.analysis"></textarea>
        </label>

        <div class="editor-field editor-field--wide">
          <span>绑定知识点（单选圆点表示主知识点）</span>
          <select v-model="pointModuleFilter">
            <option value="">全部模块</option>
            <option v-for="module in modules" :key="module.id" :value="module.id">
              {{ module.name }}
            </option>
          </select>
          <div class="point-picker">
            <label v-for="point in visiblePoints" :key="point.id" class="point-option">
              <input
                type="checkbox"
                :checked="pointSelected(point.id)"
                @change="togglePoint(point.id, ($event.target as HTMLInputElement).checked)"
              />
              <input
                v-model="form.primaryPointId"
                type="radio"
                name="primary-point"
                :value="point.id"
                :disabled="!pointSelected(point.id)"
              />
              <span>{{ point.name }}</span>
              <input
                v-if="pointSelected(point.id)"
                v-model.number="form.weights[point.id]"
                class="weight-input"
                type="number"
                min="0.0001"
                max="1"
                step="0.1"
                aria-label="知识点权重"
              />
            </label>
          </div>
        </div>
      </div>
    </ContentEditorPanel>
  </div>
</template>

<style scoped lang="scss">
.question-row {
  grid-template-columns: minmax(260px, 1.2fr) minmax(180px, 0.7fr) auto;
}

.point-picker {
  display: grid;
  max-height: 240px;
  padding: 10px;
  overflow-y: auto;
  background: #f8f9fb;
  border-radius: 12px;
  gap: 7px;
}

.point-option {
  display: grid;
  min-height: 38px;
  padding: 7px 9px;
  align-items: center;
  background: #fff;
  border-radius: 9px;
  grid-template-columns: auto auto 1fr auto;
  gap: 9px;
}

.weight-input {
  width: 74px !important;
  min-height: 30px !important;
  padding: 4px 7px !important;
}

@media (width <= 900px) {
  .question-row {
    grid-template-columns: 1fr auto;
  }
}
</style>
