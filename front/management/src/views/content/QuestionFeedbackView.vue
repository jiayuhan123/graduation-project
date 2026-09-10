<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import {
  Button as TButton,
  Loading as TLoading,
  MessagePlugin,
  Tag as TTag,
} from 'tdesign-vue-next'
import { handleQuestionFeedbackApi, pageQuestionFeedbackApi } from '@/api/content'
import PageHeader from '@/components/PageHeader.vue'
import type {
  QuestionFeedback,
  QuestionFeedbackCategory,
  QuestionFeedbackStatus,
} from '@/types/content'

const loading = ref(true)
const operatingId = ref('')
const records = ref<QuestionFeedback[]>([])
const total = ref(0)
const pages = ref(0)
const query = reactive<{
  page: number
  size: number
  status: '' | QuestionFeedbackStatus
  category: '' | QuestionFeedbackCategory
  questionId: string
}>({ page: 1, size: 12, status: '', category: '', questionId: '' })

const categoryLabels: Record<QuestionFeedbackCategory, string> = {
  STEM_ERROR: '题干错误',
  ANSWER_ERROR: '答案错误',
  AMBIGUOUS: '表述歧义',
  FORMAT_ERROR: '格式问题',
  OTHER: '其他',
}
const statusLabels: Record<QuestionFeedbackStatus, string> = {
  PENDING: '待处理',
  PROCESSING: '处理中',
  RESOLVED: '已解决',
  REJECTED: '已驳回',
}

function formatTime(value: string | null): string {
  return value ? new Date(value).toLocaleString() : '--'
}

async function load() {
  loading.value = true
  try {
    const result = await pageQuestionFeedbackApi({
      page: query.page,
      size: query.size,
      status: query.status || undefined,
      category: query.category || undefined,
      questionId: query.questionId || undefined,
    })
    records.value = result.records
    total.value = result.total
    pages.value = result.pages
    query.page = result.page
  } finally {
    loading.value = false
  }
}

function search() {
  query.page = 1
  void load()
}

function changePage(delta: number) {
  const next = query.page + delta
  if (next < 1 || next > pages.value) return
  query.page = next
  void load()
}

async function handle(
  item: QuestionFeedback,
  action: 'PROCESS' | 'RESOLVE' | 'REJECT' | 'RESOLVE_AND_DISABLE',
) {
  if (operatingId.value) return
  const needsReason = action !== 'PROCESS'
  const comment = needsReason ? window.prompt('请输入处理结论或原因')?.trim() : undefined
  if (needsReason && !comment) return
  if (
    action === 'RESOLVE_AND_DISABLE' &&
    !window.confirm(`确认解决反馈并下架题目 ${item.questionCode || item.questionId}？`)
  )
    return
  operatingId.value = item.id
  try {
    await handleQuestionFeedbackApi(item.id, { action, comment, version: item.version })
    void MessagePlugin.success(
      action === 'RESOLVE_AND_DISABLE' ? '反馈已解决，题目已下架' : '处理状态已更新',
    )
    await load()
  } finally {
    operatingId.value = ''
  }
}

onMounted(load)
</script>

