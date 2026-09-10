<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { Button as TButton, Loading as TLoading } from 'tdesign-vue-next'
import { getCurrentUserApi } from '@/api/user'
import AppIcon from '@/components/AppIcon.vue'
import PageHeader from '@/components/PageHeader.vue'
import { roleMeta } from '@/constants/roles'
import { getVisibleNavigationGroups } from '@/router/navigation'
import { useUserStore } from '@/stores/user'
import type { UserProfile } from '@/types/user'

const userStore = useUserStore()
const loading = ref(true)
const profile = ref<UserProfile | null>(null)
const profileError = ref(false)
const currentRole = computed(() => roleMeta(userStore.activeRole || 'student'))
const dateLabel = new Intl.DateTimeFormat('zh-CN', {
  month: 'long',
  day: 'numeric',
  weekday: 'long',
}).format(new Date())
const copy = computed(() => {
  if (userStore.activeRole === 'teacher')
    return {
      label: '教学工作台',
      title: '把时间留给教学',
      description: '从班级作业到学习反馈，在这里安排今天的教学。',
      hero: '了解进度，让辅导更有方向。',
      detail: '查看班级作业与学生反馈，及时跟进每一次学习。',
      guide: '教学安排',
      steps: ['维护班级与学生名单', '布置作业，跟进提交情况', '结合学情，安排后续巩固'],
    }
  if (userStore.activeRole === 'admin')
    return {
      label: '管理工作台',
      title: '平台工作，一目了然',
      description: '内容维护、用户管理与学习概况，从这里开始。',
      hero: '让每一份教学内容准备就绪。',
      detail: '维护题库与知识点，查看反馈，保持内容准确有序。',
      guide: '管理提醒',
      steps: ['维护模块与知识点', '检查题目内容与审核反馈', '核对账号状态与角色配置'],
    }
  return {
    label: '学习工作台',
    title: '今天，也向前一步',
    description: '安排练习、完成作业，慢慢积累看得见的进步。',
    hero: '选好知识点，开始练习。',
    detail: '按知识点选择练习，在回顾与巩固中找到自己的节奏。',
    guide: '学习小建议',
    steps: ['选一个想练习的知识点', '独立完成，再查看解析', '回到学习档案，巩固易错题'],
  }
})
const availableModules = computed(() =>
  getVisibleNavigationGroups({
    roles: userStore.activeRole ? [userStore.activeRole] : userStore.roles,
    permissions: userStore.permissions,
  })
    .flatMap((group) => group.items)
    .filter((item) => item.path !== '/dashboard' && !item.path.startsWith('/account/')),
)
const descriptions: Record<string, string> = {
  '/learning/practice': '按知识点与难度，开始一组练习',
  '/learning/records': '回顾练习、错题与知识点掌握情况',
  '/learning/recommendations': '查看需要继续巩固的内容',
  '/learning/photo-reviews': '查看照片批改进度与复核结果',
  '/learning/class-dashboard': '查看班级整体进度与薄弱知识点',
  '/teaching/classes': '查看班级信息与成员',
  '/teaching/assignments': '查看作业要求、截止时间与进度',
  '/teaching/submissions': '查看已提交的作业与批改反馈',
  '/content/modules': '整理教学内容与模块目录',
  '/content/questions': '维护题目、答案与解析',
  '/content/reviews': '检查待审核的教学内容',
  '/content/knowledge-points': '维护知识点与学习顺序',
  '/system/users': '管理账号状态与用户资料',
  '/system/roles': '配置角色与对应权限',
  '/system/operations': '查看平台学习与使用概况',
}
const modules = computed(() => {
  const preferred =
    userStore.activeRole === 'student'
      ? [
          '/learning/practice',
          '/teaching/assignments',
          '/learning/records',
          '/learning/recommendations',
          '/teaching/classes',
          '/learning/photo-reviews',
        ]
      : userStore.activeRole === 'teacher'
        ? [
            '/teaching/assignments',
            '/teaching/classes',
            '/learning/class-dashboard',
            '/learning/records',
            '/teaching/submissions',
            '/learning/photo-reviews',
          ]
        : [
            '/content/questions',
            '/content/reviews',
            '/system/users',
            '/content/knowledge-points',
            '/system/operations',
            '/system/roles',
          ]
  return [...availableModules.value]
    .sort((a, b) => {
      const rank = (path: string) =>
        preferred.includes(path) ? preferred.indexOf(path) : preferred.length
      return rank(a.path) - rank(b.path)
    })
    .slice(0, 6)
})
const primaryModule = computed(() => modules.value[0])
const identityLine = computed(() => {
  if (profileError.value) return '账号资料暂时无法加载'
  if (userStore.activeRole === 'student' && profile.value?.studentProfile) {
    const item = profile.value.studentProfile
    return item.gradeLevel + ' 年级 · ' + (item.semester === 1 ? '上学期' : '下学期')
  }
  if (userStore.activeRole === 'teacher')
    return profile.value?.teacherProfile?.title || '教师工作空间'
  return currentRole.value.label + '工作空间'
})
async function loadProfile() {
  loading.value = true
  profileError.value = false
  try {
    profile.value = await getCurrentUserApi()
    userStore.syncProfile(profile.value)
  } catch {
    profileError.value = true
  } finally {
    loading.value = false
  }
}
onMounted(loadProfile)
</script>

