<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import {
  Button as TButton,
  Loading as TLoading,
  MessagePlugin,
  Tag as TTag,
} from 'tdesign-vue-next'
import {
  createKnowledgePointApi,
  deleteKnowledgePointApi,
  listKnowledgePointsApi,
  pageContentModulesApi,
  updateKnowledgePointApi,
  updateKnowledgePointStatusApi,
} from '@/api/content'
import ContentEditorPanel from '@/components/ContentEditorPanel.vue'
import PageHeader from '@/components/PageHeader.vue'
import type {
  ContentModule,
  EnabledStatus,
  KnowledgePoint,
  KnowledgePointInput,
} from '@/types/content'

interface KnowledgePointForm {
  moduleId: string
  parentId: string
  code: string
  name: string
  description: string
  difficultyLevel: number
  sortOrder: number
  status: EnabledStatus
  version: number
}

const loading = ref(true)
const saving = ref(false)
const modules = ref<ContentModule[]>([])
const points = ref<KnowledgePoint[]>([])
const editingId = ref<string | null>(null)
const editorVisible = ref(false)
const query = reactive({ moduleId: '', keyword: '', status: '' })
const form = reactive<KnowledgePointForm>(emptyForm())
const moduleNames = computed(() => new Map(modules.value.map((item) => [item.id, item.name])))
const parentOptions = computed(() =>
  points.value.filter((item) => item.moduleId === form.moduleId && item.id !== editingId.value),
)

function emptyForm(): KnowledgePointForm {
  return {
    moduleId: query.moduleId || modules.value[0]?.id || '',
    parentId: '',
    code: '',
    name: '',
    description: '',
    difficultyLevel: 1,
    sortOrder: 0,
    status: 0,
    version: 0,
  }
}

async function loadPoints() {
  loading.value = true
  try {
    points.value = await listKnowledgePointsApi({
      moduleId: query.moduleId || undefined,
      keyword: query.keyword.trim() || undefined,
      status: query.status === '' ? undefined : (Number(query.status) as EnabledStatus),
    })
  } finally {
    loading.value = false
  }
}

async function loadModules() {
  const result = await pageContentModulesApi({ page: 1, size: 100 })
  modules.value = result.records
}

function search() {
  void loadPoints()
}

function openCreate() {
  editingId.value = null
  Object.assign(form, emptyForm())
  editorVisible.value = true
}

function openEdit(point: KnowledgePoint) {
  editingId.value = point.id
  Object.assign(form, {
    moduleId: point.moduleId,
    parentId: point.parentId ?? '',
    code: point.code,
    name: point.name,
    description: point.description ?? '',
    difficultyLevel: point.difficultyLevel,
    sortOrder: point.sortOrder,
    status: point.status,
    version: point.version ?? 0,
  })
  editorVisible.value = true
}

async function save() {
  saving.value = true
  try {
    const payload: KnowledgePointInput = {
      parentId: form.parentId || undefined,
      name: form.name.trim(),
      description: form.description.trim() || undefined,
      difficultyLevel: form.difficultyLevel,
      sortOrder: form.sortOrder,
    }
    if (editingId.value) {
      payload.version = form.version
      await updateKnowledgePointApi(editingId.value, payload)
    } else {
      payload.moduleId = form.moduleId
      payload.code = form.code.trim().toUpperCase()
      payload.status = form.status
      await createKnowledgePointApi(payload)
    }
    editorVisible.value = false
    void MessagePlugin.success(editingId.value ? '知识点已更新' : '知识点已创建')
    await loadPoints()
  } finally {
    saving.value = false
  }
}

async function toggleStatus(point: KnowledgePoint) {
  const status: EnabledStatus = point.status === 1 ? 0 : 1
  await updateKnowledgePointStatusApi(point.id, status, point.version ?? 0)
  void MessagePlugin.success(status === 1 ? '知识点已启用' : '知识点已禁用')
  await loadPoints()
}

async function remove(point: KnowledgePoint) {
  if (!window.confirm(`确定删除“${point.name}”吗？有关联内容时服务端会拒绝删除。`)) return
  await deleteKnowledgePointApi(point.id, point.version ?? 0)
  void MessagePlugin.success('知识点已删除')
  await loadPoints()
}

function parentName(point: KnowledgePoint): string {
  if (!point.parentId) return '顶级知识点'
  return points.value.find((item) => item.id === point.parentId)?.name || '上级知识点'
}

