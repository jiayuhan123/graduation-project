<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import {
  Button as TButton,
  Loading as TLoading,
  MessagePlugin,
  Tag as TTag,
} from 'tdesign-vue-next'
import {
  acceptRecommendationApi,
  dismissRecommendationApi,
  getStudentLearningReportApi,
  pageRecommendationsApi,
} from '@/api/learning'
import PageHeader from '@/components/PageHeader.vue'
import StudentSelector from '@/components/StudentSelector.vue'
import { useUserStore } from '@/stores/user'
import type { RecommendationItem } from '@/types/learning'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const operatingId = ref('')
const studentId = ref('')
const records = ref<RecommendationItem[]>([])
const isStudent = computed(() => userStore.activeRole === 'student')

async function load() {
  const normalizedStudentId = studentId.value.trim()
  if (!isStudent.value && !normalizedStudentId) {
    records.value = []
    return
  }
  loading.value = true
  try {
    if (isStudent.value) {
      records.value = (
        await pageRecommendationsApi({ page: 1, size: 100, status: 'PENDING' })
      ).records
    } else {
      records.value = (await getStudentLearningReportApi(normalizedStudentId)).recommendations
    }
  } finally {
    loading.value = false
  }
}

async function accept(item: RecommendationItem) {
  operatingId.value = item.id
  try {
    const detail = await acceptRecommendationApi(item.id)
    void MessagePlugin.success('巩固练习已创建')
    await router.push({ path: '/learning/practice', query: { taskId: detail.task.id } })
  } finally {
    operatingId.value = ''
  }
}

async function dismiss(item: RecommendationItem) {
  if (!window.confirm('确定暂时忽略这条巩固推荐吗？')) return
  operatingId.value = item.id
  try {
    await dismissRecommendationApi(item.id)
    records.value = records.value.filter((record) => record.id !== item.id)
    void MessagePlugin.success('推荐已忽略')
  } finally {
    operatingId.value = ''
  }
}

watch(
  () => userStore.activeRole,
  () => {
    studentId.value = ''
    records.value = []
    if (isStudent.value) void load()
  },
)
onMounted(() => {
  if (isStudent.value) void load()
})
</script>

<template>
  <div>
    <PageHeader
      eyebrow="RECOMMENDATION"
      title="巩固推荐"
      :description="
        isStudent
          ? '推荐来自真实错题和掌握度变化，只使用数据库中的固定题目。'
          : '教师和管理员可按学生查看推荐，但不能代替学生接受或忽略。'
      "
    />

    <section v-if="!isStudent" class="surface-card recommendation-query">
      <StudentSelector v-model="studentId" @change="load" />
    </section>

    <div v-if="loading" class="surface-card content-loading">
      <t-loading text="正在读取巩固推荐" />
    </div>
    <section v-else-if="records.length" class="recommendation-grid">
      <article v-for="item in records" :key="item.id" class="surface-card recommendation-card">
        <header>
          <t-tag theme="warning" variant="light">优先级 {{ item.priority }}</t-tag>
          <span>{{ item.recommendedQuestionCount }} 题</span>
        </header>
        <strong>知识点 #{{ item.knowledgePointId }}</strong>
        <p>{{ item.reasonText }}</p>
        <small v-if="item.expiresAt"
          >有效期至 {{ new Date(item.expiresAt).toLocaleString() }}</small
        >
        <footer v-if="isStudent">
          <t-button variant="outline" :disabled="Boolean(operatingId)" @click="dismiss(item)">
            暂时忽略
          </t-button>
          <t-button
            :loading="operatingId === item.id"
            :disabled="Boolean(operatingId)"
            @click="accept(item)"
          >
            开始巩固
          </t-button>
        </footer>
      </article>
    </section>
    <div v-else class="surface-card content-empty">
      {{
        !isStudent && !studentId.trim() ? '输入学生编号后查询推荐。' : '当前没有待处理的巩固推荐。'
      }}
    </div>
  </div>
</template>

<style scoped lang="scss">
.recommendation-query {
  padding: 22px;
}

.recommendation-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.recommendation-card {
  padding: 24px;
}

.recommendation-card header,
.recommendation-card footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.recommendation-card header span,
.recommendation-card small {
  color: var(--app-text-muted);
  font-size: 11px;
}

.recommendation-card > strong {
  display: block;
  margin-top: 20px;
  color: var(--app-text-strong);
  font-size: 18px;
}

.recommendation-card p {
  min-height: 52px;
  margin-top: 10px;
  color: var(--app-text);
  line-height: 1.7;
}

.recommendation-card small {
  display: block;
  margin-top: 10px;
}

.recommendation-card footer {
  margin-top: 20px;
  justify-content: flex-end;
}

@media (width <= 720px) {
  .recommendation-query {
    align-items: stretch;
    flex-direction: column;
  }

  .recommendation-grid {
    grid-template-columns: 1fr;
  }
}
</style>
