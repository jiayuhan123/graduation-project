<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { Loading as TLoading, Tag as TTag } from 'tdesign-vue-next'
import { getOwnLearningReportApi, getStudentLearningReportApi } from '@/api/learning'
import PageHeader from '@/components/PageHeader.vue'
import StudentSelector from '@/components/StudentSelector.vue'
import { useUserStore } from '@/stores/user'
import type { StudentLearningReport } from '@/types/learning'

const userStore = useUserStore()
const route = useRoute()
const loading = ref(false)
const studentId = ref('')
const report = ref<StudentLearningReport | null>(null)
const isStudent = computed(() => userStore.activeRole === 'student')

const statusLabels: Record<string, string> = {
  PENDING: '待开始',
  IN_PROGRESS: '进行中',
  COMPLETED: '已完成',
  EXPIRED: '已过期',
  CANCELLED: '已取消',
  OPEN: '待巩固',
  RETRAINING: '巩固中',
  RESOLVED: '已掌握',
}

function percentage(value: number): string {
  return `${Math.round(value * 100)}%`
}

async function loadReport() {
  const normalizedStudentId = studentId.value.trim()
  if (!isStudent.value && !normalizedStudentId) {
    report.value = null
    return
  }
  loading.value = true
  try {
    report.value = isStudent.value
      ? await getOwnLearningReportApi()
      : await getStudentLearningReportApi(normalizedStudentId)
  } finally {
    loading.value = false
  }
}

watch(
  () => userStore.activeRole,
  () => {
    studentId.value = ''
    report.value = null
    if (isStudent.value) void loadReport()
  },
)
onMounted(() => {
  if (isStudent.value) {
    void loadReport()
    return
  }
  if (typeof route.query.studentId === 'string') {
    studentId.value = route.query.studentId
    void loadReport()
  }
})
</script>

<template>
  <div>
    <PageHeader
      eyebrow="LEARNING RECORD"
      title="学习档案"
      :description="
        isStudent
          ? '查看你的练习、正确率、错题和薄弱知识点。'
          : '从可访问的学生名单中选择，快速查看练习、错题和掌握度。'
      "
    />

    <section v-if="!isStudent" class="surface-card report-query">
      <StudentSelector v-model="studentId" @change="loadReport" />
    </section>

    <div v-if="loading" class="surface-card content-loading">
      <t-loading text="正在读取学习档案" />
    </div>

    <template v-else-if="report">
      <section class="student-card surface-card">
        <div>
          <span>学生</span>
          <strong>{{ report.displayName }}</strong>
          <small>@{{ report.username }} · ID {{ report.studentId }}</small>
        </div>
        <t-tag theme="success" variant="light">学习档案已连接</t-tag>
      </section>

      <section class="overview-grid">
        <article class="surface-card">
          <strong>{{ report.overview.completedTaskCount }}</strong>
          <span>完成练习</span>
        </article>
        <article class="surface-card">
          <strong>{{ percentage(report.overview.accuracyRate) }}</strong>
          <span>累计正确率</span>
        </article>
        <article class="surface-card">
          <strong>{{ report.overview.openErrorCount }}</strong>
          <span>待巩固错题</span>
        </article>
        <article class="surface-card">
          <strong>{{ report.overview.weakKnowledgePointCount }}</strong>
          <span>薄弱知识点</span>
        </article>
      </section>

      <section class="report-grid">
        <div class="surface-card report-panel">
          <header>
            <div><strong>最近练习</strong><span>最近 10 次任务</span></div>
          </header>
          <div v-if="report.recentTasks.length" class="record-list">
            <article v-for="item in report.recentTasks" :key="item.id">
              <div>
                <strong>{{ item.title }}</strong>
                <span>{{ item.questionCount }} 题 · {{ item.sourceType }}</span>
              </div>
              <t-tag :theme="item.status === 'COMPLETED' ? 'success' : 'warning'" variant="light">
                {{ statusLabels[item.status] ?? item.status }}
              </t-tag>
            </article>
          </div>
          <p v-else class="panel-empty">暂无练习记录。</p>
        </div>

        <div class="surface-card report-panel">
          <header>
            <div><strong>薄弱知识点</strong><span>掌握度低于 60%</span></div>
          </header>
          <div v-if="report.weakKnowledgePoints.length" class="record-list">
            <article v-for="item in report.weakKnowledgePoints" :key="item.id">
              <div>
                <strong>知识点 #{{ item.knowledgePointId }}</strong>
                <span>累计作答 {{ item.totalAnswerCount }} 次</span>
              </div>
              <b>{{ percentage(item.masteryScore) }}</b>
            </article>
          </div>
          <p v-else class="panel-empty">当前没有薄弱知识点。</p>
        </div>

        <div class="surface-card report-panel report-panel--wide">
          <header>
            <div><strong>最近错题</strong><span>答对同题后会自动标记为已掌握</span></div>
          </header>
          <div v-if="report.recentErrors.length" class="record-list">
            <article v-for="item in report.recentErrors" :key="item.id">
              <div>
                <strong>题目 #{{ item.questionId }} · 知识点 #{{ item.knowledgePointId }}</strong>
                <span>{{ item.diagnosis || '答案与标准答案不一致。' }}</span>
              </div>
              <t-tag :theme="item.status === 'RESOLVED' ? 'success' : 'warning'" variant="light">
                {{ statusLabels[item.status] ?? item.status }}
              </t-tag>
            </article>
          </div>
          <p v-else class="panel-empty">暂无错题记录。</p>
        </div>
      </section>
    </template>

    <div v-else class="surface-card content-empty">
      {{ isStudent ? '完成练习后，这里会形成学习档案。' : '输入学生编号后查询学习档案。' }}
    </div>
  </div>
