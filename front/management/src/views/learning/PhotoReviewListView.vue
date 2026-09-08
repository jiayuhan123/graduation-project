<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import {
  Button as TButton,
  Loading as TLoading,
  MessagePlugin,
  Tag as TTag,
} from 'tdesign-vue-next'
import { createQuestionFeedbackApi, pagePublishedQuestionsApi } from '@/api/content'
import {
  batchApprovePhotoReviewApi,
  confirmPhotoItemApi,
  downloadPhotoReviewFileApi,
  getPhotoReviewWorkbenchDetailApi,
  getPhotoTaskApi,
  pagePhotoReviewWorkbenchApi,
  pagePhotoTasksApi,
  retryPhotoRecognitionApi,
  reviewPhotoWorkbenchItemApi,
} from '@/api/learning'
import {
  getAdminClassApi,
  getTeacherClassApi,
  pageAdminClassesApi,
  pageTeacherClassesApi,
} from '@/api/teaching'
import PageHeader from '@/components/PageHeader.vue'
import { useUserStore } from '@/stores/user'
import type { PublishedQuestion, QuestionFeedbackCategory } from '@/types/content'
import type {
  PhotoDetail,
  PhotoItem,
  PhotoReviewDetail,
  PhotoReviewItem,
  PhotoReviewTask,
  PhotoTask,
  ReviewPhotoItemInput,
} from '@/types/learning'
import type { ClassMembership, ClassStudent } from '@/types/teaching'

const userStore = useUserStore()
const isStudent = computed(() => userStore.activeRole === 'student')
const loading = ref(true)
const detailLoading = ref(false)
const operating = ref(false)
const studentTasks = ref<PhotoTask[]>([])
const studentDetail = ref<PhotoDetail | null>(null)
const records = ref<PhotoReviewTask[]>([])
const detail = ref<PhotoReviewDetail | null>(null)
const classes = ref<ClassMembership[]>([])
const students = ref<ClassStudent[]>([])
const total = ref(0)
const pages = ref(0)
const selectedTaskIds = ref<string[]>([])
const selectedItemIndex = ref(0)
const editingItem = ref<PhotoItem | null>(null)
const questions = ref<PublishedQuestion[]>([])
const questionKeyword = ref('')
const originalImageUrl = ref('')
const cropImageUrl = ref('')

const query = reactive({
  page: 1,
  size: 12,
  classId: '',
  studentId: '',
  status: '',
  reviewStatus: '',
  submittedFrom: '',
  submittedTo: '',
})
const form = reactive({ answer: '', stem: '', questionId: '', comment: '' })
const feedback = reactive<{ category: QuestionFeedbackCategory; description: string }>({
  category: 'ANSWER_ERROR',
  description: '',
})

const currentReviewItem = computed<PhotoReviewItem | null>(
  () => detail.value?.items[selectedItemIndex.value] ?? null,
)
const filteredQuestions = computed(() => {
  const keyword = questionKeyword.value.trim().toLowerCase()
  if (!keyword) return questions.value
  return questions.value.filter((item) =>
    `${item.code} ${item.stem} ${item.id}`.toLowerCase().includes(keyword),
  )
})

const statusLabels: Record<string, string> = {
  UPLOADED: '等待识别',
  PROCESSING: '识别中',
  REVIEW_REQUIRED: '待复核',
  COMPLETED: '已完成',
  FAILED: '识别失败',
  PENDING: '待处理',
  REVIEWED: '已复核',
  APPROVED: '已通过',
  CORRECTED: '已修正',
  NOT_REQUIRED: '无需复核',
  CONFIRMED: '已确认',
  REJECTED: '已忽略',
}

function answerText(value: string | null): string {
  if (!value) return ''
  try {
    const parsed = JSON.parse(value) as { value?: unknown }
    return typeof parsed.value === 'string' ? parsed.value : value
  } catch {
    return value
  }
}

function confidenceText(value: number | null): string {
  return value === null ? '--' : `${Math.round(value * 100)}%`
}

function formatTime(value: string | null): string {
  return value ? new Date(value).toLocaleString() : '--'
}

