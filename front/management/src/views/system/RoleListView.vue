<script setup lang="ts">
import AppIcon from '@/components/AppIcon.vue'
import { onMounted, ref } from 'vue'
import { Loading as TLoading, Tag as TTag } from 'tdesign-vue-next'
import { listRolesApi } from '@/api/admin'
import PageHeader from '@/components/PageHeader.vue'
import { roleMeta } from '@/constants/roles'
import type { Role } from '@/types/admin'

const loading = ref(true)
const roles = ref<Role[]>([])

onMounted(async () => {
  try {
    roles.value = await listRolesApi()
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div>
    <PageHeader
      eyebrow="平台管理"
      title="角色管理"
      description="查看角色说明、启用状态与可使用的功能。"
    />
    <div v-if="loading" class="surface-card loading-area"><t-loading text="正在加载角色" /></div>
    <div v-else class="role-grid">
      <article v-for="role in roles" :key="role.id" class="surface-card role-card">
        <header>
          <span class="role-icon"><AppIcon :name="roleMeta(role.code).icon" /></span>
          <div>
            <div class="role-title">
              <h2>{{ role.name }}</h2>
              <t-tag v-if="role.builtIn" theme="primary" variant="light" size="small">内置</t-tag>
              <t-tag :theme="role.status === 1 ? 'success' : 'danger'" variant="light" size="small">
                {{ role.status === 1 ? '启用' : '停用' }}
              </t-tag>
            </div>
            <p>{{ role.code }}</p>
          </div>
          <strong class="permission-count">{{ role.permissions.length }} 项权限</strong>
        </header>
        <p class="role-description">{{ role.description || '暂无角色说明' }}</p>
        <div class="permission-list">
          <span v-for="permission in role.permissions" :key="permission.id">
            {{ permission.name }}
          </span>
          <small v-if="!role.permissions.length">暂未配置权限</small>
        </div>
      </article>
    </div>
  </div>
</template>

<style scoped lang="scss">
.loading-area {
  display: grid;
  min-height: 320px;
  place-items: center;
}

.role-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.role-card {
  padding: clamp(22px, 4vw, 30px);
}

.role-card header {
  display: grid;
  align-items: center;
  grid-template-columns: 54px 1fr auto;
  gap: 14px;
}

.role-icon {
  display: grid;
  width: 52px;
  height: 52px;
  place-items: center;
  background: var(--app-primary-soft);
  border-radius: 17px;
  font-size: 26px;
}

.role-title {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 7px;
}

.role-title h2 {
  color: var(--app-text-strong);
  font-size: 18px;
}

.role-card header p {
  margin-top: 5px;
  color: var(--app-text-muted);
  font-family: monospace;
  font-size: 11px;
}

.permission-count {
  color: var(--app-primary);
  font-size: 12px;
}

.role-description {
  min-height: 44px;
  margin-top: 22px;
  color: #687178;
  font-size: 12px;
  line-height: 1.7;
}

.permission-list {
  display: flex;
  margin-top: 18px;
  flex-wrap: wrap;
  gap: 7px;
}

.permission-list span {
  padding: 7px 10px;
  background: #f4f5f7;
  border-radius: 999px;
  color: #596168;
  font-size: 10px;
}

.permission-list small {
  color: var(--app-text-muted);
}

@media (width <= 900px) {
  .role-grid {
    grid-template-columns: 1fr;
  }
}
</style>
