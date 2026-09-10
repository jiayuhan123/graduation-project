<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import {
  Button as TButton,
  Loading as TLoading,
  MessagePlugin,
  Tag as TTag,
} from 'tdesign-vue-next'
import { listLearningKnowledgePointsApi, pageLearningModulesApi } from '@/api/content'
import {
  completePracticeTaskApi,
  createPracticeTaskApi,
  startPracticeTaskApi,
  submitPracticeAnswerApi,
} from '@/api/learning'
import { getCurrentUserApi } from '@/api/user'
import PageHeader from '@/components/PageHeader.vue'
import type { ContentModule, KnowledgePoint } from '@/types/content'
import type { AnswerFeedback, PracticeDetail, PracticeQuestion } from '@/types/learning'

const loading = ref(true)
const route = useRoute()
const creating = ref(false)
const submitting = ref(false)
const completing = ref(false)
const modules = ref<ContentModule[]>([])
const knowledgePoints = ref<KnowledgePoint[]>([])
const moduleId = ref('')
const knowledgePointId = ref('')
const difficulty = ref(3)
const questionCount = ref(10)
const mode = ref<'NORMAL' | 'TIMED'>('NORMAL')
const detail = ref<PracticeDetail | null>(null)
const currentIndex = ref(0)
const answer = ref('')
const feedback = ref<AnswerFeedback | null>(null)
const questionStartedAt = ref(Date.now())

const currentQuestion = computed<PracticeQuestion | null>(
  () => detail.value?.questions[currentIndex.value] ?? null,
)
const isCompleted = computed(() => detail.value?.task.status === 'COMPLETED')
const progress = computed(() =>
  detail.value ? `${currentIndex.value + 1} / ${detail.value.questions.length}` : '',
)

function jsonValue(value: string | null | undefined): string {
  if (!value) return ''
  try {
    const parsed = JSON.parse(value) as { value?: unknown }
    return parsed.value === undefined ? value : String(parsed.value)
  } catch {
    return value
  }
}

async function loadCatalog() {
  loading.value = true
  try {
    const profile = await getCurrentUserApi()
    if (!profile.studentProfile) throw new Error('请先完善学生年级和学期信息')
    const page = await pageLearningModulesApi({
      page: 1,
      size: 100,
      gradeLevel: profile.studentProfile.gradeLevel,
      semester: profile.studentProfile.semester,
    })
    modules.value = page.records
    moduleId.value = page.records[0]?.id ?? ''
    await loadKnowledgePoints()
  } finally {
    loading.value = false
  }
}

async function loadKnowledgePoints() {
  knowledgePointId.value = ''
  knowledgePoints.value = moduleId.value
    ? await listLearningKnowledgePointsApi({ moduleId: moduleId.value })
    : []
  knowledgePointId.value = knowledgePoints.value[0]?.id ?? ''
}

async function createPractice() {
  if (!knowledgePointId.value) {
    void MessagePlugin.warning('请先选择知识点')
    return
  }
  creating.value = true
  try {
    const created = await createPracticeTaskApi({
      knowledgePointId: knowledgePointId.value,
      difficulty: difficulty.value,
      questionCount: questionCount.value,
      mode: mode.value,
      timeLimitSeconds:
        mode.value === 'TIMED' ? Math.max(300, questionCount.value * 60) : undefined,
      targetAccuracy: 0.8,
    })
    detail.value = await startPracticeTaskApi(created.task.id)
    switchQuestion(detail.value.questions.findIndex((item) => !item.latestAnswer))
  } finally {
    creating.value = false
  }
}

function switchQuestion(index: number) {
  if (!detail.value?.questions.length) return
  currentIndex.value = Math.max(0, Math.min(index, detail.value.questions.length - 1))
  answer.value = jsonValue(detail.value.questions[currentIndex.value]?.latestAnswer?.studentAnswer)
  feedback.value = null
  questionStartedAt.value = Date.now()
}

async function submitAnswer() {
  const question = currentQuestion.value
  if (!question || !answer.value.trim() || !detail.value) {
    void MessagePlugin.warning('请先填写答案')
    return
  }
  submitting.value = true
  try {
    const result = await submitPracticeAnswerApi(detail.value.task.id, question.id, {
      answer: answer.value.trim(),
      responseMs: Math.max(0, Date.now() - questionStartedAt.value),
    })
    question.latestAnswer = result.answer
    question.solutionVisible = result.solutionVisible
    question.standardAnswer = result.standardAnswer
    question.analysis = result.analysis
    feedback.value = result
    if (!result.solutionVisible) void MessagePlugin.success('答案已保存')
  } finally {
    submitting.value = false
  }
}

