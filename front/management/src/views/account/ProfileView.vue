<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import {
  Button as TButton,
  Form as TForm,
  FormItem as TFormItem,
  Input as TInput,
  Loading as TLoading,
  MessagePlugin,
  Tag as TTag,
  type SubmitContext,
} from 'tdesign-vue-next'
import { getCurrentUserApi, updateCurrentUserApi } from '@/api/user'
import PageHeader from '@/components/PageHeader.vue'
import { roleMeta } from '@/constants/roles'
import { useUserStore } from '@/stores/user'
import type { UpdateProfileInput, UserProfile } from '@/types/user'

const userStore = useUserStore()
const loading = ref(true)
const saving = ref(false)
const editing = ref(false)
const profile = ref<UserProfile | null>(null)
const form = reactive<UpdateProfileInput>({})

const displayName = computed(
  () => profile.value?.nickname || profile.value?.realName || profile.value?.username || '用户',
)

function formatDate(value: string | null | undefined): string {
  if (!value) return '暂无记录'
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(value))
}

function fillForm(value: UserProfile) {
  form.nickname = value.nickname ?? ''
  form.realName = value.realName ?? ''
  form.phone = value.phone ?? ''
  form.email = value.email ?? ''
  form.avatarUrl = value.avatarUrl ?? ''
}

async function loadProfile() {
  loading.value = true
  try {
    const result = await getCurrentUserApi()
    profile.value = result
    userStore.syncProfile(result)
    fillForm(result)
  } finally {
    loading.value = false
  }
}

async function saveProfile(context: SubmitContext) {
  if (context.validateResult !== true || !profile.value) return
  saving.value = true
  try {
    const result = await updateCurrentUserApi({
      nickname: form.nickname?.trim() || undefined,
      realName: form.realName?.trim() || undefined,
      phone: form.phone?.trim() || undefined,
      email: form.email?.trim() || undefined,
      avatarUrl: profile.value.avatarUrl || undefined,
    })
    profile.value = result
    userStore.syncProfile(result)
    fillForm(result)
    editing.value = false
    void MessagePlugin.success('个人资料已更新')
  } finally {
    saving.value = false
  }
}

function cancelEdit() {
  if (profile.value) fillForm(profile.value)
  editing.value = false
}

onMounted(loadProfile)
</script>

<template>
  <div>
    <PageHeader eyebrow="ACCOUNT" title="个人资料" description="Web 与小程序共享同一份账号资料。">
      <template #actions>
        <t-button v-if="profile && !editing" @click="editing = true">编辑资料</t-button>
      </template>
    </PageHeader>

    <div v-if="loading" class="surface-card loading-card">
      <t-loading size="32px" text="正在加载个人资料" />
    </div>

    <template v-else-if="profile">
      <section class="profile-hero surface-card">
        <div class="profile-avatar">
          <img v-if="profile.avatarUrl" :src="profile.avatarUrl" alt="用户头像" />
          <span v-else>{{ displayName.slice(0, 1) }}</span>
        </div>
        <div class="profile-hero__copy">
          <div class="profile-hero__title">
            <h2>{{ displayName }}</h2>
            <t-tag theme="primary" variant="light" shape="round">
              {{ roleMeta(userStore.activeRole).label }}
            </t-tag>
          </div>
          <p>@{{ profile.username }}</p>
          <span :class="['binding-state', { active: profile.wechatBound }]">
            {{ profile.wechatBound ? '● 微信身份已绑定' : '○ 微信身份未绑定' }}
          </span>
        </div>
        <div class="profile-hero__stats">
          <div>
            <strong>{{ profile.roles.length }}</strong
            ><span>角色</span>
          </div>
          <div>
            <strong>{{ profile.permissions.length }}</strong
            ><span>权限</span>
          </div>
        </div>
      </section>

      <div class="profile-grid">
        <section class="surface-card info-card">
          <div class="card-heading">
            <div>
              <h3>账号信息</h3>
              <p>用于联系和识别你的基础资料</p>
            </div>
          </div>

          <t-form v-if="editing" :data="form" label-align="top" @submit="saveProfile">
            <div class="form-grid">
              <t-form-item label="昵称" name="nickname">
                <t-input v-model="form.nickname" maxlength="50" placeholder="请输入昵称" />
              </t-form-item>
              <t-form-item label="真实姓名" name="realName">
                <t-input v-model="form.realName" maxlength="50" placeholder="请输入真实姓名" />
              </t-form-item>
              <t-form-item
                label="手机号"
                name="phone"
                :rules="[{ pattern: /^1\d{10}$/, message: '请输入正确的手机号' }]"
              >
                <t-input v-model="form.phone" maxlength="11" placeholder="选填" />
              </t-form-item>
              <t-form-item
                label="邮箱"
                name="email"
                :rules="[{ email: true, message: '请输入正确的邮箱' }]"
              >
                <t-input v-model="form.email" maxlength="254" placeholder="选填" />
              </t-form-item>
            </div>
            <div class="form-actions">
              <t-button variant="outline" @click="cancelEdit">取消</t-button>
              <t-button theme="primary" type="submit" :loading="saving">保存修改</t-button>
            </div>
          </t-form>

          <dl v-else class="detail-list">
            <div>
              <dt>真实姓名</dt>
              <dd>{{ profile.realName || '未填写' }}</dd>
            </div>
            <div>
              <dt>手机号</dt>
              <dd>{{ profile.phone || '未绑定' }}</dd>
            </div>
            <div>
              <dt>邮箱</dt>
              <dd>{{ profile.email || '未绑定' }}</dd>
            </div>
            <div>
              <dt>加入时间</dt>
              <dd>{{ formatDate(profile.createdAt) }}</dd>
            </div>
            <div>
              <dt>最近登录</dt>
              <dd>{{ formatDate(profile.lastLoginAt) }}</dd>
            </div>
          </dl>
        </section>

        <section class="surface-card role-card">
          <div class="card-heading">
            <div>
              <h3>账号身份</h3>
              <p>点击侧边栏中的身份即可切换空间</p>
            </div>
          </div>
          <div class="role-items">
            <button
              v-for="role in profile.roles"
              :key="role"
              type="button"
              :class="{ active: role === userStore.activeRole }"
              @click="userStore.setActiveRole(role)"
            >
              <span>{{ roleMeta(role).icon }}</span>
              <span
                ><strong>{{ roleMeta(role).label }}</strong
                ><small>{{ roleMeta(role).description }}</small></span
              >
              <b>{{ role === userStore.activeRole ? '当前' : '切换' }}</b>
            </button>
          </div>
        </section>
      </div>
    </template>
  </div>
