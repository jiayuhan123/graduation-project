<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import {
  Button as TButton,
  Loading as TLoading,
  MessagePlugin,
  Tag as TTag,
} from 'tdesign-vue-next'
import {
  confirmPhotoItemApi,
  getPhotoTaskApi,
  getStudentPhotoTaskApi,
  pagePhotoTasksApi,
  pageStudentPhotoTasksApi,
  reviewStudentPhotoItemApi,
} from '@/api/learning'
import PageHeader from '@/components/PageHeader.vue'
import StudentSelector from '@/components/StudentSelector.vue'
import { useUserStore } from '@/stores/user'
import type { PhotoDetail, PhotoItem, PhotoTask, ReviewPhotoItemInput } from '@/types/learning'

const userStore = useUserStore()
const loading = ref(false)
const detailLoading = ref(false)
const operating = ref(false)
const studentId = ref('')
const records = ref<PhotoTask[]>([])
const detail = ref<PhotoDetail | null>(null)
const editingItem = ref<PhotoItem | null>(null)
const form = reactive({ answer: '', questionId: '', comment: '' })
const isStudent = computed(() => userStore.activeRole === 'student')

const statusLabels: Record<string, string> = {
  UPLOADED: '等待识别',
  PROCESSING: '识别中',
  REVIEW_REQUIRED: '待复核',
  COMPLETED: '已完成',
  FAILED: '识别失败',
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

async function load() {
  if (!isStudent.value && !studentId.value) {
    records.value = []
    detail.value = null
    return
  }
  loading.value = true
  detail.value = null
  editingItem.value = null
  try {
    const page = isStudent.value
      ? await pagePhotoTasksApi({ page: 1, size: 100 })
      : await pageStudentPhotoTasksApi(studentId.value, { page: 1, size: 100 })
    records.value = page.records
    const reviewTask = records.value.find((item) => item.status === 'REVIEW_REQUIRED')
    if (reviewTask) await openTask(reviewTask)
  } finally {
    loading.value = false
  }
}

async function openTask(task: PhotoTask) {
  detailLoading.value = true
  editingItem.value = null
  try {
    detail.value = isStudent.value
      ? await getPhotoTaskApi(task.id)
      : await getStudentPhotoTaskApi(studentId.value, task.id)
  } finally {
    detailLoading.value = false
  }
}

function edit(item: PhotoItem) {
  if (item.status !== 'REVIEW_REQUIRED') return
  editingItem.value = item
  form.answer = answerText(item.recognizedAnswer)
  form.questionId = item.matchedQuestionId ?? ''
  form.comment = ''
}

async function submit(decision: ReviewPhotoItemInput['decision']) {
  if (!detail.value || !editingItem.value || operating.value) return
  if (decision === 'CORRECT' && !form.answer.trim()) {
    void MessagePlugin.warning('请填写修正后的学生答案')
    return
  }
  operating.value = true
  try {
    const payload: ReviewPhotoItemInput = {
      decision,
      recognizedAnswer: decision === 'CORRECT' ? form.answer.trim() : undefined,
      matchedQuestionId: decision === 'CORRECT' && form.questionId ? form.questionId : undefined,
      comment: form.comment.trim() || undefined,
    }
    detail.value = isStudent.value
      ? await confirmPhotoItemApi(detail.value.task.id, editingItem.value.id, payload)
      : await reviewStudentPhotoItemApi(
          studentId.value,
          detail.value.task.id,
          editingItem.value.id,
          payload,
        )
    editingItem.value = null
    void MessagePlugin.success('识别结果已处理并同步学习档案')
    await load()
  } finally {
    operating.value = false
  }
}

watch(
  () => userStore.activeRole,
  () => {
    studentId.value = ''
    records.value = []
    detail.value = null
    if (isStudent.value) void load()
  },
)
onMounted(() => {
  if (isStudent.value) void load()
})
</script>

<template>
  <div>
    <PageHeader
      eyebrow="PHOTO REVIEW"
      title="拍照批改与复核"
      :description="
        isStudent
          ? '查看识别进度，并确认系统无法可靠判断的题目。'
          : '从可访问学生中选择待复核任务，修正答案或题目匹配后直接回流学习档案。'
      "
    />

    <section v-if="!isStudent" class="surface-card student-query">
      <StudentSelector v-model="studentId" @change="load" />
    </section>

    <section class="workspace">
      <div class="surface-card task-panel">
        <header>
          <div>
            <strong>拍照任务</strong><span>{{ records.length }} 条记录</span>
          </div>
          <t-tag theme="warning" variant="light">
            {{ records.filter((item) => item.status === 'REVIEW_REQUIRED').length }} 待复核
          </t-tag>
        </header>
        <div v-if="loading" class="content-loading"><t-loading text="正在读取拍照任务" /></div>
        <div v-else-if="records.length" class="task-list">
          <button
            v-for="item in records"
            :key="item.id"
            :class="{ active: detail?.task.id === item.id }"
            type="button"
            @click="openTask(item)"
          >
            <span
              ><strong>任务 #{{ item.id }}</strong
              ><small
                >{{ new Date(item.submittedAt).toLocaleString() }} ·
                {{ item.recognizedItemCount }} 题</small
              ></span
            >
            <t-tag
              :theme="
                item.status === 'REVIEW_REQUIRED'
                  ? 'warning'
                  : item.status === 'COMPLETED'
                    ? 'success'
                    : 'default'
              "
              variant="light"
            >
              {{ statusLabels[item.status] ?? item.status }}
            </t-tag>
          </button>
        </div>
        <p v-else class="empty">
          {{ !isStudent && !studentId ? '请先选择学生。' : '暂无拍照任务。' }}
        </p>
      </div>

      <div class="surface-card detail-panel">
        <div v-if="detailLoading" class="content-loading">
          <t-loading text="正在读取识别明细" />
        </div>
        <template v-else-if="detail">
          <header>
            <div>
              <strong>识别明细</strong
              ><span>整体可信度 {{ confidenceText(detail.task.overallConfidence) }}</span>
            </div>
            <t-tag
              :theme="detail.task.status === 'REVIEW_REQUIRED' ? 'warning' : 'success'"
              variant="light"
            >
              {{ statusLabels[detail.task.status] ?? detail.task.status }}
            </t-tag>
          </header>
          <div class="item-list">
            <article
              v-for="item in detail.items"
              :key="item.id"
              :class="{ review: item.status === 'REVIEW_REQUIRED' }"
            >
              <div class="item-heading">
                <strong>第 {{ item.itemNo }} 题</strong>
                <t-tag
                  :theme="item.status === 'REVIEW_REQUIRED' ? 'warning' : 'default'"
                  variant="light"
                >
                  {{ statusLabels[item.status] ?? item.status }}
                </t-tag>
              </div>
              <p>{{ item.recognizedStem || '未识别题干' }}</p>
              <div class="answer">
                识别答案：{{ answerText(item.recognizedAnswer) || '未识别' }}
              </div>
              <small
                >可信度 {{ confidenceText(item.recognitionConfidence) }} · 题库题目
                {{ item.matchedQuestionId || '待匹配' }}</small
              >
              <t-button
                v-if="item.status === 'REVIEW_REQUIRED'"
                size="small"
                variant="outline"
                @click="edit(item)"
                >复核此题</t-button
              >
            </article>
          </div>
        </template>
        <p v-else class="empty">选择一条任务查看识别结果。</p>
      </div>
    </section>

    <section v-if="editingItem" class="surface-card review-editor">
      <header>
        <div>
          <strong>复核第 {{ editingItem.itemNo }} 题</strong
          ><span>确认原结果，或修正答案及题库匹配</span>
        </div>
      </header>
      <div class="form-grid">
        <label
          ><span>学生答案</span><textarea v-model="form.answer" rows="4" maxlength="2000" />
        </label>
        <div class="side-fields">
          <label
            ><span>匹配题目 ID</span><input v-model="form.questionId" inputmode="numeric"
          /></label>
          <label
            ><span>复核说明（选填）</span><input v-model="form.comment" maxlength="500"
          /></label>
        </div>
      </div>
      <footer>
        <t-button theme="danger" variant="outline" :disabled="operating" @click="submit('REJECT')"
          >忽略此题</t-button
        >
        <span />
        <t-button variant="outline" :disabled="operating" @click="submit('APPROVE')"
          >原结果正确</t-button
        >
        <t-button :loading="operating" @click="submit('CORRECT')">修正并确认</t-button>
      </footer>
    </section>
  </div>
</template>

<style scoped lang="scss">
.student-query {
  padding: 22px;
}

.workspace {
  display: grid;
  grid-template-columns: minmax(290px, 0.8fr) minmax(420px, 1.4fr);
  gap: 16px;
}

.task-panel,
.detail-panel,
.review-editor {
  padding: 22px;
}

.task-panel header,
.detail-panel header,
.review-editor header,
.item-heading,
.review-editor footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

header strong,
header span {
  display: block;
}

header strong {
  color: var(--app-text-strong);
  font-size: 17px;
}

header span {
  margin-top: 4px;
  color: var(--app-text-muted);
  font-size: 11px;
}

.task-list,
.item-list {
  display: grid;
  margin-top: 16px;
  gap: 9px;
}

.task-list button {
  display: flex;
  width: 100%;
  padding: 13px;
  align-items: center;
  justify-content: space-between;
  border: 1px solid transparent;
  border-radius: 11px;
  background: #f7f9f9;
  color: inherit;
  cursor: pointer;
  text-align: left;
  gap: 10px;
}

.task-list button.active {
  border-color: #73b9ab;
  background: #f1faf8;
}

.task-list span,
.task-list small {
  display: block;
}

.task-list small,
.item-list small {
  margin-top: 4px;
  color: var(--app-text-muted);
  font-size: 11px;
}

.item-list article {
  display: grid;
  padding: 15px;
  border: 1px solid #edf0f1;
  border-radius: 12px;
  gap: 10px;
}

.item-list article.review {
  border-color: #edc18e;
  background: #fffaf4;
}

.item-list p {
  color: var(--app-text-strong);
  line-height: 1.6;
}

.answer {
  padding: 10px 12px;
  border-radius: 9px;
  background: #f4f7f7;
  color: #4f5d62;
}

.item-list .t-button {
  justify-self: end;
}

.empty {
  display: grid;
  min-height: 230px;
  color: var(--app-text-muted);
  place-items: center;
  text-align: center;
}

.review-editor {
  margin-top: 16px;
}

.form-grid {
  display: grid;
  margin-top: 18px;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}

.form-grid label,
.side-fields {
  display: grid;
  gap: 8px;
}

.side-fields {
  align-content: start;
  gap: 14px;
}

.form-grid label span {
  color: var(--app-text-muted);
  font-size: 12px;
  font-weight: 700;
}

.form-grid input,
.form-grid textarea {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #dfe3e7;
  border-radius: 10px;
  color: var(--app-text-strong);
  font: inherit;
  box-sizing: border-box;
  resize: vertical;
}

.review-editor footer {
  margin-top: 16px;
}

.review-editor footer span {
  flex: 1;
}

@media (width <= 900px) {
  .workspace,
  .form-grid {
    grid-template-columns: 1fr;
  }
}

@media (width <= 640px) {
  .review-editor footer {
    flex-wrap: wrap;
  }

  .review-editor footer span {
    display: none;
  }
}
</style>
