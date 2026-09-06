<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import {
  Button as TButton,
  Loading as TLoading,
  MessagePlugin,
  Tag as TTag,
} from 'tdesign-vue-next'
import {
  addClassStudentApi,
  createTeachingClassApi,
  getTeacherClassApi,
  joinTeachingClassApi,
  leaveTeachingClassApi,
  pageStudentClassesApi,
  pageTeacherClassesApi,
  removeClassStudentApi,
  updateTeachingClassApi,
  updateTeachingClassStatusApi,
} from '@/api/teaching'
import ContentEditorPanel from '@/components/ContentEditorPanel.vue'
import PageHeader from '@/components/PageHeader.vue'
import { useUserStore } from '@/stores/user'
import type { ClassDetail, ClassInput, ClassMembership } from '@/types/teaching'

const userStore = useUserStore()
const isTeacher = computed(() => userStore.activeRole === 'teacher')
const loading = ref(true)
const saving = ref(false)
const classes = ref<ClassMembership[]>([])
const total = ref(0)
const pages = ref(0)
const page = ref(1)
const editorVisible = ref(false)
const editing = ref<ClassMembership | null>(null)
const detail = ref<ClassDetail | null>(null)
const detailVisible = ref(false)
const joinCode = ref('')
const joinStudentNo = ref('')
const studentId = ref('')
const studentNo = ref('')
const currentYear = new Date().getFullYear()
const form = reactive<ClassInput>({
  name: '',
  schoolName: '',
  academicYear: `${currentYear}-${currentYear + 1}`,
  gradeLevel: 1,
  semester: 1,
})

function resetForm() {
  Object.assign(form, {
    name: '',
    schoolName: '',
    academicYear: `${currentYear}-${currentYear + 1}`,
    gradeLevel: 1,
    semester: 1,
    version: undefined,
  })
}