</template>

<style scoped lang="scss">
.report-query {
  padding: 22px;
}

.student-card {
  display: flex;
  padding: 24px;
  align-items: center;
  justify-content: space-between;
}

.student-card span,
.student-card strong,
.student-card small {
  display: block;
}

.student-card span,
.student-card small {
  color: var(--app-text-muted);
  font-size: 12px;
}

.student-card strong {
  margin: 4px 0;
  color: var(--app-text-strong);
  font-size: 22px;
}

.overview-grid {
  display: grid;
  margin-top: 16px;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.overview-grid article {
  display: grid;
  padding: 22px;
  gap: 5px;
}

.overview-grid strong {
  color: var(--app-text-strong);
  font-size: 28px;
}

.overview-grid span {
  color: var(--app-text-muted);
  font-size: 12px;
}

.report-grid {
  display: grid;
  margin-top: 16px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.report-panel {
  padding: 22px;
}

.report-panel--wide {
  grid-column: 1 / -1;
}

.report-panel header strong,
.report-panel header span {
  display: block;
}

.report-panel header strong {
  color: var(--app-text-strong);
  font-size: 17px;
}

.report-panel header span {
  margin-top: 4px;
  color: var(--app-text-muted);
  font-size: 11px;
}

.record-list {
  display: grid;
  margin-top: 16px;
  gap: 8px;
}

.record-list article {
  display: flex;
  padding: 13px;
  align-items: center;
  justify-content: space-between;
  background: #f8f9fa;
  border-radius: 11px;
  gap: 12px;
}

.record-list strong,
.record-list span {
  display: block;
}

.record-list strong,
.record-list b {
  color: var(--app-text-strong);
}

.record-list span {
  margin-top: 4px;
  color: var(--app-text-muted);
  font-size: 11px;
}

.panel-empty {
  padding: 36px 0 20px;
  color: var(--app-text-muted);
  text-align: center;
}

@media (width <= 900px) {
  .overview-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (width <= 640px) {
  .overview-grid,
  .report-grid {
    grid-template-columns: 1fr;
  }

  .report-panel--wide {
    grid-column: auto;
  }
}
</style>
