<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { Button as TButton, MessagePlugin, Tag as TTag } from 'tdesign-vue-next'
import {
  confirmAgentActionApi,
  forgetAgentMemoryApi,
  streamAgentApi,
  submitAgentFeedbackApi,
} from '@/api/ai'
import { pageTeacherClassesApi } from '@/api/teaching'
import PageHeader from '@/components/PageHeader.vue'
import { useUserStore } from '@/stores/user'
import type { AgentActionDraft, AgentStreamEvent } from '@/types/ai'
import type { ClassMembership } from '@/types/teaching'

interface ChatMessage {
  id: string
  role: 'user' | 'assistant'
  content: string
  status?: string
  runId?: string
  feedback?: -1 | 1
  complete?: boolean
  sources?: AgentSource[]
}

interface AgentSource {
  citation: string
  title: string
  sourceUri: string
  score: number
}

const userStore = useUserStore()
const messages = ref<ChatMessage[]>([
  {
    id: crypto.randomUUID(),
    role: 'assistant',
    content:
      '我是教学决策助手。我可以读取你有权访问的班级学情、检索固定题库，并生成等待确认的作业草稿。',
  },
])
const classes = ref<ClassMembership[]>([])
const prompt = ref('')
const conversationId = ref<string>()
const running = ref(false)
const activeTool = ref('')
const actionDraft = ref<AgentActionDraft>()
const confirming = ref(false)
const confirmedAssignmentId = ref('')
const forgetting = ref(false)
const messageList = ref<HTMLElement>()
let controller: AbortController | undefined

const canSend = computed(() => prompt.value.trim().length > 0 && !running.value)

function toolLabel(name: string): string {
  const labels: Record<string, string> = {
    get_class_dashboard: '读取班级学情',
    search_questions: '检索固定题库',
    create_assignment_draft: '生成作业草稿',
    retrieve_teaching_knowledge: '检索教学知识库',
  }
  return labels[name] || name
}

function isActionDraft(value: unknown): value is AgentActionDraft {
  if (!value || typeof value !== 'object') return false
  const draft = value as Partial<AgentActionDraft>
  return (
    draft.actionType === 'CREATE_ASSIGNMENT' &&
    typeof draft.draftId === 'string' &&
    typeof draft.confirmationToken === 'string'
  )
}

function handleEvent(event: AgentStreamEvent, assistant: ChatMessage) {
  if (event.event === 'connected' || event.event === 'metadata') {
    const value = event.data.conversationId
    if (typeof value === 'string') conversationId.value = value
    const runId = event.data.runId
    if (typeof runId === 'string') assistant.runId = runId
  }
  if (event.event === 'tool_start') {
    const value = event.data.toolName
    activeTool.value = typeof value === 'string' ? toolLabel(value) : '读取数据'
    assistant.status = activeTool.value
  }
  if (event.event === 'tool_result') {
    const value = event.data.data
    if (isActionDraft(value)) actionDraft.value = value
    if (
      event.data.toolName === 'retrieve_teaching_knowledge' &&
      value &&
      typeof value === 'object'
    ) {
      const sources = (value as { sources?: unknown }).sources
      if (Array.isArray(sources)) {
        assistant.sources = sources.filter(isAgentSource)
      }
    }
    activeTool.value = ''
    assistant.status = '正在组织分析结果'
  }
  if (event.event === 'delta') {
    const value = event.data.content
    if (typeof value === 'string') assistant.content += value
    assistant.status = undefined
  }
  if (event.event === 'done') assistant.complete = true
  if (event.event === 'error') {
    const value = event.data.message
    throw new Error(typeof value === 'string' ? value : 'Agent 运行失败')
  }
}

function isAgentSource(value: unknown): value is AgentSource {
  if (!value || typeof value !== 'object') return false
  const source = value as Partial<AgentSource>
  return (
    typeof source.citation === 'string' &&
    typeof source.title === 'string' &&
    typeof source.sourceUri === 'string' &&
    typeof source.score === 'number'
  )
}

function safeSourceUrl(value: string): string | undefined {
  return /^https?:\/\//i.test(value) ? value : undefined
}

