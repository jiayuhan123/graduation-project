<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import {
  Button as TButton,
  Loading as TLoading,
  MessagePlugin,
  Tag as TTag,
} from 'tdesign-vue-next'
import {
  createContentModuleApi,
  deleteContentModuleApi,
  pageContentModulesApi,
  updateContentModuleApi,
  updateContentModuleStatusApi,
} from '@/api/content'
import ContentEditorPanel from '@/components/ContentEditorPanel.vue'
import PageHeader from '@/components/PageHeader.vue'
import type { ContentModule, ContentModuleInput, EnabledStatus } from '@/types/content'

interface ModuleForm {
  code: string
  name: string
  gradeLevel: number
  semester: number
  learningTheme: string
  publisher: string
  editionDescription: string
  description: string
  sortOrder: number
  status: EnabledStatus
  version: number
}

const loading = ref(true)
const saving = ref(false)
const modules = ref<ContentModule[]>([])
const total = ref(0)
const pages = ref(0)
const editingId = ref<string | null>(null)
const editorVisible = ref(false)
const query = reactive({ page: 1, size: 12, keyword: '', gradeLevel: '', semester: '', status: '' })
const form = reactive<ModuleForm>(emptyForm())

function emptyForm(): ModuleForm {
  return {
    code: '',
    name: '',
    gradeLevel: 1,
    semester: 1,
    learningTheme: '',
    publisher: '',
    editionDescription: '',
    description: '',
    sortOrder: 0,
    status: 0,
    version: 0,
  }
}

function resetForm(next: ModuleForm) {
  Object.assign(form, next)
}

async function loadModules() {
  loading.value = true
  try {
    const result = await pageContentModulesApi({
      page: query.page,
      size: query.size,
      keyword: query.keyword.trim() || undefined,
      gradeLevel: query.gradeLevel ? Number(query.gradeLevel) : undefined,
      semester: query.semester ? Number(query.semester) : undefined,
      status: query.status === '' ? undefined : (Number(query.status) as EnabledStatus),
    })
    modules.value = result.records
    total.value = result.total
    pages.value = result.pages
    query.page = result.page
  } finally {
    loading.value = false
  }
}

function search() {
  query.page = 1
  void loadModules()
}

function openCreate() {
  editingId.value = null
  resetForm(emptyForm())
  editorVisible.value = true
}

function openEdit(module: ContentModule) {
  editingId.value = module.id
  resetForm({
    code: module.code,
    name: module.name,
    gradeLevel: module.gradeLevel,
    semester: module.semester,
    learningTheme: module.learningTheme,
    publisher: module.publisher ?? '',
    editionDescription: module.editionDescription ?? '',
    description: module.description ?? '',
    sortOrder: module.sortOrder,
    status: module.status,
    version: module.version ?? 0,
  })
  editorVisible.value = true
}

async function save() {
  saving.value = true
  try {
    const payload: ContentModuleInput = {
      name: form.name.trim(),
      gradeLevel: form.gradeLevel,
      semester: form.semester,
      learningTheme: form.learningTheme.trim(),
      publisher: form.publisher.trim() || undefined,
      editionDescription: form.editionDescription.trim() || undefined,
      description: form.description.trim() || undefined,
      sortOrder: form.sortOrder,
    }
    if (editingId.value) {
      payload.version = form.version
      await updateContentModuleApi(editingId.value, payload)
    } else {
      payload.code = form.code.trim().toUpperCase()
      payload.status = form.status
      await createContentModuleApi(payload)
    }
    editorVisible.value = false
    void MessagePlugin.success(editingId.value ? '内容模块已更新' : '内容模块已创建')
    await loadModules()
  } finally {
    saving.value = false
  }
}

async function toggleStatus(module: ContentModule) {
  const status: EnabledStatus = module.status === 1 ? 0 : 1
  await updateContentModuleStatusApi(module.id, status, module.version ?? 0)
  void MessagePlugin.success(status === 1 ? '模块已启用' : '模块已禁用')
  await loadModules()
}

async function remove(module: ContentModule) {
  if (!window.confirm(`确定删除“${module.name}”吗？模块下存在知识点时服务端会拒绝删除。`)) return
  await deleteContentModuleApi(module.id, module.version ?? 0)
  void MessagePlugin.success('内容模块已删除')
  await loadModules()
}

function changePage(delta: number) {
  const next = query.page + delta
  if (next < 1 || next > pages.value) return
  query.page = next
  void loadModules()
}

onMounted(loadModules)
</script>

