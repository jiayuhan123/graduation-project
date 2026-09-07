<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { Loading as TLoading, Tag as TTag } from 'tdesign-vue-next'
import { pagePhotoTasksApi } from '@/api/learning'
import PageHeader from '@/components/PageHeader.vue'
import { useUserStore } from '@/stores/user'
import type { PhotoTask } from '@/types/learning'

const userStore = useUserStore()
const loading = ref(false)
const records = ref<PhotoTask[]>([])
const isStudent = computed(() => userStore.activeRole === 'student')

async function loadExistingRecords() {
  if (!isStudent.value) return
  loading.value = true
  try {
    records.value = (await pagePhotoTasksApi({ page: 1, size: 100 })).records
  } finally {
    loading.value = false
  }
}

watch(
  () => userStore.activeRole,
  () => {
    records.value = []
    if (isStudent.value) void loadExistingRecords()
  },
)
onMounted(() => {
  if (isStudent.value) void loadExistingRecords()
})
</script>

<template>
  <div>
    <PageHeader
      eyebrow="PHOTO REVIEW"
      title="拍照批改"
      description="本轮不开发 OCR 识别和人工复核命令，只保留功能说明与已有数据查询。"
    />

    <section class="surface-card unavailable-card">
      <span class="unavailable-card__icon">▣</span>
      <div>
        <t-tag theme="warning" variant="light">暂未开放</t-tag>
        <h2>OCR 拍照识别将在后续版本接入</h2>
        <p>
          当前版本聚焦固定题库在线练习、教师作业、即时判题、错题与掌握度闭环；页面不会提供上传、识别或复核操作。
        </p>
      </div>
    </section>

    <section class="surface-card history-card">
      <header>
        <div>
          <strong>已有拍照任务</strong>
          <span>{{ isStudent ? '只读查询当前学生历史数据' : '跨学生拍照任务查询本轮不开放' }}</span>
        </div>
        <t-tag theme="primary" variant="light">只读</t-tag>
      </header>
      <div v-if="loading" class="content-loading"><t-loading text="正在读取历史数据" /></div>
      <div v-else-if="records.length" class="photo-list">
        <article v-for="item in records" :key="item.id">
          <div>
            <strong>任务 #{{ item.id }}</strong>
            <span>{{ new Date(item.submittedAt).toLocaleString() }} · {{ item.pageCount }} 页</span>
          </div>
          <div>
            <span>识别题数 {{ item.recognizedItemCount }}</span>
            <span>复核状态 {{ item.reviewStatus }}</span>
          </div>
          <t-tag :theme="item.status === 'COMPLETED' ? 'success' : 'default'" variant="light">
            {{ item.status }}
          </t-tag>
        </article>
      </div>
      <p v-else class="history-empty">
        {{ isStudent ? '暂无历史拍照批改任务。' : '请切换到学生身份查看本人已有数据。' }}
      </p>
    </section>
  </div>
</template>

<style scoped lang="scss">
.unavailable-card {
  display: flex;
  padding: 30px;
  align-items: center;
  background: linear-gradient(135deg, #fff 0%, #fff7ef 100%);
  gap: 22px;
}

.unavailable-card__icon {
  display: grid;
  width: 76px;
  height: 76px;
  flex: none;
  place-items: center;
  background: #fff;
  border: 1px solid #f0dfd2;
  border-radius: 22px;
  color: #df6c27;
  font-size: 34px;
}

.unavailable-card h2 {
  margin-top: 10px;
  color: var(--app-text-strong);
  font-size: 21px;
}

.unavailable-card p {
  margin-top: 7px;
  color: var(--app-text-muted);
  line-height: 1.7;
}

.history-card {
  margin-top: 16px;
  padding: 24px;
}

.history-card > header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.history-card header strong,
.history-card header span,
.photo-list strong,
.photo-list span {
  display: block;
}

.history-card header strong {
  color: var(--app-text-strong);
  font-size: 17px;
}

.history-card header span,
.photo-list span {
  margin-top: 4px;
  color: var(--app-text-muted);
  font-size: 11px;
}

.photo-list {
  display: grid;
  margin-top: 18px;
  gap: 9px;
}

.photo-list article {
  display: grid;
  padding: 14px;
  align-items: center;
  background: #f8f9fa;
  border-radius: 12px;
  grid-template-columns: minmax(200px, 1fr) minmax(160px, auto) auto;
  gap: 12px;
}

.photo-list strong {
  color: var(--app-text-strong);
}

.history-empty {
  padding: 50px 0 28px;
  color: var(--app-text-muted);
  text-align: center;
}

@media (width <= 640px) {
  .unavailable-card {
    align-items: flex-start;
  }

  .photo-list article {
    grid-template-columns: 1fr;
  }
}
</style>