<template>
  <div class="dashboard">
    <PageHeader :eyebrow="copy.label" :title="copy.title" :description="copy.description"
      ><template #actions
        ><p class="dashboard-date">{{ dateLabel }}</p></template
      ></PageHeader
    >
    <div class="overview-grid">
      <section class="start-panel">
        <div class="start-panel__copy">
          <span class="start-panel__label">{{
            userStore.activeRole === 'student' ? '保持自己的学习节奏' : '专注今天的工作'
          }}</span>
          <h2>{{ copy.hero }}</h2>
          <p>{{ copy.detail }}</p>
          <RouterLink v-if="primaryModule" :to="primaryModule.path" class="start-button"
            >{{ primaryModule.title }}<AppIcon name="arrow" :size="18" /></RouterLink
          ><RouterLink v-else to="/account/profile" class="start-button"
            >完善个人资料<AppIcon name="arrow" :size="18"
          /></RouterLink>
        </div>
        <svg class="start-panel__drawing" viewBox="0 0 200 200" fill="none" aria-hidden="true">
          <path d="M30 160h145M45 170V30" stroke="currentColor" stroke-opacity=".35" />
          <path d="m55 140 42-85 69 85H55Z" stroke="currentColor" stroke-width="2" />
          <path
            d="M97 55v85M55 140l66-31 45 31"
            stroke="currentColor"
            stroke-opacity=".5"
            stroke-dasharray="4 5"
          />
          <circle cx="97" cy="55" r="4" fill="currentColor" />
          <circle cx="55" cy="140" r="4" fill="currentColor" />
          <circle cx="166" cy="140" r="4" fill="currentColor" />
          <path d="M102 130h10v10" stroke="currentColor" stroke-opacity=".5" />
        </svg>
      </section>
      <section class="profile-panel surface-card">
        <div class="profile-panel__heading">
          <span class="profile-avatar">{{ userStore.displayName.slice(0, 1) }}</span>
          <div>
            <h2>{{ userStore.displayName }}</h2>
            <p>{{ currentRole.label }}</p>
          </div>
        </div>
        <t-loading v-if="loading" size="small" text="加载资料中" />
        <template v-else
          ><p class="profile-panel__detail">{{ identityLine }}</p>
          <t-button v-if="profileError" size="small" variant="text" @click="loadProfile"
            >重新加载</t-button
          ><RouterLink v-else to="/account/profile" class="text-link"
            >查看个人资料<AppIcon name="arrow" :size="16" /></RouterLink
        ></template>
        <div class="profile-panel__foot">
          <AppIcon name="book" :size="16" /><span>Web 与小程序共用学习账号</span>
        </div>
      </section>
    </div>
    <div class="work-grid">
      <section class="shortcuts surface-card">
        <div class="section-heading">
          <h2>{{ userStore.activeRole === 'student' ? '学习与班级' : '常用功能' }}</h2>
          <span>快速进入</span>
        </div>
        <div v-if="modules.length" class="module-grid">
          <RouterLink v-for="item in modules" :key="item.path" :to="item.path" class="module-link"
            ><span class="module-icon"><AppIcon :name="item.icon" :size="22" /></span
            ><span class="module-copy"
              ><strong>{{ item.title }}</strong
              ><small>{{ descriptions[item.path] || '查看并管理相关记录' }}</small></span
            ><AppIcon name="chevron" :size="15"
          /></RouterLink>
        </div>
        <p v-else class="no-modules">当前身份暂无可用功能，可联系管理员配置。</p>
      </section>
      <aside class="guide-panel surface-card">
        <div class="section-heading">
          <h2>{{ copy.guide }}</h2>
          <AppIcon name="document" :size="18" />
        </div>
        <ol>
          <li v-for="(step, index) in copy.steps" :key="step">
            <span>{{ String(index + 1).padStart(2, '0') }}</span>
            <p>{{ step }}</p>
          </li>
        </ol>
        <RouterLink to="/account/security" class="security-link"
          ><AppIcon name="shield" :size="18" /><span>管理登录密码与账号安全</span
          ><AppIcon name="chevron" :size="14"
        /></RouterLink>
      </aside>
    </div>
  </div>
