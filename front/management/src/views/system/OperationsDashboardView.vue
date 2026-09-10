<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { Button as TButton, Loading as TLoading, Tag as TTag } from 'tdesign-vue-next'
import {
  getContentOperationsStatisticsApi,
  getGameOperationsStatisticsApi,
  getLearningOperationsStatisticsApi,
  getTeachingOperationsStatisticsApi,
  getUserOperationsStatisticsApi,
} from '@/api/operations'
import PageHeader from '@/components/PageHeader.vue'
import type {
  ContentOperationsStatistics,
  GameOperationsStatistics,
  LearningOperationsStatistics,
  TeachingOperationsStatistics,
  UserOperationsStatistics,
} from '@/types/operations'

const loading = ref(true)
const errors = ref<string[]>([])
const users = ref<UserOperationsStatistics | null>(null)
const teaching = ref<TeachingOperationsStatistics | null>(null)
const content = ref<ContentOperationsStatistics | null>(null)
const learning = ref<LearningOperationsStatistics | null>(null)
const game = ref<GameOperationsStatistics | null>(null)

const cards = computed(() => [
  {
    label: '平台用户',
    value: users.value?.totalUsers ?? '--',
    detail: `${users.value?.activeUsers ?? '--'} 个正常账号`,
  },
  {
    label: '活跃班级',
    value: teaching.value?.activeClasses ?? '--',
    detail: `${teaching.value?.activeStudentMemberships ?? '--'} 个在班学生`,
  },
  {
    label: '启用题目',
    value: content.value?.enabledQuestions ?? '--',
    detail: `${content.value?.pendingFeedback ?? '--'} 条待处理反馈`,
  },
  {
    label: 'OCR 任务',
    value: learning.value?.totalOcrTasks ?? '--',
    detail: `${learning.value?.pendingReviewTasks ?? '--'} 个待复核`,
  },
  {
    label: 'PK 对局',
    value: game.value?.totalPkSessions ?? '--',
    detail: `${percentage(game.value?.completionRate)} 完成率`,
  },
])

function percentage(value: number | undefined): string {
  return value === undefined ? '--' : `${Math.round(value * 100)}%`
}

async function settle<T>(label: string, loader: () => Promise<T>, assign: (value: T) => void) {
  try {
    assign(await loader())
  } catch {
    errors.value.push(label)
  }
}

async function load() {
  loading.value = true
  errors.value = []
  await Promise.all([
    settle('用户统计', getUserOperationsStatisticsApi, (value) => (users.value = value)),
    settle('教学统计', getTeachingOperationsStatisticsApi, (value) => (teaching.value = value)),
    settle('内容统计', getContentOperationsStatisticsApi, (value) => (content.value = value)),
    settle(
      '学习与 OCR 统计',
      getLearningOperationsStatisticsApi,
      (value) => (learning.value = value),
    ),
    settle('PK 统计', getGameOperationsStatisticsApi, (value) => (game.value = value)),
  ])
  loading.value = false
}

onMounted(load)
</script>

