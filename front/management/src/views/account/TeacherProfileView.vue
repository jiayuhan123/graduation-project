<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import {
  Button as TButton,
  Form as TForm,
  FormItem as TFormItem,
  Input as TInput,
  Loading as TLoading,
  MessagePlugin,
  Textarea as TTextarea,
  type SubmitContext,
} from 'tdesign-vue-next'
import { getTeacherProfileApi, updateTeacherProfileApi } from '@/api/user'
import PageHeader from '@/components/PageHeader.vue'
import type { TeacherProfile, UpdateTeacherProfileInput } from '@/types/user'

const loading = ref(true)
const saving = ref(false)
const profile = ref<TeacherProfile | null>(null)
const form = reactive<UpdateTeacherProfileInput>({ title: '', introduction: '' })

async function loadProfile() {
  loading.value = true
  try {
    profile.value = await getTeacherProfileApi()
    form.title = profile.value.title ?? ''
    form.introduction = profile.value.introduction ?? ''
  } finally {
    loading.value = false
  }
}

async function saveProfile(context: SubmitContext) {
  if (context.validateResult !== true) return
  saving.value = true
  try {
    profile.value = await updateTeacherProfileApi({
      title: form.title?.trim() || undefined,
      introduction: form.introduction?.trim() || undefined,
    })
    void MessagePlugin.success('教师资料已更新')
  } finally {
    saving.value = false
  }
}

onMounted(loadProfile)
</script>

<template>
  <div>
    <PageHeader eyebrow="TEACHER" title="教师资料" description="维护展示给学生和家长的教学身份。" />
    <section class="surface-card teacher-card">
      <div v-if="loading" class="loading-area"><t-loading text="正在加载教师资料" /></div>
      <template v-else>
        <div class="teacher-card__intro">
          <span>📐</span>
          <div>
            <h2>{{ form.title || '教师资料待完善' }}</h2>
            <p>{{ profile?.teacherNo ? `教师工号 ${profile.teacherNo}` : '暂未设置教师工号' }}</p>
          </div>
        </div>
        <t-form class="profile-form" :data="form" label-align="top" @submit="saveProfile">
          <t-form-item label="职称" name="title">
            <t-input v-model="form.title" maxlength="50" placeholder="例如：小学数学一级教师" />
          </t-form-item>
          <t-form-item label="公开简介" name="introduction">
            <t-textarea
              v-model="form.introduction"
              :maxlength="500"
              :autosize="{ minRows: 5, maxRows: 8 }"
              placeholder="介绍教学经历、擅长方向等"
            />
          </t-form-item>
          <t-button type="submit" :loading="saving">保存教师资料</t-button>
        </t-form>
      </template>
    </section>
  </div>
</template>

<style scoped lang="scss">
.teacher-card {
  max-width: 760px;
  padding: clamp(24px, 5vw, 42px);
}

.loading-area {
  display: grid;
  min-height: 260px;
  place-items: center;
}

.teacher-card__intro {
  display: flex;
  padding: 24px;
  align-items: center;
  background: linear-gradient(135deg, #edf7ff, #f8fcff);
  border: 1px solid #d8ebfa;
  border-radius: 22px;
  gap: 18px;
}

.teacher-card__intro > span {
  display: grid;
  width: 62px;
  height: 62px;
  place-items: center;
  background: #fff;
  border-radius: 18px;
  font-size: 31px;
}

.teacher-card__intro h2 {
  color: var(--app-text-strong);
  font-size: 21px;
}

.teacher-card__intro p {
  margin-top: 6px;
  color: var(--app-text-muted);
  font-size: 12px;
}

.profile-form {
  margin-top: 30px;
}
</style>
