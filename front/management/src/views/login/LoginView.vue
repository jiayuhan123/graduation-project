<script setup lang="ts">
import AppIcon from '@/components/AppIcon.vue'
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  Button as TButton,
  Form as TForm,
  FormItem as TFormItem,
  Input as TInput,
  MessagePlugin,
  Option as TOption,
  Select as TSelect,
  type FormRules,
  type SubmitContext,
} from 'tdesign-vue-next'
import { beginWechatWebLoginApi } from '@/api/user'
import BrandLogo from '@/components/BrandLogo.vue'
import { appConfig } from '@/config/app'
import { STORAGE_KEYS } from '@/constants/storage'
import { safeRedirect } from '@/router/redirect'
import { useUserStore } from '@/stores/user'
import type { LoginParams, WechatWebAuthorizeInput } from '@/types/user'
import { storage } from '@/utils/storage'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const loginMode = ref<'wechat' | 'password'>('password')
const passwordLoading = ref(false)
const wechatLoading = ref(false)
const wechatLoginAvailable = appConfig.wechatWebLoginEnabled

const passwordForm = reactive<LoginParams>({ username: '', password: '' })
const wechatForm = reactive<WechatWebAuthorizeInput>({ gradeLevel: 3, semester: 1 })

const passwordRules: FormRules<LoginParams> = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

async function handlePasswordLogin(context: SubmitContext) {
  if (context.validateResult !== true) return
  passwordLoading.value = true
  try {
    await userStore.login({ ...passwordForm, username: passwordForm.username.trim() })
    void MessagePlugin.success('登录成功')
    await router.replace(safeRedirect(route.query.redirect, '/dashboard'))
  } finally {
    passwordLoading.value = false
  }
}

async function handleWechatLogin() {
  if (!wechatLoginAvailable) {
    void MessagePlugin.info('微信扫码登录暂未开放，请先使用账号密码登录')
    return
  }
  if (wechatLoading.value) return
  wechatLoading.value = true
  try {
    const authorization = await beginWechatWebLoginApi({
      ...wechatForm,
      nickname: wechatForm.nickname?.trim() || undefined,
      realName: wechatForm.realName?.trim() || undefined,
    })
    storage.setString(STORAGE_KEYS.loginRedirect, safeRedirect(route.query.redirect, '/dashboard'))
    window.location.assign(authorization.authorizationUrl)
  } finally {
    wechatLoading.value = false
  }
}
</script>

