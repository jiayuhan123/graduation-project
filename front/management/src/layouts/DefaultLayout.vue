<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { MessagePlugin } from 'tdesign-vue-next'
import AppIcon from '@/components/AppIcon.vue'
import BrandLogo from '@/components/BrandLogo.vue'
import { roleMeta } from '@/constants/roles'
import { getVisibleNavigationGroups } from '@/router/navigation'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const mobileOpen = ref(false)
const isMobile = ref(false)
const sidebar = ref<HTMLElement>()
const menuButton = ref<HTMLButtonElement>()
const closeButton = ref<HTMLButtonElement>()
let mediaQuery: MediaQueryList | undefined
let previousOverflow = ''
const visibleNavigationGroups = computed(() =>
  getVisibleNavigationGroups({
    roles: userStore.activeRole ? [userStore.activeRole] : userStore.roles,
    permissions: userStore.permissions,
  }),
)
const currentRole = computed(() =>
  roleMeta(userStore.activeRole || userStore.roles[0] || 'student'),
)
const pageTitle = computed(
  () =>
    visibleNavigationGroups.value
      .flatMap((group) => group.items)
      .find((item) => item.path === route.path)?.title ||
    route.meta.title ||
    '数学成长空间',
)
const activeGroup = computed(
  () =>
    visibleNavigationGroups.value.find((group) =>
      group.items.some((item) => item.path === route.path),
    )?.title,
)

