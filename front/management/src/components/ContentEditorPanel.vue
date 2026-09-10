<script setup lang="ts">
import { Button as TButton, Loading as TLoading } from 'tdesign-vue-next'

defineProps<{
  title: string
  description?: string
  busy?: boolean
  submitText?: string
}>()

const emit = defineEmits<{
  close: []
  submit: []
}>()
</script>

<template>
  <div class="editor-mask" role="presentation" @mousedown.self="emit('close')">
    <section class="editor-panel" role="dialog" aria-modal="true" :aria-label="title">
      <header>
        <div>
          <h2>{{ title }}</h2>
          <p v-if="description">{{ description }}</p>
        </div>
        <button class="editor-close" type="button" aria-label="关闭" @click="emit('close')">
          ×
        </button>
      </header>
      <form @submit.prevent="emit('submit')">
        <div class="editor-body"><slot /></div>
        <footer>
          <t-button type="button" variant="outline" :disabled="busy" @click="emit('close')">
            取消
          </t-button>
          <t-button type="submit" :disabled="busy">
            <t-loading v-if="busy" size="small" />
            <span v-else>{{ submitText || '保存' }}</span>
          </t-button>
        </footer>
      </form>
    </section>
  </div>
</template>

<style scoped lang="scss">
.editor-mask {
  position: fixed;
  z-index: 100;
  inset: 0;
  display: grid;
  padding: 24px;
  overflow-y: auto;
  background: rgb(20 38 68 / 35%);
  place-items: center;
}

.editor-panel {
  width: min(720px, 100%);
  max-height: calc(100vh - 48px);
  overflow-y: auto;
  background: #fff;
  border-radius: 12px;
  box-shadow: none;
}

.editor-panel > header {
  position: sticky;
  z-index: 2;
  top: 0;
  display: flex;
  padding: 24px 26px 18px;
  align-items: flex-start;
  justify-content: space-between;
  background: rgb(255 255 255 / 96%);
  border-bottom: 1px solid var(--app-border-color);
}

h2 {
  color: var(--app-text-strong);
  font-size: 18px;
  font-weight: 600;
}

header p {
  margin-top: 5px;
  color: var(--app-text-muted);
  font-size: 12px;
}

.editor-close {
  width: 34px;
  height: 34px;
  background: #f3f4f6;
  border: 0;
  border-radius: 50%;
  color: #747b82;
  cursor: pointer;
  font-size: 23px;
  line-height: 1;
}

.editor-body {
  padding: 24px 26px;
}

footer {
  position: sticky;
  bottom: 0;
  display: flex;
  padding: 16px 26px 22px;
  justify-content: flex-end;
  background: rgb(255 255 255 / 96%);
  border-top: 1px solid var(--app-border-color);
  gap: 10px;
}

@media (width <= 640px) {
  .editor-mask {
    padding: 0;
    align-items: end;
  }

  .editor-panel {
    max-height: 92vh;
    border-radius: 12px 12px 0 0;
  }
}
</style>