async function scrollToBottom() {
  await nextTick()
  messageList.value?.scrollTo({ top: messageList.value.scrollHeight, behavior: 'smooth' })
}

async function sendMessage(message = prompt.value) {
  const content = message.trim()
  if (!content || running.value) return
  const userMessage: ChatMessage = { id: crypto.randomUUID(), role: 'user', content }
  const assistant: ChatMessage = {
    id: crypto.randomUUID(),
    role: 'assistant',
    content: '',
    status: '正在理解问题',
  }
  messages.value.push(userMessage, assistant)
  prompt.value = ''
  actionDraft.value = undefined
  confirmedAssignmentId.value = ''
  running.value = true
  controller = new AbortController()
  await scrollToBottom()
  try {
    await streamAgentApi(
      {
        conversationId: conversationId.value,
        role: userStore.activeRole as 'teacher' | 'admin',
        message: content,
      },
      (event) => {
        handleEvent(event, assistant)
        void scrollToBottom()
      },
      controller.signal,
    )
    if (!assistant.content) assistant.content = '本次运行没有生成可展示的答复。'
  } catch (error) {
    if ((error as Error).name === 'AbortError') return
    assistant.status = undefined
    assistant.content ||= error instanceof Error ? error.message : 'Agent 服务暂时不可用。'
    void MessagePlugin.error(assistant.content)
  } finally {
    running.value = false
    activeTool.value = ''
    controller = undefined
    await scrollToBottom()
  }
}

async function confirmDraft() {
  if (!actionDraft.value || confirming.value) return
  confirming.value = true
  try {
    const result = await confirmAgentActionApi(actionDraft.value.confirmationToken)
    confirmedAssignmentId.value = result.assignmentId
    actionDraft.value = undefined
    void MessagePlugin.success('作业草稿已创建，可前往作业管理继续检查或发布')
  } finally {
    confirming.value = false
  }
}

async function submitFeedback(message: ChatMessage, rating: -1 | 1) {
  if (!message.runId || message.feedback) return
  await submitAgentFeedbackApi({ runId: message.runId, rating })
  message.feedback = rating
  void MessagePlugin.success('感谢反馈，已用于 Agent 质量评测')
}

async function forgetMemory() {
  if (!conversationId.value || forgetting.value) return
  if (!window.confirm('确定清除本会话的短期与长期记忆吗？该操作不可撤销。')) return
  forgetting.value = true
  try {
    await forgetAgentMemoryApi(conversationId.value)
    conversationId.value = undefined
    actionDraft.value = undefined
    confirmedAssignmentId.value = ''
    messages.value = [
      {
        id: crypto.randomUUID(),
        role: 'assistant',
        content: '会话记忆已清除。接下来的对话会从全新上下文开始。',
      },
    ]
    void MessagePlugin.success('Agent 会话记忆已清除')
  } finally {
    forgetting.value = false
  }
}

async function loadClasses() {
  if (userStore.activeRole !== 'teacher') return
  try {
    classes.value = (await pageTeacherClassesApi({ page: 1, size: 20 })).records.filter(
      (item) => item.classStatus === 'ACTIVE',
    )
  } catch {
    classes.value = []
  }
}

function analyzeClass(item: ClassMembership) {
  void sendMessage(
    `分析班级 ${item.classId}（${item.name}）最近 30 天的学习情况，指出主要薄弱点和教学建议。`,
  )
}

onMounted(loadClasses)
onBeforeUnmount(() => controller?.abort())
</script>

