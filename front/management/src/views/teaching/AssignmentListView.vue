<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import {
  Button as TButton,
  Loading as TLoading,
  MessagePlugin,
  Tag as TTag,
} from 'tdesign-vue-next'
import {
  changeAssignmentStatusApi,
  createAssignmentApi,
  getStudentAssignmentApi,
  getTeacherAssignmentApi,
  getTeacherClassApi,
  pageQuestionCandidatesApi,
  pageStudentAssignmentsApi,
  pageTeacherAssignmentsApi,
  pageTeacherClassesApi,
  publishAssignmentApi,
  updateAssignmentApi,
} from '@/api/teaching'
import ContentEditorPanel from '@/components/ContentEditorPanel.vue'
import PageHeader from '@/components/PageHeader.vue'
import { useUserStore } from '@/stores/user'
import type {
  AssignmentDetail,
  AssignmentQuestionInput,
  AssignmentStatus,
  AssignmentSummary,
  AssignmentType,
  ClassMembership,
  ClassStudent,
  QuestionCandidate,
  StudentAssignment,
} from '@/types/teaching'

interface SelectedQuestion extends AssignmentQuestionInput {
  candidate: QuestionCandidate
}
interface AssignmentForm {
  classId: string
  title: string
  description: string
  assignmentType: AssignmentType
  startAt: string
  dueAt: string
  settings: string
  version?: number
}

const userStore = useUserStore()
const isTeacher = computed(() => userStore.activeRole === 'teacher')
const loading = ref(true)
const saving = ref(false)
const assignments = ref<AssignmentSummary[]>([])
const studentAssignments = ref<StudentAssignment[]>([])
const classes = ref<ClassMembership[]>([])
const candidates = ref<QuestionCandidate[]>([])
const selectedQuestions = ref<SelectedQuestion[]>([])
const total = ref(0)
const pages = ref(0)
const page = ref(1)
const status = ref('')
const candidateKeyword = ref('')
const editorVisible = ref(false)
const detailVisible = ref(false)
const publishVisible = ref(false)
const editingId = ref<string | null>(null)
const detail = ref<AssignmentDetail | null>(null)
const publishTarget = ref<AssignmentSummary | null>(null)
const publishStudents = ref<ClassStudent[]>([])
const studentLayers = reactive<Record<string, string>>({})
const statusLabels: Record<AssignmentStatus, string> = {
  DRAFT: '草稿',
  PUBLISHED: '已发布',
  CLOSED: '已结束',
  CANCELLED: '已取消',
}

function localTime(offsetMinutes: number): string {
  const date = new Date(Date.now() + offsetMinutes * 60_000)
  const local = new Date(date.getTime() - date.getTimezoneOffset() * 60_000)
  return local.toISOString().slice(0, 16)
}
function toLocal(value: string): string {
  const date = new Date(value)
  return new Date(date.getTime() - date.getTimezoneOffset() * 60_000).toISOString().slice(0, 16)
}
function toOffset(value: string): string {
  return new Date(value).toISOString()
}

const form = reactive<AssignmentForm>({
  classId: '',
  title: '',
  description: '',
  assignmentType: 'UNIFIED',
  startAt: localTime(0),
  dueAt: localTime(24 * 60),
  settings: '',
})
const layerCodes = computed(() =>
  [...new Set(selectedQuestions.value.map((item) => item.layerCode))].sort(),
)
const layerTotals = computed(() =>
  Object.fromEntries(
    layerCodes.value.map((layer) => [
      layer,
      selectedQuestions.value
        .filter((item) => item.layerCode === layer)
        .reduce((sum, item) => sum + Number(item.score), 0),
    ]),
  ),
)

async function loadAssignments() {
  loading.value = true
  try {
    if (isTeacher.value) {
      const result = await pageTeacherAssignmentsApi({
        page: page.value,
        size: 12,
        status: (status.value || undefined) as AssignmentStatus | undefined,
      })
      assignments.value = result.records
      studentAssignments.value = []
      total.value = result.total
      pages.value = result.pages
      page.value = result.page
    } else {
      const result = await pageStudentAssignmentsApi({
        page: page.value,
        size: 12,
        status: (status.value || undefined) as never,
      })
      studentAssignments.value = result.records
      assignments.value = []
      total.value = result.total
      pages.value = result.pages
      page.value = result.page
    }
  } finally {
    loading.value = false
  }
}