function asOffset(value: string): string | undefined {
  return value ? new Date(value).toISOString() : undefined
}

function revokeImageUrls() {
  if (originalImageUrl.value) URL.revokeObjectURL(originalImageUrl.value)
  if (cropImageUrl.value) URL.revokeObjectURL(cropImageUrl.value)
  originalImageUrl.value = ''
  cropImageUrl.value = ''
}

async function loadStudentTasks() {
  const page = await pagePhotoTasksApi({ page: 1, size: 100 })
  studentTasks.value = page.records
  const first = page.records.find((item) => item.status === 'REVIEW_REQUIRED') ?? page.records[0]
  if (first) await openStudentTask(first)
}

async function openStudentTask(task: PhotoTask) {
  detailLoading.value = true
  editingItem.value = null
  try {
    studentDetail.value = await getPhotoTaskApi(task.id)
  } finally {
    detailLoading.value = false
  }
}

async function loadClasses() {
  if (isStudent.value) return
  const result =
    userStore.activeRole === 'admin'
      ? await pageAdminClassesApi({ page: 1, size: 100 })
      : await pageTeacherClassesApi({ page: 1, size: 100 })
  classes.value = result.records.filter((item) => item.classStatus === 'ACTIVE')
  if (userStore.activeRole === 'teacher' && !query.classId) {
    query.classId = classes.value[0]?.classId ?? ''
  }
  await loadClassStudents()
}

async function loadClassStudents() {
  students.value = []
  if (!query.classId) return
  const classDetail =
    userStore.activeRole === 'admin'
      ? await getAdminClassApi(query.classId)
      : await getTeacherClassApi(query.classId)
  students.value = classDetail.students.filter((item) => item.status === 'ACTIVE')
  if (query.studentId && !students.value.some((item) => item.studentId === query.studentId)) {
    query.studentId = ''
  }
}

function workbenchParams() {
  return {
    page: query.page,
    size: query.size,
    classId: query.classId || undefined,
    studentId: query.studentId || undefined,
    status: query.status || undefined,
    reviewStatus: query.reviewStatus || undefined,
    submittedFrom: asOffset(query.submittedFrom),
    submittedTo: asOffset(query.submittedTo),
  }
}

async function refreshWorkbenchList() {
  const result = await pagePhotoReviewWorkbenchApi(workbenchParams())
  records.value = result.records
  total.value = result.total
  pages.value = result.pages
  query.page = result.page
}

async function loadWorkbench() {
  loading.value = true
  detail.value = null
  selectedTaskIds.value = []
  revokeImageUrls()
  try {
    await refreshWorkbenchList()
    if (records.value[0]) await openWorkbenchTask(records.value[0])
  } finally {
    loading.value = false
  }
}

async function loadImages() {
  revokeImageUrls()
  if (!detail.value) return
  const ownerId = detail.value.summary.studentId
  const cropId = currentReviewItem.value?.item.cropFileId
  const [original, crop] = await Promise.all([
    downloadPhotoReviewFileApi(ownerId, detail.value.summary.task.originalFileId),
    cropId ? downloadPhotoReviewFileApi(ownerId, cropId) : Promise.resolve(null),
  ])
  originalImageUrl.value = URL.createObjectURL(original)
  cropImageUrl.value = crop ? URL.createObjectURL(crop) : ''
}

async function openWorkbenchTask(task: PhotoReviewTask) {
  detailLoading.value = true
  editingItem.value = null
  selectedItemIndex.value = 0
  try {
    detail.value = await getPhotoReviewWorkbenchDetailApi(task.task.id)
    await loadImages()
  } finally {
    detailLoading.value = false
  }
}

async function selectItem(index: number) {
  if (!detail.value || index < 0 || index >= detail.value.items.length) return
  selectedItemIndex.value = index
  editingItem.value = null
  await loadImages()
}

function beginEdit(item: PhotoItem) {
  if (item.status !== 'REVIEW_REQUIRED') return
  editingItem.value = item
  form.answer = answerText(item.recognizedAnswer)
  form.stem = item.recognizedStem ?? ''
  form.questionId = item.matchedQuestionId ?? ''
  form.comment = ''
}