onMounted(async () => {
  await loadModules()
  await loadPoints()
})
</script>

<template>
  <div>
    <PageHeader
      eyebrow="教学内容"
      title="知识点"
      description="维护模块内的知识层级、基础难度和展示顺序。"
    >
      <template #actions
        ><t-button :disabled="modules.length === 0" @click="openCreate"
          >新建知识点</t-button
        ></template
      >
    </PageHeader>

    <section class="surface-card">
      <div class="content-toolbar">
        <input v-model="query.keyword" placeholder="搜索编码或名称" @keyup.enter="search" />
        <select v-model="query.moduleId" @change="search">
          <option value="">全部模块</option>
          <option v-for="module in modules" :key="module.id" :value="module.id">
            {{ module.name }}
          </option>
        </select>
        <select v-model="query.status" @change="search">
          <option value="">全部状态</option>
          <option value="1">已启用</option>
          <option value="0">已禁用</option>
        </select>
        <span></span>
        <t-button variant="outline" @click="search">查询</t-button>
      </div>

      <div v-if="loading" class="content-loading"><t-loading text="正在加载知识点" /></div>
      <div v-else-if="points.length" class="content-list">
        <article v-for="point in points" :key="point.id" class="content-row">
          <div class="content-row__main">
            <strong>{{ point.name }}</strong>
            <span>{{ point.code }} · {{ parentName(point) }}</span>
          </div>
          <div class="content-row__meta">
            {{ moduleNames.get(point.moduleId) || point.moduleId }} · 难度
            {{ point.difficultyLevel }}
          </div>
          <div class="content-row__actions">
            <t-tag :theme="point.status === 1 ? 'success' : 'default'" variant="light">
              {{ point.status === 1 ? '已启用' : '已禁用' }}
            </t-tag>
            <t-button size="small" variant="outline" @click="openEdit(point)">编辑</t-button>
            <t-button size="small" variant="text" @click="toggleStatus(point)">
              {{ point.status === 1 ? '禁用' : '启用' }}
            </t-button>
            <t-button size="small" theme="danger" variant="text" @click="remove(point)"
              >删除</t-button
            >
          </div>
        </article>
      </div>
      <div v-else class="content-empty">
        {{ modules.length ? '暂无符合条件的知识点。' : '请先创建内容模块，再维护知识点。' }}
      </div>
    </section>

    <ContentEditorPanel
      v-if="editorVisible"
      :title="editingId ? '编辑知识点' : '新建知识点'"
      description="所属模块和稳定编码创建后不可修改。"
      :busy="saving"
      @close="editorVisible = false"
      @submit="save"
    >
      <div class="editor-grid">
        <label class="editor-field editor-field--wide">
          <span>所属模块</span>
          <select
            v-model="form.moduleId"
            required
            :disabled="Boolean(editingId)"
            @change="form.parentId = ''"
          >
            <option disabled value="">请选择模块</option>
            <option v-for="module in modules" :key="module.id" :value="module.id">
              {{ module.name }}
            </option>
          </select>
        </label>
        <label class="editor-field">
          <span>稳定编码</span>
          <input
            v-model="form.code"
            required
            maxlength="64"
            :disabled="Boolean(editingId)"
            placeholder="G1-S1-ADD-WITHIN-5"
          />
        </label>
        <label class="editor-field"
          ><span>知识点名称</span><input v-model="form.name" required maxlength="100"
        /></label>
        <label class="editor-field editor-field--wide">
          <span>上级知识点</span>
          <select v-model="form.parentId">
            <option value="">无（顶级知识点）</option>
            <option v-for="point in parentOptions" :key="point.id" :value="point.id">
              {{ point.name }}
            </option>
          </select>
        </label>
        <label class="editor-field"
          ><span>基础难度</span
          ><input v-model.number="form.difficultyLevel" type="number" min="1" max="5" required
        /></label>
        <label class="editor-field"
          ><span>展示顺序</span
          ><input v-model.number="form.sortOrder" type="number" min="0" required
        /></label>
        <label v-if="!editingId" class="editor-field">
          <span>初始状态</span>
          <select v-model.number="form.status">
            <option :value="0">禁用</option>
            <option :value="1">启用</option>
          </select>
        </label>
        <label class="editor-field editor-field--wide"
          ><span>知识点说明</span><textarea v-model="form.description" maxlength="1000"></textarea>
        </label>
      </div>
    </ContentEditorPanel>
  </div>
</template>
