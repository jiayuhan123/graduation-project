<script setup lang="ts">
import { useRouter } from 'vue-router'
import { Button as TButton } from 'tdesign-vue-next'

const router = useRouter()
</script>

<template>
  <main class="error-page">
    <section>
      <h1>404</h1>
      <p>访问的页面不存在</p>
      <t-button theme="primary" @click="router.push({ name: 'dashboard' })">返回首页</t-button>
    </section>
  </main>
</template>

<style scoped>
.error-page {
  display: grid;
  min-height: 100%;
  place-items: center;
}
</style>