function decisionPayload(decision: ReviewPhotoItemInput['decision']): ReviewPhotoItemInput {
  return {
    decision,
    recognizedStem: decision === 'CORRECT' ? form.stem.trim() || undefined : undefined,
    recognizedAnswer: decision === 'CORRECT' ? form.answer.trim() : undefined,
    matchedQuestionId: decision === 'CORRECT' && form.questionId ? form.questionId : undefined,
    comment: form.comment.trim() || undefined,
  }
}

async function submitStudentDecision(decision: ReviewPhotoItemInput['decision']) {
  if (!studentDetail.value || !editingItem.value || operating.value) return
  if (decision === 'CORRECT' && !form.answer.trim()) {
    void MessagePlugin.warning('请填写修正后的学生答案')
    return
  }
  operating.value = true
  try {
    studentDetail.value = await confirmPhotoItemApi(
      studentDetail.value.task.id,
      editingItem.value.id,
      decisionPayload(decision),
    )
    editingItem.value = null
    void MessagePlugin.success('识别结果已确认并同步学习档案')
    await loadStudentTasks()
  } finally {
    operating.value = false
  }
}

async function submitWorkbenchDecision(decision: ReviewPhotoItemInput['decision']) {
  if (!detail.value || !editingItem.value || operating.value) return
  if (decision === 'CORRECT' && !form.answer.trim()) {
    void MessagePlugin.warning('请填写修正后的学生答案')
    return
  }
  operating.value = true
  try {
    detail.value = await reviewPhotoWorkbenchItemApi(
      detail.value.summary.task.id,
      editingItem.value.id,
      decisionPayload(decision),
    )
    editingItem.value = null
    void MessagePlugin.success('复核结果已保存，处理记录已留痕')
    await Promise.all([loadImages(), refreshWorkbenchList()])
  } finally {
    operating.value = false
  }
}

async function retryRecognition() {
  if (!detail.value || operating.value) return
  operating.value = true
  try {
    detail.value = await retryPhotoRecognitionApi(detail.value.summary.task.id)
    void MessagePlugin.success('任务已重新进入识别队列')
    await refreshWorkbenchList()
  } finally {
    operating.value = false
  }
}

async function batchApprove() {
  if (!selectedTaskIds.value.length || operating.value) return
  operating.value = true
  try {
    const result = await batchApprovePhotoReviewApi(selectedTaskIds.value)
    void MessagePlugin.success(
      `已通过 ${result.approvedItemCount} 个高可信结果，完成 ${result.completedTaskCount} 个任务`,
    )
    await loadWorkbench()
  } finally {
    operating.value = false
  }
}

async function submitQuestionFeedback() {
  const questionId = currentReviewItem.value?.matchedQuestion?.questionId
  if (!questionId || !feedback.description.trim() || operating.value) return
  operating.value = true
  try {
    await createQuestionFeedbackApi(questionId, {
      category: feedback.category,
      description: feedback.description.trim(),
    })
    feedback.description = ''
    void MessagePlugin.success('题目问题已提交给管理员处理')
  } finally {
    operating.value = false
  }
}

function search() {
  query.page = 1
  void loadWorkbench()
}

function changePage(delta: number) {
  const next = query.page + delta
  if (next < 1 || next > pages.value) return
  query.page = next
  void loadWorkbench()
}

function handleShortcut(event: KeyboardEvent) {
  if (isStudent.value || !detail.value) return
  const target = event.target as HTMLElement | null
  if (target?.matches('input, textarea, select')) return
  if (event.key === 'ArrowLeft') void selectItem(selectedItemIndex.value - 1)
  if (event.key === 'ArrowRight') void selectItem(selectedItemIndex.value + 1)
}

async function initialize() {
  loading.value = true
  try {
    const questionPromise = pagePublishedQuestionsApi({ page: 1, size: 100 })
    if (isStudent.value) {
      const [, questionPage] = await Promise.all([loadStudentTasks(), questionPromise])
      questions.value = questionPage.records
    } else {
      const [, questionPage] = await Promise.all([loadClasses(), questionPromise])
      questions.value = questionPage.records
      await loadWorkbench()
    }
  } finally {
    loading.value = false
  }
}

