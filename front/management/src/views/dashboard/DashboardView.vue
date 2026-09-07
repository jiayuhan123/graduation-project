<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Loading as TLoading, Tag as TTag } from 'tdesign-vue-next'
import { getCurrentUserApi } from '@/api/user'
import PageHeader from '@/components/PageHeader.vue'
import { roleMeta } from '@/constants/roles'
import { useUserStore } from '@/stores/user'
import type { UserProfile } from '@/types/user'

interface ModuleCard {
  title: string
  description: string
  icon: string
  path?: string
  permission?: string
  pending?: boolean
}

const router = useRouter()
const userStore = useUserStore()
const loading = ref(true)
const profile = ref<UserProfile | null>(null)

const currentRole = computed(() => roleMeta(userStore.activeRole || 'student'))

const roleModules = computed<ModuleCard[]>(() => {
  const common: ModuleCard[] = [
    { title: '个人资料', description: '昵称、姓名与联系方式', icon: '☺', path: '/account/profile' },
    { title: '账号安全', description: '设置或修改登录密码', icon: '◇', path: '/account/security' },
  ]
  const modules: Record<string, ModuleCard[]> = {
    student: [
      { title: '学生资料', description: '年级与当前学期', icon: '🎒', path: '/account/student' },
      {
        title: '个性练习',
        description: '固定题库自适应组题',
        icon: '✎',
        path: '/learning/practice',
      },
      {
        title: '学习档案',
        description: '练习、错题与掌握度',
        icon: '⌁',
        path: '/learning/records',
      },
      {
        title: '巩固推荐',
        description: '接受或忽略个性化推荐',
        icon: '◎',
        path: '/learning/recommendations',
      },
      {
        title: '拍照批改',
        description: 'OCR 暂未开放，可查历史',
        icon: '▣',
        path: '/learning/photo-reviews',
      },
      { title: '数学闯关', description: '游戏服务接入后开放', icon: '★', pending: true },
    ],
    teacher: [
      { title: '教师资料', description: '职称与公开简介', icon: '📐', path: '/account/teacher' },
      { title: '班级管理', description: '教学服务接入后开放', icon: '♧', pending: true },
      { title: '作业管理', description: '教学服务接入后开放', icon: '▤', pending: true },
      {
        title: '学情反馈',
        description: '查询本人班级学生学情',
        icon: '⌁',
        path: '/learning/records',
      },
    ],
    admin: [
      {
        title: '用户目录',
        description: '查询用户、状态与角色',
        icon: '♧',
        path: '/system/users',
        permission: 'system:user:manage',
      },
      {
        title: '角色管理',
        description: '查看角色和权限配置',
        icon: '♢',
        path: '/system/roles',
        permission: 'system:role:manage',
      },
      {
        title: '权限字典',
        description: '核对平台权限能力',
        icon: '✦',
        path: '/system/permissions',
        permission: 'system:role:manage',
      },
      {
        title: '学生学情',
        description: '查询全部学生学习档案',
        icon: '⌁',
        path: '/learning/records',
        permission: 'teaching:report:query',
      },
      { title: '内容管理', description: '内容服务接入后开放', icon: '▦', pending: true },
    ],
  }
  return [...(modules[userStore.activeRole] ?? []), ...common].filter(
    (item) => !item.permission || userStore.hasPermission(item.permission),
  )
})

const identityLine = computed(() => {
  if (!profile.value) return '正在连接账号资料'
  if (userStore.activeRole === 'student' && profile.value.studentProfile) {
    const item = profile.value.studentProfile
    return `${item.gradeLevel} 年级 · ${item.semester === 1 ? '上学期' : '下学期'}${item.studentNo ? ` · ${item.studentNo}` : ''}`
  }
  if (userStore.activeRole === 'teacher' && profile.value.teacherProfile) {
    return (
      [profile.value.teacherProfile.title, profile.value.teacherProfile.teacherNo]
        .filter(Boolean)
        .join(' · ') || '教师资料待完善'
    )
  }
  if (userStore.activeRole === 'admin') return `拥有 ${profile.value.permissions.length} 项有效权限`
  return '当前角色资料已连接'
})