async function loadTeacherData() {
  if (!isTeacher.value) return
  const [classPage, questionPage] = await Promise.all([
    pageTeacherClassesApi({ page: 1, size: 100 }),
    pageQuestionCandidatesApi({ page: 1, size: 100 }),
  ])
  classes.value = classPage.records.filter((item) => item.classStatus === 'ACTIVE')
  candidates.value = questionPage.records
}

function resetForm() {
  Object.assign(form, {
    classId: classes.value[0]?.classId ?? '',
    title: '',
    description: '',
    assignmentType: 'UNIFIED',
    startAt: localTime(0),
    dueAt: localTime(24 * 60),
    settings: '',
    version: undefined,
  })
  selectedQuestions.value = []
}
function openCreate() {
  editingId.value = null
  resetForm()
  editorVisible.value = true
}

async function openEdit(item: AssignmentSummary) {
  const result = await getTeacherAssignmentApi(item.id)
  editingId.value = item.id
  Object.assign(form, {
    classId: item.classId,
    title: item.title,
    description: item.description ?? '',
    assignmentType: item.assignmentType,
    startAt: toLocal(item.startAt),
    dueAt: toLocal(item.dueAt),
    settings: item.settings ?? '',
    version: item.version,
  })
  selectedQuestions.value = result.questions.map((question) => ({
    questionId: question.questionId,
    layerCode: question.layerCode,
    sequenceNo: question.sequenceNo,
    score: question.score,
    candidate: {
      id: question.questionId,
      code: question.questionCode,
      questionType: question.questionType,
      stem: question.stem,
      options: question.options,
      standardAnswer: question.standardAnswer ?? '',
      analysis: question.analysis,
      difficulty: question.difficulty,
      estimatedSeconds: null,
    },
  }))
  editorVisible.value = true
}

function selected(questionId: string) {
  return selectedQuestions.value.some((item) => item.questionId === questionId)
}
function toggleQuestion(candidate: QuestionCandidate, checked: boolean) {
  if (checked && !selected(candidate.id))
    selectedQuestions.value.push({
      questionId: candidate.id,
      layerCode: form.assignmentType === 'UNIFIED' ? 'ALL' : 'A',
      sequenceNo: selectedQuestions.value.length + 1,
      score: 10,
      candidate,
    })
  if (!checked)
    selectedQuestions.value = selectedQuestions.value
      .filter((item) => item.questionId !== candidate.id)
      .map((item, index) => ({ ...item, sequenceNo: index + 1 }))
}
function findSelected(questionId: string) {
  return selectedQuestions.value.find((item) => item.questionId === questionId)
}
function normalizeLayers() {
  selectedQuestions.value.forEach((item) => {
    const normalized = item.layerCode.trim().toUpperCase()
    if (form.assignmentType === 'UNIFIED') item.layerCode = 'ALL'
    else item.layerCode = !normalized || normalized === 'ALL' ? 'A' : normalized
  })
}

async function searchCandidates() {
  const result = await pageQuestionCandidatesApi({
    page: 1,
    size: 100,
    keyword: candidateKeyword.value.trim() || undefined,
  })
  const selectedMap = new Map(selectedQuestions.value.map((item) => [item.questionId, item]))
  candidates.value = result.records
  selectedQuestions.value.forEach((item) => {
    if (!result.records.some((candidate) => candidate.id === item.questionId))
      candidates.value.push(item.candidate)
  })
  candidates.value.forEach((candidate) => {
    const current = selectedMap.get(candidate.id)
    if (current) current.candidate = candidate
  })
}

