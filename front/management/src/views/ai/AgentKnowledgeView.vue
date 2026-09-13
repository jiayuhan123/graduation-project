<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import {
  Button as TButton,
  Loading as TLoading,
  MessagePlugin,
  Tag as TTag,
} from 'tdesign-vue-next'
import {
  deleteAgentKnowledgeApi,
  getAgentQualityApi,
  listAgentKnowledgeApi,
  upsertAgentKnowledgeApi,
} from '@/api/ai'
import ContentEditorPanel from '@/components/ContentEditorPanel.vue'
import PageHeader from '@/components/PageHeader.vue'
import type {
  AgentKnowledgeDocument,
  AgentKnowledgeInput,
  AgentKnowledgeSourceType,
  AgentQualitySummary,
} from '@/types/ai'

const loading = ref(true)
const saving = ref(false)
const editorVisible = ref(false)
const documents = ref<AgentKnowledgeDocument[]>([])
const quality = ref<AgentQualitySummary>()
const form = reactive<AgentKnowledgeInput>({
  title: '',
  sourceUri: '',
  sourceType: 'TEXTBOOK',
  content: '',
  metadata: {},
})
const metadataText = ref('{}')

async function load() {
  loading.value = true
  try {
    const [items, summary] = await Promise.all([listAgentKnowledgeApi(), getAgentQualityApi()])
    documents.value = items
    quality.value = summary
  } finally {
    loading.value = false
  }
}

function openCreate() {
  Object.assign(form, {
    title: '',
    sourceUri: `manual://${crypto.randomUUID()}`,
    sourceType: 'TEXTBOOK' as AgentKnowledgeSourceType,
    content: '',
    metadata: {},
  })
  metadataText.value = '{}'
  editorVisible.value = true
}

async function save() {
  let metadata: Record<string, unknown>
  try {
    metadata = JSON.parse(metadataText.value) as Record<string, unknown>
  } catch {
    void MessagePlugin.error('元数据必须是合法 JSON 对象')
    return
  }
  if (!metadata || Array.isArray(metadata) || typeof metadata !== 'object') {
    void MessagePlugin.error('元数据必须是 JSON 对象')
    return
  }
  saving.value = true
  try {
    await upsertAgentKnowledgeApi({
      ...form,
      title: form.title.trim(),
      sourceUri: form.sourceUri.trim(),
      content: form.content.trim(),
      metadata,
    })
    editorVisible.value = false
    void MessagePlugin.success('文档已切片、向量化并写入知识库')
    await load()
  } finally {
    saving.value = false
  }
}

async function remove(document: AgentKnowledgeDocument) {
  if (!window.confirm(`确定从 Agent 知识库删除“${document.title}”吗？`)) return
  await deleteAgentKnowledgeApi(document.id)
  void MessagePlugin.success('知识文档已删除')
  await load()
}

function percent(value = 0): string {
  return `${(value * 100).toFixed(1)}%`
}

onMounted(load)
</script>

<template>
  <div>
    <PageHeader
      eyebrow="AI 治理"
      title="Agent 知识库"
      description="管理可追溯教学资料；相同来源地址再次写入会自动更新索引。"
    >
      <template #actions><t-button @click="openCreate">导入知识文档</t-button></template>
    </PageHeader>

    <section v-if="quality" class="quality-grid">
      <article class="surface-card">
        <span>近 30 天运行</span><strong>{{ quality.runCount }}</strong>
      </article>
      <article class="surface-card">
        <span>运行成功率</span><strong>{{ percent(quality.successRate) }}</strong>
      </article>
      <article class="surface-card">
        <span>平均 RAG 命中</span><strong>{{ quality.averageRagHits.toFixed(1) }}</strong>
      </article>
      <article class="surface-card">
        <span>反馈好评率</span><strong>{{ percent(quality.positiveRate) }}</strong>
      </article>
    </section>

    <section class="surface-card knowledge-card">
      <div v-if="loading" class="content-loading"><t-loading text="正在加载 Agent 知识库" /></div>
      <div v-else-if="documents.length" class="content-list">
        <article v-for="document in documents" :key="document.id" class="content-row">
          <div class="content-row__main">
            <strong>{{ document.title }}</strong>
            <span>{{ document.sourceUri }}</span>
          </div>
          <div class="content-row__meta">
            {{ document.chunkCount }} 个片段 · {{ new Date(document.updatedAt).toLocaleString() }}
          </div>
          <div class="content-row__actions">
            <t-tag theme="success" variant="light">{{ document.sourceType }}</t-tag>
            <t-button size="small" theme="danger" variant="text" @click="remove(document)"
              >删除</t-button
            >
          </div>
        </article>
      </div>
      <div v-else class="content-empty">
        知识库为空。导入教材、教案或教学规范后，Agent 才能生成带来源的回答。
      </div>
    </section>

    <ContentEditorPanel
      v-if="editorVisible"
      title="导入 Agent 知识文档"
      description="文本会在服务端切片并生成向量；请勿导入学生隐私或未授权资料。"
      :busy="saving"
      @close="editorVisible = false"
      @submit="save"
    >
      <div class="editor-grid">
        <label class="editor-field"
          ><span>标题</span><input v-model="form.title" required maxlength="200"
        /></label>
        <label class="editor-field">
          <span>资料类型</span>
          <select v-model="form.sourceType">
            <option value="TEXTBOOK">教材</option>
            <option value="LESSON">教案</option>
            <option value="POLICY">教学规范</option>
            <option value="MARKDOWN">Markdown</option>
            <option value="TEXT">普通文本</option>
          </select>
        </label>
        <label class="editor-field editor-field--wide">
          <span>来源地址</span><input v-model="form.sourceUri" required maxlength="1000" />
        </label>
        <label class="editor-field editor-field--wide">
          <span>筛选元数据（JSON）</span><textarea v-model="metadataText" rows="3" />
        </label>
        <label class="editor-field editor-field--wide">
          <span>正文</span
          ><textarea v-model="form.content" required minlength="20" maxlength="100000" rows="16" />
        </label>
      </div>
    </ContentEditorPanel>
  </div>
</template>

<style scoped lang="scss">
.quality-grid {
  display: grid;
  margin-bottom: 18px;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.quality-grid article {
  padding: 18px;
}

.quality-grid span,
.quality-grid strong {
  display: block;
}

.quality-grid span {
  color: var(--app-text-muted);
  font-size: 12px;
}

.quality-grid strong {
  margin-top: 7px;
  font-size: 24px;
}

.knowledge-card {
  padding: 20px;
}

@media (width <= 900px) {
  .quality-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