function retryQuestion() {
  answer.value = ''
  feedback.value = null
  questionStartedAt.value = Date.now()
}

async function completePractice() {
  if (!detail.value) return
  const unanswered = detail.value.questions.findIndex((item) => !item.latestAnswer)
  if (unanswered >= 0) {
    switchQuestion(unanswered)
    void MessagePlugin.warning('请先完成全部题目')
    return
  }
  completing.value = true
  try {
    detail.value = await completePracticeTaskApi(detail.value.task.id)
    feedback.value = null
    void MessagePlugin.success('练习已完成，结果已计入学习档案')
  } finally {
    completing.value = false
  }
}

onMounted(async () => {
  const taskId = typeof route.query.taskId === 'string' ? route.query.taskId : ''
  if (!taskId) {
    await loadCatalog()
    return
  }
  try {
    detail.value = await startPracticeTaskApi(taskId)
    switchQuestion(detail.value.questions.findIndex((item) => !item.latestAnswer))
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div>
    <PageHeader
      eyebrow="自主学习"
      title="个性练习"
      description="选择教材和知识点，按自己的节奏完成练习与回顾。"
    />

    <div v-if="loading" class="surface-card loading-card">
      <t-loading text="正在加载教材目录" />
    </div>

    <section v-else-if="!detail" class="practice-setup surface-card">
      <div class="setup-heading">
        <h2>设置一组练习</h2>
        <p>先选学习内容，再确定难度和题量。</p>
      </div>
      <label
        ><span>教材模块</span
        ><select v-model="moduleId" @change="loadKnowledgePoints">
          <option v-for="item in modules" :key="item.id" :value="item.id">{{ item.name }}</option>
        </select></label
      >
      <label
        ><span>知识点</span
        ><select v-model="knowledgePointId">
          <option v-for="item in knowledgePoints" :key="item.id" :value="item.id">
            {{ item.name }}
          </option>
        </select></label
      >
      <label
        ><span>难度</span
        ><select v-model="difficulty">
          <option v-for="item in 5" :key="item" :value="item">难度 {{ item }}</option>
        </select></label
      >
      <label
        ><span>题量</span
        ><select v-model="questionCount">
          <option v-for="item in [5, 10, 15, 20]" :key="item" :value="item">{{ item }} 题</option>
        </select></label
      >
      <label
        ><span>模式</span
        ><select v-model="mode">
          <option value="NORMAL">普通练习</option>
          <option value="TIMED">限时练习</option>
        </select></label
      >
      <t-button
        class="practice-action"
        :loading="creating"
        :disabled="!knowledgePointId"
        @click="createPractice"
        >开始练习</t-button
      >
    </section>

    <template v-else-if="currentQuestion">
      <section class="session-heading surface-card">
        <div>
          <strong>{{ detail.task.title }}</strong
          ><span
            >{{ detail.task.mode === 'TIMED' ? '限时练习' : '普通练习' }} · 难度
            {{ currentQuestion.difficulty }}</span
          >
        </div>
        <t-tag theme="primary" variant="light">{{ progress }}</t-tag>
      </section>
      <section class="question-card surface-card">
        <span class="question-number">第 {{ currentQuestion.sequenceNo }} 题</span>
        <h2>{{ currentQuestion.stem }}</h2>
        <pre v-if="currentQuestion.options" class="options">{{ currentQuestion.options }}</pre>
        <input
          v-model="answer"
          :disabled="isCompleted"
          maxlength="200"
          placeholder="请输入答案"
          @keyup.enter="submitAnswer"
        />

        <div
          v-if="feedback?.solutionVisible"
          class="feedback"
          :class="feedback.answer.correct ? 'feedback--correct' : 'feedback--wrong'"
        >
          <strong>{{ feedback.answer.correct ? '回答正确' : '再检查一下' }}</strong>
          <span v-if="!feedback.answer.correct"
            >标准答案：{{ jsonValue(feedback.standardAnswer) }}</span
          >
          <span v-if="feedback.analysis">解析：{{ feedback.analysis }}</span>
        </div>
        <div
          v-else-if="isCompleted && currentQuestion.solutionVisible"
          class="feedback"
          :class="currentQuestion.latestAnswer?.correct ? 'feedback--correct' : 'feedback--wrong'"
        >
          <strong>{{ currentQuestion.latestAnswer?.correct ? '回答正确' : '回答错误' }}</strong>
          <span>你的答案：{{ jsonValue(currentQuestion.latestAnswer?.studentAnswer) }}</span>
          <span>标准答案：{{ jsonValue(currentQuestion.standardAnswer) }}</span>
          <span v-if="currentQuestion.analysis">解析：{{ currentQuestion.analysis }}</span>
        </div>

        <div v-if="!isCompleted" class="question-actions">
          <t-button v-if="!feedback" :loading="submitting" @click="submitAnswer">提交答案</t-button>
          <t-button v-else-if="!feedback.answer.correct" variant="outline" @click="retryQuestion"
            >再答一次</t-button
          >
        </div>
      </section>
      <div class="session-navigation">
        <t-button
          variant="outline"
          :disabled="currentIndex === 0"
          @click="switchQuestion(currentIndex - 1)"
          >上一题</t-button
        >
        <t-button
          v-if="currentIndex < detail.questions.length - 1"
          variant="outline"
          @click="switchQuestion(currentIndex + 1)"
          >下一题</t-button
        >
        <t-button
          v-else-if="!isCompleted"
          theme="default"
          :loading="completing"
          @click="completePractice"
          >完成练习</t-button
        >
        <t-button v-else variant="outline" @click="detail = null">再来一组</t-button>
      </div>
    </template>
  </div>
</template>

<style scoped lang="scss">
.setup-heading {
  grid-column: 1 / -1;
  margin-bottom: 6px;
  padding-bottom: 20px;
  border-bottom: 1px solid var(--app-border-color);
}

.setup-heading h2 {
  color: var(--app-text-strong);
  font-size: 17px;
  font-weight: 600;
}

.setup-heading p {
  margin-top: 8px;
  color: var(--app-text-muted);
  font-size: 12px;
}

.practice-start {
  display: flex;
  grid-column: 1 / -1;
  margin-top: 10px;
  padding-top: 20px;
  border-top: 1px solid var(--app-border-color);
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;
}

.practice-start .practice-action {
  min-width: 140px;
  height: 40px;
}

.practice-start p {
  color: var(--app-text-muted);
  font-size: 12px;
  line-height: 1.7;
}

.loading-card {
  display: grid;
  min-height: 280px;
  place-items: center;
}

.practice-setup {
  display: grid;
  padding: 28px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.practice-setup label {
  display: grid;
  gap: 8px;
}

.practice-setup label span {
  color: var(--app-text-muted);
  font-size: 12px;
  font-weight: 700;
}

.practice-setup select,
.question-card input {
  min-height: 44px;
  padding: 9px 12px;
  background: #fff;
  border: 1px solid #dfe3e7;
  border-radius: 11px;
  color: var(--app-text-strong);
  font: inherit;
}

.practice-action {
  align-self: end;
}

.session-heading {
  display: flex;
  padding: 22px 26px;
  align-items: center;
  justify-content: space-between;
}

.session-heading strong,
.session-heading span {
  display: block;
}

.session-heading strong {
  color: var(--app-text-strong);
  font-size: 20px;
}

.session-heading span {
  margin-top: 5px;
  color: var(--app-text-muted);
  font-size: 12px;
}

.question-card {
  margin-top: 18px;
  padding: clamp(26px, 5vw, 48px);
}

.question-number {
  color: var(--app-primary);
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.08em;
}

.question-card h2 {
  margin-top: 18px;
  color: var(--app-text-strong);
  font-size: clamp(22px, 3vw, 32px);
  line-height: 1.6;
}

.options {
  margin-top: 18px;
  padding: 16px;
  background: #f7f8fa;
  border-radius: 12px;
  color: var(--app-text);
  font: inherit;
  line-height: 1.6;
  white-space: pre-wrap;
}

.question-card input {
  width: 100%;
  margin-top: 28px;
  font-size: 18px;
}

.feedback {
  display: grid;
  margin-top: 20px;
  padding: 18px;
  border-radius: 14px;
  gap: 6px;
  line-height: 1.6;
}

.feedback--correct {
  background: #eaf8ef;
  color: #247c44;
}

.feedback--wrong {
  background: #fff0e9;
  color: #a74b24;
}

.question-actions {
  margin-top: 20px;
}

.session-navigation {
  display: flex;
  margin-top: 18px;
  justify-content: space-between;
  gap: 12px;
}

@media (width <= 640px) {
  .practice-setup {
    grid-template-columns: 1fr;
  }
}
</style>
