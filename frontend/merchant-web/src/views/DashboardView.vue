<template>
  <div>
    <div class="metric-grid">
      <div v-for="item in metrics" :key="item.label" class="metric-card">
        <span>{{ item.label }}</span>
        <div class="metric-value">{{ item.value }}</div>
        <el-tag :type="tagType(item.tone)" effect="light">{{ item.trend }}</el-tag>
      </div>
    </div>
    <div class="split-grid">
      <div class="panel">
        <h2 class="section-title">待处理售后</h2>
        <el-table v-loading="loading" :data="afterSaleData" height="310">
          <el-table-column prop="afterSaleNo" label="售后单号" min-width="150" />
          <el-table-column label="原因">
            <template #default="{ row }">{{ reasonText(row.reasonType) }}</template>
          </el-table-column>
          <el-table-column prop="requestedAmount" label="金额" width="100" />
          <el-table-column label="状态" width="130">
            <template #default="{ row }">{{ afterSaleStatusText(row.status) }}</template>
          </el-table-column>
          <el-table-column label="优先级" width="100">
            <template #default="{ row }">{{ priorityText(row.priority) }}</template>
          </el-table-column>
        </el-table>
      </div>
      <div class="panel">
        <h2 class="section-title">服务动态</h2>
        <div v-for="item in conversationData" :key="item.id" class="status-line">
          <div>
            <strong>{{ serviceOrderNo(item) }}</strong>
            <div class="page-kicker">{{ latestUserMessage(item) }}</div>
          </div>
          <el-tag>{{ conversationStatusText(item.status) }}</el-tag>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { loadDemoChatConversations, loadTwentyMallMerchantAfterSales, loadTwentyMallMerchantReviews } from '../api'
import { ElMessage } from 'element-plus'
import { afterSales, conversations, reviews } from '../data/mock'
import { getMerchantBindings } from '../utils/auth'

const afterSaleData = ref<typeof afterSales>([])
const conversationData = ref<typeof conversations>([])
const reviewData = ref<typeof reviews>([])
const loading = ref(false)

onMounted(async () => {
  loading.value = true
  try {
    const merchantAccounts = boundMerchantAccounts()
    const [loadedAfterSales, loadedConversations, loadedReviews] = await Promise.all([
      Promise.all(merchantAccounts.map((accountNo) => loadTwentyMallMerchantAfterSales(accountNo))),
      loadDemoChatConversations(merchantAccounts),
      Promise.all(merchantAccounts.map((accountNo) => loadTwentyMallMerchantReviews(accountNo)))
    ])
    afterSaleData.value = loadedAfterSales.flat().slice(0, 8) as typeof afterSales
    conversationData.value = (loadedConversations as typeof conversations).slice(0, 6)
    reviewData.value = loadedReviews.flat() as typeof reviews
  } catch {
    afterSaleData.value = []
    conversationData.value = []
    reviewData.value = []
    ElMessage({ type: 'error', message: '工作台数据读取失败，请确认后端服务和数据库已启动' })
  }
  loading.value = false
})

const pendingAfterSaleCount = computed(() => afterSaleData.value.filter((item) => !['COMPLETED', 'CLOSED', 'REJECTED'].includes(item.status)).length)
const todayConversationCount = computed(() => conversationData.value.length)
const highRiskReviewCount = computed(() => reviewData.value.filter((item) => item.riskLevel === 'HIGH').length)

const metrics = computed(() => [
  { label: '待处理售后', value: String(pendingAfterSaleCount.value), trend: `共 ${afterSaleData.value.length} 单`, tone: 'warning' },
  { label: '今日会话', value: String(todayConversationCount.value), trend: `共 ${conversationData.value.length} 条`, tone: 'primary' },
  { label: '高风险评价', value: String(highRiskReviewCount.value), trend: `共 ${reviewData.value.length} 条`, tone: highRiskReviewCount.value ? 'danger' : 'success' }
])

function tagType(tone: string) {
  return tone === 'danger' ? 'danger' : tone === 'warning' ? 'warning' : tone === 'success' ? 'success' : 'primary'
}

function reasonText(value: string) {
  const map: Record<string, string> = {
    PRODUCT_QUALITY: '商品质量问题',
    LOGISTICS_DELAY: '物流延迟',
    WRONG_GOODS: '商品错发',
    SIZE_MISMATCH: '尺码不符',
    NOT_AS_DESCRIBED: '描述不符',
    OTHER: '其他原因'
  }
  return map[value] || value
}

function afterSaleStatusText(value: string) {
  const map: Record<string, string> = {
    PROCESSING: '处理中',
    PENDING_REVIEW: '待审核',
    APPROVED: '已通过',
    REJECTED: '已拒绝',
    COMPLETED: '已完成',
    CLOSED: '已关闭'
  }
  return map[value] || value
}

function priorityText(value: string) {
  const map: Record<string, string> = {
    HIGH: '高',
    MEDIUM: '中',
    NORMAL: '普通',
    LOW: '低'
  }
  return map[value] || value
}

function conversationStatusText(value: string) {
  const map: Record<string, string> = {
    AGENT_SERVING: '人工接待中',
    AI_SERVING: 'AI接待中',
    CLOSED: '已关闭'
  }
  return map[value] || value
}

function boundMerchantAccounts() {
  return getMerchantBindings()
    .filter((item) => item.platformCode === 'TWENTY_MALL' && item.accountNo)
    .map((item) => item.accountNo as string)
}

function serviceOrderNo(item: typeof conversations[number]) {
  return item.orderNo || item.conversationNo
}

function latestUserMessage(item: typeof conversations[number]) {
  return item.lastMessage || '暂无用户消息'
}

</script>
