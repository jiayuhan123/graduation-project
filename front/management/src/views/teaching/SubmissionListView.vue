<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import {
  Button as TButton,
  Loading as TLoading,
  MessagePlugin,
  Tag as TTag,
} from 'tdesign-vue-next'
import {
  listAssignmentSubmissionsApi,
  pageStudentAssignmentsApi,
  pageTeacherAssignmentsApi,
  updateSubmissionCommentApi,
} from '@/api/teaching'
import PageHeader from '@/components/PageHeader.vue'
import { useUserStore } from '@/stores/user'
import type { AssignmentSummary, StudentAssignment, SubmissionDetail } from '@/types/teaching'

const userStore = useUserStore()
const isTeacher = computed(() => userStore.activeRole === 'teacher')
const loading = ref(true)
const assignments = ref<AssignmentSummary[]>([])
const studentAssignments = ref<StudentAssignment[]>([])
const submissions = ref<SubmissionDetail[]>([])
const selectedAssignmentId = ref('')
const status = ref('')
const comments = ref<Record<string, string>>({})
const statusLabels: Record<string, string> = {
  ASSIGNED: '待开始',
  IN_PROGRESS: '进行中',
  SUBMITTED: '已提交',
  GRADED: '已批改',
  OVERDUE: '已逾期',
}

async function load() {
  loading.value = true
  try {
    if (isTeacher.value) {
      const page = await pageTeacherAssignmentsApi({ page: 1, size: 100 })
      assignments.value = page.records.filter(
        (item) => item.status !== 'DRAFT' && item.status !== 'CANCELLED',
      )
      if (!assignments.value.some((item) => item.id === selectedAssignmentId.value))
        selectedAssignmentId.value = assignments.value[0]?.id ?? ''
      await loadSubmissions()
    } else {
      const page = await pageStudentAssignmentsApi({
        page: 1,
        size: 100,
        status: (status.value || undefined) as never,
      })
      studentAssignments.value = page.records
    }
  } finally {
    loading.value = false
  }
}

async function loadSubmissions() {
  if (!selectedAssignmentId.value) {
    submissions.value = []
    return
  }
  submissions.value = await listAssignmentSubmissionsApi(
    selectedAssignmentId.value,
    status.value || undefined,
  )
  comments.value = Object.fromEntries(
    submissions.value.map((item) => [item.submission.id, item.submission.teacherComment ?? '']),
  )
}

async function saveComment(item: SubmissionDetail) {
  const updated = await updateSubmissionCommentApi(
    item.submission.id,
    comments.value[item.submission.id] ?? '',
    item.submission.version,
  )
  const index = submissions.value.findIndex((entry) => entry.submission.id === item.submission.id)
  if (index >= 0) submissions.value[index] = updated
  void MessagePlugin.success('教师评语已保存')
}

function switchAssignment() {
  void loadSubmissions()
}
function switchStatus() {
  if (isTeacher.value) void loadSubmissions()
  else void load()
}

watch(
  () => userStore.activeRole,
  () => {
    status.value = ''
    selectedAssignmentId.value = ''
    void load()
  },
)
onMounted(load)
</script>