async function loadClasses() {
  loading.value = true
  try {
    const result = isTeacher.value
      ? await pageTeacherClassesApi({ page: page.value, size: 12 })
      : await pageStudentClassesApi({ page: page.value, size: 12 })
    classes.value = result.records
    total.value = result.total
    pages.value = result.pages
    page.value = result.page
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editing.value = null
  resetForm()
  editorVisible.value = true
}
function openEdit(item: ClassMembership) {
  editing.value = item
  Object.assign(form, {
    name: item.name,
    schoolName: item.schoolName ?? '',
    academicYear: item.academicYear,
    gradeLevel: item.gradeLevel,
    semester: item.semester,
    version: item.version,
  })
  editorVisible.value = true
}

async function saveClass() {
  saving.value = true
  try {
    const payload = { ...form, schoolName: form.schoolName?.trim() || undefined }
    if (editing.value) await updateTeachingClassApi(editing.value.classId, payload)
    else await createTeachingClassApi(payload)
    editorVisible.value = false
    void MessagePlugin.success(editing.value ? '班级资料已更新' : '班级已创建')
    await loadClasses()
  } finally {
    saving.value = false
  }
}

async function toggleStatus(item: ClassMembership) {
  const next = item.classStatus === 'ACTIVE' ? 'ARCHIVED' : 'ACTIVE'
  if (next === 'ARCHIVED' && !window.confirm(`确定归档“${item.name}”吗？`)) return
  await updateTeachingClassStatusApi(item.classId, next, item.version)
  void MessagePlugin.success(next === 'ACTIVE' ? '班级已恢复' : '班级已归档')
  await loadClasses()
}

async function openDetail(item: ClassMembership) {
  detail.value = await getTeacherClassApi(item.classId)
  studentId.value = ''
  studentNo.value = ''
  detailVisible.value = true
}

async function addStudent() {
  if (!detail.value || !studentId.value.trim()) return
  await addClassStudentApi(
    detail.value.classInfo.classId,
    studentId.value.trim(),
    studentNo.value.trim() || undefined,
  )
  detail.value = await getTeacherClassApi(detail.value.classInfo.classId)
  studentId.value = ''
  studentNo.value = ''
  void MessagePlugin.success('学生已加入班级')
}

async function removeStudent(student: ClassDetail['students'][number]) {
  if (
    !detail.value ||
    !window.confirm(`确定移出“${student.displayName || student.username || student.studentId}”吗？`)
  )
    return
  await removeClassStudentApi(detail.value.classInfo.classId, student.studentId)
  detail.value = await getTeacherClassApi(detail.value.classInfo.classId)
  void MessagePlugin.success('学生已移出')
}

async function joinClass() {
  const classCode = joinCode.value.trim().toUpperCase()
  if (!classCode) return
  await joinTeachingClassApi(classCode, joinStudentNo.value.trim() || undefined)
  joinCode.value = ''
  joinStudentNo.value = ''
  void MessagePlugin.success('加入班级成功')
  await loadClasses()
}

async function leaveClass(item: ClassMembership) {
  if (!window.confirm(`确定退出“${item.name}”吗？`)) return
  await leaveTeachingClassApi(item.classId)
  void MessagePlugin.success('已退出班级')
  await loadClasses()
}

function changePage(delta: number) {
  const next = page.value + delta
  if (next < 1 || next > pages.value) return
  page.value = next
  void loadClasses()
}

watch(
  () => userStore.activeRole,
  () => {
    page.value = 1
    void loadClasses()
  },
)
onMounted(loadClasses)
</script>

<template>
  <div>
    <PageHeader
      eyebrow="TEACHING"
      :title="isTeacher ? '班级管理' : '我的班级'"
      :description="
        isTeacher
          ? `管理班级与在班学生，当前共 ${total} 个班级。`
          : '使用教师提供的班级码加入班级，已发布作业会出现在作业中心。'
      "
    >
      <template v-if="isTeacher" #actions
        ><t-button @click="openCreate">创建班级</t-button></template
      >
    </PageHeader>

    <form v-if="!isTeacher" class="surface-card join-card" @submit.prevent="joinClass">
      <div><strong>加入新班级</strong><span>班级码由任课教师提供</span></div>
      <input v-model="joinCode" maxlength="12" required placeholder="输入 6—12 位班级码" />
      <input v-model="joinStudentNo" maxlength="32" placeholder="班内学号（选填）" />
      <t-button type="submit">加入班级</t-button>
    </form>

    <section class="surface-card">
      <div v-if="loading" class="content-loading"><t-loading text="正在加载班级" /></div>
      <div v-else-if="classes.length" class="class-grid">
        <article v-for="item in classes" :key="item.classId" class="class-card">
          <div class="class-card__top">
            <span class="class-code">{{ item.classCode }}</span
            ><t-tag
              :theme="
                item.classStatus === 'ACTIVE' && item.membershipStatus === 'ACTIVE'
                  ? 'success'
                  : 'default'
              "
              variant="light"
              >{{
                item.membershipStatus === 'LEFT'
                  ? '已退出'
                  : item.classStatus === 'ACTIVE'
                    ? '进行中'
                    : '已归档'
              }}</t-tag
            >
          </div>
          <h2>{{ item.name }}</h2>
          <p>{{ item.schoolName || '未填写学校' }}</p>
          <div class="class-card__meta">
            <span>{{ item.academicYear }} 学年</span
            ><span
              >{{ item.gradeLevel }} 年级 · {{ item.semester === 1 ? '上学期' : '下学期' }}</span
            >
          </div>
          <div class="class-card__actions">
            <template v-if="isTeacher"
              ><t-button size="small" variant="outline" @click="openDetail(item)">学生名单</t-button
              ><t-button size="small" variant="text" @click="openEdit(item)">编辑</t-button
              ><t-button size="small" variant="text" @click="toggleStatus(item)">{{
                item.classStatus === 'ACTIVE' ? '归档' : '恢复'
              }}</t-button></template
            >
            <t-button
              v-else-if="item.membershipStatus === 'ACTIVE'"
              size="small"
              theme="danger"
              variant="text"
              @click="leaveClass(item)"
              >退出班级</t-button
            >
          </div>
        </article>
      </div>
      <div v-else class="content-empty">
        {{ isTeacher ? '还没有班级，创建第一个教学班吧。' : '还没有加入班级。' }}
      </div>
      <footer class="content-pagination">
        <span>第 {{ page }} / {{ Math.max(pages, 1) }} 页</span>
        <div>
          <t-button size="small" variant="outline" :disabled="page <= 1" @click="changePage(-1)"
            >上一页</t-button
          ><t-button size="small" variant="outline" :disabled="page >= pages" @click="changePage(1)"
            >下一页</t-button
          >
        </div>
      </footer>
    </section>

    <ContentEditorPanel
      v-if="editorVisible"
      :title="editing ? '编辑班级' : '创建班级'"
      :busy="saving"
      @close="editorVisible = false"
      @submit="saveClass"
    >
      <div class="editor-grid">
        <label class="editor-field editor-field--wide"
          ><span>班级名称</span
          ><input v-model="form.name" required maxlength="100" placeholder="例如：一年级 2 班"
        /></label>
        <label class="editor-field editor-field--wide"
          ><span>学校名称</span><input v-model="form.schoolName" maxlength="150" placeholder="选填"
        /></label>
        <label class="editor-field"
          ><span>学年</span><input v-model="form.academicYear" required pattern="[0-9]{4}-[0-9]{4}"
        /></label>
        <label class="editor-field"
          ><span>年级</span
          ><select v-model.number="form.gradeLevel">
            <option v-for="grade in 6" :key="grade" :value="grade">{{ grade }} 年级</option>
          </select></label
        >
        <label class="editor-field"
          ><span>学期</span
          ><select v-model.number="form.semester">
            <option :value="1">上学期</option>
            <option :value="2">下学期</option>
          </select></label
        >
      </div>
    </ContentEditorPanel>

    <ContentEditorPanel
      v-if="detailVisible && detail"
      title="学生名单"
      :description="`${detail.classInfo.name} · ${detail.students.filter((item) => item.status === 'ACTIVE').length} 名在班学生`"
      submit-text="关闭"
      @close="detailVisible = false"
      @submit="detailVisible = false"
    >
      <div class="student-adder">
        <input v-model="studentId" inputmode="numeric" required placeholder="学生用户 ID" /><input
          v-model="studentNo"
          maxlength="32"
          placeholder="班内学号（选填）"
        /><t-button type="button" size="small" @click="addStudent">添加</t-button>
      </div>
      <div class="student-list">
        <div v-for="student in detail.students" :key="student.studentId" class="student-row">
          <div>
            <strong>{{
              student.displayName || student.username || `学生 ${student.studentId}`
            }}</strong
            ><span>{{ student.studentNoInClass || `用户 ID ${student.studentId}` }}</span>
          </div>
          <t-tag :theme="student.status === 'ACTIVE' ? 'success' : 'default'" variant="light">{{
            student.status === 'ACTIVE' ? '在班' : '已退出'
          }}</t-tag
          ><t-button
            v-if="student.status === 'ACTIVE'"
            size="small"
            theme="danger"
            variant="text"
            @click="removeStudent(student)"
            >移出</t-button
          >
        </div>
        <p v-if="!detail.students.length" class="muted">暂无学生。</p>
      </div>
    </ContentEditorPanel>
  </div>
</template>

<style scoped lang="scss">
.join-card {
  display: grid;
  margin-bottom: 18px;
  padding: 18px;
  align-items: center;
  grid-template-columns: 1fr 220px 220px auto;
  gap: 12px;
}

.join-card strong,
.join-card span {
  display: block;
}

.join-card span {
  margin-top: 4px;
  color: var(--app-text-muted);
  font-size: 12px;
}

.join-card input,
.student-adder input {
  min-height: 40px;
  padding: 9px 12px;
  border: 1px solid #dfe3e7;
  border-radius: 10px;
  font: inherit;
}

.class-grid {
  display: grid;
  padding: 22px;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 14px;
}

.class-card {
  padding: 20px;
  background: linear-gradient(145deg, #fff, #fffaf7);
  border: 1px solid #eceef0;
  border-radius: 19px;
}

.class-card__top,
.class-card__actions,
.class-card__meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.class-code {
  color: #dc691f;
  font-size: 12px;
  font-weight: 750;
  letter-spacing: 0.12em;
}

.class-card h2 {
  margin-top: 20px;
  color: var(--app-text-strong);
  font-size: 19px;
}

.class-card p {
  margin-top: 6px;
  color: var(--app-text-muted);
}

.class-card__meta {
  margin-top: 22px;
  align-items: flex-start;
  color: var(--app-text-muted);
  font-size: 12px;
}

.class-card__actions {
  margin-top: 20px;
  justify-content: flex-end;
  flex-wrap: wrap;
}

.student-adder {
  display: grid;
  margin-bottom: 16px;
  grid-template-columns: 1fr 1fr auto;
  gap: 8px;
}

.student-list {
  display: grid;
  gap: 8px;
}

.student-row {
  display: grid;
  padding: 12px;
  align-items: center;
  background: #f8f9fb;
  border-radius: 12px;
  grid-template-columns: 1fr auto auto;
  gap: 8px;
}

.student-row strong,
.student-row span {
  display: block;
}

.student-row span {
  margin-top: 3px;
  color: var(--app-text-muted);
  font-size: 11px;
}

@media (width <= 900px) {
  .join-card {
    grid-template-columns: 1fr 1fr;
  }
}

@media (width <= 640px) {
  .join-card,
  .student-adder {
    grid-template-columns: 1fr;
  }

  .class-grid {
    padding: 14px;
    grid-template-columns: 1fr;
  }
}
</style>