function switchRole(role: string) {
  userStore.setActiveRole(role)
}

function openModule(module: ModuleCard) {
  if (module.path) void router.push(module.path)
}

onMounted(async () => {
  try {
    profile.value = await getCurrentUserApi()
    userStore.syncProfile(profile.value)
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div>
    <PageHeader
      eyebrow="YOUR SPACE"
      :title="`欢迎回来，${userStore.displayName}`"
      description="一个账号连接多种身份，当前页面会随角色和权限自动切换。"
    >
      <template #actions>
        <div v-if="userStore.roles.length > 1" class="role-switcher">
          <button
            v-for="role in userStore.roles"
            :key="role"
            type="button"
            :class="{ active: role === userStore.activeRole }"
            @click="switchRole(role)"
          >
            {{ roleMeta(role).icon }} {{ roleMeta(role).label }}
          </button>
        </div>
      </template>
    </PageHeader>

    <div v-if="loading" class="surface-card loading-card">
      <t-loading size="32px" text="正在连接用户服务" />
    </div>

    <template v-else>
      <section class="identity-hero surface-card">
        <span class="identity-hero__glow" />
        <div class="identity-hero__icon">{{ currentRole.icon }}</div>
        <div class="identity-hero__content">
          <p>当前身份</p>
          <h2>{{ currentRole.label }}</h2>
          <strong>{{ identityLine }}</strong>
          <span>{{ currentRole.description }}</span>
        </div>
        <t-tag theme="success" variant="light" shape="round">用户服务已连接</t-tag>
      </section>

      <section class="section-block">
        <div class="section-heading">
          <div>
            <h2>{{ currentRole.label }}功能</h2>
            <p>只展示当前 RBAC 身份可使用的模块</p>
          </div>
        </div>
        <div class="module-grid">
          <button
            v-for="item in roleModules"
            :key="item.title"
            type="button"
            class="module-card surface-card"
            :class="{ 'module-card--pending': item.pending }"
            :disabled="item.pending"
            @click="openModule(item)"
          >
            <span class="module-card__icon">{{ item.icon }}</span>
            <span class="module-card__copy">
              <strong>{{ item.title }}</strong>
              <small>{{ item.description }}</small>
            </span>
            <span v-if="item.pending" class="module-card__status">待接入</span>
            <span v-else class="module-card__arrow">→</span>
          </button>
        </div>
      </section>

      <section class="status-card surface-card">
        <div>
          <strong>{{ profile?.roles.length ?? 0 }}</strong>
          <span>账号角色</span>
        </div>
        <div>
          <strong>{{ profile?.permissions.length ?? 0 }}</strong>
          <span>有效权限</span>
        </div>
        <div>
          <strong>{{ profile?.wechatBound ? '已绑定' : '未绑定' }}</strong>
          <span>微信身份</span>
        </div>
        <div>
          <strong>{{ profile?.passwordSet ? '已设置' : '未设置' }}</strong>
          <span>账号密码</span>
        </div>
      </section>
    </template>
  </div>
</template>

<style scoped lang="scss">
.role-switcher {
  display: flex;
  padding: 5px;
  background: #eceff2;
  border-radius: 999px;
}

.role-switcher button {
  padding: 9px 14px;
  background: transparent;
  border: 0;
  border-radius: 999px;
  color: #737b82;
  cursor: pointer;
  font: inherit;
  font-size: 12px;
}

.role-switcher button.active {
  background: #fff;
  box-shadow: 0 4px 14px rgb(45 52 54 / 9%);
  color: #d9651d;
  font-weight: 700;
}

.loading-card {
  display: grid;
  min-height: 260px;
  place-items: center;
}

.identity-hero {
  position: relative;
  display: flex;
  min-height: 210px;
  padding: clamp(28px, 5vw, 50px);
  overflow: hidden;
  align-items: center;
  background: linear-gradient(135deg, #fff 0%, #fff8f2 70%, #fff2e8 100%);
  gap: 24px;
}

.identity-hero__glow {
  position: absolute;
  top: -150px;
  right: -90px;
  width: 380px;
  height: 380px;
  background: rgb(255 138 61 / 9%);
  border-radius: 50%;
}

.identity-hero__icon {
  position: relative;
  display: grid;
  width: 100px;
  height: 100px;
  flex: none;
  place-items: center;
  background: #fff;
  border: 1px solid #f2e5dc;
  border-radius: 30px;
  box-shadow: 0 16px 38px rgb(75 55 40 / 9%);
  font-size: 46px;
}

.identity-hero__content {
  position: relative;
  flex: 1;
}

.identity-hero__content p {
  color: #9a8577;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.12em;
}

.identity-hero__content h2 {
  margin-top: 6px;
  color: var(--app-text-strong);
  font-size: 34px;
}

.identity-hero__content strong,
.identity-hero__content span {
  display: block;
}

.identity-hero__content strong {
  margin-top: 10px;
  color: #535c62;
}

.identity-hero__content span {
  margin-top: 7px;
  color: var(--app-text-muted);
  font-size: 12px;
}

.section-block {
  margin-top: 34px;
}

.section-heading {
  display: flex;
  margin-bottom: 15px;
  align-items: flex-end;
  justify-content: space-between;
}

.section-heading h2 {
  color: var(--app-text-strong);
  font-size: 19px;
}

.section-heading p {
  margin-top: 5px;
  color: var(--app-text-muted);
  font-size: 12px;
}

.module-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.module-card {
  display: flex;
  min-height: 108px;
  padding: 21px;
  align-items: center;
  border: 1px solid var(--app-border-color);
  color: inherit;
  cursor: pointer;
  font: inherit;
  gap: 15px;
  text-align: left;
  transition:
    transform 160ms ease,
    box-shadow 160ms ease;
}

.module-card:not(:disabled):hover {
  box-shadow: 0 22px 54px rgb(45 52 54 / 12%);
  transform: translateY(-2px);
}

.module-card__icon {
  display: grid;
  width: 46px;
  height: 46px;
  flex: none;
  place-items: center;
  background: #fff3eb;
  border-radius: 15px;
  color: #e36d24;
  font-size: 22px;
}

.module-card__copy {
  display: flex;
  min-width: 0;
  flex: 1;
  flex-direction: column;
}

.module-card__copy strong {
  color: var(--app-text-strong);
  font-size: 15px;
}

.module-card__copy small {
  margin-top: 6px;
  overflow: hidden;
  color: var(--app-text-muted);
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.module-card__arrow {
  color: #c0c5ca;
  font-size: 18px;
}

.module-card__status {
  padding: 5px 7px;
  background: #f1f2f4;
  border-radius: 999px;
  color: #9aa0a6;
  font-size: 9px;
}

.module-card--pending {
  background: rgb(255 255 255 / 66%);
  box-shadow: none;
  cursor: default;
}

.module-card--pending .module-card__icon {
  background: #f2f3f5;
  color: #a0a6ad;
}

.status-card {
  display: grid;
  margin-top: 34px;
  padding: 26px 12px;
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.status-card > div {
  display: flex;
  align-items: center;
  flex-direction: column;
}

.status-card > div + div {
  border-left: 1px solid #eef0f2;
}

.status-card strong {
  color: var(--app-text-strong);
  font-size: 22px;
}

.status-card span {
  margin-top: 7px;
  color: var(--app-text-muted);
  font-size: 11px;
}

@media (width <= 1050px) {
  .module-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (width <= 640px) {
  .identity-hero {
    min-height: auto;
    align-items: flex-start;
    flex-wrap: wrap;
  }

  .identity-hero__icon {
    width: 72px;
    height: 72px;
    border-radius: 22px;
    font-size: 34px;
  }

  .identity-hero__content h2 {
    font-size: 27px;
  }

  .module-grid {
    grid-template-columns: 1fr;
  }

  .status-card {
    grid-template-columns: repeat(2, 1fr);
    row-gap: 22px;
  }

  .status-card > div:nth-child(3) {
    border-left: 0;
  }
}
</style>