watch(
  () => userStore.activeRole,
  () => void initialize(),
)
onMounted(() => {
  window.addEventListener('keydown', handleShortcut)
  void initialize()
})
onUnmounted(() => {
  window.removeEventListener('keydown', handleShortcut)
  revokeImageUrls()
})
</script>

<template>
  <div>
    <PageHeader
      eyebrow="OCR REVIEW"
      title="OCR 复核工作台"
      :description="
        isStudent
          ? '查看识别进度，并确认系统无法可靠判断的题目。'
          : '集中筛选识别任务，对照原图、切题图和标准题目完成复核并全程留痕。'
      "
    />

    <template v-if="isStudent">
      <section class="workspace student-workspace">
        <div class="surface-card task-panel">
          <header>
            <div>
              <strong>拍照任务</strong><span>{{ studentTasks.length }} 条记录</span>
            </div>
          </header>
          <div v-if="loading" class="content-loading"><t-loading text="正在读取拍照任务" /></div>
          <div v-else class="task-list">
            <button
              v-for="task in studentTasks"
              :key="task.id"
              :class="{ active: studentDetail?.task.id === task.id }"
              type="button"
              @click="openStudentTask(task)"
            >
              <span
                ><strong>任务 #{{ task.id }}</strong
                ><small
                  >{{ formatTime(task.submittedAt) }} · {{ task.recognizedItemCount }} 题</small
                ></span
              >
              <t-tag variant="light">{{ statusLabels[task.status] ?? task.status }}</t-tag>
            </button>
          </div>
        </div>
        <div class="surface-card detail-panel">
          <div v-if="detailLoading" class="content-loading">
            <t-loading text="正在读取识别明细" />
          </div>
          <div v-else-if="studentDetail" class="item-list">
            <article
              v-for="item in studentDetail.items"
              :key="item.id"
              :class="{ review: item.status === 'REVIEW_REQUIRED' }"
            >
              <div class="item-heading">
                <strong>第 {{ item.itemNo }} 题</strong
                ><t-tag variant="light">{{ statusLabels[item.status] ?? item.status }}</t-tag>
              </div>
              <p>{{ item.recognizedStem || '未识别题干' }}</p>
              <div class="answer">
                识别答案：{{ answerText(item.recognizedAnswer) || '未识别' }}
              </div>
              <small>可信度 {{ confidenceText(item.recognitionConfidence) }}</small>
              <t-button
                v-if="item.status === 'REVIEW_REQUIRED'"
                size="small"
                variant="outline"
                @click="beginEdit(item)"
                >确认此题</t-button
              >
            </article>
          </div>
          <p v-else class="empty">暂无拍照任务。</p>
        </div>
      </section>
    </template>

    <template v-else>
      <section class="surface-card filters">
        <label
          ><span>班级</span
          ><select v-model="query.classId" @change="loadClassStudents">
            <option value="">全部班级</option>
            <option v-for="item in classes" :key="item.classId" :value="item.classId">
              {{ item.name }} · {{ item.classCode }}
            </option>
          </select></label
        >
        <label
          ><span>学生</span
          ><select v-model="query.studentId">
            <option value="">全部学生</option>
            <option v-for="item in students" :key="item.studentId" :value="item.studentId">
              {{ item.displayName || item.username || item.studentId }}
            </option>
          </select></label
        >
        <label
          ><span>识别状态</span
          ><select v-model="query.status">
            <option value="">全部</option>
            <option value="REVIEW_REQUIRED">待复核</option>
            <option value="FAILED">识别失败</option>
            <option value="PROCESSING">识别中</option>
            <option value="COMPLETED">已完成</option>
          </select></label
        >
        <label
          ><span>复核状态</span
          ><select v-model="query.reviewStatus">
            <option value="">全部</option>
            <option value="PENDING">待处理</option>
            <option value="APPROVED">已通过</option>
            <option value="CORRECTED">已修正</option>
            <option value="NOT_REQUIRED">无需复核</option>
          </select></label
        >
        <label
          ><span>提交开始</span><input v-model="query.submittedFrom" type="datetime-local"
        /></label>
        <label
          ><span>提交结束</span><input v-model="query.submittedTo" type="datetime-local"
        /></label>
        <t-button @click="search">查询</t-button>
      </section>

      <section class="batch-bar surface-card">
        <span>共 {{ total }} 个任务，已选择 {{ selectedTaskIds.length }} 个</span>
        <t-button :disabled="!selectedTaskIds.length" :loading="operating" @click="batchApprove"
          >批量通过高可信结果</t-button
        >
      </section>

      <section class="workspace">
        <div class="surface-card task-panel">
          <div v-if="loading" class="content-loading"><t-loading text="正在筛选 OCR 任务" /></div>
          <div v-else-if="records.length" class="task-list">
            <label
              v-for="task in records"
              :key="task.task.id"
              class="task-row"
              :class="{ active: detail?.summary.task.id === task.task.id }"
            >
              <input v-model="selectedTaskIds" type="checkbox" :value="task.task.id" />
              <button type="button" @click="openWorkbenchTask(task)">
                <span
                  ><strong>{{ task.studentName }}</strong
                  ><small
                    >#{{ task.task.id }} · {{ formatTime(task.task.submittedAt) }}</small
                  ></span
                >
                <span class="task-status"
                  ><t-tag
                    :theme="
                      task.task.status === 'FAILED'
                        ? 'danger'
                        : task.pendingItemCount
                          ? 'warning'
                          : 'success'
                    "
                    variant="light"
                    >{{ statusLabels[task.task.status] ?? task.task.status }}</t-tag
                  ><small>{{ task.pendingItemCount }} 题待处理</small></span
                >
              </button>
            </label>
          </div>
          <p v-else class="empty">当前筛选条件下没有任务。</p>
          <footer class="pagination">
            <span>第 {{ query.page }} / {{ Math.max(1, pages) }} 页</span>
            <div>
              <t-button
                size="small"
                variant="outline"
                :disabled="query.page <= 1"
                @click="changePage(-1)"
                >上一页</t-button
              ><t-button
                size="small"
                variant="outline"
                :disabled="query.page >= pages"
                @click="changePage(1)"
                >下一页</t-button
              >
            </div>
          </footer>
        </div>

        <div class="surface-card detail-panel workbench-detail">
          <div v-if="detailLoading" class="content-loading">
            <t-loading text="正在加载复核材料" />
          </div>
          <section v-else-if="detail && !currentReviewItem" class="failed-detail">
            <header class="detail-heading">
              <div>
                <strong
                  >{{ detail.summary.studentName }} · 任务 #{{ detail.summary.task.id }}</strong
                >
                <span>{{ detail.summary.task.failureReason || '任务暂无识别明细' }}</span>
              </div>
              <t-button
                v-if="detail.summary.task.status === 'FAILED'"
                theme="danger"
                :loading="operating"
                @click="retryRecognition"
                >重新识别</t-button
              >
            </header>
            <div class="image-frame failed-image">
              <img v-if="originalImageUrl" :src="originalImageUrl" alt="识别失败的原始图片" />
              <span v-else>原始图片加载中</span>
            </div>
          </section>
          <template v-else-if="detail && currentReviewItem">
            <header class="detail-heading">
              <div>
                <strong
                  >{{ detail.summary.studentName }} · 任务 #{{ detail.summary.task.id }}</strong
                ><span>整体可信度 {{ confidenceText(detail.summary.task.overallConfidence) }}</span>
              </div>
              <div class="heading-actions">
                <t-button
                  v-if="detail.summary.task.status === 'FAILED'"
                  theme="danger"
                  variant="outline"
                  :loading="operating"
                  @click="retryRecognition"
                  >重新识别</t-button
                ><t-tag variant="light">{{
                  statusLabels[detail.summary.task.status] ?? detail.summary.task.status
                }}</t-tag>
              </div>
            </header>

            <nav class="item-navigation">
              <t-button
                size="small"
                variant="outline"
                :disabled="selectedItemIndex === 0"
                @click="selectItem(selectedItemIndex - 1)"
                >← 上一题</t-button
              >
              <div>
                <button
                  v-for="(entry, index) in detail.items"
                  :key="entry.item.id"
                  type="button"
                  :class="{
                    active: index === selectedItemIndex,
                    pending: entry.item.status === 'REVIEW_REQUIRED',
                  }"
                  @click="selectItem(index)"
                >
                  {{ entry.item.itemNo }}
                </button>
              </div>
              <t-button
                size="small"
                variant="outline"
                :disabled="selectedItemIndex >= detail.items.length - 1"
                @click="selectItem(selectedItemIndex + 1)"
                >下一题 →</t-button
              >
            </nav>

            <div class="comparison-grid">
              <article>
                <strong>原始答题图片</strong>
                <div class="image-frame">
                  <img v-if="originalImageUrl" :src="originalImageUrl" alt="原始答题图片" /><span
                    v-else
                    >图片加载中</span
                  >
                </div>
              </article>
              <article>
                <strong>第 {{ currentReviewItem.item.itemNo }} 题切题图</strong>
                <div class="image-frame">
                  <img v-if="cropImageUrl" :src="cropImageUrl" alt="切题图片" /><span v-else
                    >暂无切题图</span
                  >
                </div>
              </article>
              <article class="recognized-card">
                <strong>OCR 识别结果</strong>
                <p>{{ currentReviewItem.item.recognizedStem || '未识别题干' }}</p>
                <div class="answer">
                  {{ answerText(currentReviewItem.item.recognizedAnswer) || '未识别答案' }}
                </div>
                <small
                  >可信度 {{ confidenceText(currentReviewItem.item.recognitionConfidence) }}</small
                >
              </article>
              <article class="standard-card">
                <strong>匹配的标准题目</strong
                ><template v-if="currentReviewItem.matchedQuestion"
                  ><b>{{ currentReviewItem.matchedQuestion.code }}</b>
                  <p>{{ currentReviewItem.matchedQuestion.stem }}</p>
                  <div class="answer">
                    标准答案：{{ currentReviewItem.matchedQuestion.standardAnswer }}
                  </div></template
                >
                <p v-else class="empty">尚未匹配题库题目。</p>
              </article>
            </div>

            <div class="review-actions">
              <t-tag
                :theme="currentReviewItem.item.status === 'REVIEW_REQUIRED' ? 'warning' : 'success'"
                variant="light"
                >{{
                  statusLabels[currentReviewItem.item.status] ?? currentReviewItem.item.status
                }}</t-tag
              ><t-button
                v-if="currentReviewItem.item.status === 'REVIEW_REQUIRED'"
                @click="beginEdit(currentReviewItem.item)"
                >复核当前题</t-button
              >
            </div>

            <section v-if="currentReviewItem.matchedQuestion" class="feedback-box">
              <strong>发现标准题目有误？</strong>
              <select v-model="feedback.category">
                <option value="STEM_ERROR">题干错误</option>
                <option value="ANSWER_ERROR">答案错误</option>
                <option value="AMBIGUOUS">表述歧义</option>
                <option value="FORMAT_ERROR">格式问题</option>
                <option value="OTHER">其他</option>
              </select>
              <input v-model="feedback.description" maxlength="1000" placeholder="说明具体问题" />
              <t-button
                size="small"
                variant="outline"
                :disabled="!feedback.description.trim()"
                @click="submitQuestionFeedback"
                >提交反馈</t-button
              >
            </section>

            <section class="history-panel">
              <strong>修改历史与处理人</strong>
              <div v-if="detail.history.length" class="history-list">
                <article v-for="item in detail.history" :key="item.id">
                  <span
                    ><b>{{ item.reviewerName }}</b> · {{ item.decision }}</span
                  ><small
                    >{{ formatTime(item.reviewedAt)
                    }}{{ item.comment ? ` · ${item.comment}` : '' }}</small
                  >
                </article>
              </div>
              <p v-else class="empty">暂无人工处理记录。</p>
            </section>
          </template>
          <p v-else class="empty">选择任务后查看复核材料。</p>
        </div>
      </section>
    </template>

    <section v-if="editingItem" class="surface-card review-editor">
      <header>
        <div>
          <strong>复核第 {{ editingItem.itemNo }} 题</strong
          ><span>可以修改识别文本、答案和题库匹配</span>
        </div>
      </header>
      <div class="form-grid">
        <label
          ><span>识别题干</span><textarea v-model="form.stem" rows="4" maxlength="4000" />
        </label>
        <label
          ><span>学生答案</span><textarea v-model="form.answer" rows="4" maxlength="2000" />
        </label>
        <label class="question-picker"
          ><span>标准题目</span
          ><input v-model="questionKeyword" placeholder="筛选题号、题干或 ID" /><select
            v-model="form.questionId"
          >
            <option value="">不匹配题库题目</option>
            <option v-for="item in filteredQuestions" :key="item.id" :value="item.id">
              {{ item.code }} · {{ item.stem }}
            </option>
          </select></label
        >
        <label
          ><span>复核说明</span
          ><input v-model="form.comment" maxlength="500" placeholder="选填，将写入修改历史"
        /></label>
      </div>
      <footer>
        <t-button
          theme="danger"
          variant="outline"
          :disabled="operating"
          @click="isStudent ? submitStudentDecision('REJECT') : submitWorkbenchDecision('REJECT')"
          >忽略此题</t-button
        ><span /><t-button
          variant="outline"
          :disabled="operating"
          @click="isStudent ? submitStudentDecision('APPROVE') : submitWorkbenchDecision('APPROVE')"
          >原结果正确</t-button
        ><t-button
          :loading="operating"
          @click="isStudent ? submitStudentDecision('CORRECT') : submitWorkbenchDecision('CORRECT')"
          >修正并确认</t-button
        >
      </footer>
    </section>
  </div>