</template>

<style scoped lang="scss">
.dashboard-date {
  color: var(--app-text-muted);
  font-size: 12px;
}

.overview-grid {
  display: grid;
  grid-template-columns: minmax(0, 2.15fr) minmax(240px, 1fr);
  gap: 22px;
}

.start-panel {
  position: relative;
  display: flex;
  min-height: 244px;
  padding: 32px;
  overflow: hidden;
  align-items: center;
  background: #3567c8;
  border-radius: 12px;
  color: #fff;
}

.start-panel__copy {
  position: relative;
  z-index: 1;
  max-width: 80%;
}

.start-panel__label {
  color: #dce8ff;
  font-size: 12px;
}

.start-panel h2 {
  max-width: 440px;
  margin-top: 14px;
  font-size: clamp(22px, 2.2vw, 28px);
  font-weight: 600;
  line-height: 1.55;
  text-wrap: balance;
}

.start-panel p {
  max-width: 380px;
  margin-top: 10px;
  color: #dce8ff;
  font-size: 12px;
  line-height: 1.8;
}

.start-button {
  display: inline-flex;
  margin-top: 25px;
  padding: 10px 15px;
  align-items: center;
  background: #fff;
  border-radius: 6px;
  color: #2953a6;
  gap: 22px;
  text-decoration: none;
  font-size: 13px;
  font-weight: 600;
}

.start-button:hover {
  background: #edf3fe;
}

.start-button:focus-visible {
  outline-color: #fff;
}

.start-panel__drawing {
  position: absolute;
  right: -6px;
  bottom: 14px;
  width: 190px;
  color: #dce8ff;
  opacity: 0.3;
}

.profile-panel {
  display: flex;
  min-width: 0;
  padding: 26px;
  flex-direction: column;
  align-items: flex-start;
}

.profile-panel__heading {
  display: flex;
  min-width: 0;
  width: 100%;
  align-items: center;
  gap: 12px;
}

.profile-panel__heading > div {
  min-width: 0;
}

.profile-avatar {
  display: grid;
  width: 44px;
  height: 44px;
  flex: none;
  place-items: center;
  background: var(--app-primary-soft);
  border-radius: 50%;
  color: var(--app-primary);
  font-size: 20px;
}

