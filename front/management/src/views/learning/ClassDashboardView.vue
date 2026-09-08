<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  Button as TButton,
  Loading as TLoading,
  MessagePlugin,
  Tag as TTag,
} from 'tdesign-vue-next'
import { createClassTargetedPracticeApi, getClassLearningDashboardApi } from '@/api/learning'
import { pageTeacherClassesApi } from '@/api/teaching'
import PageHeader from '@/components/PageHeader.vue'
import type { ClassWeakKnowledgePoint, ClassLearningDashboard } from '@/types/learning'
import type { ClassMembership } from '@/types/teaching'

const router = useRouter()
const loading = ref(true)
const operatingPointId = ref('')
const classes = ref<ClassMembership[]>([])
const classId = ref('')
const days = ref(30)
const dashboard = ref<ClassLearningDashboard | null>(null)

const maxDailyAnswers = computed(() =>
  Math.max(1, ...(dashboard.value?.dailyTrends.map((item) => item.answerCount) ?? [1])),
)

function percentage(value: number): string {
  return `${Math.round(value * 100)}%`
}

function shortDate(value: string): string {
  return value.slice(5)
}

function formatTime(value: string | null): string {
  return value ? new Date(value).toLocaleString() : '暂无学习'
}

async function loadClasses() {
  const result = await pageTeacherClassesApi({ page: 1, size: 100 })
  classes.value = result.records.filter((item) => item.classStatus === 'ACTIVE')
  if (!classes.value.some((item) => item.classId === classId.value)) {
    classId.value = classes.value[0]?.classId ?? ''
  }
}

async function loadDashboard() {
  if (!classId.value) {
    dashboard.value = null
    loading.value = false
    return
  }
  loading.value = true
  try {
    dashboard.value = await getClassLearningDashboardApi(classId.value, days.value)
  } finally {
    loading.value = false
  }
}

async function createPractice(point: ClassWeakKnowledgePoint) {
  if (!dashboard.value || operatingPointId.value) return
  if (
    !window.confirm(
      `将为“${dashboard.value.className}”全部 ${dashboard.value.metrics.studentCount} 名学生生成“${point.knowledgePointName}”针对练习，是否继续？`,
    )
  )
    return
  operatingPointId.value = point.knowledgePointId
  try {
    const result = await createClassTargetedPracticeApi(classId.value, {
      knowledgePointId: point.knowledgePointId,
      questionCount: 10,
      mode: 'REVIEW',
      targetAccuracy: 0.8,
    })
    void MessagePlugin.success(`已生成 ${result.createdCount} 份学生练习`)
    await loadDashboard()
  } finally {
    operatingPointId.value = ''
  }
}

async function createAssignment(point: ClassWeakKnowledgePoint) {
  await router.push({
    path: '/teaching/assignments',
    query: {
      create: '1',
      classId: classId.value,
      knowledgePointId: point.knowledgePointId,
      knowledgePointName: point.knowledgePointName,
    },
  })
}

async function viewStudent(studentId: string) {
  await router.push({ path: '/learning/records', query: { studentId } })
}

async function initialize() {
  loading.value = true
  try {
    await loadClasses()
    await loadDashboard()
  } finally {
    loading.value = false
  }
}

onMounted(initialize)
</script>

