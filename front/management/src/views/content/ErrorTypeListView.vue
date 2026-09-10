<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import {
  Button as TButton,
  Loading as TLoading,
  MessagePlugin,
  Tag as TTag,
} from 'tdesign-vue-next'
import {
  createErrorTypeApi,
  deleteErrorTypeApi,
  listErrorTypesApi,
  updateErrorTypeApi,
  updateErrorTypeStatusApi,
} from '@/api/content'
import ContentEditorPanel from '@/components/ContentEditorPanel.vue'
import PageHeader from '@/components/PageHeader.vue'
import type {
  EnabledStatus,
  ErrorCategory,
  ErrorType,
  ErrorTypeInput,
  QuestionType,
} from '@/types/content'

interface ErrorTypeForm {
  code: string
  name: string
  category: ErrorCategory
  applicableQuestionType: QuestionType | ''
  description: string
  explanationTemplate: string
  remediationHint: string
  status: EnabledStatus
  version: number
}

const categoryLabels: Record<ErrorCategory, string> = {
  CALCULATION: '计算错误',
  PLACE_VALUE: '数位错误',
  CARRY: '进位错误',
  BORROW: '退位错误',
  FORMAT: '格式错误',
  CONCEPT: '概念错误',
  OTHER: '其他',
}
const questionTypeLabels: Record<QuestionType, string> = {
  ARITHMETIC: '口算',
  FILL_BLANK: '填空',
  VERTICAL: '竖式',
  CHOICE: '选择',
  JUDGEMENT: '判断',
}

const loading = ref(true)
const saving = ref(false)
const errorTypes = ref<ErrorType[]>([])
const editingId = ref<string | null>(null)
const editorVisible = ref(false)
const query = reactive({ keyword: '', category: '', status: '' })
const form = reactive<ErrorTypeForm>(emptyForm())

function emptyForm(): ErrorTypeForm {
  return {
    code: '',
    name: '',
    category: 'CALCULATION',
    applicableQuestionType: '',
    description: '',
    explanationTemplate: '',
    remediationHint: '',
    status: 0,
    version: 0,
  }
}

