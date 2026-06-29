<template>
  <div class="login-wrap">
    <div class="login-card">
      <div class="brand-block">
        <div class="brand-mark">售</div>
        <div>
          <h1>商家售后中台</h1>
          <p>手机号验证码登录，后续接入真实短信验证</p>
        </div>
      </div>
      <el-form @submit.prevent>
        <el-form-item label="手机号">
          <el-input v-model="phone" placeholder="merchant_admin_demo" />
        </el-form-item>
        <el-form-item label="验证码">
          <el-input v-model="code" placeholder="123456" />
        </el-form-item>
        <el-button type="primary" native-type="button" style="width: 100%" :loading="loading" @click="login">登录</el-button>
        <el-button class="wechat-login" native-type="button" @click="wechatLogin">
          <img :src="wechatIcon" alt="" />
          <span>微信一键登录</span>
        </el-button>
      </el-form>
      <div class="login-tip">演示手机号：merchant_admin_demo / 123456</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { setAuth } from '../utils/auth'
import wechatIcon from '../assets/platforms/wechat.png'

const router = useRouter()
const phone = ref('merchant_admin_demo')
const code = ref('123456')
const loading = ref(false)

function login() {
  loading.value = true
  try {
    if (phone.value.trim() !== 'merchant_admin_demo' || code.value.trim() !== '123456') {
      ElMessage({ type: 'error', message: '手机号或验证码错误' })
      return
    }
    setAuth('demo-token', {
      userId: 2,
      username: 'merchant_admin_demo',
      nickname: '店铺管理员',
      merchantId: 1,
      roles: ['MERCHANT_ADMIN']
    })
    router.push('/platform')
  } finally {
    loading.value = false
  }
}

function wechatLogin() {
  ElMessage({ type: 'info', message: '微信一键登录后续接入' })
}
</script>

<style scoped>
.wechat-login {
  width: 100%;
  margin: 10px 0 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.wechat-login img {
  width: 22px;
  height: 22px;
  border-radius: 5px;
}
</style>