<template>
  <div>
    <PageHeader
      eyebrow="CLASS INSIGHT"
      title="班级学情看板"
      description="从班级整体发现薄弱知识点和异常学生，并直接生成针对练习或作业。"
    />

    <section class="surface-card filters">
      <label>
        <span>班级</span>
        <select v-model="classId" @change="loadDashboard">
          <option v-for="item in classes" :key="item.classId" :value="item.classId">
            {{ item.name }} · {{ item.gradeLevel }} 年级
          </option>
        </select>
      </label>
      <label>
        <span>统计周期</span>
        <select v-model.number="days" @change="loadDashboard">
          <option :value="7">最近 7 天</option>
          <option :value="30">最近 30 天</option>
          <option :value="60">最近 60 天</option>
          <option :value="90">最近 90 天</option>
        </select>
      </label>
      <t-button variant="outline" :loading="loading" @click="loadDashboard">刷新数据</t-button>
    </section>

    <section v-if="loading" class="surface-card content-loading">
      <t-loading text="正在聚合班级学习数据" />
    </section>

    <template v-else-if="dashboard">
      <section class="metric-grid">
        <article class="surface-card">
          <strong>{{ percentage(dashboard.metrics.accuracyRate) }}</strong
          ><span>平均正确率</span
          ><small
            >{{ dashboard.metrics.correctAnswerCount }} /
            {{ dashboard.metrics.answerCount }} 题</small
          >
        </article>
        <article class="surface-card">
          <strong>{{ percentage(dashboard.metrics.completionRate) }}</strong
          ><span>练习完成率</span
          ><small
            >{{ dashboard.metrics.completedTaskCount }} /
            {{ dashboard.metrics.taskCount }} 次</small
          >
        </article>
        <article class="surface-card">
          <strong>{{ dashboard.metrics.openErrorCount }}</strong
          ><span>待巩固错题</span><small>覆盖 {{ dashboard.metrics.studentCount }} 名学生</small>
        </article>
        <article class="surface-card">
          <strong>{{ dashboard.alerts.length }}</strong
          ><span>学生提醒</span
          ><small
            >{{
              dashboard.alerts.filter((item) => item.severity === 'HIGH').length
            }}
            条高优先级</small
          >
        </article>
      </section>

      <section class="surface-card trend-panel">
        <header>
          <div><strong>班级学习趋势</strong><span>每日答题量与正确率</span></div>
        </header>
        <div class="trend-scroll">
          <div v-for="item in dashboard.dailyTrends" :key="item.date" class="trend-day">
            <div class="trend-track">
              <i
                :style="{ height: `${Math.max(4, (item.answerCount / maxDailyAnswers) * 100)}%` }"
              />
            </div>
            <strong>{{ item.answerCount }}</strong>
            <span>{{ percentage(item.accuracyRate) }}</span>
            <small>{{ shortDate(item.date) }}</small>
          </div>
        </div>
      </section>

      <section class="two-column">
        <div class="surface-card panel">
          <header>
            <div><strong>薄弱知识点排行</strong><span>按受影响学生数排序</span></div>
          </header>
          <div v-if="dashboard.weakKnowledgePoints.length" class="rank-list">
            <article
              v-for="(item, index) in dashboard.weakKnowledgePoints"
              :key="item.knowledgePointId"
            >
              <b>{{ index + 1 }}</b>
              <div>
                <strong>{{ item.knowledgePointName }}</strong
                ><span
                  >{{ item.affectedStudentCount }} 名学生薄弱 · 平均掌握度
                  {{ percentage(item.averageMastery) }}</span
                >
              </div>
              <div class="row-actions">
                <t-button
                  size="small"
                  variant="outline"
                  :loading="operatingPointId === item.knowledgePointId"
                  @click="createPractice(item)"
                  >生成练习</t-button
                >
                <t-button size="small" @click="createAssignment(item)">布置作业</t-button>
              </div>
            </article>
          </div>
          <p v-else class="empty">当前周期没有明显薄弱知识点。</p>
        </div>

        <div class="surface-card panel">
          <header>
            <div><strong>异常学生提醒</strong><span>正确率、活跃度、错题和掌握度</span></div>
          </header>
          <div v-if="dashboard.alerts.length" class="alert-list">
            <button
              v-for="item in dashboard.alerts"
              :key="`${item.studentId}-${item.code}`"
              type="button"
              @click="viewStudent(item.studentId)"
            >
              <t-tag :theme="item.severity === 'HIGH' ? 'danger' : 'warning'" variant="light">{{
                item.severity === 'HIGH' ? '重点' : '关注'
              }}</t-tag>
              <span
                ><strong>{{ item.displayName }}</strong
                ><small>{{ item.message }}</small></span
              >
              <i>查看 ›</i>
            </button>
          </div>
          <p v-else class="empty">暂无需要关注的学生。</p>
        </div>
      </section>

      <section class="two-column">
        <div class="surface-card panel">
          <header>
            <div><strong>高频错题</strong><span>当前统计周期</span></div>
          </header>
          <div class="compact-list">
            <article v-for="item in dashboard.frequentWrongQuestions" :key="item.questionId">
              <span
                ><strong>{{ item.questionCode || `题目 #${item.questionId}` }}</strong
                ><small>{{ item.stem }}</small></span
              >
              <b>{{ item.errorCount }} 次</b>
            </article>
            <p v-if="!dashboard.frequentWrongQuestions.length" class="empty">暂无错题数据。</p>
          </div>
        </div>
        <div class="surface-card panel">
          <header>
            <div><strong>高频错误类型</strong><span>用于调整讲解重点</span></div>
          </header>
          <div class="compact-list">
            <article v-for="item in dashboard.frequentErrorTypes" :key="item.errorTypeId">
              <span
                ><strong>{{ item.errorTypeName }}</strong
                ><small>{{ item.affectedStudentCount }} 名学生受到影响</small></span
              >
              <b>{{ item.errorCount }} 次</b>
            </article>
            <p v-if="!dashboard.frequentErrorTypes.length" class="empty">暂无错因数据。</p>
          </div>
        </div>
      </section>

      <section class="surface-card panel student-panel">
        <header>
          <div><strong>学生明细</strong><span>点击学生下钻到个人学习档案</span></div>
        </header>
        <div class="table-scroll">
          <table>
            <thead>
              <tr>
                <th>学生</th>
                <th>答题</th>
                <th>正确率</th>
                <th>完成练习</th>
                <th>待巩固错题</th>
                <th>薄弱知识点</th>
                <th>最近学习</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="item in dashboard.students"
                :key="item.studentId"
                @click="viewStudent(item.studentId)"
              >
                <td>
                  <strong>{{ item.displayName }}</strong
                  ><small>@{{ item.username || item.studentId }}</small>
                </td>
                <td>{{ item.answerCount }}</td>
                <td>{{ percentage(item.accuracyRate) }}</td>
                <td>{{ item.completedTaskCount }} / {{ item.taskCount }}</td>
                <td>{{ item.openErrorCount }}</td>
                <td>{{ item.weakKnowledgePointCount }}</td>
                <td>{{ formatTime(item.lastActivityAt) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>
    </template>

    <section v-else class="surface-card empty-state">当前没有可查看的有效班级。</section>
  </div>
</template>

<style scoped lang="scss">
.filters {
  display: flex;
  padding: 18px 22px;
  align-items: end;
  gap: 14px;
}

.filters label {
  display: grid;
  min-width: 190px;
  gap: 6px;
}

.filters span,
header span {
  color: var(--app-text-muted);
  font-size: 12px;
}

.filters select {
  min-height: 38px;
  padding: 0 10px;
  border: 1px solid var(--app-border-color);
  border-radius: 9px;
  background: #fff;
}

.metric-grid {
  display: grid;
  margin-top: 16px;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.metric-grid article {
  display: grid;
  padding: 22px;
  gap: 5px;
}

.metric-grid strong {
  color: var(--app-text-strong);
  font-size: 30px;
}

.metric-grid span {
  font-weight: 700;
}

.metric-grid small,
.rank-list span,
.compact-list small,
td small {
  display: block;
  color: var(--app-text-muted);
}

.trend-panel,
.panel {
  margin-top: 16px;
  padding: 22px;
}

header {
  display: flex;
  margin-bottom: 18px;
  justify-content: space-between;
}

header strong,
header span {
  display: block;
}

.trend-scroll {
  display: flex;
  min-height: 190px;
  padding-top: 8px;
  overflow-x: auto;
  align-items: end;
  gap: 8px;
}

.trend-day {
  display: grid;
  min-width: 34px;
  flex: 1;
  justify-items: center;
  gap: 3px;
}

.trend-track {
  display: flex;
  width: 12px;
  height: 105px;
  align-items: end;
  overflow: hidden;
  border-radius: 999px;
  background: #f0f2f4;
}

.trend-track i {
  display: block;
  width: 100%;
  border-radius: 999px;
  background: linear-gradient(#ff9b5d, #e96f24);
}

.trend-day strong {
  font-size: 11px;
}

.trend-day span,
.trend-day small {
  color: var(--app-text-muted);
  font-size: 9px;
  white-space: nowrap;
}

.two-column {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.rank-list,
.alert-list,
.compact-list {
  display: grid;
  gap: 10px;
}

.rank-list article,
.compact-list article,
.alert-list button {
  display: flex;
  padding: 13px;
  align-items: center;
  border: 1px solid var(--app-border-color);
  border-radius: 12px;
  background: #fff;
  gap: 12px;
}

.rank-list article > b {
  display: grid;
  width: 28px;
  height: 28px;
  place-items: center;
  border-radius: 9px;
  background: #fff2e8;
  color: #d96722;
}

.rank-list article > div:nth-child(2),
.compact-list article span,
.alert-list button span {
  min-width: 0;
  flex: 1;
}

.rank-list strong,
.rank-list span,
.compact-list strong,
.compact-list small,
.alert-list strong,
.alert-list small {
  display: block;
}

.row-actions {
  display: flex;
  gap: 7px;
}

.alert-list button {
  width: 100%;
  color: inherit;
  cursor: pointer;
  font: inherit;
  text-align: left;
}

.alert-list button i {
  color: var(--app-text-muted);
  font-size: 11px;
  font-style: normal;
}

.compact-list article > b {
  color: #df6d28;
}

.student-panel {
  margin-bottom: 28px;
}

.table-scroll {
  overflow-x: auto;
}

table {
  width: 100%;
  border-collapse: collapse;
}

th,
td {
  padding: 12px;
  border-bottom: 1px solid var(--app-border-color);
  text-align: left;
  white-space: nowrap;
}

th {
  color: var(--app-text-muted);
  font-size: 11px;
}

tbody tr {
  cursor: pointer;
}

tbody tr:hover {
  background: #fff8f2;
}

.empty,
.empty-state {
  padding: 30px;
  color: var(--app-text-muted);
  text-align: center;
}

.content-loading {
  margin-top: 16px;
  padding: 64px;
  text-align: center;
}

@media (width <= 900px) {
  .metric-grid,
  .two-column {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .filters {
    align-items: stretch;
    flex-direction: column;
  }
}

@media (width <= 620px) {
  .metric-grid,
  .two-column {
    grid-template-columns: 1fr;
  }

  .rank-list article {
    align-items: flex-start;
    flex-wrap: wrap;
  }

  .row-actions {
    width: 100%;
    justify-content: flex-end;
  }
}
</style>
