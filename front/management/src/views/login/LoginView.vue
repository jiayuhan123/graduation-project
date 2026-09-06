<script setup lang="ts">
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
    <span class="orb orb--orange" aria-hidden="true" />
    <span class="orb orb--blue" aria-hidden="true" />

    <section class="login-intro">
      <BrandLogo />
      <div class="login-intro__copy">
        <p class="eyebrow">ONE ACCOUNT · EVERY ROLE</p>
        <h1>让每一次数学成长<br />都有迹可循</h1>
        <p>Web 与微信小程序共享同一账号。学生、教师和管理员登录后，只看到当前角色对应的空间。</p>
      </div>
      <div class="role-preview" aria-label="支持的角色">
        <div><span>🎒</span><strong>学生</strong><small>练习与成长</small></div>
        <div><span>📐</span><strong>教师</strong><small>教学与反馈</small></div>
        <div><span>🛡️</span><strong>管理员</strong><small>用户与权限</small></div>
      </div>
    </section>

    <section class="login-panel">
      <div class="login-panel__heading">
        <span class="welcome-badge">欢迎回来</span>
        <h2>登录数学成长空间</h2>
        <p>同一个账号可以拥有并切换多个角色</p>
      </div>

      <div class="mode-switch" role="tablist" aria-label="登录方式">
        <button
          type="button"
          role="tab"
          :aria-selected="loginMode === 'wechat'"
          :class="{ active: loginMode === 'wechat' }"
          @click="loginMode = 'wechat'"
        >
          微信扫码 <span v-if="!wechatLoginAvailable" class="mode-status">未开放</span>
        </button>
        <button
          type="button"
          role="tab"
          :aria-selected="loginMode === 'password'"
          :class="{ active: loginMode === 'password' }"
          @click="loginMode = 'password'"
        >
          账号密码
        </button>
      </div>

      <div v-if="loginMode === 'wechat'" class="wechat-login">
        <div class="wechat-visual">
          <div class="qr-frame" aria-hidden="true">
            <div class="qr-pattern">▦</div>
            <span class="wechat-mark">微</span>
          </div>
          <div>
            <strong>{{
              wechatLoginAvailable ? '使用微信扫码登录' : '微信扫码登录暂未开放'
            }}</strong>
            <p v-if="wechatLoginAvailable">
              点击下方按钮打开微信官方二维码，手机扫码确认后会自动返回。
            </p>
            <p v-else>网站部署并完成微信开放平台审核后开放，请先使用账号密码登录。</p>
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
          仅接收微信用于识别账号的 OpenID / UnionID，不保存微信访问令牌。
        </p>
      </div>

      <t-form
        v-else
        class="password-form"
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
        <t-form-item>
          <t-button theme="primary" type="submit" size="large" :loading="passwordLoading" block>
            登录
          </t-button>
        </t-form-item>
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
  position: relative;
  display: grid;
  min-height: 100vh;
  padding: clamp(28px, 6vw, 80px);
  overflow: hidden;
  background: linear-gradient(135deg, #fff9f4 0%, #f8f9fb 48%, #f2f7fb 100%);
  grid-template-columns: minmax(320px, 1fr) minmax(380px, 500px);
  gap: clamp(48px, 9vw, 140px);
  place-items: center;
}

.orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(4px);
  pointer-events: none;
}

.orb--orange {
  top: -170px;
  right: 28%;
  width: 420px;
  height: 420px;
  background: rgb(255 138 61 / 11%);
}

.orb--blue {
  bottom: -220px;
  left: 8%;
  width: 460px;
  height: 460px;
  background: rgb(77 171 247 / 9%);
}

.login-intro,
.login-panel {
  position: relative;
  z-index: 1;
}

.login-intro {
  width: min(100%, 620px);
}

.login-intro__copy {
  margin-top: clamp(58px, 9vh, 110px);
}

.eyebrow {
  color: var(--app-primary);
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.2em;
}

.login-intro h1 {
  margin-top: 18px;
  color: #252b30;
  font-size: clamp(42px, 5.2vw, 72px);
  font-weight: 850;
  letter-spacing: -0.055em;
  line-height: 1.13;
}

.login-intro__copy > p:last-child {
  max-width: 530px;
  margin-top: 26px;
  color: #747c83;
  font-size: 16px;
  line-height: 1.9;
}

