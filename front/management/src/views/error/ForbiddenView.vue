<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<template>
  <main class="error-page">
    <el-result icon="warning" title="403" sub-title="你没有访问该页面的权限">
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