<template>
  <div>
    <PageHeader
      eyebrow="平台管理"
      title="运营数据总览"
      description="统一查看用户、班级、题库、OCR 和 PK 的关键运行指标。"
    />

    <section v-if="errors.length" class="surface-card warning-bar">
      <span>部分服务暂时未返回：{{ errors.join('、') }}</span>
      <t-button size="small" variant="outline" @click="load">重新加载</t-button>
    </section>
    <section v-if="loading" class="surface-card loading">
      <t-loading text="正在汇总各服务运营数据" />
    </section>

    <template v-else>
      <section class="metric-grid">
        <article v-for="item in cards" :key="item.label" class="surface-card">
          <strong>{{ item.value }}</strong
          ><span>{{ item.label }}</span
          ><small>{{ item.detail }}</small>
        </article>
      </section>

      <section class="detail-grid">
        <article class="surface-card panel">
          <header><strong>用户构成</strong><t-tag variant="light">账号</t-tag></header>
          <dl v-if="users">
            <div>
              <dt>学生</dt>
              <dd>{{ users.studentUsers }}</dd>
            </div>
            <div>
              <dt>教师</dt>
              <dd>{{ users.teacherUsers }}</dd>
            </div>
            <div>
              <dt>管理员</dt>
              <dd>{{ users.adminUsers }}</dd>
            </div>
            <div>
              <dt>已禁用</dt>
              <dd>{{ users.disabledUsers }}</dd>
            </div>
          </dl>
          <p v-else>暂无数据</p>
        </article>
        <article class="surface-card panel">
          <header><strong>班级与作业</strong><t-tag variant="light">教学</t-tag></header>
          <dl v-if="teaching">
            <div>
              <dt>班级总数</dt>
              <dd>{{ teaching.totalClasses }}</dd>
            </div>
            <div>
              <dt>教师成员关系</dt>
              <dd>{{ teaching.teacherMemberships }}</dd>
            </div>
            <div>
              <dt>已发布作业</dt>
              <dd>{{ teaching.publishedAssignments }}</dd>
            </div>
            <div>
              <dt>已完成提交</dt>
              <dd>{{ teaching.completedSubmissions }} / {{ teaching.totalSubmissions }}</dd>
            </div>
          </dl>
          <p v-else>暂无数据</p>
        </article>
        <article class="surface-card panel">
          <header><strong>题库健康度</strong><t-tag variant="light">内容</t-tag></header>
          <dl v-if="content">
            <div>
              <dt>题目总数</dt>
              <dd>{{ content.totalQuestions }}</dd>
            </div>
            <div>
              <dt>草稿</dt>
              <dd>{{ content.draftQuestions }}</dd>
            </div>
            <div>
              <dt>已下架</dt>
              <dd>{{ content.disabledQuestions }}</dd>
            </div>
            <div>
              <dt>待处理反馈</dt>
              <dd>{{ content.pendingFeedback }}</dd>
            </div>
          </dl>
          <p v-else>暂无数据</p>
        </article>
        <article class="surface-card panel emphasis">
          <header>
            <strong>OCR 质量</strong><t-tag theme="warning" variant="light">重点</t-tag>
          </header>
          <dl v-if="learning">
            <div>
              <dt>识别失败率</dt>
              <dd>{{ percentage(learning.ocrFailureRate) }}</dd>
            </div>
            <div>
              <dt>人工复核率</dt>
              <dd>{{ percentage(learning.manualReviewRate) }}</dd>
            </div>
            <div>
              <dt>复核确认准确率</dt>
              <dd>{{ percentage(learning.reviewAccuracyRate) }}</dd>
            </div>
            <div>
              <dt>修正 / 驳回</dt>
              <dd>{{ learning.correctedItems }} / {{ learning.rejectedItems }}</dd>
            </div>
          </dl>
          <p v-else>暂无数据</p>
        </article>
        <article class="surface-card panel">
          <header><strong>PK 运行情况</strong><t-tag variant="light">对战</t-tag></header>
          <dl v-if="game">
            <div>
              <dt>开局数</dt>
              <dd>{{ game.totalPkSessions }}</dd>
            </div>
            <div>
              <dt>进行中</dt>
              <dd>{{ game.activePkSessions }}</dd>
            </div>
            <div>
              <dt>完成率</dt>
              <dd>{{ percentage(game.completionRate) }}</dd>
            </div>
            <div>
              <dt>异常结束率</dt>
              <dd>{{ percentage(game.abnormalRate) }}</dd>
            </div>
          </dl>
          <p v-else>暂无数据</p>
        </article>
        <article class="surface-card panel">
          <header><strong>学习活动</strong><t-tag variant="light">练习</t-tag></header>
          <dl v-if="learning">
            <div>
              <dt>练习任务</dt>
              <dd>{{ learning.totalPracticeTasks }}</dd>
            </div>
            <div>
              <dt>已完成</dt>
              <dd>{{ learning.completedPracticeTasks }}</dd>
            </div>
            <div>
              <dt>答题总量</dt>
              <dd>{{ learning.totalAnswers }}</dd>
            </div>
            <div>
              <dt>答题正确率</dt>
              <dd>{{ percentage(learning.answerAccuracyRate) }}</dd>
            </div>
          </dl>
          <p v-else>暂无数据</p>
        </article>
      </section>
    </template>
  </div>
</template>

<style scoped lang="scss">
.warning-bar {
  display: flex;
  padding: 12px 18px;
  align-items: center;
  justify-content: space-between;
  color: #a4551f;
}

.loading {
  display: grid;
  min-height: 320px;
  place-items: center;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 12px;
}

.metric-grid article {
  display: grid;
  padding: 20px;
  gap: 4px;
}

.metric-grid strong {
  color: var(--app-text-strong);
  font-size: 29px;
}

.metric-grid span {
  font-weight: 700;
}

.metric-grid small {
  color: var(--app-text-muted);
}

.detail-grid {
  display: grid;
  margin: 14px 0 28px;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.panel {
  padding: 20px;
}

.panel.emphasis {
  border-color: #f2bd81;
  background: var(--app-primary-soft);
}

.panel header {
  display: flex;
  margin-bottom: 15px;
  align-items: center;
  justify-content: space-between;
}

.panel dl {
  display: grid;
  margin: 0;
  gap: 9px;
}

.panel dl div {
  display: flex;
  padding: 10px 12px;
  justify-content: space-between;
  border-radius: 9px;
  background: #f7f9f9;
}

.panel dt {
  color: var(--app-text-muted);
}

.panel dd {
  margin: 0;
  color: var(--app-text-strong);
  font-weight: 600;
}

.panel p {
  color: var(--app-text-muted);
}

@media (width <= 1050px) {
  .metric-grid {
    grid-template-columns: repeat(3, 1fr);
  }

  .detail-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (width <= 650px) {
  .metric-grid,
  .detail-grid {
    grid-template-columns: 1fr;
  }
}
</style>