</template>

<style scoped lang="scss">
.filters {
  display: grid;
  padding: 18px 22px;
  align-items: end;
  grid-template-columns: repeat(3, minmax(150px, 1fr)) repeat(2, minmax(190px, 1fr)) auto;
  gap: 10px;
}

.filters label,
.review-editor label {
  display: grid;
  gap: 6px;
}

.filters span,
.review-editor label > span,
header span {
  color: var(--app-text-muted);
  font-size: 12px;
}

.filters select,
.filters input,
.review-editor input,
.review-editor select,
.review-editor textarea,
.feedback-box input,
.feedback-box select {
  width: 100%;
  min-height: 38px;
  padding: 8px 10px;
  border: 1px solid var(--app-border-color);
  border-radius: 9px;
  background: #fff;
  color: inherit;
  font: inherit;
}

.batch-bar {
  display: flex;
  margin-top: 12px;
  padding: 13px 18px;
  align-items: center;
  justify-content: space-between;
}

.workspace {
  display: grid;
  margin-top: 14px;
  grid-template-columns: minmax(300px, 0.7fr) minmax(600px, 1.5fr);
  gap: 14px;
}

.student-workspace {
  margin-top: 0;
}

.task-panel,
.detail-panel,
.review-editor {
  padding: 20px;
}

