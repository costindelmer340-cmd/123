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
          <el-table-column prop="reasonType" label="原因" />
          <el-table-column prop="requestedAmount" label="金额" width="100" />
          <el-table-column prop="status" label="状态" width="130" />
          <el-table-column prop="priority" label="优先级" width="100" />
        </el-table>
      </div>
      <div class="panel">
        <h2 class="section-title">服务动态</h2>
        <div v-for="item in conversationData" :key="item.id" class="status-line">
          <div>
            <strong>{{ item.conversationNo }}</strong>
            <div class="page-kicker">{{ item.lastMessage }}</div>
          </div>
          <el-tag>{{ item.status }}</el-tag>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { loadAfterSales, loadConversations } from '../api'
import { loadListWithFallback } from '../api/normalize'
import { afterSales, conversations } from '../data/mock'

const afterSaleData = ref<typeof afterSales>(afterSales)
const conversationData = ref<typeof conversations>(conversations)
const loading = ref(false)

onMounted(async () => {
  loading.value = true
  const [loadedAfterSales, loadedConversations] = await Promise.all([
    loadListWithFallback(() => loadAfterSales({ status: 'PENDING_REVIEW' }), afterSales),
    loadListWithFallback(() => loadConversations(), conversations)
  ])
  afterSaleData.value = loadedAfterSales.slice(0, 8)
  conversationData.value = loadedConversations.slice(0, 6)
  loading.value = false
})

const metrics = computed(() => [
  { label: '待处理售后', value: String(afterSaleData.value.length), trend: '+6', tone: 'warning' },
  { label: '今日会话', value: String(conversationData.value.length), trend: '+22', tone: 'primary' },
  { label: '高风险评价', value: '7', trend: '-3', tone: 'danger' },
  { label: '工单按时率', value: '94%', trend: '+4%', tone: 'success' }
])

function tagType(tone: string) {
  return tone === 'danger' ? 'danger' : tone === 'warning' ? 'warning' : tone === 'success' ? 'success' : 'primary'
}
</script>
