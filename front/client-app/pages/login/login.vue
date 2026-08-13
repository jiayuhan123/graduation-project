<template>
  <view class="login-container">
    <view class="login-box">
      <!-- 顶部 Logo / 标题区域 -->
      <view class="header">
        <text class="title">{{ isLoginMode ? '欢迎回来' : '注册账号' }}</text>
        <text class="subtitle">{{ isLoginMode ? '登录您的账号以继续' : '加入我们开启新体验' }}</text>
      </view>

      <!-- 登录/注册表单区 -->
      <view class="form-area">
        <u-form :model="form" ref="uForm" labelPosition="top">
          <!-- 账号输入框 -->
          <u-form-item prop="username">
            <u-input 
              v-model="form.username" 
              placeholder="请输入账号 (手机号或用户名)" 
              clearable 
              prefixIcon="account"
              customStyle="background-color: #f5f6f7; border-radius: 12rpx; padding: 12rpx 20rpx;"
            ></u-input>
          </u-form-item>

          <!-- 密码输入框 -->
          <u-form-item prop="password">
            <u-input 
              v-model="form.password" 
              type="password" 
              placeholder="请输入密码" 
              clearable 
              prefixIcon="lock"
              customStyle="background-color: #f5f6f7; border-radius: 12rpx; padding: 12rpx 20rpx;"
            ></u-input>
          </u-form-item>

          <!-- 注册时独有的表单项 -->
          <view v-show="!isLoginMode" class="fade-in">
            <u-form-item prop="phone">
              <u-input 
                v-model="form.phone" 
                placeholder="请输入手机号" 
                clearable 
                prefixIcon="phone"
                customStyle="background-color: #f5f6f7; border-radius: 12rpx; padding: 12rpx 20rpx;"
              ></u-input>
            </u-form-item>
            <u-form-item prop="realName">
              <u-input 
                v-model="form.realName" 
                placeholder="请输入真实姓名 (选填)" 
                clearable 
                prefixIcon="star"
                customStyle="background-color: #f5f6f7; border-radius: 12rpx; padding: 12rpx 20rpx;"
              ></u-input>
            </u-form-item>
          </view>
        </u-form>

        <!-- 提交按钮 -->
        <view class="btn-group">
          <u-button 
            type="primary" 
            shape="circle" 
            :loading="loading" 
            @click="handleSubmit"
            customStyle="background: linear-gradient(90deg, #3c8ce7 0%, #00eaff 100%); border: none; box-shadow: 0 8rpx 16rpx rgba(60,140,231,0.3);"
          >
            {{ isLoginMode ? '登 录' : '注 册' }}
          </u-button>
        </view>

        <!-- 模式切换区 -->
        <view class="switch-mode">
          <text class="tips">{{ isLoginMode ? '没有账号？' : '已有账号？' }}</text>
          <text class="link" @click="toggleMode">{{ isLoginMode ? '立即注册' : '去登录' }}</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { loginApi, registerApi } from '@/api/user'
import { TOKEN_KEY } from '@/utils/request'
import type { UserInfo } from '@/types/user'

// 状态控制
const isLoginMode = ref(true)
const loading = ref(false)

// 表单数据
const form = reactive({
  username: '',
  password: '',
  phone: '',
  realName: ''
})

// 切换模式
const toggleMode = () => {
  isLoginMode.value = !isLoginMode.value
  // 清空表单
  form.username = ''
  form.password = ''
  form.phone = ''
  form.realName = ''
}

// 提交表单
const handleSubmit = async () => {
  if (!form.username || !form.password) {
    uni.showToast({ title: '账号和密码不能为空', icon: 'none' })
    return
  }
  if (!isLoginMode.value && !form.phone) {
    uni.showToast({ title: '手机号不能为空', icon: 'none' })
    return
  }

  loading.value = true
  try {
    if (isLoginMode.value) {
      // 执行登录
      const res = await loginApi({
        username: form.username,
        password: form.password
      })

      // 保存 Token 和用户信息
      uni.setStorageSync(TOKEN_KEY, res.token)
      const userInfo: UserInfo = {
        userId: res.userId,
        username: res.username,
        role: res.role
      }
      uni.setStorageSync('userInfo', JSON.stringify(userInfo))

      uni.showToast({ title: '登录成功', icon: 'success' })
      setTimeout(() => {
        // 返回首页
        uni.reLaunch({ url: '/pages/index/index' })
      }, 1000)

    } else {
      // 执行注册
      await registerApi({
        username: form.username,
        password: form.password,
        phone: form.phone,
        realName: form.realName
      })

      uni.showToast({ title: '注册成功，请登录', icon: 'success' })
      setTimeout(() => {
        // 自动切回登录模式
        toggleMode()
      }, 1000)
    }
  } catch (error) {
    console.error('提交失败', error)
  } finally {
    loading.value = false
  }
}
</script>

<style lang="scss" scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  /* 现代渐变背景 */
  background: linear-gradient(135deg, #fdfbfb 0%, #ebedee 100%);
  padding: 40rpx;
}

.login-box {
  width: 100%;
  max-width: 600rpx;
  background: rgba(255, 255, 255, 0.9);
  /* 玻璃拟态效果 */
  backdrop-filter: blur(20px);
  border-radius: 30rpx;
  padding: 60rpx 40rpx;
  box-shadow: 0 20rpx 60rpx rgba(0, 0, 0, 0.05);
}

.header {
  margin-bottom: 60rpx;
  text-align: center;
  
  .title {
    display: block;
    font-size: 48rpx;
    font-weight: bold;
    color: #333;
    margin-bottom: 16rpx;
  }
  
  .subtitle {
    font-size: 28rpx;
    color: #888;
  }
}

.form-area {
  .btn-group {
    margin-top: 60rpx;
    margin-bottom: 40rpx;
  }
}

.switch-mode {
  text-align: center;
  font-size: 28rpx;
  
  .tips {
    color: #888;
  }
  
  .link {
    color: #3c8ce7;
    margin-left: 10rpx;
    font-weight: 500;
  }
}

.fade-in {
  animation: fadeIn 0.3s ease-in-out;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(-10rpx); }
  to { opacity: 1; transform: translateY(0); }
}
</style>