async function loadErrorTypes() {
  loading.value = true
  try {
    errorTypes.value = await listErrorTypesApi({
      keyword: query.keyword.trim() || undefined,
      category: (query.category || undefined) as ErrorCategory | undefined,
      status: query.status === '' ? undefined : (Number(query.status) as EnabledStatus),
    })
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingId.value = null
  Object.assign(form, emptyForm())
  editorVisible.value = true
}

function openEdit(errorType: ErrorType) {
  editingId.value = errorType.id
  Object.assign(form, {
    code: errorType.code,
    name: errorType.name,
    category: errorType.category,
    applicableQuestionType: errorType.applicableQuestionType ?? '',
    description: errorType.description ?? '',
    explanationTemplate: errorType.explanationTemplate ?? '',
    remediationHint: errorType.remediationHint ?? '',
    status: errorType.status,
    version: errorType.version,
  })
  editorVisible.value = true
}

async function save() {
  saving.value = true
  try {
    const payload: ErrorTypeInput = {
      name: form.name.trim(),
      category: form.category,
      applicableQuestionType: form.applicableQuestionType || undefined,
      description: form.description.trim() || undefined,
      explanationTemplate: form.explanationTemplate.trim() || undefined,
      remediationHint: form.remediationHint.trim() || undefined,
    }
    if (editingId.value) {
      payload.version = form.version
      await updateErrorTypeApi(editingId.value, payload)
    } else {
      payload.code = form.code.trim().toUpperCase()
      payload.status = form.status
      await createErrorTypeApi(payload)
    }
    editorVisible.value = false
    void MessagePlugin.success(editingId.value ? '错因类型已更新' : '错因类型已创建')
    await loadErrorTypes()
  } finally {
    saving.value = false
  }
}

async function toggleStatus(errorType: ErrorType) {
  const status: EnabledStatus = errorType.status === 1 ? 0 : 1
  await updateErrorTypeStatusApi(errorType.id, status, errorType.version)
  void MessagePlugin.success(status === 1 ? '错因已启用' : '错因已禁用')
  await loadErrorTypes()
}

async function remove(errorType: ErrorType) {
  if (!window.confirm(`确定删除错因“${errorType.name}”吗？`)) return
  await deleteErrorTypeApi(errorType.id, errorType.version)
  void MessagePlugin.success('错因类型已删除')
  await loadErrorTypes()
}

onMounted(loadErrorTypes)
</script>

<template>
  <div>
    <PageHeader eyebrow="教学内容" title="错因字典" description="维护可解释错因和针对性补救提示。">
      <template #actions><t-button @click="openCreate">新建错因</t-button></template>
    </PageHeader>

    <section class="surface-card">
      <div class="content-toolbar">
        <input v-model="query.keyword" placeholder="搜索编码或名称" @keyup.enter="loadErrorTypes" />
        <select v-model="query.category" @change="loadErrorTypes">
          <option value="">全部分类</option>
          <option v-for="(label, code) in categoryLabels" :key="code" :value="code">
            {{ label }}
          </option>
        </select>
        <select v-model="query.status" @change="loadErrorTypes">
          <option value="">全部状态</option>
          <option value="1">已启用</option>
          <option value="0">已禁用</option>
        </select>
        <span></span>
        <t-button variant="outline" @click="loadErrorTypes">查询</t-button>
      </div>

      <div v-if="loading" class="content-loading"><t-loading text="正在加载错因字典" /></div>
      <div v-else-if="errorTypes.length" class="content-list">
        <article v-for="errorType in errorTypes" :key="errorType.id" class="content-row">
          <div class="content-row__main">
            <strong>{{ errorType.name }}</strong>
            <span>{{ errorType.code }} · {{ categoryLabels[errorType.category] }}</span>
          </div>
          <div class="content-row__meta">
            {{
              errorType.applicableQuestionType
                ? questionTypeLabels[errorType.applicableQuestionType]
                : '全部题型'
            }}
          </div>
          <div class="content-row__actions">
            <t-tag :theme="errorType.status === 1 ? 'success' : 'default'" variant="light">{{
              errorType.status === 1 ? '已启用' : '已禁用'
            }}</t-tag>
            <t-button size="small" variant="outline" @click="openEdit(errorType)">编辑</t-button>
            <t-button size="small" variant="text" @click="toggleStatus(errorType)">{{
              errorType.status === 1 ? '禁用' : '启用'
            }}</t-button>
            <t-button size="small" theme="danger" variant="text" @click="remove(errorType)"
              >删除</t-button
            >
          </div>
        </article>
      </div>
      <div v-else class="content-empty">暂无错因类型，后续可在这里创建或导入。</div>
    </section>

    <ContentEditorPanel
      v-if="editorVisible"
      :title="editingId ? '编辑错因类型' : '新建错因类型'"
      description="解释模板面向学生展示，请避免使用指责性语言。"
      :busy="saving"
      @close="editorVisible = false"
      @submit="save"
    >
      <div class="editor-grid">
        <label class="editor-field"
          ><span>稳定编码</span
          ><input
            v-model="form.code"
            required
            maxlength="64"
            :disabled="Boolean(editingId)"
            placeholder="CARRY_MISSED"
        /></label>
        <label class="editor-field"
          ><span>错因名称</span><input v-model="form.name" required maxlength="100"
        /></label>
        <label class="editor-field">
          <span>错因分类</span>
          <select v-model="form.category">
            <option v-for="(label, code) in categoryLabels" :key="code" :value="code">
              {{ label }}
            </option>
          </select>
        </label>
        <label class="editor-field">
          <span>适用题型</span>
          <select v-model="form.applicableQuestionType">
            <option value="">全部题型</option>
            <option v-for="(label, code) in questionTypeLabels" :key="code" :value="code">
              {{ label }}
            </option>
          </select>
        </label>
        <label v-if="!editingId" class="editor-field">
          <span>初始状态</span
          ><select v-model.number="form.status">
            <option :value="0">禁用</option>
            <option :value="1">启用</option>
          </select>
        </label>
        <label class="editor-field editor-field--wide"
          ><span>判定说明</span><textarea v-model="form.description" maxlength="500"></textarea>
        </label>
        <label class="editor-field editor-field--wide"
          ><span>学生解释模板</span
          ><textarea v-model="form.explanationTemplate" maxlength="1000"></textarea>
        </label>
        <label class="editor-field editor-field--wide"
          ><span>补救练习提示</span
          ><textarea v-model="form.remediationHint" maxlength="1000"></textarea>
        </label>
      </div>
    </ContentEditorPanel>
  </div>
</template>
