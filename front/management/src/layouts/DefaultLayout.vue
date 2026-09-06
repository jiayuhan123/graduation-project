<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Button as TButton, MessagePlugin } from 'tdesign-vue-next'
import { hasAccess } from '@/auth/permission'
import BrandLogo from '@/components/BrandLogo.vue'
import { roleMeta } from '@/constants/roles'
import { navigationGroups } from '@/router/navigation'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const accessSubject = computed(() => ({
  roles: userStore.activeRole ? [userStore.activeRole] : userStore.roles,
  permissions: userStore.permissions,
}))

const visibleNavigationGroups = computed(() =>
  navigationGroups
    .map((group) => ({
      ...group,
      items: group.items.filter((item) => hasAccess(accessSubject.value, item)),
    }))
    .filter((group) => group.items.length > 0),
)

const currentRole = computed(() =>
  roleMeta(userStore.activeRole || userStore.roles[0] || 'student'),
)

function switchRole(role: string) {
  if (role === userStore.activeRole) return
  userStore.setActiveRole(role)
  void router.push({ name: 'dashboard' })
}

async function handleLogout() {
  const serverLoggedOut = await userStore.logout()
  if (serverLoggedOut) void MessagePlugin.success('已退出登录')
  else void MessagePlugin.warning('本地已退出，服务端注销请求未完成')
  await router.replace({ name: 'login' })
}
</script>

<template>
  <div class="app-shell">
    <aside class="sidebar">
      <BrandLogo />

      <section class="identity-card">
        <div class="identity-card__top">
          <span class="identity-card__icon">{{ currentRole.icon }}</span>
          <span>
            <small>当前身份</small>
            <strong>{{ currentRole.label }}</strong>
          </span>
        </div>
        <p>{{ currentRole.description }}</p>
        <div v-if="userStore.roles.length > 1" class="role-list" aria-label="切换身份">
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
      </section>

      <nav class="sidebar__nav" aria-label="主导航">
        <section v-for="group in visibleNavigationGroups" :key="group.title">
          <p class="nav-group-title">{{ group.title }}</p>
          <RouterLink
            v-for="item in group.items"
            :key="item.path"
            :to="item.path"
            class="nav-link"
            :class="{ active: route.path === item.path }"
          >
            <span class="nav-link__icon">{{ item.icon }}</span>
            <span>{{ item.title }}</span>
          </RouterLink>
        </section>
      </nav>

      <footer class="sidebar__footer">
        <div class="avatar">
          <img v-if="userStore.userInfo?.avatarUrl" :src="userStore.userInfo.avatarUrl" alt="" />
          <span v-else>{{ userStore.displayName.slice(0, 1) }}</span>
        </div>
        <div class="sidebar__user">
          <strong>{{ userStore.displayName }}</strong>
          <small>@{{ userStore.userInfo?.username }}</small>
        </div>
        <t-button variant="text" shape="circle" title="退出登录" @click="handleLogout">↗</t-button>
      </footer>
    </aside>

    <header class="mobile-header">
      <BrandLogo compact />
      <div class="mobile-header__identity">
        <span>{{ currentRole.icon }}</span>
        <strong>{{ userStore.displayName }}</strong>
      </div>
    </header>

    <main class="main-content">
      <RouterView />
    </main>

    <nav class="mobile-nav" aria-label="移动端导航">
      <template v-for="group in visibleNavigationGroups" :key="group.title">
        <RouterLink
          v-for="item in group.items"
          :key="item.path"
          :to="item.path"
          :class="{ active: route.path === item.path }"
        >
          <span>{{ item.icon }}</span>
          <small>{{ item.title }}</small>
        </RouterLink>
      </template>
    </nav>
  </div>
</template>

<style scoped lang="scss">
.app-shell {
  min-height: 100vh;
}

.sidebar {
  position: fixed;
  z-index: 10;
  top: 0;
  bottom: 0;
  left: 0;
  display: flex;
  width: var(--app-sidebar-width);
  padding: 24px 20px 18px;
  overflow-y: auto;
  background: rgb(255 255 255 / 94%);
  border-right: 1px solid var(--app-border-color);
  flex-direction: column;
  backdrop-filter: blur(18px);
}