<template>
  <main class="login-page">
    <section class="login-intro">
      <BrandLogo />
      <div class="login-intro__copy">
        <p class="eyebrow">数学成长空间</p>
        <h1>学有方法，<br /><span>进步有迹可循。</span></h1>
        <p>让练习更有方向，让教学更有依据。<br />在每一次思考中，积累一点新的收获。</p>
      </div>
      <div class="math-sheet" aria-hidden="true">
        <div class="math-sheet__heading">
          <span>探索 · 理解 · 应用</span><span>MATHEMATICS</span>
        </div>
        <svg viewBox="0 0 460 220" fill="none">
          <defs>
            <pattern id="math-grid" width="22" height="22" patternUnits="userSpaceOnUse">
              <path d="M22 0H0v22" stroke="#e8eef9" stroke-width="1" />
            </pattern>
          </defs>
          <rect width="460" height="220" fill="url(#math-grid)" />
          <path d="M34 176h226M56 192V30" stroke="#acbdd9" />
          <path d="m58 172 55-106 117 106Z" fill="#edf3fe" stroke="#3567c8" stroke-width="2" />
          <path d="M113 66v106" stroke="#3567c8" stroke-dasharray="5 5" />
          <path d="M113 162h10v10" stroke="#3567c8" />
          <circle cx="113" cy="66" r="4" fill="#3567c8" />
          <circle cx="58" cy="172" r="4" fill="#3567c8" />
          <circle cx="230" cy="172" r="4" fill="#3567c8" />
          <text x="102" y="48" fill="#3567c8" font-size="14" font-family="Georgia">A</text>
          <text x="40" y="174" fill="#3567c8" font-size="14" font-family="Georgia">B</text>
          <text x="240" y="175" fill="#3567c8" font-size="14" font-family="Georgia">C</text>
          <text x="295" y="93" fill="#20304b" font-size="24" font-family="Georgia">S = ½ ah</text>
          <path d="M296 111h116" stroke="#c6d6f3" />
          <text x="295" y="141" fill="#7b889b" font-size="11">从理解开始</text>
        </svg>
        <div class="math-sheet__foot">
          <span>把问题想明白，把知识学扎实。</span><AppIcon name="arrow" :size="18" />
        </div>
      </div>
      <div class="role-preview" aria-label="支持的角色">
        <span><AppIcon name="book" :size="16" />学生学习</span
        ><span><AppIcon name="edit" :size="16" />教师教学</span
        ><span><AppIcon name="shield" :size="16" />平台管理</span>
      </div>
    </section>

    <section class="login-panel">
      <div class="login-panel__heading">
        <p class="welcome-label">欢迎回来</p>
        <h2>登录你的账号</h2>
        <p>使用小程序账号，继续学习与教学</p>
      </div>

      <div class="mode-switch" role="group" aria-label="登录方式">
        <button
          type="button"
          :aria-pressed="loginMode === 'password'"
          :class="{ active: loginMode === 'password' }"
          @click="loginMode = 'password'"
        >
          账号密码
        </button>
        <button
          type="button"
          :aria-pressed="loginMode === 'wechat'"
          :class="{ active: loginMode === 'wechat' }"
          @click="loginMode = 'wechat'"
        >
          微信扫码 <span v-if="!wechatLoginAvailable" class="mode-status">未开放</span>
        </button>
      </div>

      <div v-if="loginMode === 'wechat'" class="wechat-login">
        <div class="wechat-visual">
          <span class="wechat-icon"><AppIcon name="camera" :size="28" /></span>
          <div>
            <strong>{{
              wechatLoginAvailable ? '使用微信扫码登录' : '微信扫码登录暂未开放'
            }}</strong>
            <p v-if="wechatLoginAvailable">
              点击下方按钮打开微信官方二维码，手机扫码确认后会自动返回。
            </p>
            <p v-else>暂时无法使用微信扫码，请切换到账号密码登录。</p>
          </div>
        </div>

        <div v-if="wechatLoginAvailable" class="first-login-fields">
          <p>首次登录资料 <span>已有账号扫码时不会覆盖</span></p>
          <div class="two-column">
            <t-input v-model="wechatForm.nickname" maxlength="50" placeholder="昵称（选填）" />
            <t-input v-model="wechatForm.realName" maxlength="50" placeholder="真实姓名（选填）" />
          </div>
          <div class="two-column">
            <t-select v-model="wechatForm.gradeLevel" placeholder="年级">
              <t-option v-for="grade in 6" :key="grade" :value="grade" :label="`${grade} 年级`" />
            </t-select>
            <t-select v-model="wechatForm.semester" placeholder="学期">
              <t-option :value="1" label="上学期" />
              <t-option :value="2" label="下学期" />
            </t-select>
          </div>
        </div>

        <t-button
          block
          size="large"
          :disabled="!wechatLoginAvailable"
          :loading="wechatLoading"
          @click="handleWechatLogin"
        >
          {{ wechatLoginAvailable ? '打开微信登录二维码' : '功能暂未开放' }}
        </t-button>
        <p v-if="wechatLoginAvailable" class="privacy-note">
          在微信中确认登录后，将自动返回你的工作空间。
        </p>
      </div>

      <t-form
        v-else
        class="password-form"
        :colon="false"
        label-align="top"
        :data="passwordForm"
        :rules="passwordRules"
        @submit="handlePasswordLogin"
      >
        <t-form-item label="用户名 / 手机号 / 邮箱" name="username">
          <t-input
            v-model="passwordForm.username"
            name="username"
            autocomplete="username"
            maxlength="254"
            placeholder="请输入登录账号"
            clearable
          />
        </t-form-item>
        <t-form-item label="密码" name="password">
          <t-input
            v-model="passwordForm.password"
            name="password"
            type="password"
            autocomplete="current-password"
            maxlength="72"
            placeholder="请输入密码"
            clearable
          />
        </t-form-item>
        <t-button theme="primary" type="submit" size="large" :loading="passwordLoading" block>
          登录
        </t-button>
        <div class="registration-guide">
          <strong>还没有账号？</strong>
          <p>请先前往微信小程序完成注册，再进入“账号安全”设置登录账号和密码。</p>
        </div>
        <p class="password-note">教师和管理员可使用后台分配的账号登录。</p>
      </t-form>
    </section>
  </main>
</template>

<style scoped lang="scss">
.login-page {
  display: grid;
  min-height: 100vh;
  background: #fff;
  grid-template-columns: 1.1fr 1fr;
}

.login-intro {
  display: flex;
  padding: 36px clamp(32px, 5vw, 80px);
  background: #edf3fe;
  flex-direction: column;
  justify-content: center;
}

.login-intro > .brand {
  margin-bottom: auto;
}

.login-intro__copy {
  margin-top: 48px;
}

.eyebrow {
  color: var(--app-primary);
  font-size: 12px;
  letter-spacing: 0.14em;
}

.login-intro h1 {
  margin-top: 18px;
  color: var(--app-text-strong);
  font-size: clamp(32px, 3.2vw, 48px);
  font-weight: 600;
  line-height: 1.5;
  letter-spacing: -0.035em;
}

.login-intro h1 span {
  color: var(--app-primary);
}

.login-intro__copy > p:last-child {
  margin-top: 20px;
  color: #687e9e;
  font-size: 14px;
  line-height: 1.9;
}

.math-sheet {
  max-width: 520px;
  margin-top: 36px;
  padding: 18px 22px 0;
  background: rgb(255 255 255 / 85%);
  border: 1px solid #d7e3f7;
  border-radius: 8px;
}

.math-sheet__heading {
  display: flex;
  padding-bottom: 15px;
  align-items: center;
  justify-content: space-between;
  color: #687e9e;
  gap: 12px;
  font-size: 11px;
}

