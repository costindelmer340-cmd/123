<template>
  <div class="panel">
    <div class="toolbar">
      <el-input v-model="keyword" placeholder="搜索订单号、买家" style="max-width: 320px" />
      <el-button type="primary" :loading="syncing" @click="syncOrders">同步订单</el-button>
    </div>
    <el-table v-loading="loading" :data="filteredOrders">
      <el-table-column prop="externalOrderNo" label="订单号" min-width="170" />
      <el-table-column prop="buyerMaskedName" label="买家" width="90" />
      <el-table-column prop="orderStatus" label="订单状态" width="130" />
      <el-table-column prop="payStatus" label="支付状态" width="110" />
      <el-table-column prop="logisticsStatus" label="物流状态" width="130" />
      <el-table-column prop="afterSaleStatus" label="售后状态" width="130" />
      <el-table-column prop="totalAmount" label="金额" width="100" />
      <el-table-column prop="orderedAt" label="下单时间" min-width="160" />
      <el-table-column label="操作" width="110">
        <template #default>
          <el-button link type="primary" @click="toastTodo('订单详情')">详情</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { orders } from '../data/mock'
import { loadOrders } from '../api'
import { loadListWithFallback } from '../api/normalize'
import { toastTodo } from '../utils/feedback'
import { ElMessage } from 'element-plus'

const keyword = ref('')
const orderData = ref<typeof orders>([])
const loading = ref(false)
const syncing = ref(false)

onMounted(async () => {
  loading.value = true
  orderData.value = await loadListWithFallback(() => loadOrders(), orders)
  loading.value = false
})

const filteredOrders = computed(() => {
  const source = orderData.value.length ? orderData.value : orders
  return source.filter((item) => item.externalOrderNo.includes(keyword.value) || item.buyerMaskedName.includes(keyword.value))
})

async function syncOrders() {
  syncing.value = true
  await new Promise((resolve) => window.setTimeout(resolve, 500))
  orderData.value = [...orders]
  syncing.value = false
  ElMessage({ type: 'success', message: '订单已同步到最新演示数据' })
}
</script>