.task-panel header,
.detail-heading,
.review-editor header,
.review-editor footer,
.item-heading,
.review-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

header strong,
header span,
.task-list span,
.task-list small {
  display: block;
}

header strong {
  color: var(--app-text-strong);
  font-size: 16px;
}

.task-list,
.item-list {
  display: grid;
  gap: 8px;
}

.task-row {
  display: flex;
  padding: 5px 8px;
  align-items: center;
  border: 1px solid transparent;
  border-radius: 11px;
  background: #f7f9f9;
  gap: 5px;
}

.task-row.active {
  border-color: #65aa9c;
  background: #f0faf7;
}

.task-row > button,
.task-list > button {
  display: flex;
  width: 100%;
  padding: 10px;
  align-items: center;
  justify-content: space-between;
  border: 0;
  background: transparent;
  color: inherit;
  cursor: pointer;
  text-align: left;
  gap: 10px;
}

.task-list > button {
  border-radius: 11px;
  background: #f7f9f9;
}

.task-list > button.active {
  background: #f0faf7;
  box-shadow: inset 0 0 0 1px #65aa9c;
}

.task-list small,
.item-list small,
.history-list small {
  margin-top: 4px;
  color: var(--app-text-muted);
  font-size: 11px;
}

.task-status {
  text-align: right;
}

