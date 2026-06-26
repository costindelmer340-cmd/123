<template>
  <el-container class="app-shell">
    <el-aside width="236px" class="sidebar">
      <div class="brand">
        <div class="brand-mark">售</div>
        <div>
          <strong>商家售后中台</strong>
          <span>Douyin After-sale</span>
        </div>
      </div>
      <el-menu router :default-active="$route.path" class="nav-menu">
        <el-menu-item v-for="item in navItems" :key="item.path" :index="item.path">
          <component :is="item.icon" class="nav-icon" />
          <span>{{ item.label }}</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="topbar">
        <div>
          <div class="page-kicker">星河数码抖音旗舰店</div>
          <h1>{{ currentTitle }}</h1>
        </div>
        <div class="top-actions">
          <el-tag :type="isDemoMode ? 'warning' : 'success'" effect="plain">{{ isDemoMode ? '演示模式' : '真实登录' }}</el-tag>
          <el-avatar :size="36">{{ avatarText }}</el-avatar>
          <el-button link type="primary" @click="logout">退出</el-button>
        </div>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { BarChart3, BookOpen, Bot, ClipboardList, Headphones, PackageSearch, RefreshCcw, Store } from 'lucide-vue-next'
import { clearAuth, getStoredUser, getToken } from '../utils/auth'

const route = useRoute()
const router = useRouter()
const navItems = [
  { path: '/dashboard', label: '工作台', icon: BarChart3 },
  { path: '/platform', label: '店铺绑定', icon: Store },
  { path: '/orders', label: '外部订单', icon: PackageSearch },
  { path: '/after-sales', label: '售后处理', icon: RefreshCcw },
  { path: '/conversations', label: '实时客服', icon: Headphones },
  { path: '/tickets', label: '工单处理', icon: ClipboardList },
  { path: '/reviews', label: '评价分析', icon: Bot },
  { path: '/knowledge', label: '知识库', icon: BookOpen }
]

const currentTitle = computed(() => navItems.find((item) => item.path === route.path)?.label ?? '工作台')
const isDemoMode = computed(() => getToken() === 'demo-token')
const avatarText = computed(() => {
  const user = getStoredUser<{ nickname?: string; username?: string }>()
  return (user?.nickname || user?.username || '商').slice(0, 1)
})

function logout() {
  clearAuth()
  router.push('/login')
}
</script>