<template>
  <div>
    <PageHeader
      eyebrow="TEACHING"
      :title="isTeacher ? '学生提交' : '提交记录'"
      :description="
        isTeacher
          ? '按作业查看学生进度、成绩并维护教师评语。'
          : '查看你的作业分配、提交和批改状态。'
      "
    />
    <section class="surface-card">
      <div class="content-toolbar submission-toolbar">
        <select v-if="isTeacher" v-model="selectedAssignmentId" @change="switchAssignment">
          <option value="">暂无已发布作业</option>
          <option v-for="item in assignments" :key="item.id" :value="item.id">
            {{ item.title }}
          </option>
        </select>
        <select v-model="status" @change="switchStatus">
          <option value="">全部状态</option>
          <option v-for="(label, code) in statusLabels" :key="code" :value="code">
            {{ label }}
          </option>
        </select>
        <span class="muted">练习启动、答题与自动批改将在学习服务接入后更新这里的进度。</span>
      </div>
      <div v-if="loading" class="content-loading"><t-loading text="正在加载提交记录" /></div>
      <div v-else-if="isTeacher && submissions.length" class="submission-list">
        <article v-for="item in submissions" :key="item.submission.id" class="submission-card">
          <div class="submission-card__identity">
            <strong>{{
              item.displayName || item.username || `学生 ${item.submission.studentId}`
            }}</strong
            ><span
              >{{ item.username ? `@${item.username}` : `ID ${item.submission.studentId}` }} ·
              {{ item.submission.layerCode }} 层</span
            >
          </div>
          <div class="submission-card__score">
            <strong>{{ item.submission.score ?? '—' }}</strong
            ><span>当前得分</span>
          </div>
          <t-tag
            :theme="item.submission.status === 'GRADED' ? 'success' : 'warning'"
            variant="light"
            >{{ statusLabels[item.submission.status] }}</t-tag
          >
          <div class="comment-editor">
            <textarea
              v-model="comments[item.submission.id]"
              maxlength="500"
              placeholder="给学生写一句评语（选填）"
            ></textarea
            ><t-button size="small" variant="outline" @click="saveComment(item)">保存评语</t-button>
          </div>
        </article>
      </div>
      <div v-else-if="!isTeacher && studentAssignments.length" class="submission-list">
        <article
          v-for="item in studentAssignments"
          :key="item.submission.id"
          class="submission-card student-submission"
        >
          <div class="submission-card__identity">
            <strong>{{ item.assignment.title }}</strong
            ><span
              >{{ item.submission.layerCode }} 层 · 截止
              {{ new Date(item.assignment.dueAt).toLocaleString() }}</span
            >
          </div>
          <div class="submission-card__score">
            <strong>{{ item.submission.score ?? '—' }}</strong
            ><span>{{
              item.submission.totalCount
                ? `${item.submission.correctCount ?? 0}/${item.submission.totalCount} 题正确`
                : '尚未评分'
            }}</span>
          </div>
          <t-tag
            :theme="item.submission.status === 'GRADED' ? 'success' : 'warning'"
            variant="light"
            >{{ statusLabels[item.submission.status] }}</t-tag
          >
          <p v-if="item.submission.teacherComment" class="teacher-comment">
            教师评语：{{ item.submission.teacherComment }}
          </p>
        </article>
      </div>
      <div v-else class="content-empty">
        {{ isTeacher ? '当前作业暂无学生提交记录。' : '暂无符合条件的提交记录。' }}
      </div>
    </section>
  </div>
</template>

<style scoped lang="scss">
.submission-toolbar {
  grid-template-columns: minmax(200px, 1fr) 160px minmax(260px, 2fr);
}

.submission-list {
  display: grid;
  padding: 0 22px 22px;
  gap: 12px;
}

.submission-card {
  display: grid;
  padding: 18px;
  align-items: center;
  background: #fff;
  border: 1px solid #eceef0;
  border-radius: 17px;
  grid-template-columns: minmax(180px, 1fr) 90px auto minmax(280px, 1.3fr);
  gap: 15px;
}

.submission-card__identity strong,
.submission-card__identity span,
.submission-card__score strong,
.submission-card__score span {
  display: block;
}

.submission-card__identity strong,
.submission-card__score strong {
  color: var(--app-text-strong);
}

.submission-card__identity span,
.submission-card__score span {
  margin-top: 4px;
  color: var(--app-text-muted);
  font-size: 11px;
}

.submission-card__score strong {
  font-size: 20px;
}

.comment-editor {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 8px;
}

.comment-editor textarea {
  min-height: 54px;
  padding: 8px 10px;
  resize: vertical;
  border: 1px solid #dfe3e7;
  border-radius: 10px;
  font: inherit;
}

.student-submission {
  grid-template-columns: minmax(200px, 1fr) 100px auto;
}

.teacher-comment {
  padding: 10px;
  background: #fff6ef;
  border-radius: 10px;
  color: #8f4a1e;
  grid-column: 1 / -1;
}

@media (width <= 900px) {
  .submission-card {
    grid-template-columns: 1fr auto;
  }

  .comment-editor {
    grid-column: 1 / -1;
  }
}

@media (width <= 640px) {
  .submission-toolbar,
  .submission-card,
  .student-submission,
  .comment-editor {
    grid-template-columns: 1fr;
  }
}
</style>
