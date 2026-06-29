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
            <strong>{{ item.conversationNo }}</strong>
            <div class="page-kicker">{{ item.lastMessage }}</div>
          </div>
          <el-tag>{{ conversationStatusText(item.status) }}</el-tag>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { loadAfterSales, loadConversations, loadReviews, loadTickets } from '../api'
import { loadListWithFallback } from '../api/normalize'
import { afterSales, conversations, reviews, tickets } from '../data/mock'

const afterSaleData = ref<typeof afterSales>(afterSales)
const conversationData = ref<typeof conversations>(conversations)
const reviewData = ref<typeof reviews>(reviews)
const ticketData = ref<typeof tickets>(tickets)
const loading = ref(false)

onMounted(async () => {
  loading.value = true
  const [loadedAfterSales, loadedConversations, loadedReviews, loadedTickets] = await Promise.all([
    loadListWithFallback(() => loadAfterSales({ status: 'PENDING_REVIEW' }), afterSales),
    loadListWithFallback(() => loadConversations(), conversations),
    loadListWithFallback(() => loadReviews(), reviews),
    loadListWithFallback(() => loadTickets(), tickets)
  ])
  afterSaleData.value = loadedAfterSales.slice(0, 8)
  conversationData.value = loadedConversations.slice(0, 6)
  reviewData.value = loadedReviews
  ticketData.value = loadedTickets
  loading.value = false
})

const pendingAfterSaleCount = computed(() => afterSaleData.value.filter((item) => !['COMPLETED', 'CLOSED', 'REJECTED'].includes(item.status)).length)
const todayConversationCount = computed(() => conversationData.value.length)
const highRiskReviewCount = computed(() => reviewData.value.filter((item) => item.riskLevel === 'HIGH').length)
const onTimeCompletedTicketCount = computed(() => ticketData.value.filter((item) => isTicketCompletedWithin24Hours(item)).length)
const onTimeTicketRate = computed(() => {
  const total = ticketData.value.length
  if (!total) {
    return '100%'
  }
  const rate = Math.round((onTimeCompletedTicketCount.value / total) * 100)
  return `${rate}%`
})

const metrics = computed(() => [
  { label: '待处理售后', value: String(pendingAfterSaleCount.value), trend: `共 ${afterSaleData.value.length} 单`, tone: 'warning' },
  { label: '今日会话', value: String(todayConversationCount.value), trend: `共 ${conversationData.value.length} 条`, tone: 'primary' },
  { label: '高风险评价', value: String(highRiskReviewCount.value), trend: `共 ${reviewData.value.length} 条`, tone: highRiskReviewCount.value ? 'danger' : 'success' },
  { label: '工单按时率', value: onTimeTicketRate.value, trend: `24小时内 ${onTimeCompletedTicketCount.value}/${ticketData.value.length}`, tone: onTimeTicketTone.value }
])

const onTimeTicketTone = computed(() => {
  const rate = Number(onTimeTicketRate.value.replace('%', ''))
  if (rate >= 90) return 'success'
  if (rate >= 60) return 'warning'
  return 'danger'
})

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

type TicketForRate = typeof tickets[number] & {
  completedAt?: string
  resolvedAt?: string
  closedAt?: string
}

function isTicketCompletedWithin24Hours(ticket: TicketForRate) {
  if (!['RESOLVED', 'CLOSED', 'COMPLETED'].includes(ticket.status)) {
    return false
  }
  const start = parseDateTime(ticket.createdAt)
  const completed = parseDateTime(ticket.completedAt || ticket.resolvedAt || ticket.closedAt)
  if (!start || !completed) {
    return false
  }
  const hours = (completed.getTime() - start.getTime()) / (1000 * 60 * 60)
  return hours >= 0 && hours <= 24
}

function parseDateTime(value?: string) {
  if (!value) {
    return null
  }
  const normalized = value.replace(/\./g, '-')
  const date = new Date(normalized)
  return Number.isNaN(date.getTime()) ? null : date
}
</script>