<template>
  <div class="agent-page">
    <PageHeader
      eyebrow="AI 教学助手"
      title="教学决策 Agent"
      description="基于真实班级学情调用受控工具；作业写入必须由你确认。"
    />

    <div class="agent-layout">
      <aside class="surface-card context-panel">
        <div>
          <span class="section-label">快捷分析</span>
          <h2>选择班级</h2>
          <p>Agent 只会读取你有权访问的数据。</p>
        </div>
        <button
          v-for="item in classes"
          :key="item.classId"
          class="class-prompt"
          type="button"
          :disabled="running"
          @click="analyzeClass(item)"
        >
          <strong>{{ item.name }}</strong>
          <span>{{ item.gradeLevel }} 年级 · ID {{ item.classId }}</span>
        </button>
        <p v-if="!classes.length" class="muted">暂无可用班级，也可以在对话中输入班级 ID。</p>
        <div class="guardrail-card">
          <strong>可信执行边界</strong>
          <span>模型不能直接判题、修改掌握度或发布作业。</span>
        </div>
      </aside>

      <section class="surface-card chat-panel">
        <div class="chat-toolbar">
          <span>短期上下文存储于 Redis，长期语义记忆按用户隔离</span>
          <button
            type="button"
            :disabled="!conversationId || running || forgetting"
            @click="forgetMemory"
          >
            {{ forgetting ? '清除中…' : '清除会话记忆' }}
          </button>
        </div>
        <div ref="messageList" class="message-list" aria-live="polite">
          <article
            v-for="message in messages"
            :key="message.id"
            class="message"
            :class="`message--${message.role}`"
          >
            <span>{{ message.role === 'user' ? '你' : '教学 Agent' }}</span>
            <p v-if="message.content">{{ message.content }}</p>
            <p v-else-if="message.status" class="thinking"><i />{{ message.status }}</p>
            <div v-if="message.sources?.length" class="source-list">
              <span v-for="source in message.sources" :key="source.citation">
                <a
                  v-if="safeSourceUrl(source.sourceUri)"
                  :href="safeSourceUrl(source.sourceUri)"
                  target="_blank"
                  rel="noopener noreferrer"
                >
                  [{{ source.citation }}] {{ source.title }}
                </a>
                <em v-else>[{{ source.citation }}] {{ source.title }}</em>
                · 匹配度 {{ (source.score * 100).toFixed(0) }}%
              </span>
            </div>
            <div
              v-if="
                message.role === 'assistant' && message.runId && message.content && message.complete
              "
              class="feedback-actions"
            >
              <button
                type="button"
                :class="{ active: message.feedback === 1 }"
                :disabled="Boolean(message.feedback)"
                @click="submitFeedback(message, 1)"
              >
                有帮助
              </button>
              <button
                type="button"
                :class="{ active: message.feedback === -1 }"
                :disabled="Boolean(message.feedback)"
                @click="submitFeedback(message, -1)"
              >
                需改进
              </button>
            </div>
          </article>

          <article v-if="actionDraft" class="action-card">
            <div>
              <t-tag theme="warning" variant="light">等待人工确认</t-tag>
              <h3>{{ actionDraft.title }}</h3>
              <p>{{ actionDraft.summary }}</p>
              <small
                >确认令牌将在 {{ new Date(actionDraft.expiresAt).toLocaleString() }} 失效</small
              >
            </div>
            <t-button :loading="confirming" @click="confirmDraft">确认创建作业草稿</t-button>
          </article>

          <article v-if="confirmedAssignmentId" class="success-card">
            作业草稿 {{ confirmedAssignmentId }} 已创建，请前往“作业管理”检查后发布。
          </article>
        </div>

        <form class="composer" @submit.prevent="sendMessage()">
          <textarea
            v-model="prompt"
            maxlength="4000"
            rows="3"
            :disabled="running"
            placeholder="例如：分析 12 班最近 30 天的学情，并给出分层训练建议"
            @keydown.ctrl.enter="sendMessage()"
          />
          <div>
            <span>{{ running ? activeTool || 'Agent 正在处理' : 'Ctrl + Enter 发送' }}</span>
            <t-button type="submit" :disabled="!canSend" :loading="running">发送</t-button>
          </div>
        </form>
      </section>
    </div>
  </div>
</template>

<style scoped lang="scss">
.agent-layout {
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr);
  gap: 18px;
}

.context-panel {
  display: flex;
  min-height: 620px;
  padding: 20px;
  flex-direction: column;
  gap: 10px;
}

.section-label {
  color: var(--app-primary);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.12em;
}

.context-panel h2 {
  margin: 8px 0 4px;
  font-size: 18px;
}

.context-panel p,
.class-prompt span,
.guardrail-card span {
  color: var(--app-text-muted);
  font-size: 12px;
}