.math-sheet__heading > span:last-child {
  font-size: 9px;
  letter-spacing: 0.12em;
}

.math-sheet svg:not(.app-icon) {
  display: block;
  width: 100%;
}

.math-sheet__foot {
  display: flex;
  padding: 15px 0;
  align-items: center;
  justify-content: space-between;
  border-top: 1px solid var(--app-border-color);
  color: #687e9e;
  font-size: 11px;
}

.role-preview {
  display: flex;
  margin-top: auto;
  padding-top: 30px;
  flex-wrap: wrap;
  gap: 24px;
  color: #687e9e;
  font-size: 11px;
}

.role-preview > span {
  display: flex;
  align-items: center;
  gap: 7px;
}

.login-panel {
  width: min(100%, 460px);
  margin: auto;
  padding: 56px 36px;
}

.welcome-label {
  color: var(--app-text-muted);
  font-size: 13px;
}

.login-panel__heading h2 {
  margin-top: 12px;
  color: var(--app-text-strong);
  font-size: 28px;
  font-weight: 600;
}

.login-panel__heading > p:last-child {
  margin-top: 10px;
  color: var(--app-text-muted);
  font-size: 13px;
}

.mode-switch {
  display: flex;
  margin-top: 32px;
  border-bottom: 1px solid var(--app-border-color);
  gap: 26px;
}

.mode-switch button {
  position: relative;
  padding: 14px 0;
  background: transparent;
  border: 0;
  color: var(--app-text-muted);
  cursor: pointer;
  font: inherit;
  font-size: 13px;
}

.mode-switch button.active {
  color: var(--app-primary);
  font-weight: 600;
}

.mode-switch button.active::after {
  position: absolute;
  inset: auto 0 -1px;
  height: 2px;
  background: var(--app-primary);
  content: '';
}

.mode-status {
  margin-left: 3px;
  padding: 2px 4px;
  background: #f2f4f7;
  border-radius: 3px;
  color: var(--app-text-muted);
  font-size: 9px;
  font-weight: 400;
}

.wechat-login,
.password-form {
  margin-top: 28px;
}

.password-form :deep(.t-form__label) {
  padding-bottom: 8px;
  color: var(--app-text);
  font-size: 12px;
}

.password-form :deep(.t-input) {
  min-height: 44px;
  border-radius: 6px;
}

.password-form :deep(.t-button) {
  height: 44px;
  border-radius: 6px;
}

.password-form :deep(.t-form__item) {
  margin-bottom: 22px;
}

.wechat-visual {
  display: flex;
  margin-bottom: 22px;
  padding: 20px;
  align-items: flex-start;
  background: #f5f7fb;
  border: 1px solid var(--app-border-color);
  border-radius: 8px;
  gap: 16px;
}

.wechat-icon {
  display: grid;
  width: 44px;
  height: 44px;
  flex: none;
  place-items: center;
  background: #fff;
  border-radius: 8px;
  color: var(--app-primary);
}

.wechat-visual strong {
  color: var(--app-text-strong);
  font-size: 14px;
  font-weight: 500;
}

.wechat-visual p {
  margin-top: 8px;
  color: var(--app-text-muted);
  font-size: 12px;
  line-height: 1.7;
}

.first-login-fields {
  margin: 22px 0;
}

.first-login-fields > p {
  color: var(--app-text);
  font-size: 12px;
}

.first-login-fields > p span {
  display: block;
  margin-top: 5px;
  color: var(--app-text-muted);
  font-size: 11px;
}

.two-column {
  display: grid;
  margin-top: 12px;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 10px;
}

.privacy-note,
.password-note {
  margin-top: 18px;
  color: var(--app-text-muted);
  font-size: 11px;
  line-height: 1.8;
}

.registration-guide {
  margin-top: 28px;
  padding-top: 24px;
  border-top: 1px solid var(--app-border-color);
  font-size: 12px;
  line-height: 1.8;
}

.registration-guide strong {
  color: var(--app-text);
  font-weight: 500;
}

.registration-guide p {
  margin-top: 6px;
  color: var(--app-text-muted);
}

@media (width <= 980px) {
  .login-intro {
    padding: 32px;
  }

  .login-panel {
    padding: 40px 28px;
  }

  .math-sheet {
    padding-right: 14px;
    padding-left: 14px;
  }

  .role-preview {
    gap: 16px;
  }
}

@media (width <= 720px) {
  .login-page {
    display: block;
  }

  .login-intro {
    padding: 22px 24px;
    background: #fff;
    border-bottom: 1px solid var(--app-border-color);
  }

  .login-intro__copy,
  .math-sheet,
  .role-preview {
    display: none;
  }

  .login-panel {
    max-width: 460px;
    padding: 40px 24px;
  }

  .login-panel__heading h2 {
    font-size: 26px;
  }
}

@media (width <= 360px) {
  .login-intro {
    padding-right: 20px;
    padding-left: 20px;
  }

  .login-panel {
    padding: 32px 20px;
  }

  .mode-switch {
    gap: 22px;
  }

  .wechat-visual {
    padding: 14px;
    gap: 10px;
  }
}
</style>
