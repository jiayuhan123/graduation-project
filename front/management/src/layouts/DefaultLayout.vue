<script setup lang="ts">
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

/** 登出并跳转登录页 */
async function handleLogout() {
  const serverLoggedOut = await userStore.logout()
  if (serverLoggedOut) ElMessage.success('已退出登录')
  else ElMessage.warning('本地已退出，服务端注销请求未完成')
  await router.replace({ name: 'login' })
}
</script>

<template>
  <el-container class="layout">
    <!-- 顶部导航栏 -->
    <el-header class="layout-header">
      <div class="header-title">项目管理后台</div>
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
  height: var(--app-header-height);
  background-color: #fff;
  border-bottom: 1px solid var(--app-border-color);

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
  padding: var(--app-content-padding);
  background-color: var(--app-page-bg);
}
</style>
