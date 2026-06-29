<template>
  <div class="panel">
    <div class="toolbar">
      <div>
        <h2 class="section-title">店铺绑定</h2>
        <span class="page-kicker">用于同步订单、售后、评价和回写处理结果</span>
      </div>
    </div>
    <div class="platform-grid">
      <div v-for="item in platformOptions" :key="item.code" class="platform-card">
        <img :src="item.icon" :alt="item.name" />
        <div>
          <strong>{{ item.name }}</strong>
          <span>{{ item.desc }}</span>
        </div>
        <el-button type="primary" @click="bindPlatform(item)">绑定</el-button>
      </div>
    </div>
    <el-alert
      v-if="!bindingData.length"
      title="当前一级商家账号尚未绑定任何二级电商平台商家账号，请先完成店铺绑定。"
      type="warning"
      :closable="false"
      show-icon
      style="margin-bottom: 16px"
    />
    <el-alert
      v-else
      title="已绑定的电商平台商家账号将作为订单、售后、客服和评价数据来源。"
      type="success"
      :closable="false"
      show-icon
      style="margin-bottom: 16px"
    />
    <el-table v-if="bindingData.length" v-loading="loading" :data="bindingData" border style="margin-bottom: 16px">
      <el-table-column prop="platformName" label="平台" width="160" />
      <el-table-column prop="externalShopId" label="店铺ID" min-width="180" />
      <el-table-column prop="shopName" label="店铺名称" min-width="220" />
      <el-table-column prop="accountNo" label="绑定账号" width="160" />
      <el-table-column label="授权状态" width="120">
        <template #default="{ row }">
          <el-tag :type="row.authStatus === 'ACTIVE' ? 'success' : 'warning'">{{ authStatusText(row.authStatus) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="lastSyncedAt" label="最近同步" width="180" />
      <el-table-column label="操作" width="110" fixed="right">
        <template #default="{ row }">
          <el-button link type="danger" @click="unbindPlatform(row)">解绑</el-button>
        </template>
      </el-table-column>
    </el-table>
    <div v-if="bindingData.length" class="split-grid">
      <div class="panel">
        <h2 class="section-title">同步任务</h2>
        <div v-for="item in syncTaskData" :key="item.id" class="status-line">
          <div>
            <strong>{{ item.taskName }}</strong>
            <div class="page-kicker">上次：{{ item.lastRunAt || '-' }} · 下次：{{ item.nextRunAt || '-' }}</div>
          </div>
          <div class="inline-actions">
            <el-tag :type="item.enabled ? 'success' : 'info'">{{ item.enabled ? '已启用' : '已停用' }}</el-tag>
            <el-button link type="primary" :loading="syncingType === item.taskType" @click="runSync(item.taskType)">触发</el-button>
          </div>
        </div>
      </div>
      <div class="panel">
        <h2 class="section-title">开放平台准备项</h2>
        <el-check-tag checked>App Key</el-check-tag>
        <el-check-tag checked>App Secret</el-check-tag>
        <el-check-tag checked>回调地址</el-check-tag>
        <el-check-tag>真实权限审批</el-check-tag>
      </div>
    </div>
    <el-dialog v-model="twentyMallDialogVisible" title="绑定20商城账号" width="460px">
      <el-alert
        title="商家端演示账号：20230141 / 123456（极光外设旗舰店），20230142 / 123456（黑曜通勤箱包店）"
        type="info"
        :closable="false"
        style="margin-bottom: 16px"
      />
      <el-form label-width="88px">
        <el-form-item label="账号">
          <el-input v-model="twentyMallForm.accountNo" placeholder="请输入20商城商家账号" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="twentyMallForm.password" type="password" show-password placeholder="请输入密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="twentyMallDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="twentyMallBinding" @click="submitTwentyMallBind">确认绑定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, watch, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { loadSyncTasks, triggerSync } from '../api'
import { syncTasks } from '../data/mock'
import { isDemoMode } from '../utils/auth'
import { getMerchantBindings, getTwentyMallMerchantName, removeMerchantBinding, saveMerchantBinding, type MerchantPlatformBinding } from '../utils/auth'
import douyinIcon from '../assets/platforms/douyin.png'
import taobaoIcon from '../assets/platforms/taobao.png'
import pddIcon from '../assets/platforms/pinduoduo.png'
import jdIcon from '../assets/platforms/jd.png'
import twentyMallIcon from '../assets/platforms/twenty-mall.png'

const bindingData = ref<MerchantPlatformBinding[]>([])
const syncTaskData = ref<typeof syncTasks>(syncTasks)
const loading = ref(false)
const syncingType = ref('')
const twentyMallDialogVisible = ref(false)
const twentyMallBinding = ref(false)
const twentyMallForm = ref({ accountNo: '', password: '' })
const platformOptions = [
  { code: 'DOUYIN', name: '抖音商城', desc: '同步抖店订单与售后', icon: douyinIcon },
  { code: 'TAOBAO', name: '淘宝', desc: '预留淘宝店铺接入', icon: taobaoIcon },
  { code: 'PDD', name: '拼多多', desc: '预留拼多多店铺接入', icon: pddIcon },
  { code: 'JD', name: '京东', desc: '预留京东店铺接入', icon: jdIcon },
  { code: 'TWENTY_MALL', name: '20商城', desc: '自建数据库模拟电商平台', icon: twentyMallIcon }
]

onMounted(async () => {
  if (new URLSearchParams(window.location.search).get('needBind') === '1') {
    ElMessage({ type: 'warning', message: '请先绑定至少一个电商平台商家账号' })
  }
  loadLocalBindings()
})

watch(bindingData, async (value) => {
  if (!value.length) {
    syncTaskData.value = []
    return
  }
  syncTaskData.value = syncTasks
}, { immediate: true })

function loadLocalBindings() {
  bindingData.value = getMerchantBindings()
}

async function runSync(syncType: string) {
  const currentBinding = bindingData.value[0]
  if (!currentBinding) {
    ElMessage({ type: 'warning', message: '请先绑定电商平台商家账号' })
    return
  }
  syncingType.value = syncType
  try {
    if (isDemoMode()) {
      await new Promise((resolve) => window.setTimeout(resolve, 300))
      ElMessage({ type: 'success', message: '演示模式下已模拟触发同步' })
      return
    }
    await triggerSync(currentBinding.id, syncType)
    ElMessage({ type: 'success', message: '同步任务已触发' })
    syncTaskData.value = await loadSyncTasks(currentBinding.id) as typeof syncTasks
  } catch {
    ElMessage({ type: 'warning', message: '后端暂不可用，当前为演示触发' })
  } finally {
    syncingType.value = ''
  }
}

function bindPlatform(item: (typeof platformOptions)[number]) {
  if (item.code === 'TWENTY_MALL') {
    twentyMallForm.value = { accountNo: '', password: '' }
    twentyMallDialogVisible.value = true
    return
  }
  mockAuthorize(item.name)
}

async function submitTwentyMallBind() {
  const accountNo = twentyMallForm.value.accountNo.trim()
  const password = twentyMallForm.value.password.trim()
  if (!accountNo || !password) {
    ElMessage({ type: 'warning', message: '请输入20商城账号和密码' })
    return
  }
  twentyMallBinding.value = true
  try {
    const response = await fetch('/api/twenty-mall/bind', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ accountNo, password, role: 'MERCHANT' })
    })
    const payload = await response.json()
    if (payload.code !== '200') {
      ElMessage({ type: 'error', message: payload.message || '账号或密码错误' })
      return
    }
    mockAuthorize('20商城', accountNo)
    twentyMallDialogVisible.value = false
    ElMessage({ type: 'success', message: `20商城商家账号 ${accountNo} 绑定成功` })
  } catch {
    ElMessage({ type: 'error', message: '请先启动后端服务' })
  } finally {
    twentyMallBinding.value = false
  }
}