.pagination {
  display: flex;
  margin-top: 16px;
  align-items: center;
  justify-content: space-between;
  color: var(--app-text-muted);
  font-size: 12px;
}

.pagination div {
  display: flex;
  gap: 7px;
}

.item-navigation {
  display: grid;
  margin: 16px 0;
  align-items: center;
  grid-template-columns: auto 1fr auto;
  gap: 10px;
}

.item-navigation > div {
  display: flex;
  overflow-x: auto;
  justify-content: center;
  gap: 6px;
}

.item-navigation button:not(.t-button) {
  width: 31px;
  height: 31px;
  flex: 0 0 auto;
  border: 1px solid var(--app-border-color);
  border-radius: 8px;
  background: #fff;
  cursor: pointer;
}

.item-navigation button.active {
  border-color: #237c6b;
  background: #237c6b;
  color: #fff;
}

.item-navigation button.pending:not(.active) {
  border-color: #efa958;
  background: #fff8ef;
}

.comparison-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.comparison-grid article {
  display: grid;
  min-height: 220px;
  padding: 14px;
  align-content: start;
  border: 1px solid var(--app-border-color);
  border-radius: 12px;
  gap: 10px;
}

.image-frame {
  display: grid;
  height: 260px;
  overflow: auto;
  border-radius: 9px;
  background: #f2f4f5;
  place-items: center;
}