function updateViewport() {
  isMobile.value = mediaQuery?.matches ?? false
  if (!isMobile.value) mobileOpen.value = false
}
function closeMenu() {
  mobileOpen.value = false
}
function handleSidebarKeydown(event: KeyboardEvent) {
  if (!mobileOpen.value) return
  if (event.key === 'Escape') {
    event.preventDefault()
    closeMenu()
    return
  }
  if (event.key !== 'Tab') return
  const elements = Array.from(
    sidebar.value?.querySelectorAll<HTMLElement>('a, button, select') ?? [],
  ).filter((element) => element.getClientRects().length && !element.hasAttribute('disabled'))
  const first = elements[0]
  const last = elements[elements.length - 1]
  if (event.shiftKey && document.activeElement === first) {
    event.preventDefault()
    last?.focus()
  }
  if (!event.shiftKey && document.activeElement === last) {
    event.preventDefault()
    first?.focus()
  }
}
watch(mobileOpen, async (open) => {
  if (open) {
    previousOverflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    await nextTick()
    closeButton.value?.focus()
  } else {
    document.body.style.overflow = previousOverflow
    await nextTick()
    if (isMobile.value) menuButton.value?.focus()
  }
})
watch(() => route.fullPath, closeMenu)
onMounted(() => {
  mediaQuery = window.matchMedia('(max-width: 820px)')
  updateViewport()
  mediaQuery.addEventListener('change', updateViewport)
})
onBeforeUnmount(() => {
  mediaQuery?.removeEventListener('change', updateViewport)
  if (mobileOpen.value) document.body.style.overflow = previousOverflow
})
function switchRole(event: Event) {
  const role = (event.target as HTMLSelectElement).value
  if (role === userStore.activeRole) return
  userStore.setActiveRole(role)
  closeMenu()
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
    <div v-if="mobileOpen" class="sidebar-backdrop" aria-hidden="true" @click="closeMenu" />
    <aside
      id="app-navigation"
      ref="sidebar"
      class="sidebar"
      :class="{ 'sidebar--open': mobileOpen }"
      :inert="isMobile && !mobileOpen"
      :role="isMobile ? 'dialog' : undefined"
      :aria-modal="isMobile && mobileOpen ? true : undefined"
      aria-label="工作空间导航"
      @keydown="handleSidebarKeydown"
    >
      <div class="sidebar__brand">
        <RouterLink to="/dashboard" aria-label="数学成长空间工作台" @click="closeMenu"
          ><BrandLogo
        /></RouterLink>
        <button
          ref="closeButton"
          class="icon-button sidebar__close"
          type="button"
          aria-label="关闭导航"
          @click="closeMenu"
        >
          <AppIcon name="close" />
        </button>
      </div>
      <div class="workspace-identity">
        <AppIcon :name="currentRole.icon" :size="18" />
        <select
          v-if="userStore.roles.length > 1"
          :value="userStore.activeRole"
          aria-label="切换身份"
          @change="switchRole"
        >
          <option v-for="role in userStore.roles" :key="role" :value="role">
            {{ roleMeta(role).label }}工作空间
          </option>
        </select>
        <span v-else>{{ currentRole.label }}工作空间</span>
      </div>
      <nav class="sidebar__nav" aria-label="主导航">
        <section v-for="group in visibleNavigationGroups" :key="group.title">
          <p v-if="group.title !== '工作台'" class="nav-group-title">{{ group.title }}</p>
          <RouterLink
            v-for="item in group.items"
            :key="item.path"
            :to="item.path"
            class="nav-link"
            :class="{ active: route.path === item.path }"
            :aria-current="route.path === item.path ? 'page' : undefined"
            @click="closeMenu"
          >
            <AppIcon :name="item.icon" :size="18" /><span>{{ item.title }}</span>
          </RouterLink>
        </section>
      </nav>
      <footer class="sidebar__footer">
        <RouterLink to="/account/profile" class="sidebar__account" @click="closeMenu">
          <span class="avatar"
            ><img
              v-if="userStore.userInfo?.avatarUrl"
              :src="userStore.userInfo.avatarUrl"
              alt=""
            /><span v-else>{{ userStore.displayName.slice(0, 1) }}</span></span
          >
          <span class="sidebar__user"
            ><strong>{{ userStore.displayName }}</strong
            ><small>个人账号</small></span
          >
        </RouterLink>
        <button
          class="icon-button"
          type="button"
          aria-label="退出登录"
          title="退出登录"
          @click="handleLogout"
        >
          <AppIcon name="logout" :size="18" />
        </button>
      </footer>
    </aside>
    <div class="workspace" :inert="mobileOpen">
      <header class="workspace-header">
        <button
          ref="menuButton"
          class="icon-button menu-button"
          type="button"
          aria-label="打开导航"
          aria-controls="app-navigation"
          :aria-expanded="mobileOpen"
          @click="mobileOpen = true"
        >
          <AppIcon name="menu" />
        </button>
        <div class="breadcrumb">
          <span v-if="activeGroup && activeGroup !== pageTitle">{{ activeGroup }}</span
          ><AppIcon
            v-if="activeGroup && activeGroup !== pageTitle"
            name="chevron"
            :size="12"
          /><strong>{{ pageTitle }}</strong>
        </div>
        <RouterLink class="header-account" to="/account/profile"
          ><span class="role-label">{{ currentRole.label }}</span
          ><span>{{ userStore.displayName }}</span
          ><AppIcon name="user" :size="17"
        /></RouterLink>
      </header>
      <main id="main-content" class="main-content">
        <div class="page-container"><RouterView /></div>
      </main>
    </div>
  </div>
</template>

<style scoped lang="scss">
.app-shell {
  min-height: 100vh;
}

.sidebar {
  position: fixed;
  z-index: 30;
  inset: 0 auto 0 0;
  display: flex;
  width: var(--app-sidebar-width);
  background: #fff;
  border-right: 1px solid var(--app-border-color);
  flex-direction: column;
}

.sidebar__brand {
  display: flex;
  min-height: 80px;
  padding: 20px 18px;
  align-items: center;
  justify-content: space-between;
}

.sidebar__brand a {
  text-decoration: none;
}

.workspace-identity {
  display: flex;
  min-height: 40px;
  margin: 0 16px 18px;
  padding: 9px 12px;
  align-items: center;
  background: #f5f7fb;
  border: 1px solid var(--app-border-color);
  border-radius: 6px;
  color: var(--app-text);
  gap: 9px;
  font-size: 12px;
}

.workspace-identity select {
  min-width: 0;
  width: 100%;
  background: transparent;
  border: 0;
  color: inherit;
  cursor: pointer;
  font: inherit;
}

.sidebar__nav {
  display: flex;
  min-height: 0;
  padding: 0 12px 24px;
  overflow-y: auto;
  flex: 1;
  flex-direction: column;
  gap: 24px;
  scrollbar-width: thin;
}

.nav-group-title {
  margin: 0 14px 8px;
  color: var(--app-text-muted);
  font-size: 11px;
}

.nav-link {
  display: flex;
  min-height: 40px;
  margin-top: 3px;
  padding: 9px 14px;
  align-items: center;
  border-radius: 6px;
  color: var(--app-text);
  gap: 12px;
  text-decoration: none;
  font-size: 13px;
  transition: background 150ms;
}

.nav-link:hover {
  background: #f5f7fb;
}

.nav-link.active {
  background: var(--app-primary-soft);
  color: var(--app-primary);
  font-weight: 600;
}

.sidebar__footer {
  display: flex;
  padding: 16px;
  align-items: center;
  border-top: 1px solid var(--app-border-color);
  gap: 6px;
}

.sidebar__account {
  display: flex;
  min-width: 0;
  align-items: center;
  flex: 1;
  gap: 10px;
  text-decoration: none;
}

.avatar {
  display: grid;
  width: 34px;
  height: 34px;
  flex: none;
  place-items: center;
  overflow: hidden;
  background: var(--app-primary-soft);
  border-radius: 50%;
  color: var(--app-primary);
  font-weight: 600;
}

.avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.sidebar__user {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 3px;
}

.sidebar__user strong {
  overflow: hidden;
  color: var(--app-text-strong);
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 12px;
  font-weight: 500;
}

.sidebar__user small {
  color: var(--app-text-muted);
  font-size: 10px;
}

.icon-button {
  display: inline-flex;
  width: 34px;
  height: 34px;
  padding: 0;
  align-items: center;
  justify-content: center;
  flex: none;
  background: transparent;
  border: 0;
  border-radius: 6px;
  color: var(--app-text);
  cursor: pointer;
}

.icon-button:hover {
  background: var(--app-primary-soft);
  color: var(--app-primary);
}

.workspace {
  min-width: 0;
  margin-left: var(--app-sidebar-width);
}

.workspace-header {
  display: flex;
  min-height: 64px;
  padding: 0 clamp(24px, 3vw, 48px);
  align-items: center;
  background: #fff;
  border-bottom: 1px solid var(--app-border-color);
  gap: 12px;
}

.breadcrumb {
  display: flex;
  min-width: 0;
  align-items: center;
  color: var(--app-text-muted);
  gap: 12px;
  font-size: 12px;
}

.breadcrumb strong {
  overflow: hidden;
  color: var(--app-text);
  font-weight: 500;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.header-account {
  display: flex;
  max-width: 45%;
  margin-left: auto;
  align-items: center;
  color: var(--app-text);
  gap: 12px;
  text-decoration: none;
  font-size: 12px;
}

.header-account > span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.role-label {
  padding: 3px 8px;
  background: #f5f7fb;
  border-radius: 4px;
  color: var(--app-text-muted);
  font-size: 11px;
}

.main-content {
  padding: 32px clamp(24px, 3vw, 48px) 56px;
}

.page-container {
  max-width: 1440px;
  margin: 0 auto;
}

.menu-button,
.sidebar__close,
.sidebar-backdrop {
  display: none;
}

@media (width <= 820px) {
  .sidebar {
    width: min(288px, calc(100vw - 40px));
    visibility: hidden;
    transform: translateX(-100%);
  }

  .sidebar--open {
    visibility: visible;
    transform: translateX(0);
  }

  .sidebar__brand {
    padding: 20px 16px;
  }

  .menu-button,
  .sidebar__close {
    display: inline-flex;
  }

  .sidebar-backdrop {
    position: fixed;
    z-index: 25;
    inset: 0;
    display: block;
    background: rgb(20 38 68 / 35%);
  }

  .workspace {
    margin-left: 0;
  }

  .workspace-header {
    min-height: 60px;
    padding: 0 16px;
  }

  .breadcrumb > span,
  .breadcrumb > .app-icon,
  .header-account .role-label {
    display: none;
  }

  .main-content {
    padding: 24px 18px 40px;
  }
}

@media (width <= 380px) {
  .main-content {
    padding-right: 14px;
    padding-left: 14px;
  }

  .workspace-header {
    padding-right: 14px;
    padding-left: 10px;
    gap: 6px;
  }
}
</style>