async function saveAssignment() {
  if (!selectedQuestions.value.length) {
    void MessagePlugin.warning('请至少选择一道题')
    return
  }
  normalizeLayers()
  if (form.assignmentType === 'LAYERED' && layerCodes.value.length < 2) {
    void MessagePlugin.warning('分层作业至少需要 A、B 两个层级')
    return
  }
  const totals = Object.values(layerTotals.value)
  if (totals.some((value) => Math.abs(value - (totals[0] ?? 0)) > 0.001)) {
    void MessagePlugin.warning('分层作业各层总分必须相同')
    return
  }
  const normalizedQuestions = layerCodes.value.flatMap((layerCode) =>
    selectedQuestions.value
      .filter((item) => item.layerCode === layerCode)
      .sort((left, right) => left.sequenceNo - right.sequenceNo)
      .map((item, index) => ({ ...item, sequenceNo: index + 1 })),
  )
  saving.value = true
  try {
    const payload = {
      classId: form.classId,
      title: form.title.trim(),
      description: form.description.trim() || undefined,
      assignmentType: form.assignmentType,
      startAt: toOffset(form.startAt),
      dueAt: toOffset(form.dueAt),
      settings: form.settings.trim() || undefined,
      questions: normalizedQuestions.map(({ questionId, layerCode, sequenceNo, score }) => ({
        questionId,
        layerCode: layerCode.trim().toUpperCase(),
        sequenceNo,
        score,
      })),
      version: form.version,
    }
    if (editingId.value) await updateAssignmentApi(editingId.value, payload)
    else await createAssignmentApi(payload)
    editorVisible.value = false
    void MessagePlugin.success(editingId.value ? '作业草稿已更新' : '作业草稿已创建')
    await loadAssignments()
  } finally {
    saving.value = false
  }
}

async function openDetail(item: AssignmentSummary) {
  detail.value = isTeacher.value
    ? await getTeacherAssignmentApi(item.id)
    : await getStudentAssignmentApi(item.id)
  detailVisible.value = true
}

async function preparePublish(item: AssignmentSummary) {
  publishTarget.value = item
  if (item.assignmentType === 'UNIFIED') {
    await publishAssignmentApi(item.id, item.version, [])
    void MessagePlugin.success('作业已发布')
    await loadAssignments()
    return
  }
  const [assignmentDetail, classDetail] = await Promise.all([
    getTeacherAssignmentApi(item.id),
    getTeacherClassApi(item.classId),
  ])
  const layers = [
    ...new Set(assignmentDetail.questions.map((question) => question.layerCode)),
  ].sort()
  publishStudents.value = classDetail.students.filter((student) => student.status === 'ACTIVE')
  Object.keys(studentLayers).forEach((key) => delete studentLayers[key])
  publishStudents.value.forEach((student) => {
    studentLayers[student.studentId] = layers[0] ?? 'A'
  })
  detail.value = assignmentDetail
  publishVisible.value = true
}

async function publishLayered() {
  if (!publishTarget.value) return
  await publishAssignmentApi(
    publishTarget.value.id,
    publishTarget.value.version,
    publishStudents.value.map((student) => ({
      studentId: student.studentId,
      layerCode: studentLayers[student.studentId] ?? 'A',
    })),
  )
  publishVisible.value = false
  void MessagePlugin.success('分层作业已发布')
  await loadAssignments()
}

async function changeStatus(item: AssignmentSummary, action: 'CLOSE' | 'CANCEL') {
  if (!window.confirm(`确定${action === 'CLOSE' ? '结束' : '取消'}“${item.title}”吗？`)) return
  await changeAssignmentStatusApi(item.id, action, item.version)
  void MessagePlugin.success(action === 'CLOSE' ? '作业已结束' : '作业已取消')
  await loadAssignments()
}
function changePage(delta: number) {
  const next = page.value + delta
  if (next < 1 || next > pages.value) return
  page.value = next
  void loadAssignments()
}

function filterAssignments() {
  page.value = 1
  void loadAssignments()
}

watch(() => form.assignmentType, normalizeLayers)
watch(
  () => userStore.activeRole,
  async () => {
    page.value = 1
    status.value = ''
    await Promise.all([loadAssignments(), loadTeacherData()])
  },
)
onMounted(() => Promise.all([loadAssignments(), loadTeacherData()]))
</script>

