<template>
  <div class="panel">
    <div class="toolbar">
      <el-segmented v-model="status" :options="['全部', 'PENDING_REVIEW', 'PROCESSING']" />
      <el-button type="primary" @click="batchApprove">批量审核</el-button>
    </div>
    <el-table v-loading="loading" :data="filteredAfterSales">
      <el-table-column prop="afterSaleNo" label="售后单号" min-width="170" />
      <el-table-column prop="afterSaleType" label="类型" width="140" />
      <el-table-column prop="reasonType" label="原因" width="160" />
      <el-table-column prop="requestedAmount" label="申请金额" width="110" />
      <el-table-column prop="status" label="状态" width="140" />
      <el-table-column prop="priority" label="优先级" width="100" />
      <el-table-column prop="reviewOpinion" label="审核意见" min-width="190" />
      <el-table-column prop="writeBackStatus" label="回写状态" width="130" />
      <el-table-column prop="createdAt" label="创建时间" min-width="160" />
      <el-table-column label="操作" width="160">
        <template #default="{ row }">
          <el-button link type="primary" @click="approveAfterSale(row.id)">审核</el-button>
          <el-button link @click="writeBackAfterSale(row.id)">回写</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { afterSales } from '../data/mock'
import { loadAfterSales } from '../api'
import { loadListWithFallback } from '../api/normalize'
import { ElMessage } from 'element-plus'

const status = ref('全部')
const afterSalesData = ref<typeof afterSales>([])
const loading = ref(false)

onMounted(async () => {
  loading.value = true
  afterSalesData.value = await loadListWithFallback(() => loadAfterSales(), afterSales)
  loading.value = false
})

const filteredAfterSales = computed(() => {
  if (status.value === '全部') {
    return afterSalesData.value
  }
  return afterSalesData.value.filter((item) => item.status === status.value)
})

function batchApprove() {
  let changed = 0
  afterSalesData.value = afterSalesData.value.map((item) => {
    if (item.status !== 'PENDING_REVIEW') {
      return item
    }
    changed += 1
    return {
      ...item,
      status: 'PROCESSING',
      priority: item.priority || 'NORMAL',
      reviewOpinion: '批量审核通过，进入售后处理',
      writeBackStatus: 'PENDING'
    }
  })
  ElMessage({
    type: changed ? 'success' : 'info',
    message: changed ? `已审核 ${changed} 条待处理售后` : '当前没有待审核售后'
  })
}

function approveAfterSale(afterSaleId: number) {
  let changed = false
  afterSalesData.value = afterSalesData.value.map((item) => {
    if (item.id !== afterSaleId) {
      return item
    }
    changed = item.status === 'PENDING_REVIEW'
    return {
      ...item,
      status: 'PROCESSING',
      priority: item.reasonType === 'PRODUCT_QUALITY' ? 'HIGH' : item.priority,
      reviewOpinion: changed ? '审核通过，等待平台退款或退货流程' : '已复核，当前售后继续处理',
      writeBackStatus: 'PENDING'
    }
  })
  ElMessage({ type: 'success', message: changed ? '售后审核已通过' : '售后复核结果已更新' })
}

function writeBackAfterSale(afterSaleId: number) {
  afterSalesData.value = afterSalesData.value.map((item) => {
    if (item.id !== afterSaleId) {
      return item
    }
    return {
      ...item,
      writeBackStatus: 'SUCCESS',
      reviewOpinion: item.reviewOpinion || '处理结果已同步外部平台'
    }
  })
  ElMessage({ type: 'success', message: '售后处理结果已模拟回写到抖音平台' })
}
</script>
