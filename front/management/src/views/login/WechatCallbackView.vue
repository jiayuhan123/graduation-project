<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Button as TButton, Loading as TLoading } from 'tdesign-vue-next'
import BrandLogo from '@/components/BrandLogo.vue'
import { STORAGE_KEYS } from '@/constants/storage'
import { safeRedirect } from '@/router/redirect'
import { useUserStore } from '@/stores/user'
import { storage } from '@/utils/storage'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const status = ref<'loading' | 'error'>('loading')
const errorMessage = ref('')
const denied = computed(() => !route.query.code)

onMounted(async () => {
  const code = typeof route.query.code === 'string' ? route.query.code : ''
  const state = typeof route.query.state === 'string' ? route.query.state : ''
  if (!code || !state) {
    status.value = 'error'
    errorMessage.value = denied.value ? '你已取消微信授权' : '微信回调参数不完整'
    return
  }

  try {
    await userStore.completeWechatWebLogin({ code, state })
    const target = safeRedirect(storage.getString(STORAGE_KEYS.loginRedirect), '/dashboard')
    storage.remove(STORAGE_KEYS.loginRedirect)
    await router.replace(target)
  } catch (error) {
    status.value = 'error'
    errorMessage.value = error instanceof Error ? error.message : '微信登录失败，请重新扫码'
  }
})
</script>

<template>
  <main class="callback-page">
    <section class="callback-card">
      <BrandLogo />
      <template v-if="status === 'loading'">
        <t-loading size="36px" />
        <h1>正在确认微信身份</h1>
        <p>请稍候，我们正在建立安全会话。</p>
      </template>
      <template v-else>
        <span class="error-mark">!</span>
        <h1>微信登录未完成</h1>
        <p>{{ errorMessage }}</p>
        <t-button @click="router.replace({ name: 'login' })">返回登录</t-button>
      </template>
    </section>
  </main>
</template>

<style scoped lang="scss">
.callback-page {
  display: grid;
  min-height: 100vh;
  padding: 24px;
  background: var(--app-primary-soft);
  place-items: center;
}

.callback-card {
  display: flex;
  width: min(100%, 430px);
  padding: 42px;
  align-items: center;
  background: #fff;
  border: 1px solid var(--app-border-color);
  border-radius: 12px;
  box-shadow: none;
  flex-direction: column;
  text-align: center;
}

.callback-card .brand {
  margin-bottom: 40px;
}

.callback-card h1 {
  margin-top: 22px;
  color: var(--app-text-strong);
  font-size: 23px;
}

.callback-card p {
  margin: 9px 0 26px;
  color: var(--app-text-muted);
}

.error-mark {
  display: grid;
  width: 46px;
  height: 46px;
  place-items: center;
  background: #fff0ef;
  border-radius: 50%;
  color: #d54941;
  font-size: 23px;
  font-weight: 600;
}
</style>