<template>
  <div>
    <PageHeader
      eyebrow="教学内容"
      title="题目错误反馈"
      :description="`共收到 ${total} 条题目问题反馈，可跟进、解决、驳回或联动下架题目。`"
    />
    <section class="surface-card feedback-card">
      <div class="filters">
        <select v-model="query.status">
          <option value="">全部状态</option>
          <option value="PENDING">待处理</option>
          <option value="PROCESSING">处理中</option>
          <option value="RESOLVED">已解决</option>
          <option value="REJECTED">已驳回</option>
        </select>
        <select v-model="query.category">
          <option value="">全部类型</option>
          <option v-for="(label, value) in categoryLabels" :key="value" :value="value">
            {{ label }}
          </option>
        </select>
        <input v-model="query.questionId" inputmode="numeric" placeholder="题目 ID" />
        <t-button @click="search">查询</t-button>
      </div>
      <div v-if="loading" class="loading"><t-loading text="正在读取题目反馈" /></div>
      <div v-else-if="records.length" class="feedback-list">
        <article v-for="item in records" :key="item.id">
          <header>
            <div>
              <strong>{{ item.questionCode || `题目 #${item.questionId}` }}</strong
              ><span>{{ formatTime(item.createdAt) }} · 反馈人 #{{ item.reporterId }}</span>
            </div>
            <div>
              <t-tag variant="light">{{ categoryLabels[item.category] }}</t-tag
              ><t-tag
                :theme="
                  item.status === 'PENDING'
                    ? 'warning'
                    : item.status === 'RESOLVED'
                      ? 'success'
                      : 'default'
                "
                variant="light"
                >{{ statusLabels[item.status] }}</t-tag
              >
            </div>
          </header>
          <p class="stem">{{ item.questionStem || '题目已不存在' }}</p>
          <p class="description">{{ item.description }}</p>
          <div v-if="item.handleComment" class="result">
            <strong>处理结论：</strong>{{ item.handleComment
            }}<small>处理人 #{{ item.handlerId }} · {{ formatTime(item.handledAt) }}</small>
          </div>
          <footer v-if="item.status === 'PENDING' || item.status === 'PROCESSING'">
            <t-button
              v-if="item.status === 'PENDING'"
              size="small"
              variant="outline"
              :loading="operatingId === item.id"
              @click="handle(item, 'PROCESS')"
              >开始处理</t-button
            ><span /><t-button
              size="small"
              theme="danger"
              variant="outline"
              :disabled="Boolean(operatingId)"
              @click="handle(item, 'REJECT')"
              >驳回</t-button
            ><t-button
              size="small"
              variant="outline"
              :disabled="Boolean(operatingId)"
              @click="handle(item, 'RESOLVE')"
              >标记解决</t-button
            ><t-button
              size="small"
              theme="danger"
              :disabled="Boolean(operatingId)"
              @click="handle(item, 'RESOLVE_AND_DISABLE')"
              >解决并下架题目</t-button
            >
          </footer>
        </article>
      </div>
      <p v-else class="empty">当前筛选条件下没有反馈。</p>
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
    </section>
  </div>
</template>

<style scoped lang="scss">
.feedback-card {
  padding: 22px;
}

.filters {
  display: grid;
  grid-template-columns: minmax(120px, 0.8fr) minmax(120px, 0.8fr) minmax(0, 1fr) auto;
  gap: 10px;
}

.filters select,
.filters input {
  min-width: 0;
  min-height: 38px;
  padding: 8px 10px;
  border: 1px solid var(--app-border-color);
  border-radius: 9px;
  background: #fff;
  color: inherit;
  font: inherit;
}

.loading,
.empty {
  display: grid;
  min-height: 280px;
  color: var(--app-text-muted);
  place-items: center;
}

.feedback-list {
  display: grid;
  margin-top: 20px;
  gap: 11px;
}

.feedback-list article {
  padding: 17px;
  border: 1px solid var(--app-border-color);
  border-radius: 12px;
}

.feedback-list header,
.feedback-list header > div,
.feedback-list footer,
.pagination {
  display: flex;
  align-items: center;
  gap: 8px;
}

.feedback-list header {
  justify-content: space-between;
}

.feedback-list header strong,
.feedback-list header span {
  display: block;
}

.feedback-list header span,
.result small {
  margin-top: 4px;
  color: var(--app-text-muted);
  font-size: 11px;
}

.stem {
  color: var(--app-text-muted);
  line-height: 1.55;
}

.description {
  padding: 11px 13px;
  border-radius: 9px;
  background: var(--app-primary-soft);
  line-height: 1.6;
}

.result {
  padding: 11px 13px;
  border-radius: 9px;
  background: #f3f8f7;
}

.result small {
  display: block;
}

.feedback-list footer {
  margin-top: 13px;
}

.feedback-list footer > span {
  flex: 1;
}

.pagination {
  margin-top: 18px;
  justify-content: space-between;
  color: var(--app-text-muted);
}

@media (width <= 720px) {
  .filters {
    grid-template-columns: 1fr;
  }

  .feedback-list header,
  .feedback-list footer {
    align-items: stretch;
    flex-direction: column;
  }

  .feedback-list footer > span {
    display: none;
  }
}
</style>
