<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { pageAdminUsersApi } from '@/api/admin'
import { getTeacherClassApi, pageTeacherClassesApi } from '@/api/teaching'
import { useUserStore } from '@/stores/user'

interface StudentOption {
  id: string
  label: string
  secondary: string
}

const props = defineProps<{ modelValue: string }>()
const emit = defineEmits<{ 'update:modelValue': [value: string]; change: [value: string] }>()
const userStore = useUserStore()
const loading = ref(false)
const keyword = ref('')
const options = ref<StudentOption[]>([])

const filteredOptions = computed(() => {
  const normalized = keyword.value.trim().toLowerCase()
  if (!normalized) return options.value
  return options.value.filter((item) =>
    `${item.label} ${item.secondary} ${item.id}`.toLowerCase().includes(normalized),
  )
})

function displayName(item: {
  displayName?: string | null
  username?: string | null
  id?: string
  studentId?: string
}): string {
  return item.displayName || item.username || `学生 ${item.id ?? item.studentId}`
}

async function loadTeacherStudents(): Promise<StudentOption[]> {
  const memberships = (await pageTeacherClassesApi({ page: 1, size: 100 })).records.filter(
    (item) => item.classStatus === 'ACTIVE' && item.membershipStatus === 'ACTIVE',
  )
  const details = await Promise.all(memberships.map((item) => getTeacherClassApi(item.classId)))
  const byId = new Map<string, StudentOption>()
  for (const detail of details) {
    for (const student of detail.students.filter((item) => item.status === 'ACTIVE')) {
      byId.set(student.studentId, {
        id: student.studentId,
        label: displayName(student),
        secondary: `${detail.classInfo.name}${student.studentNoInClass ? ` · ${student.studentNoInClass}` : ''}`,
      })
    }
  }
  return [...byId.values()].sort((left, right) => left.label.localeCompare(right.label, 'zh-CN'))
}

async function loadAdminStudents(): Promise<StudentOption[]> {
  const result: StudentOption[] = []
  let page = 1
  while (true) {
    const response = await pageAdminUsersApi({ page, size: 100, roleCode: 'student', status: 1 })
    result.push(
      ...response.records.map((student) => ({
        id: student.id,
        label: displayName({
          id: student.id,
          username: student.username,
          displayName: student.realName || student.nickname,
        }),
        secondary: student.studentProfile?.studentNo
          ? `学号 ${student.studentProfile.studentNo}`
          : `ID ${student.id}`,
      })),
    )
    if (page >= response.pages) break
    page += 1
  }
  return result
}

async function load() {
  if (userStore.activeRole !== 'teacher' && userStore.activeRole !== 'admin') return
  loading.value = true
  try {
    options.value =
      userStore.activeRole === 'teacher' ? await loadTeacherStudents() : await loadAdminStudents()
    if (!props.modelValue && options.value[0]) {
      emit('update:modelValue', options.value[0].id)
      emit('change', options.value[0].id)
    }
  } finally {
    loading.value = false
  }
}

function select(event: Event) {
  const value = (event.target as HTMLSelectElement).value
  emit('update:modelValue', value)
  emit('change', value)
}

watch(
  () => userStore.activeRole,
  () => void load(),
)
onMounted(() => void load())
</script>

<template>
  <div class="student-selector">
    <label>
      <span>查找学生</span>
      <input v-model="keyword" placeholder="姓名、账号、学号或 ID" />
    </label>
    <label>
      <span>选择学生</span>
      <select :value="modelValue" :disabled="loading || !filteredOptions.length" @change="select">
        <option value="">{{ loading ? '正在加载学生…' : '请选择学生' }}</option>
        <option v-for="item in filteredOptions" :key="item.id" :value="item.id">
          {{ item.label }} · {{ item.secondary }}
        </option>
      </select>
    </label>
    <small v-if="!loading && !options.length">
      {{ userStore.activeRole === 'teacher' ? '当前有效班级中还没有学生。' : '暂无可用学生账号。' }}
    </small>
    <small v-else>共 {{ options.length }} 名可访问学生</small>
  </div>
</template>

<style scoped lang="scss">
.student-selector {
  display: grid;
  align-items: end;
  grid-template-columns: minmax(180px, 280px) minmax(260px, 1fr) auto;
  gap: 12px;
}

.student-selector label {
  display: grid;
  gap: 7px;
}

.student-selector label span {
  color: var(--app-text-muted);
  font-size: 12px;
  font-weight: 700;
}

.student-selector input,
.student-selector select {
  width: 100%;
  min-height: 42px;
  padding: 9px 12px;
  border: 1px solid #dfe3e7;
  border-radius: 10px;
  background: #fff;
  color: var(--app-text-strong);
  font: inherit;
}

.student-selector small {
  align-self: center;
  color: var(--app-text-muted);
}

@media (width <= 720px) {
  .student-selector {
    grid-template-columns: 1fr;
  }
}
</style>