async function unbindPlatform(row: MerchantPlatformBinding) {
  const confirmed = window.confirm(`确定要解绑 ${row.platformName} 店铺 ${row.shopName} 吗？解绑后该店铺数据将不再显示。`)
  if (!confirmed) return
  removeMerchantBinding(row.platformCode, row.externalShopId)
  loadLocalBindings()
  ElMessage({ type: 'success', message: '已解绑' })
}

function authStatusText(status: string) {
  const statusMap: Record<string, string> = {
    ACTIVE: '已授权',
    EXPIRED: '已过期',
    UNBOUND: '未绑定',
    DISABLED: '已停用'
  }
  return statusMap[status] || status
}

function mockAuthorize(platformName = '抖音商城', accountNo = '') {
  const codeMap: Record<string, string> = {
    抖音商城: 'DOUYIN',
    淘宝: 'TAOBAO',
    拼多多: 'PDD',
    京东: 'JD',
    '20商城': 'TWENTY_MALL'
  }
  const code = codeMap[platformName] || 'MOCK'
  const binding: MerchantPlatformBinding = {
    id: Date.now(),
    platformCode: code,
    platformName,
    authStatus: 'ACTIVE',
    externalShopId: accountNo ? `TM_SHOP_${accountNo}` : `${code}_SHOP_DEMO`,
    shopName: accountNo ? getTwentyMallMerchantName(accountNo) : `${platformName}模拟店铺`,
    sellerNick: accountNo ? getTwentyMallMerchantName(accountNo) : `${platformName}商家`,
    accountNo: accountNo || '模拟授权',
    lastSyncedAt: '2026-06-27 16:20:00'
  }
  saveMerchantBinding(binding)
  loadLocalBindings()
  ElMessage({ type: 'success', message: `${platformName}店铺已完成模拟绑定` })
}
</script>

<style scoped>
.platform-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
  margin-bottom: 16px;
}

.platform-card {
  display: grid;
  grid-template-columns: 48px 1fr auto;
  align-items: center;
  gap: 12px;
  border: 1px solid #e4e8f0;
  border-radius: 8px;
  padding: 14px;
  background: #fff;
}

.platform-card img {
  width: 48px;
  height: 48px;
  border-radius: 10px;
  object-fit: cover;
}

.platform-card strong,
.platform-card span {
  display: block;
}

.platform-card span {
  margin-top: 4px;
  color: #64748b;
  font-size: 12px;
}

@media (max-width: 1280px) {
  .platform-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