<template>
  <div>
    <PageHeader
      eyebrow="CONTENT"
      title="内容模块"
      :description="`按年级、学期和学习主题维护内容目录，当前共 ${total} 条。`"
    >
      <template #actions><t-button @click="openCreate">新建模块</t-button></template>
    </PageHeader>

    <section class="surface-card">
      <div class="content-toolbar">
        <input
          v-model="query.keyword"
          placeholder="搜索编码、名称或学习主题"
          @keyup.enter="search"
        />
        <select v-model="query.gradeLevel" @change="search">
          <option value="">全部年级</option>
          <option v-for="grade in 6" :key="grade" :value="grade">{{ grade }} 年级</option>
        </select>
        <select v-model="query.semester" @change="search">
          <option value="">全部学期</option>
          <option value="1">上学期</option>
          <option value="2">下学期</option>
        </select>
        <select v-model="query.status" @change="search">
          <option value="">全部状态</option>
          <option value="1">已启用</option>
          <option value="0">已禁用</option>
        </select>
        <t-button variant="outline" @click="search">查询</t-button>
      </div>

      <div v-if="loading" class="content-loading"><t-loading text="正在加载内容模块" /></div>
      <div v-else-if="modules.length" class="content-list">
        <article v-for="module in modules" :key="module.id" class="content-row">
          <div class="content-row__main">
            <strong>{{ module.name }}</strong>
            <span>{{ module.code }} · {{ module.learningTheme }}</span>
          </div>
          <div class="content-row__meta">
            {{ module.gradeLevel }} 年级{{ module.semester === 1 ? '上' : '下' }}学期 ·
            {{ module.publisher || '通用内容' }}
          </div>
          <div class="content-row__actions">
            <t-tag :theme="module.status === 1 ? 'success' : 'default'" variant="light">
              {{ module.status === 1 ? '已启用' : '已禁用' }}
            </t-tag>
            <t-button size="small" variant="outline" @click="openEdit(module)">编辑</t-button>
            <t-button size="small" variant="text" @click="toggleStatus(module)">
              {{ module.status === 1 ? '禁用' : '启用' }}
            </t-button>
            <t-button size="small" theme="danger" variant="text" @click="remove(module)"
              >删除</t-button
            >
          </div>
        </article>
      </div>
      <div v-else class="content-empty">暂无内容模块，后续可在这里创建或导入。</div>

      <footer class="content-pagination">
        <span>第 {{ query.page }} / {{ Math.max(pages, 1) }} 页</span>
        <div>
          <t-button
            size="small"
            variant="outline"
            :disabled="query.page <= 1"
            @click="changePage(-1)"
          >
            上一页
          </t-button>
          <t-button
            size="small"
            variant="outline"
            :disabled="query.page >= pages"
            @click="changePage(1)"
          >
            下一页
          </t-button>
        </div>
      </footer>
    </section>

    <ContentEditorPanel
      v-if="editorVisible"
      :title="editingId ? '编辑内容模块' : '新建内容模块'"
      description="稳定编码创建后不可修改；状态切换在列表中单独完成。"
      :busy="saving"
      @close="editorVisible = false"
      @submit="save"
    >
      <div class="editor-grid">
        <label class="editor-field">
          <span>稳定编码</span>
          <input
            v-model="form.code"
            required
            maxlength="64"
            :disabled="Boolean(editingId)"
            placeholder="PEP-2022-G1-S1-ADD-5"
          />
        </label>
        <label class="editor-field">
          <span>模块名称</span>
          <input v-model="form.name" required maxlength="100" />
        </label>
        <label class="editor-field">
          <span>年级</span>
          <select v-model.number="form.gradeLevel">
            <option v-for="grade in 6" :key="grade" :value="grade">{{ grade }} 年级</option>
          </select>
        </label>
        <label class="editor-field">
          <span>学期</span>
          <select v-model.number="form.semester">
            <option :value="1">上学期</option>
            <option :value="2">下学期</option>
          </select>
        </label>
        <label class="editor-field editor-field--wide">
          <span>学习主题</span>
          <input v-model="form.learningTheme" required maxlength="100" />
        </label>
        <label class="editor-field"
          ><span>出版社</span><input v-model="form.publisher" maxlength="100"
        /></label>
        <label class="editor-field"
          ><span>版本说明</span><input v-model="form.editionDescription" maxlength="100"
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
          ><span>模块说明</span><textarea v-model="form.description" maxlength="500"></textarea>
        </label>
      </div>
    </ContentEditorPanel>
  </div>
</template>