.role-preview {
  display: grid;
  max-width: 560px;
  margin-top: 54px;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.role-preview > div {
  display: grid;
  padding: 17px;
  background: rgb(255 255 255 / 70%);
  border: 1px solid rgb(232 234 237 / 90%);
  border-radius: 18px;
  grid-template-columns: auto 1fr;
  column-gap: 10px;
}

.role-preview span {
  grid-row: 1 / 3;
  font-size: 25px;
}

.role-preview strong {
  color: #3f474d;
  font-size: 13px;
}

.role-preview small {
  margin-top: 3px;
  color: #9aa0a6;
  font-size: 10px;
}

.login-panel {
  width: 100%;
  padding: clamp(28px, 4vw, 44px);
  background: rgb(255 255 255 / 94%);
  border: 1px solid rgb(229 232 235 / 95%);
  border-radius: 32px;
  box-shadow: 0 30px 90px rgb(45 52 54 / 13%);
  backdrop-filter: blur(18px);
}

.welcome-badge {
  display: inline-flex;
  padding: 6px 10px;
  background: #fff1e8;
  border-radius: 999px;
  color: #d9651d;
  font-size: 11px;
  font-weight: 700;
}

.login-panel__heading h2 {
  margin-top: 15px;
  color: var(--app-text-strong);
  font-size: 26px;
}

.login-panel__heading p {
  margin-top: 8px;
  color: var(--app-text-muted);
}

.mode-switch {
  display: flex;
  margin-top: 28px;
  padding: 5px;
  background: #f2f3f5;
  border-radius: 999px;
}

.mode-switch button {
  width: 50%;
  padding: 10px 14px;
  background: transparent;
  border: 0;
  border-radius: 999px;
  color: #7d858c;
  cursor: pointer;
  font: inherit;
}

.mode-switch button.active {
  background: #fff;
  box-shadow: 0 5px 15px rgb(45 52 54 / 8%);
  color: #d9651d;
  font-weight: 700;
}

.mode-status {
  display: inline-flex;
  margin-left: 4px;
  padding: 2px 6px;
  background: #eceff1;
  border-radius: 999px;
  color: #8d959b;
  font-size: 9px;
  line-height: 1.4;
  vertical-align: 1px;
}

.wechat-login,
.password-form {
  margin-top: 26px;
}

.wechat-visual {
  display: flex;
  padding: 18px;
  align-items: center;
  background: #f2faf5;
  border: 1px solid #dcefe3;
  border-radius: 20px;
  gap: 17px;
}

.qr-frame {
  position: relative;
  display: grid;
  width: 76px;
  height: 76px;
  flex: none;
  place-items: center;
  background: #fff;
  border: 7px solid #fff;
  border-radius: 15px;
  box-shadow: 0 6px 18px rgb(19 112 58 / 10%);
}

.qr-pattern {
  color: #2f3c35;
  font-family: monospace;
  font-size: 55px;
  line-height: 1;
}

.wechat-mark {
  position: absolute;
  display: grid;
  width: 25px;
  height: 25px;
  place-items: center;
  background: #07c160;
  border: 2px solid #fff;
  border-radius: 7px;
  color: #fff;
  font-size: 10px;
  font-weight: 800;
}

.wechat-visual strong {
  color: #33443a;
  font-size: 15px;
}

.wechat-visual p {
  margin-top: 6px;
  color: #748078;
  font-size: 12px;
  line-height: 1.6;
}

.first-login-fields {
  margin: 22px 0;
}

.first-login-fields > p {
  margin-bottom: 10px;
  color: #555e64;
  font-size: 12px;
  font-weight: 700;
}

.first-login-fields > p span {
  margin-left: 6px;
  color: #a0a6ad;
  font-weight: 400;
}

.two-column {
  display: grid;
  margin-top: 10px;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.privacy-note,
.password-note {
  margin-top: 14px;
  color: #a0a6ad;
  font-size: 11px;
  line-height: 1.6;
  text-align: center;
}

.registration-guide {
  margin-top: 18px;
  padding: 14px 16px;
  background: #fff8f2;
  border: 1px solid #ffe1cc;
  border-radius: 15px;
  color: #6d5748;
  font-size: 12px;
  line-height: 1.65;
}

.registration-guide strong {
  color: #cb6423;
}

.registration-guide p {
  margin-top: 3px;
}

@media (width <= 980px) {
  .login-page {
    display: block;
    padding: 28px 20px 50px;
  }

  .login-intro {
    margin: 0 auto;
  }

  .login-intro__copy {
    margin-top: 44px;
    text-align: center;
  }

  .login-intro h1 {
    font-size: clamp(38px, 9vw, 58px);
  }

  .login-intro__copy > p:last-child {
    margin-right: auto;
    margin-left: auto;
  }

  .role-preview {
    margin: 30px auto 36px;
  }

  .login-panel {
    max-width: 520px;
    margin: 0 auto;
  }
}

@media (width <= 560px) {
  .role-preview {
    display: none;
  }

  .login-intro__copy {
    margin: 38px 0 28px;
  }

  .login-intro__copy > p:last-child {
    font-size: 14px;
  }

  .login-panel {
    border-radius: 24px;
  }

  .two-column {
    grid-template-columns: 1fr;
  }
}
</style>
