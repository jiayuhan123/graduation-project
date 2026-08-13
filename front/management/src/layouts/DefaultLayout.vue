<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

/** 登出并跳转登录页 */
function handleLogout() {
  userStore.logout()
  router.push({ name: 'login' })
}
</script>

<template>
  <el-container class="layout">
    <!-- 顶部导航栏 -->
    <el-header class="layout-header">
      <div class="header-title">文明祭扫管理后台</div>
      <el-dropdown @command="handleLogout">
        <span class="user-info">
          <el-avatar :size="28">
            {{ userStore.userInfo?.username?.charAt(0)?.toUpperCase() }}
          </el-avatar>
          <span class="nickname">{{ userStore.userInfo?.username ?? '管理员' }}</span>
        </span>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="logout">退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </el-header>

    <!-- 主内容区 -->
    <el-main class="layout-main">
      <RouterView />
    </el-main>
  </el-container>
</template>

<style scoped lang="scss">
.layout {
  height: 100%;
}

.layout-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background-color: #fff;
  border-bottom: 1px solid #e4e7ed;

  .header-title {
    font-size: 18px;
    font-weight: 600;
    color: #303133;
  }

  .user-info {
    display: flex;
    align-items: center;
    gap: 8px;
    cursor: pointer;

    .nickname {
      font-size: 14px;
      color: #606266;
    }
  }
}

.layout-main {
  padding: 16px;
  background-color: #f5f7fa;
}
</style>
