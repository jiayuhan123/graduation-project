<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import {
  Button as TButton,
  Input as TInput,
  Loading as TLoading,
  Option as TOption,
  Select as TSelect,
  Tag as TTag,
} from 'tdesign-vue-next'
import { listRolesApi, pageAdminUsersApi } from '@/api/admin'
import PageHeader from '@/components/PageHeader.vue'
import type { AdminUser, AdminUserQuery, Role } from '@/types/admin'

const loading = ref(true)
const users = ref<AdminUser[]>([])
const roles = ref<Role[]>([])
const total = ref(0)
const pages = ref(0)
const query = reactive<Required<Pick<AdminUserQuery, 'page' | 'size'>> & AdminUserQuery>({
  page: 1,
  size: 10,
  keyword: '',
  roleCode: '',
})

function displayName(user: AdminUser): string {
  return user.nickname || user.realName || user.username
}

function formatDate(value: string | null): string {
  return value ? value.replace('T', ' ').slice(0, 16) : '暂无记录'
}

async function loadUsers() {
  loading.value = true
  try {
    const result = await pageAdminUsersApi({
      page: query.page,
      size: query.size,
      keyword: query.keyword?.trim() || undefined,
      roleCode: query.roleCode || undefined,
    })
    users.value = result.records
    total.value = result.total
    pages.value = result.pages
    query.page = result.page
  } finally {
    loading.value = false
  }
}

function search() {
  query.page = 1
  void loadUsers()
}

function changePage(delta: number) {
  const next = query.page + delta
  if (next < 1 || next > pages.value) return
  query.page = next
  void loadUsers()
}

onMounted(async () => {
  const [, roleData] = await Promise.all([loadUsers(), listRolesApi()])
  roles.value = roleData
})
</script>

<template>
  <div>
    <PageHeader
      eyebrow="平台管理"
      title="用户目录"
      :description="`当前共 ${total} 个账号，可按角色或账号信息查找。`"
    />

    <section class="surface-card directory-card">
      <div class="filters">
        <t-input
          v-model="query.keyword"
          clearable
          placeholder="搜索用户名、姓名、手机或邮箱"
          @enter="search"
        />
        <t-select v-model="query.roleCode" clearable placeholder="全部角色" @change="search">
          <t-option v-for="role in roles" :key="role.code" :value="role.code" :label="role.name" />
        </t-select>
        <t-button @click="search">查询</t-button>
      </div>

      <div v-if="loading" class="loading-area"><t-loading text="正在加载用户目录" /></div>
      <div v-else-if="users.length" class="user-list">
        <article v-for="user in users" :key="user.id" class="user-row">
          <div class="user-avatar">
            <img v-if="user.avatarUrl" :src="user.avatarUrl" alt="" />
            <span v-else>{{ displayName(user).slice(0, 1) }}</span>
          </div>
          <div class="user-main">
            <div class="user-name">
              <strong>{{ displayName(user) }}</strong>
              <t-tag :theme="user.status === 1 ? 'success' : 'danger'" variant="light" size="small">
                {{ user.status === 1 ? '正常' : '已禁用' }}
              </t-tag>
              <span v-if="user.wechatBound" class="wechat-badge">微</span>
            </div>
            <span>@{{ user.username }}</span>
          </div>
          <div class="user-roles">
            <t-tag v-for="role in user.roles" :key="role.code" variant="light" size="small">
              {{ role.name }}
            </t-tag>
          </div>
          <div class="user-contact">
            <strong>{{ user.phone || user.email || '未填写联系方式' }}</strong>
            <span>最近登录 {{ formatDate(user.lastLoginAt) }}</span>
          </div>
        </article>
      </div>
      <div v-else class="empty-state">没有找到符合条件的用户</div>

      <footer class="pagination-bar">
        <span>第 {{ query.page }} / {{ Math.max(pages, 1) }} 页</span>
        <div>
          <t-button
            variant="outline"
            size="small"
            :disabled="query.page <= 1"
            @click="changePage(-1)"
          >
            上一页
          </t-button>
          <t-button
            variant="outline"
            size="small"
            :disabled="query.page >= pages"
            @click="changePage(1)"
          >
            下一页
          </t-button>
        </div>
      </footer>
    </section>
  </div>
</template>

<style scoped lang="scss">
.directory-card {
  padding: clamp(20px, 3vw, 30px);
}

.filters {
  display: grid;
  grid-template-columns: minmax(260px, 1fr) 180px auto;
  gap: 10px;
}

.loading-area,
.empty-state {
  display: grid;
  min-height: 300px;
  color: var(--app-text-muted);
  place-items: center;
}

.user-list {
  margin-top: 24px;
  border-top: 1px solid #eceef0;
}

.user-row {
  display: grid;
  min-height: 78px;
  padding: 16px 4px;
  align-items: center;
  border-bottom: 1px solid #eff1f3;
  grid-template-columns: 42px minmax(150px, 1.1fr) minmax(150px, 0.8fr) minmax(190px, 1fr);
  gap: 14px;
}

.user-avatar {
  display: grid;
  width: 38px;
  height: 38px;
  place-items: center;
  overflow: hidden;
  background: var(--app-primary-soft);
  border-radius: 50%;
  color: var(--app-primary);
  font-size: 17px;
  font-weight: 700;
}

.user-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.user-main,
.user-contact {
  display: flex;
  min-width: 0;
  flex-direction: column;
}

.user-name {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 7px;
}

.user-name strong {
  overflow: hidden;
  color: var(--app-text-strong);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.wechat-badge {
  display: grid;
  width: 21px;
  height: 21px;
  place-items: center;
  background: #e8f8ef;
  border-radius: 7px;
  color: #079848;
  font-size: 9px;
  font-weight: 600;
}

.user-main > span,
.user-contact span {
  margin-top: 5px;
  overflow: hidden;
  color: var(--app-text-muted);
  font-size: 10px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-roles {
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
}

.user-contact {
  text-align: right;
}

.user-contact strong {
  overflow: hidden;
  color: #596168;
  font-size: 12px;
  font-weight: 500;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pagination-bar {
  display: flex;
  padding-top: 22px;
  align-items: center;
  justify-content: space-between;
  color: var(--app-text-muted);
  font-size: 11px;
}

.pagination-bar > div {
  display: flex;
  gap: 8px;
}

@media (width <= 900px) {
  .user-row {
    grid-template-columns: 52px 1fr auto;
  }

  .user-contact {
    display: none;
  }
}

@media (width <= 620px) {
  .filters {
    grid-template-columns: 1fr;
  }

  .user-row {
    grid-template-columns: 48px 1fr;
  }

  .user-roles {
    padding-left: 62px;
    grid-column: 1 / -1;
  }
}
</style>
