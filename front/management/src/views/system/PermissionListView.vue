<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { Input as TInput, Loading as TLoading } from 'tdesign-vue-next'
import { listPermissionsApi } from '@/api/admin'
import PageHeader from '@/components/PageHeader.vue'
import type { Permission } from '@/types/admin'

const loading = ref(true)
const keyword = ref('')
const permissions = ref<Permission[]>([])
const filteredPermissions = computed(() => {
  const value = keyword.value.trim().toLowerCase()
  if (!value) return permissions.value
  return permissions.value.filter(
    (item) =>
      item.code.toLowerCase().includes(value) ||
      item.name.toLowerCase().includes(value) ||
      item.description?.toLowerCase().includes(value),
  )
})

function domain(code: string): string {
  return code.split(':')[0]?.toUpperCase() || 'SYSTEM'
}

onMounted(async () => {
  try {
    permissions.value = await listPermissionsApi()
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div>
    <PageHeader
      eyebrow="RBAC"
      title="权限字典"
      description="页面只做可视化核对，最终鉴权始终由用户服务和网关执行。"
    >
      <template #actions>
        <t-input v-model="keyword" clearable placeholder="搜索权限名称或编码" />
      </template>
    </PageHeader>
    <div v-if="loading" class="surface-card loading-area">
      <t-loading text="正在加载权限字典" />
    </div>
    <section v-else class="surface-card permission-table">
      <article v-for="permission in filteredPermissions" :key="permission.id">
        <span class="domain-badge">{{ domain(permission.code) }}</span>
        <div>
          <strong>{{ permission.name }}</strong>
          <code>{{ permission.code }}</code>
        </div>
        <p>{{ permission.description || '暂无权限说明' }}</p>
      </article>
      <div v-if="!filteredPermissions.length" class="empty-state">没有匹配的权限</div>
    </section>
  </div>
</template>

<style scoped lang="scss">
.loading-area,
.empty-state {
  display: grid;
  min-height: 280px;
  color: var(--app-text-muted);
  place-items: center;
}

.permission-table {
  padding: 8px clamp(18px, 3vw, 30px);
}

.permission-table article {
  display: grid;
  min-height: 84px;
  padding: 17px 0;
  align-items: center;
  border-bottom: 1px solid #eff1f3;
  grid-template-columns: 88px minmax(220px, 0.8fr) minmax(260px, 1.2fr);
  gap: 20px;
}

.permission-table article:last-of-type {
  border-bottom: 0;
}

.domain-badge {
  display: inline-flex;
  width: fit-content;
  padding: 6px 9px;
  background: #fff1e8;
  border-radius: 8px;
  color: #d9651d;
  font-size: 9px;
  font-weight: 800;
  letter-spacing: 0.08em;
}

.permission-table article > div {
  display: flex;
  min-width: 0;
  flex-direction: column;
}

.permission-table strong {
  color: var(--app-text-strong);
  font-size: 13px;
}

.permission-table code {
  margin-top: 5px;
  color: #868e95;
  font-size: 10px;
}

.permission-table p {
  color: #687178;
  font-size: 12px;
  line-height: 1.6;
}

@media (width <= 720px) {
  .permission-table article {
    grid-template-columns: auto 1fr;
    gap: 10px 14px;
  }

  .permission-table p {
    grid-column: 1 / -1;
  }
}
</style>
