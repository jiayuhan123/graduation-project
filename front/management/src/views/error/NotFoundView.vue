<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<template>
  <main class="error-page">
    <el-result icon="info" title="404" sub-title="访问的页面不存在">
      <template #extra>
        <el-button type="primary" @click="router.push({ name: 'dashboard' })">返回首页</el-button>
      </template>
    </el-result>
  </main>
</template>

<style scoped>
.error-page {
  display: grid;
  min-height: 100%;
  place-items: center;
}
</style>