.identity-card {
  margin-top: 28px;
  padding: 17px;
  background: linear-gradient(145deg, #fff7f1, #fff);
  border: 1px solid #ffe3d0;
  border-radius: 22px;
}

.identity-card__top {
  display: flex;
  align-items: center;
  gap: 12px;
}

.identity-card__icon {
  display: grid;
  width: 42px;
  height: 42px;
  place-items: center;
  background: #fff;
  border-radius: 14px;
  box-shadow: var(--app-shadow-soft);
  font-size: 21px;
}

.identity-card small,
.identity-card strong {
  display: block;
}

.identity-card small {
  color: var(--app-text-muted);
  font-size: 11px;
}

.identity-card strong {
  margin-top: 2px;
  color: var(--app-text-strong);
  font-size: 17px;
}

.identity-card p {
  margin-top: 12px;
  color: var(--app-text-muted);
  font-size: 12px;
  line-height: 1.55;
}

.role-list {
  display: flex;
  margin-top: 14px;
  flex-wrap: wrap;
  gap: 6px;
}

.role-list button {
  padding: 6px 9px;
  background: #f5f6f8;
  border: 0;
  border-radius: 999px;
  color: #727980;
  cursor: pointer;
  font: inherit;
  font-size: 11px;
}

.role-list button.active {
  background: #fff0e6;
  color: #d9651d;
  font-weight: 700;
}

.sidebar__nav {
  display: flex;
  margin-top: 22px;
  flex: 1;
  flex-direction: column;
  gap: 17px;
}

.nav-group-title {
  margin: 0 12px 7px;
  color: #adb2b8;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.12em;
}

.nav-link {
  display: flex;
  min-height: 42px;
  padding: 9px 12px;
  align-items: center;
  border-radius: 13px;
  color: #616970;
  gap: 11px;
  text-decoration: none;
  transition: 160ms ease;
}

.nav-link:hover {
  background: #f7f8fa;
  color: var(--app-text-strong);
}

.nav-link.active {
  background: #fff1e8;
  color: #d9651d;
  font-weight: 700;
}

.nav-link__icon {
  display: grid;
  width: 25px;
  height: 25px;
  place-items: center;
  font-size: 17px;
}

.sidebar__footer {
  display: flex;
  padding-top: 16px;
  align-items: center;
  border-top: 1px solid #f0f1f3;
  gap: 10px;
}

.avatar {
  display: grid;
  width: 38px;
  height: 38px;
  flex: none;
  place-items: center;
  overflow: hidden;
  background: linear-gradient(135deg, #ffad73, #ff7a2a);
  border-radius: 13px;
  color: #fff;
  font-weight: 700;
}

.avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.sidebar__user {
  display: flex;
  min-width: 0;
  flex: 1;
  flex-direction: column;
}

.sidebar__user strong,
.sidebar__user small {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.sidebar__user strong {
  font-size: 13px;
}

.sidebar__user small {
  margin-top: 2px;
  color: #9aa0a6;
  font-size: 10px;
}

.main-content {
  min-height: 100vh;
  margin-left: var(--app-sidebar-width);
  padding: 40px clamp(24px, 5vw, 72px) 60px;
}

.mobile-header,
.mobile-nav {
  display: none;
}

@media (width <= 820px) {
  .sidebar {
    display: none;
  }

  .mobile-header {
    position: sticky;
    z-index: 10;
    top: 0;
    display: flex;
    height: 66px;
    padding: 0 20px;
    align-items: center;
    justify-content: space-between;
    background: rgb(255 255 255 / 90%);
    border-bottom: 1px solid var(--app-border-color);
    backdrop-filter: blur(16px);
  }

  .mobile-header__identity {
    display: flex;
    align-items: center;
    gap: 7px;
    font-size: 13px;
  }

  .main-content {
    margin-left: 0;
    padding: 24px 18px 100px;
  }

  .mobile-nav {
    position: fixed;
    z-index: 20;
    right: 12px;
    bottom: 12px;
    left: 12px;
    display: flex;
    max-width: calc(100vw - 24px);
    padding: 7px;
    overflow-x: auto;
    background: rgb(255 255 255 / 96%);
    border: 1px solid rgb(225 228 232 / 90%);
    border-radius: 20px;
    box-shadow: 0 16px 44px rgb(45 52 54 / 14%);
    backdrop-filter: blur(18px);
  }

  .mobile-nav a {
    display: flex;
    min-width: 66px;
    padding: 7px 8px;
    align-items: center;
    border-radius: 14px;
    color: #858c93;
    flex-direction: column;
    gap: 3px;
    text-decoration: none;
  }

  .mobile-nav a > span {
    font-size: 17px;
  }

  .mobile-nav a small {
    font-size: 10px;
    white-space: nowrap;
  }

  .mobile-nav a.active {
    background: #fff1e8;
    color: #d9651d;
    font-weight: 700;
  }
}
</style>
