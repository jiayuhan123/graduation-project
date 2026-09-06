<script setup lang="ts">
import { onMounted, ref } from 'vue'
import {
  Button as TButton,
  Loading as TLoading,
  MessagePlugin,
  Tag as TTag,
} from 'tdesign-vue-next'
import { getQuestionApi, pageQuestionsApi, reviewQuestionApi } from '@/api/content'
import ContentEditorPanel from '@/components/ContentEditorPanel.vue'
import PageHeader from '@/components/PageHeader.vue'
import type { QuestionDetail, QuestionSummary } from '@/types/content'

const loading = ref(true)
const reviewing = ref(false)
const questions = ref<QuestionSummary[]>([])
const detail = ref<QuestionDetail | null>(null)
const remark = ref('')

async function loadDrafts() {
  loading.value = true
  try {
    const result = await pageQuestionsApi({ page: 1, size: 100, status: 'DRAFT' })
    questions.value = result.records
  } finally {
    loading.value = false
  }
}

async function openReview(question: QuestionSummary) {
  detail.value = await getQuestionApi(question.id)
  remark.value = ''
}

async function review(decision: 'APPROVE' | 'RETURN') {
  if (!detail.value) return
  reviewing.value = true
  try {
    await reviewQuestionApi(detail.value.id, {
      decision,
      remark: remark.value.trim() || undefined,
      version: detail.value.version,
    })
    detail.value = null
    void MessagePlugin.success(decision === 'APPROVE' ? '题目已发布' : '题目已退回草稿')
    await loadDrafts()
  } finally {
    reviewing.value = false
  }
}

function formatJson(value: string | null): string {
  if (!value) return '无'
  try {
    return JSON.stringify(JSON.parse(value) as unknown, null, 2)
  } catch {
    return value
  }
}

onMounted(loadDrafts)
</script>

<template>
  <div>
    <PageHeader
      eyebrow="QUALITY"
      title="内容审核"
      :description="`当前有 ${questions.length} 道草稿题等待核对，审核动作会进入审计日志。`"
    />

    <section class="surface-card review-card">
      <div v-if="loading" class="content-loading"><t-loading text="正在加载待审核题目" /></div>
      <div v-else-if="questions.length" class="review-grid">
        <article v-for="question in questions" :key="question.id" class="review-item">
          <div>
            <span class="review-item__code">{{ question.code }}</span>
            <h2>{{ question.stem }}</h2>
            <p>{{ question.questionType }} · 难度 {{ question.difficulty }}</p>
          </div>
          <div class="review-item__action">
            <t-tag theme="warning" variant="light">草稿</t-tag>
            <t-button size="small" @click="openReview(question)">开始审核</t-button>
          </div>
        </article>
      </div>
      <div v-else class="content-empty">当前没有待审核题目。</div>
    </section>

    <ContentEditorPanel
      v-if="detail"
      title="审核题目"
      description="核对题面、答案、解析、生成规则和知识点后再发布。"
      :busy="reviewing"
      submit-text="通过并发布"
      @close="detail = null"
      @submit="review('APPROVE')"
    >
      <div class="review-detail">
        <section>
          <span>题目编码</span><strong>{{ detail.code }}</strong>
        </section>
        <section>
          <span>题干</span><strong>{{ detail.stem }}</strong>
        </section>
        <section>
          <span>标准答案</span>
          <pre>{{ formatJson(detail.standardAnswer) }}</pre>
        </section>
        <section>
          <span>选项</span>
          <pre>{{ formatJson(detail.options) }}</pre>
        </section>
        <section>
          <span>生成规则</span>
          <pre>{{ formatJson(detail.generationRule) }}</pre>
        </section>
        <section>
          <span>解析</span>
          <p>{{ detail.analysis || '未填写解析' }}</p>
        </section>
        <section>
          <span>知识点</span>
          <div class="binding-list">
            <t-tag
              v-for="binding in detail.knowledgePoints"
              :key="binding.knowledgePointId"
              variant="light"
            >
              {{ binding.knowledgePointName || binding.knowledgePointCode
              }}{{ binding.primary ? '（主）' : '' }}
            </t-tag>
          </div>
        </section>
        <label class="editor-field"
          ><span>审核说明（可选）</span><textarea v-model="remark" maxlength="500"></textarea>
        </label>
        <t-button theme="danger" variant="outline" :disabled="reviewing" @click="review('RETURN')">
          退回草稿
        </t-button>
      </div>
    </ContentEditorPanel>
  </div>
</template>

<style scoped lang="scss">
.review-card {
  padding: 22px;
}

.review-grid {
  display: grid;
  gap: 12px;
}

.review-item {
  display: flex;
  padding: 19px;
  align-items: center;
  justify-content: space-between;
  background: #fffaf6;
  border: 1px solid #ffe5d3;
  border-radius: 18px;
  gap: 20px;
}

.review-item__code {
  color: var(--app-primary);
  font-size: 11px;
  font-weight: 700;
}

.review-item h2 {
  margin-top: 6px;
  color: var(--app-text-strong);
  font-size: 16px;
}

.review-item p {
  margin-top: 6px;
  color: var(--app-text-muted);
  font-size: 12px;
}

.review-item__action {
  display: flex;
  align-items: center;
  gap: 9px;
}

.review-detail {
  display: grid;
  gap: 16px;
}

.review-detail section {
  padding: 14px;
  background: #f8f9fb;
  border-radius: 13px;
}

.review-detail section > span {
  display: block;
  margin-bottom: 7px;
  color: var(--app-text-muted);
  font-size: 11px;
  font-weight: 700;
}

.review-detail strong,
.review-detail p,
.review-detail pre {
  color: var(--app-text-strong);
  line-height: 1.65;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

.binding-list {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
}

@media (width <= 640px) {
  .review-item {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
