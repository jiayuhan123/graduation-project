<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  Button as TButton,
  Form as TForm,
  FormItem as TFormItem,
  Input as TInput,
  Loading as TLoading,
  MessagePlugin,
  Tag as TTag,
  type FormRules,
  type SubmitContext,
} from 'tdesign-vue-next'
import { changePasswordApi, getCurrentUserApi, initializePasswordApi } from '@/api/user'
import PageHeader from '@/components/PageHeader.vue'
import { useUserStore } from '@/stores/user'
import type { UserProfile } from '@/types/user'

interface PasswordForm {
  oldPassword: string
  newPassword: string
  confirmPassword: string
}

const router = useRouter()
const userStore = useUserStore()
const loading = ref(true)
const saving = ref(false)
const profile = ref<UserProfile | null>(null)
const form = reactive<PasswordForm>({ oldPassword: '', newPassword: '', confirmPassword: '' })

const rules = computed<FormRules<PasswordForm>>(() => ({
  oldPassword: profile.value?.passwordSet
    ? [{ required: true, message: '请输入当前密码', trigger: 'blur' }]
    : [],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 72, message: '密码长度为 6–72 位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (value) => value === form.newPassword,
      message: '两次输入的密码不一致',
      trigger: 'blur',
    },
  ],
}))

onMounted(async () => {
  try {
    profile.value = await getCurrentUserApi()
    userStore.syncProfile(profile.value)
  } finally {
    loading.value = false
  }
})

async function submit(context: SubmitContext) {
  if (context.validateResult !== true || !profile.value) return
  saving.value = true
  try {
    if (profile.value.passwordSet) {
      await changePasswordApi({ oldPassword: form.oldPassword, newPassword: form.newPassword })
    } else {
      await initializePasswordApi({ newPassword: form.newPassword })
    }
    userStore.clearSession()
    void MessagePlugin.success('密码已更新，请重新登录')
    await router.replace({ name: 'login' })
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div>
    <PageHeader
      eyebrow="个人账号"
      title="账号安全"
      description="密码变更后会撤销当前账号的全部登录会话。"
    />
    <div class="security-grid">
      <section class="surface-card password-card">
        <div v-if="loading" class="loading-area"><t-loading text="正在检查账号安全状态" /></div>
        <template v-else-if="profile">
          <div class="card-heading">
            <span class="security-icon">◇</span>
            <div>
              <h2>{{ profile.passwordSet ? '修改登录密码' : '设置登录密码' }}</h2>
              <p>
                {{
                  profile.passwordSet
                    ? '验证当前密码后设置新密码'
                    : '设置后也可在 Web 端使用账号密码登录'
                }}
              </p>
            </div>
          </div>
          <t-form :data="form" :rules="rules" label-align="top" @submit="submit">
            <t-form-item v-if="profile.passwordSet" label="当前密码" name="oldPassword">
              <t-input
                v-model="form.oldPassword"
                type="password"
                autocomplete="current-password"
                placeholder="请输入当前密码"
              />
            </t-form-item>
            <t-form-item label="新密码" name="newPassword">
              <t-input
                v-model="form.newPassword"
                type="password"
                autocomplete="new-password"
                placeholder="6–72 位字符"
              />
            </t-form-item>
            <t-form-item label="确认新密码" name="confirmPassword">
              <t-input
                v-model="form.confirmPassword"
                type="password"
                autocomplete="new-password"
                placeholder="请再次输入新密码"
              />
            </t-form-item>
            <t-button type="submit" :loading="saving">
              {{ profile.passwordSet ? '确认修改' : '设置密码' }}
            </t-button>
          </t-form>
        </template>
      </section>

      <aside class="surface-card security-status">
        <h3>登录方式</h3>
        <div class="status-item">
          <span class="status-item__icon status-item__icon--wechat">微</span>
          <span><strong>微信身份</strong><small>小程序与 Web 跨端识别</small></span>
          <t-tag :theme="profile?.wechatBound ? 'success' : 'default'" variant="light">
            {{ profile?.wechatBound ? '已绑定' : '未绑定' }}
          </t-tag>
        </div>
        <div class="status-item">
          <span class="status-item__icon">Aa</span>
          <span><strong>账号密码</strong><small>用户名、手机号或邮箱登录</small></span>
          <t-tag :theme="profile?.passwordSet ? 'success' : 'warning'" variant="light">
            {{ profile?.passwordSet ? '已设置' : '待设置' }}
          </t-tag>
        </div>
        <p class="security-tip">
          账号必须至少保留一种登录方式。微信访问令牌不会在平台数据库中保存。
        </p>
      </aside>
    </div>
  </div>
</template>

<style scoped lang="scss">
.security-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(300px, 0.72fr);
  gap: 20px;
}

.password-card,
.security-status {
  padding: clamp(24px, 4vw, 34px);
}

.password-card {
  max-width: 720px;
}

.loading-area {
  display: grid;
  min-height: 300px;
  place-items: center;
}

.card-heading {
  display: flex;
  margin-bottom: 28px;
  align-items: center;
  gap: 15px;
}

.security-icon {
  display: grid;
  width: 52px;
  height: 52px;
  place-items: center;
  background: var(--app-primary-soft);
  border-radius: 16px;
  color: var(--app-primary);
  font-size: 23px;
}

.card-heading h2,
.security-status h3 {
  color: var(--app-text-strong);
  font-size: 19px;
}

.card-heading p {
  margin-top: 5px;
  color: var(--app-text-muted);
  font-size: 11px;
}

.status-item {
  display: grid;
  padding: 18px 0;
  align-items: center;
  border-bottom: 1px solid #eff1f3;
  grid-template-columns: 44px 1fr auto;
  gap: 12px;
}

.status-item__icon {
  display: grid;
  width: 40px;
  height: 40px;
  place-items: center;
  background: #f0f3f6;
  border-radius: 13px;
  color: #596168;
  font-size: 12px;
  font-weight: 600;
}

.status-item__icon--wechat {
  background: #e9f8ef;
  color: #079848;
}

.status-item > span:nth-child(2) {
  display: flex;
  flex-direction: column;
}

.status-item strong {
  color: var(--app-text-strong);
  font-size: 13px;
}

.status-item small {
  margin-top: 4px;
  color: var(--app-text-muted);
  font-size: 10px;
}

.security-tip {
  margin-top: 20px;
  padding: 15px;
  background: #f7f8fa;
  border-radius: 14px;
  color: var(--app-text-muted);
  font-size: 11px;
  line-height: 1.7;
}

@media (width <= 980px) {
  .security-grid {
    grid-template-columns: 1fr;
  }
}
</style>