.profile-panel h2 {
  overflow: hidden;
  color: var(--app-text-strong);
  font-size: 16px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.profile-panel__heading p {
  margin-top: 5px;
  color: var(--app-text-muted);
  font-size: 11px;
}

.profile-panel__detail {
  margin-top: 22px;
  font-size: 13px;
}

.text-link {
  display: inline-flex;
  margin-top: 13px;
  align-items: center;
  color: var(--app-primary);
  gap: 10px;
  text-decoration: none;
  font-size: 12px;
}

.profile-panel__foot {
  display: flex;
  width: 100%;
  margin-top: auto;
  padding-top: 20px;
  align-items: center;
  color: var(--app-text-muted);
  gap: 7px;
  font-size: 11px;
}

.work-grid {
  display: grid;
  margin-top: 26px;
  grid-template-columns: minmax(0, 2.15fr) minmax(240px, 1fr);
  gap: 22px;
  align-items: start;
}

.section-heading {
  display: flex;
  padding: 22px 24px 18px;
  align-items: center;
  justify-content: space-between;
  color: var(--app-text-muted);
  gap: 12px;
}

.section-heading h2 {
  color: var(--app-text-strong);
  font-size: 16px;
  font-weight: 600;
}

.section-heading > span {
  font-size: 11px;
}

.module-grid {
  display: grid;
  padding: 0 24px 12px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  column-gap: 24px;
}

.module-link {
  display: flex;
  min-width: 0;
  min-height: 104px;
  padding: 18px 0;
  align-items: center;
  border-top: 1px solid var(--app-border-color);
  color: var(--app-text-muted);
  gap: 12px;
  text-decoration: none;
}

.module-icon {
  display: grid;
  width: 38px;
  height: 38px;
  flex: none;
  place-items: center;
  background: #f5f7fb;
  border-radius: 8px;
  color: var(--app-primary);
}

.module-copy {
  display: flex;
  min-width: 0;
  flex: 1;
  flex-direction: column;
  gap: 7px;
}

.module-copy strong {
  color: var(--app-text-strong);
  font-size: 13px;
  font-weight: 500;
}

.module-copy small {
  font-size: 11px;
  line-height: 1.65;
}

.module-link:hover strong {
  color: var(--app-primary);
}

.module-link:hover .module-icon {
  background: var(--app-primary-soft);
}

.guide-panel ol {
  padding: 0 24px 16px;
  list-style: none;
}

.guide-panel li {
  display: flex;
  padding: 17px 0;
  align-items: baseline;
  border-top: 1px solid var(--app-border-color);
  gap: 14px;
}

.guide-panel li > span {
  color: #8ba3c8;
  font-size: 12px;
  font-variant-numeric: tabular-nums;
}

.guide-panel li p {
  font-size: 12px;
  line-height: 1.7;
}

.security-link {
  display: flex;
  margin: 0 24px 24px;
  padding-top: 18px;
  align-items: center;
  border-top: 1px solid var(--app-border-color);
  color: var(--app-primary);
  gap: 8px;
  text-decoration: none;
  font-size: 11px;
}

.security-link > span {
  flex: 1;
}

.no-modules {
  padding: 24px;
  font-size: 13px;
}

@media (width <= 1150px) {
  .overview-grid,
  .work-grid {
    grid-template-columns: minmax(0, 1.65fr) minmax(220px, 1fr);
    gap: 16px;
  }

  .module-grid {
    grid-template-columns: 1fr;
  }

  .module-link {
    min-height: 82px;
  }

  .start-panel {
    padding: 26px;
  }

  .start-panel__copy {
    max-width: 100%;
  }

  .start-panel__drawing {
    opacity: 0.15;
  }
}

@media (width <= 620px) {
  .overview-grid,
  .work-grid {
    grid-template-columns: 1fr;
  }

  .dashboard-date {
    font-size: 11px;
  }

  .start-panel {
    min-height: 250px;
    padding: 24px;
  }

  .start-panel__copy {
    max-width: 90%;
  }

  .start-panel h2 {
    font-size: 24px;
  }

  .profile-panel {
    padding: 20px 24px;
  }

  .profile-panel__detail {
    margin-top: 16px;
  }

  .profile-panel__foot {
    margin-top: 16px;
    padding-top: 16px;
    border-top: 1px solid var(--app-border-color);
  }

  .work-grid {
    margin-top: 20px;
  }

  .module-link {
    min-height: 90px;
  }
}
</style>