<template>
  <div>
    <PageHeader
      eyebrow="TEACHING"
      :title="isTeacher ? '作业管理' : '我的作业'"
      :description="
        isTeacher
          ? `创建统一或分层作业，当前共 ${total} 份。`
          : '查看老师发布给你的作业与完成状态。'
      "
    >
      <template v-if="isTeacher" #actions
        ><t-button :disabled="!classes.length || !candidates.length" @click="openCreate"
          >创建作业</t-button
        ></template
      >
    </PageHeader>
    <section class="surface-card">
      <div class="content-toolbar assignment-toolbar">
        <select v-model="status" @change="filterAssignments">
          <option value="">全部状态</option>
          <template v-if="isTeacher"
            ><option v-for="(label, code) in statusLabels" :key="code" :value="code">
              {{ label }}
            </option></template
          ><template v-else
            ><option value="ASSIGNED">待开始</option>
            <option value="IN_PROGRESS">进行中</option>
            <option value="SUBMITTED">已提交</option>
            <option value="GRADED">已批改</option>
            <option value="OVERDUE">已逾期</option></template
          >
        </select>
        <span class="muted">发布后题目快照和学生名单固定，保证作业过程可追溯。</span>
      </div>
      <div v-if="loading" class="content-loading"><t-loading text="正在加载作业" /></div>
      <div v-else-if="isTeacher && assignments.length" class="content-list">
        <article v-for="item in assignments" :key="item.id" class="content-row assignment-row">
          <div class="content-row__main">
            <strong>{{ item.title }}</strong
            ><span
              >{{
                classes.find((entry) => entry.classId === item.classId)?.name ||
                `班级 ${item.classId}`
              }}
              · {{ item.assignmentType === 'LAYERED' ? '分层作业' : '统一作业' }}</span
            >
          </div>
          <div class="content-row__meta">
            {{ new Date(item.startAt).toLocaleString() }} —
            {{ new Date(item.dueAt).toLocaleString() }} · {{ item.totalScore }} 分
          </div>
          <div class="content-row__actions">
            <t-tag
              :theme="
                item.status === 'PUBLISHED'
                  ? 'success'
                  : item.status === 'DRAFT'
                    ? 'warning'
                    : 'default'
              "
              variant="light"
              >{{ statusLabels[item.status] }}</t-tag
            ><t-button size="small" variant="outline" @click="openDetail(item)">查看</t-button
            ><t-button
              v-if="item.status === 'DRAFT'"
              size="small"
              variant="text"
              @click="openEdit(item)"
              >编辑</t-button
            ><t-button v-if="item.status === 'DRAFT'" size="small" @click="preparePublish(item)"
              >发布</t-button
            ><t-button
              v-if="item.status === 'PUBLISHED'"
              size="small"
              variant="text"
              @click="changeStatus(item, 'CLOSE')"
              >结束</t-button
            ><t-button
              v-if="item.status === 'DRAFT' || item.status === 'PUBLISHED'"
              size="small"
              theme="danger"
              variant="text"
              @click="changeStatus(item, 'CANCEL')"
              >取消</t-button
            >
          </div>
        </article>
      </div>
      <div v-else-if="!isTeacher && studentAssignments.length" class="content-list">
        <article
          v-for="item in studentAssignments"
          :key="item.assignment.id"
          class="content-row assignment-row"
        >
          <div class="content-row__main">
            <strong>{{ item.assignment.title }}</strong
            ><span
              >{{
                item.assignment.assignmentType === 'LAYERED'
                  ? `${item.submission.layerCode} 层作业`
                  : '统一作业'
              }}
              · {{ item.assignment.totalScore }} 分</span
            >
          </div>
          <div class="content-row__meta">
            截止 {{ new Date(item.assignment.dueAt).toLocaleString() }}
          </div>
          <div class="content-row__actions">
            <t-tag
              :theme="item.submission.status === 'GRADED' ? 'success' : 'warning'"
              variant="light"
              >{{ item.submission.status }}</t-tag
            ><t-button size="small" variant="outline" @click="openDetail(item.assignment)"
              >查看题目</t-button
            >
          </div>
        </article>
      </div>
      <div v-else class="content-empty">
        {{
          isTeacher ? '暂无作业；如不能创建，请先准备启用题目和有效班级。' : '老师还没有发布作业。'
        }}
      </div>
      <footer class="content-pagination">
        <span>第 {{ page }} / {{ Math.max(pages, 1) }} 页</span>
        <div>
          <t-button size="small" variant="outline" :disabled="page <= 1" @click="changePage(-1)"
            >上一页</t-button
          ><t-button size="small" variant="outline" :disabled="page >= pages" @click="changePage(1)"
            >下一页</t-button
          >
        </div>
      </footer>
    </section>

    <ContentEditorPanel
      v-if="editorVisible"
      :title="editingId ? '编辑作业草稿' : '创建作业草稿'"
      description="统一作业所有题目使用 ALL 层；分层作业至少设置两个层级且各层总分相同。"
      :busy="saving"
      @close="editorVisible = false"
      @submit="saveAssignment"
    >
      <div class="editor-grid">
        <label class="editor-field"
          ><span>班级</span
          ><select v-model="form.classId" required :disabled="Boolean(editingId)">
            <option v-for="item in classes" :key="item.classId" :value="item.classId">
              {{ item.name }}
            </option>
          </select></label
        >
        <label class="editor-field"
          ><span>作业类型</span
          ><select v-model="form.assignmentType">
            <option value="UNIFIED">统一作业</option>
            <option value="LAYERED">分层作业</option>
          </select></label
        >
        <label class="editor-field editor-field--wide"
          ><span>标题</span><input v-model="form.title" required maxlength="128"
        /></label>
        <label class="editor-field"
          ><span>开始时间</span
          ><input v-model="form.startAt" type="datetime-local" required /></label
        ><label class="editor-field"
          ><span>截止时间</span><input v-model="form.dueAt" type="datetime-local" required
        /></label>
        <label class="editor-field editor-field--wide"
          ><span>作业说明</span><textarea v-model="form.description" maxlength="1000"></textarea>
        </label>
        <div class="editor-field editor-field--wide question-picker">
          <span>从已启用题库选题</span>
          <div class="candidate-search">
            <input
              v-model="candidateKeyword"
              placeholder="搜索题目编码或题干"
              @keyup.enter.prevent="searchCandidates"
            /><t-button type="button" size="small" variant="outline" @click="searchCandidates"
              >查询</t-button
            >
          </div>
          <div class="candidate-list">
            <label v-for="candidate in candidates" :key="candidate.id" class="candidate-row"
              ><input
                type="checkbox"
                :checked="selected(candidate.id)"
                @change="
                  toggleQuestion(candidate, ($event.target as HTMLInputElement).checked)
                " /><span
                ><strong>{{ candidate.stem }}</strong
                ><small>{{ candidate.code }} · 难度 {{ candidate.difficulty }}</small></span
              ><template v-if="findSelected(candidate.id)"
                ><input
                  v-model="findSelected(candidate.id)!.layerCode"
                  class="short-input"
                  maxlength="32"
                  :disabled="form.assignmentType === 'UNIFIED'"
                  aria-label="层级" /><input
                  v-model.number="findSelected(candidate.id)!.score"
                  class="short-input"
                  type="number"
                  min="0.01"
                  step="0.5"
                  aria-label="分值" /></template
            ></label>
          </div>
          <p v-if="form.assignmentType === 'LAYERED' && layerCodes.length" class="layer-summary">
            层级总分：<span v-for="layer in layerCodes" :key="layer"
              >{{ layer }} = {{ layerTotals[layer] }} 分</span
            >
          </p>
        </div>
      </div>
    </ContentEditorPanel>

    <ContentEditorPanel
      v-if="detailVisible && detail"
      :title="detail.assignment.title"
      :description="`${detail.assignment.totalScore} 分 · ${detail.questions.length} 道题`"
      submit-text="关闭"
      @close="detailVisible = false"
      @submit="detailVisible = false"
      ><div class="detail-questions">
        <article v-for="question in detail.questions" :key="question.id">
          <div>
            <span
              >{{ question.layerCode }} · 第 {{ question.sequenceNo }} 题 ·
              {{ question.score }} 分</span
            ><strong>{{ question.stem }}</strong>
          </div>
          <small>{{ question.questionCode }} · 难度 {{ question.difficulty }}</small>
          <p v-if="question.standardAnswer">答案：{{ question.standardAnswer }}</p>
          <p v-if="question.analysis">解析：{{ question.analysis }}</p>
        </article>
      </div></ContentEditorPanel
    >

    <ContentEditorPanel
      v-if="publishVisible && publishTarget && detail"
      title="分配学生层级"
      description="发布后学生名单与层级固定；请为每名在班学生选择层级。"
      submit-text="确认发布"
      @close="publishVisible = false"
      @submit="publishLayered"
      ><div class="layer-students">
        <label v-for="student in publishStudents" :key="student.studentId"
          ><span
            >{{ student.displayName || student.username || student.studentId
            }}<small>{{ student.studentNoInClass || '暂无班内学号' }}</small></span
          ><select v-model="studentLayers[student.studentId]">
            <option
              v-for="layer in [...new Set(detail.questions.map((question) => question.layerCode))]"
              :key="layer"
              :value="layer"
            >
              {{ layer }} 层
            </option>
          </select></label
        >
      </div></ContentEditorPanel
    >
  </div>
