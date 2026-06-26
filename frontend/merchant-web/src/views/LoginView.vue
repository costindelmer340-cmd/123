<template>
  <div class="login-wrap">
    <div class="login-card">
      <div class="brand-block">
        <div class="brand-mark">售</div>
        <div>
          <h1>商家售后中台</h1>
          <p>使用后端 JWT 登录后进入工作台</p>
        </div>
      </div>
      <el-form @submit.prevent>
        <el-form-item label="用户名">
          <el-input v-model="username" placeholder="merchant_admin_demo" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="password" type="password" show-password placeholder="123456" />
        </el-form-item>
        <el-button type="primary" native-type="button" style="width: 100%" :loading="loading" @click="login">登录</el-button>
        <el-button native-type="button" style="width: 100%; margin: 10px 0 0" @click="enterDemo">演示进入</el-button>
      </el-form>
      <div class="login-tip">演示账号：merchant_admin_demo / 123456</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '../api/client'
import { ElMessage } from 'element-plus'
import axios from 'axios'
import { setAuth } from '../utils/auth'

const router = useRouter()
const username = ref('merchant_admin_demo')
const password = ref('123456')
const loading = ref(false)

async function login() {
  loading.value = true
  try {
    if (!username.value.trim() || !password.value.trim()) {
      ElMessage({ type: 'warning', message: '请输入用户名和密码' })
      return
    }
    const response = await api.post('/auth/login', { username: username.value, password: password.value })
    const payload = response.data.data
    if (!payload?.accessToken) {
      throw new Error(response.data.message || '登录响应缺少 token')
    }
    setAuth(payload.accessToken, payload.user)
    router.push('/dashboard')
  } catch (error) {
    const backendMessage = axios.isAxiosError(error) ? error.response?.data?.message : ''
    const message = backendMessage === 'MyBatisSystemException'
      ? '后端无法连接 MySQL，请检查数据库账号密码，或点击“演示进入”'
      : '用户名或密码错误，或后端暂不可用'
    ElMessage({ type: 'error', message })
  } finally {
    loading.value = false
  }
}

function enterDemo() {
  setAuth('demo-token', {
    userId: 2,
    username: 'merchant_admin_demo',
    nickname: '店铺管理员',
    merchantId: 1,
    roles: ['MERCHANT_ADMIN']
  })
  router.push('/dashboard')
}
</script>