.image-frame img {
  display: block;
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
}

.failed-image {
  height: 520px;
  margin-top: 16px;
}

.image-frame span {
  color: var(--app-text-muted);
}

.recognized-card p,
.standard-card p {
  line-height: 1.65;
}

.answer {
  padding: 10px 12px;
  border-radius: 9px;
  background: #f4f7f7;
  color: #4f5d62;
}

.review-actions {
  padding: 14px 0;
}

.feedback-box {
  display: grid;
  padding: 14px;
  align-items: center;
  grid-template-columns: auto 130px 1fr auto;
  border-radius: 11px;
  background: #fff8ef;
  gap: 9px;
}

.history-panel {
  padding-top: 17px;
  border-top: 1px solid var(--app-border-color);
}

.history-list {
  display: grid;
  margin-top: 10px;
  gap: 7px;
}

.history-list article {
  padding: 10px 12px;
  border-radius: 9px;
  background: #f7f9f9;
}

.item-list article {
  display: grid;
  padding: 14px;
  border: 1px solid var(--app-border-color);
  border-radius: 11px;
  gap: 9px;
}

.item-list article.review {
  border-color: #edbd82;
  background: #fffaf4;
}

.item-list .t-button {
  justify-self: end;
}

.review-editor {
  margin-top: 14px;
}

.form-grid {
  display: grid;
  margin-top: 16px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.question-picker {
  grid-column: 1 / -1;
  grid-template-columns: 1fr 2fr;
}

.question-picker > span {
  grid-column: 1 / -1;
}

.review-editor footer {
  margin-top: 15px;
}

.review-editor footer > span {
  flex: 1;
}

.empty,
.content-loading {
  padding: 32px;
  color: var(--app-text-muted);
  text-align: center;
}

@media (width <= 1180px) {
  .filters {
    grid-template-columns: repeat(3, 1fr);
  }

  .workspace {
    grid-template-columns: 340px minmax(0, 1fr);
  }
}

@media (width <= 900px) {
  .workspace,
  .comparison-grid,
  .form-grid {
    grid-template-columns: 1fr;
  }

  .feedback-box {
    grid-template-columns: 1fr;
  }

  .question-picker {
    grid-column: auto;
    grid-template-columns: 1fr;
  }
}

@media (width <= 640px) {
  .filters {
    grid-template-columns: 1fr;
  }

  .batch-bar,
  .review-editor footer {
    align-items: stretch;
    flex-direction: column;
  }

  .review-editor footer > span {
    display: none;
  }
}
</style>