.class-prompt {
  padding: 12px;
  text-align: left;
  background: #f8f9fb;
  border: 1px solid transparent;
  border-radius: 10px;
  cursor: pointer;
}

.class-prompt:hover {
  background: #fff;
  border-color: var(--app-primary);
}

.class-prompt strong,
.class-prompt span,
.guardrail-card strong,
.guardrail-card span {
  display: block;
}

.class-prompt span,
.guardrail-card span {
  margin-top: 4px;
}

.guardrail-card {
  padding: 14px;
  background: #f0f7f4;
  border-radius: 10px;
  margin-top: auto;
}

.chat-panel {
  display: grid;
  min-height: 620px;
  overflow: hidden;
  grid-template-rows: auto minmax(0, 1fr) auto;
}

.chat-toolbar {
  display: flex;
  padding: 10px 16px;
  align-items: center;
  justify-content: space-between;
  color: var(--app-text-muted);
  border-bottom: 1px solid var(--app-border-color);
  font-size: 12px;
}

.chat-toolbar button,
.feedback-actions button {
  padding: 4px 8px;
  color: var(--app-text-muted);
  background: transparent;
  border: 1px solid var(--app-border-color);
  border-radius: 6px;
  cursor: pointer;
}

.chat-toolbar button:disabled,
.feedback-actions button:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

.feedback-actions {
  display: flex;
  margin-top: 6px;
  gap: 6px;
}

.source-list {
  display: grid;
  padding: 10px 12px;
  margin-top: 7px;
  color: var(--app-text-muted);
  background: #f7faf9;
  border: 1px solid #dfece6;
  border-radius: 8px;
  font-size: 12px;
  gap: 5px;
}

.source-list span,
.source-list em {
  font-style: normal;
}

.source-list a {
  color: var(--app-primary);
}

.feedback-actions button.active {
  color: var(--app-primary);
  border-color: var(--app-primary);
}

.message-list {
  max-height: 620px;
  padding: 24px;
  overflow-y: auto;
}

.message {
  max-width: 82%;
  margin-bottom: 18px;
}

.message > span {
  display: block;
  margin-bottom: 5px;
  color: var(--app-text-muted);
  font-size: 11px;
}

.message p {
  padding: 13px 15px;
  margin: 0;
  line-height: 1.7;
  white-space: pre-wrap;
  background: #f5f7f9;
  border-radius: 4px 14px 14px;
}

.message--user {
  margin-left: auto;
}

.message--user > span {
  text-align: right;
}

.message--user p {
  color: #fff;
  background: var(--app-primary);
  border-radius: 14px 4px 14px 14px;
}

.thinking {
  color: var(--app-text-muted);
}

.thinking i {
  display: inline-block;
  width: 7px;
  height: 7px;
  background: var(--app-primary);
  border-radius: 50%;
  margin-right: 8px;
  animation: pulse 1s infinite alternate;
}

.action-card,
.success-card {
  display: flex;
  padding: 16px;
  align-items: center;
  justify-content: space-between;
  background: #fff8e8;
  border: 1px solid #f2d99b;
  border-radius: 12px;
  gap: 16px;
}

.action-card h3 {
  margin: 8px 0 4px;
}

.action-card p,
.action-card small {
  display: block;
  margin: 0;
  color: var(--app-text-muted);
}

.success-card {
  color: #176946;
  background: #edf8f2;
  border-color: #b9e3cc;
}

.composer {
  padding: 16px;
  border-top: 1px solid var(--app-border-color);
}

.composer textarea {
  width: 100%;
  padding: 12px;
  resize: none;
  border: 1px solid #dfe3e7;
  border-radius: 10px;
  font: inherit;
}

.composer > div {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 8px;
}

.composer span {
  color: var(--app-text-muted);
  font-size: 12px;
}

@keyframes pulse {
  to {
    opacity: 0.25;
    transform: scale(0.7);
  }
}

@media (width <= 900px) {
  .agent-layout {
    grid-template-columns: 1fr;
  }

  .context-panel {
    min-height: auto;
  }

  .guardrail-card {
    margin-top: 8px;
  }
}
</style>