</template>

<style scoped lang="scss">
.assignment-toolbar {
  grid-template-columns: 180px 1fr;
}

.assignment-row {
  grid-template-columns: minmax(220px, 0.8fr) minmax(260px, 1fr) auto;
}

.question-picker {
  min-height: 300px;
}

.candidate-search {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 8px;
}

.candidate-list {
  display: grid;
  max-height: 360px;
  overflow-y: auto;
  gap: 7px;
}

.candidate-row {
  display: grid;
  padding: 10px;
  align-items: center;
  background: #f8f9fb;
  border-radius: 11px;
  grid-template-columns: auto minmax(0, 1fr) 72px 72px;
  gap: 8px;
}

.candidate-row span,
.candidate-row strong,
.candidate-row small {
  display: block;
  min-width: 0;
}

.candidate-row strong {
  overflow: hidden;
  color: var(--app-text-strong);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.candidate-row small {
  margin-top: 3px;
  color: var(--app-text-muted);
}

.short-input {
  min-width: 0;
  padding: 6px !important;
}

.layer-summary {
  display: flex;
  padding: 9px;
  background: #fff6ef;
  border-radius: 9px;
  color: #a7521b;
  flex-wrap: wrap;
  gap: 10px;
  font-size: 12px;
}

.detail-questions,
.layer-students {
  display: grid;
  gap: 9px;
}

.detail-questions article {
  padding: 14px;
  background: #f8f9fb;
  border-radius: 12px;
}

.detail-questions span,
.detail-questions strong,
.detail-questions small,
.detail-questions p {
  display: block;
}

.detail-questions span,
.detail-questions small,
.detail-questions p {
  color: var(--app-text-muted);
  font-size: 12px;
}

.detail-questions strong {
  margin-top: 5px;
  color: var(--app-text-strong);
}

.detail-questions p {
  margin-top: 8px;
  white-space: pre-wrap;
}

.layer-students label {
  display: grid;
  padding: 11px;
  align-items: center;
  background: #f8f9fb;
  border-radius: 11px;
  grid-template-columns: 1fr 120px;
  gap: 10px;
}

.layer-students small {
  display: block;
  margin-top: 3px;
  color: var(--app-text-muted);
}

.layer-students select {
  min-height: 38px;
  padding: 7px;
  border: 1px solid #dfe3e7;
  border-radius: 9px;
}

@media (width <= 900px) {
  .assignment-row {
    grid-template-columns: 1fr auto;
  }
}

@media (width <= 640px) {
  .assignment-toolbar,
  .candidate-row {
    grid-template-columns: 1fr;
  }

  .candidate-row > input[type='checkbox'] {
    justify-self: start;
  }
}
</style>
