<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import {
  Button as TButton,
  Form as TForm,
  FormItem as TFormItem,
  Loading as TLoading,
  MessagePlugin,
  Option as TOption,
  Select as TSelect,
  type SubmitContext,
} from 'tdesign-vue-next'
import { getStudentProfileApi, updateStudentProfileApi } from '@/api/user'
import PageHeader from '@/components/PageHeader.vue'
import type { StudentProfile, UpdateStudentProfileInput } from '@/types/user'

const loading = ref(true)
const saving = ref(false)
const profile = ref<StudentProfile | null>(null)
const form = reactive<UpdateStudentProfileInput>({ gradeLevel: 3, semester: 1 })

async function loadProfile() {
  loading.value = true
  try {
    profile.value = await getStudentProfileApi()
    form.gradeLevel = profile.value.gradeLevel
    form.semester = profile.value.semester
  } finally {
    loading.value = false
  }
}

async function saveProfile(context: SubmitContext) {
  if (context.validateResult !== true) return
  saving.value = true
  try {
    profile.value = await updateStudentProfileApi({ ...form })
    void MessagePlugin.success('学生资料已更新')
  } finally {
    saving.value = false
  }
}

onMounted(loadProfile)
</script>

<template>
  <div>
    <PageHeader
      eyebrow="STUDENT"
      title="学生资料"
      description="年级和学期会用于匹配适合的学习内容。"
    />
    <section class="surface-card student-card">
      <div v-if="loading" class="loading-area"><t-loading text="正在加载学生资料" /></div>
      <template v-else>
        <div class="student-card__intro">
          <span>🎒</span>
          <div>
            <h2>{{ form.gradeLevel }} 年级 · {{ form.semester === 1 ? '上学期' : '下学期' }}</h2>
            <p>{{ profile?.studentNo ? `学号 ${profile.studentNo}` : '暂未设置学号' }}</p>
          </div>
        </div>
        <t-form class="profile-form" :data="form" label-align="top" @submit="saveProfile">
          <t-form-item label="当前年级" name="gradeLevel">
            <t-select v-model="form.gradeLevel">
              <t-option v-for="grade in 6" :key="grade" :value="grade" :label="`${grade} 年级`" />
            </t-select>
          </t-form-item>
          <t-form-item label="当前学期" name="semester">
            <t-select v-model="form.semester">
              <t-option :value="1" label="上学期" />
              <t-option :value="2" label="下学期" />
            </t-select>
          </t-form-item>
          <t-button type="submit" :loading="saving">保存学生资料</t-button>
        </t-form>
      </template>
    </section>
  </div>
</template>

<style scoped lang="scss">
.student-card {
  max-width: 760px;
  padding: clamp(24px, 5vw, 42px);
}

.loading-area {
  display: grid;
  min-height: 260px;
  place-items: center;
}

.student-card__intro {
  display: flex;
  padding: 24px;
  align-items: center;
  background: linear-gradient(135deg, #fff4eb, #fffaf6);
  border: 1px solid #ffe1cd;
  border-radius: 22px;
  gap: 18px;
}

.student-card__intro > span {
  display: grid;
  width: 62px;
  height: 62px;
  place-items: center;
  background: #fff;
  border-radius: 18px;
  font-size: 31px;
}

.student-card__intro h2 {
  color: var(--app-text-strong);
  font-size: 21px;
}

.student-card__intro p {
  margin-top: 6px;
  color: var(--app-text-muted);
  font-size: 12px;
}

.profile-form {
  display: grid;
  margin-top: 30px;
  grid-template-columns: 1fr 1fr;
  gap: 0 18px;
}

.profile-form > :last-child {
  place-self: end start;
  grid-column: 1 / -1;
}

@media (width <= 560px) {
  .profile-form {
    grid-template-columns: 1fr;
  }
}
</style>