</template>

<style scoped lang="scss">
.loading-card {
  display: grid;
  min-height: 280px;
  place-items: center;
}

.profile-hero {
  display: flex;
  padding: clamp(25px, 4vw, 38px);
  align-items: center;
  background: linear-gradient(135deg, #fff, #fff8f2);
  gap: 22px;
}

.profile-avatar {
  display: grid;
  width: 94px;
  height: 94px;
  flex: none;
  place-items: center;
  overflow: hidden;
  background: linear-gradient(135deg, #ffad73, #ff7a2a);
  border-radius: 29px;
  box-shadow: 0 14px 34px rgb(255 122 42 / 22%);
  color: #fff;
  font-size: 38px;
  font-weight: 800;
}

.profile-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.profile-hero__copy {
  min-width: 0;
  flex: 1;
}

.profile-hero__title {
  display: flex;
  align-items: center;
  gap: 12px;
}

.profile-hero h2 {
  color: var(--app-text-strong);
  font-size: 28px;
}

.profile-hero__copy p {
  margin-top: 7px;
  color: var(--app-text-muted);
}

.binding-state {
  display: inline-block;
  margin-top: 10px;
  color: #a0a6ad;
  font-size: 12px;
}

.binding-state.active {
  color: #169c53;
}

.profile-hero__stats {
  display: flex;
  gap: 32px;
}

.profile-hero__stats > div {
  display: flex;
  align-items: center;
  flex-direction: column;
}

.profile-hero__stats strong {
  color: var(--app-text-strong);
  font-size: 24px;
}

.profile-hero__stats span {
  margin-top: 4px;
  color: var(--app-text-muted);
  font-size: 11px;
}

.profile-grid {
  display: grid;
  margin-top: 20px;
  grid-template-columns: minmax(0, 1.45fr) minmax(300px, 0.8fr);
  gap: 20px;
}

.info-card,
.role-card {
  padding: clamp(22px, 4vw, 32px);
}

.card-heading {
  display: flex;
  margin-bottom: 24px;
  align-items: center;
  justify-content: space-between;
}

.card-heading h3 {
  color: var(--app-text-strong);
  font-size: 18px;
}

.card-heading p {
  margin-top: 5px;
  color: var(--app-text-muted);
  font-size: 11px;
}

.detail-list > div {
  display: grid;
  padding: 15px 0;
  border-top: 1px solid #f0f1f3;
  grid-template-columns: 120px 1fr;
}

.detail-list dt {
  color: var(--app-text-muted);
}

.detail-list dd {
  color: var(--app-text-strong);
  text-align: right;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 18px;
}

.form-actions {
  display: flex;
  margin-top: 10px;
  justify-content: flex-end;
  gap: 10px;
}

.role-items {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.role-items button {
  display: grid;
  width: 100%;
  padding: 14px;
  align-items: center;
  background: #f8f9fa;
  border: 1px solid #eceef0;
  border-radius: 16px;
  color: inherit;
  cursor: pointer;
  font: inherit;
  grid-template-columns: 40px 1fr auto;
  gap: 11px;
  text-align: left;
}

.role-items button.active {
  background: #fff4ed;
  border-color: #ffdac2;
}

.role-items button > span:first-child {
  font-size: 24px;
}

.role-items button > span:nth-child(2) {
  display: flex;
  min-width: 0;
  flex-direction: column;
}

.role-items strong {
  color: var(--app-text-strong);
  font-size: 13px;
}

.role-items small {
  margin-top: 3px;
  overflow: hidden;
  color: var(--app-text-muted);
  font-size: 10px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.role-items b {
  color: #d9651d;
  font-size: 10px;
}

@media (width <= 980px) {
  .profile-grid {
    grid-template-columns: 1fr;
  }
}

@media (width <= 620px) {
  .profile-hero {
    align-items: flex-start;
    flex-wrap: wrap;
  }

  .profile-avatar {
    width: 72px;
    height: 72px;
    border-radius: 22px;
    font-size: 30px;
  }

  .profile-hero__stats {
    width: 100%;
    padding-top: 18px;
    justify-content: space-around;
    border-top: 1px solid #f0e9e4;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
