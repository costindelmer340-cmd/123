<template>
  <div class="panel">
    <div class="toolbar">
      <el-input v-model="keyword" placeholder="搜索订单号、买家" style="max-width: 320px" />
      <el-button type="primary" :loading="syncing" @click="syncOrders">同步订单</el-button>
    </div>
    <el-table v-loading="loading" :data="filteredOrders">
      <el-table-column prop="externalOrderNo" label="订单号" min-width="170" />
      <el-table-column prop="merchantName" label="商家" min-width="150" />
      <el-table-column prop="buyerMaskedName" label="买家" width="90" />
      <el-table-column label="订单状态" width="130">
        <template #default="{ row }">{{ formatStatus('order', row.orderStatus) }}</template>
      </el-table-column>
      <el-table-column label="支付状态" width="110">
        <template #default="{ row }">{{ formatStatus('pay', row.payStatus) }}</template>
      </el-table-column>
      <el-table-column label="物流状态" width="130">
        <template #default="{ row }">{{ formatStatus('logistics', row.logisticsStatus) }}</template>
      </el-table-column>
      <el-table-column label="售后状态" width="130">
        <template #default="{ row }">{{ formatStatus('afterSale', row.afterSaleStatus) }}</template>
      </el-table-column>
      <el-table-column prop="totalAmount" label="金额" width="100" />
      <el-table-column prop="orderedAt" label="下单时间" min-width="160" />
      <el-table-column label="操作" width="110">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-dialog v-model="detailVisible" title="订单详情" width="560px">
      <el-descriptions v-if="currentOrder" border :column="2">
        <el-descriptions-item label="订单号">{{ currentOrder.externalOrderNo }}</el-descriptions-item>
        <el-descriptions-item label="商家">{{ currentOrder.merchantName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="买家">{{ currentOrder.buyerMaskedName }}</el-descriptions-item>
        <el-descriptions-item label="订单状态">{{ formatStatus('order', currentOrder.orderStatus) }}</el-descriptions-item>
        <el-descriptions-item label="支付状态">{{ formatStatus('pay', currentOrder.payStatus) }}</el-descriptions-item>
        <el-descriptions-item label="物流状态">{{ formatStatus('logistics', currentOrder.logisticsStatus) }}</el-descriptions-item>
        <el-descriptions-item label="售后状态">{{ formatStatus('afterSale', currentOrder.afterSaleStatus) }}</el-descriptions-item>
        <el-descriptions-item label="订单金额">{{ currentOrder.totalAmount }}</el-descriptions-item>
        <el-descriptions-item label="下单时间">{{ currentOrder.orderedAt }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { orders } from '../data/mock'
import { loadOrders, loadTwentyMallMerchantOrders } from '../api'
import { loadListWithFallback } from '../api/normalize'
import { ElMessage } from 'element-plus'
import { getMerchantBindings } from '../utils/auth'

type MerchantOrder = {
  id: number
  externalOrderNo: string
  buyerMaskedName: string
  orderStatus: string
  payStatus: string
  logisticsStatus: string
  afterSaleStatus: string
  totalAmount: number
  orderedAt: string
  merchantName?: string
  productName?: string
}

const keyword = ref('')
const fallbackOrders = orders.map((item) => ({ ...item })) as MerchantOrder[]
const orderData = ref<MerchantOrder[]>([])
const loading = ref(false)
const syncing = ref(false)
const detailVisible = ref(false)
const currentOrder = ref<MerchantOrder | null>(null)
const statusText = {
  order: {
    COMPLETED: '已完成',
    SHIPPED: '已发货',
    PENDING: '待处理',
    CANCELED: '已取消',
    PAID: '已支付',
    UNPAID: '待支付',
    REFUNDED: '已退款',
    CLOSED: '已关闭'
  } as Record<string, string>,
  pay: {
    PAID: '已支付',
    UNPAID: '未支付',
    REFUNDED: '已退款',
    REFUNDING: '退款中',
    PART_REFUNDED: '部分退款'
  } as Record<string, string>,
  logistics: {
    RECEIVED: '已签收',
    IN_TRANSIT: '运输中',
    WAITING: '待发货',
    WAIT_SHIPPING: '待发货',
    NOT_SHIPPED: '待发货',
    SHIPPED: '已发货',
    DELIVERED: '已送达',
    SIGNED: '已签收'
  } as Record<string, string>,
  afterSale: {
    AFTER_SALE: '售后中',
    NONE: '未申请',
    COMPLETED: '已完成',
    PROCESSING: '处理中',
    APPLIED: '已申请',
    REJECTED: '已拒绝',
    CLOSED: '已关闭'
  } as Record<string, string>
}

function formatStatus(type: keyof typeof statusText, value: string) {
  if (!value) return '-'
  return statusText[type][value] || value
}

onMounted(async () => {
  loading.value = true
  orderData.value = await loadBoundMerchantOrders()
  loading.value = false
})

const filteredOrders = computed(() => {
  const source = orderData.value.length ? orderData.value : fallbackOrders
  return source.filter((item) => {
    const text = `${item.externalOrderNo || ''}${item.buyerMaskedName || ''}${item.merchantName || ''}${item.productName || ''}`
    return text.includes(keyword.value)
  })
})

async function syncOrders() {
  syncing.value = true
  orderData.value = await loadBoundMerchantOrders()
  syncing.value = false
  ElMessage({ type: 'success', message: '订单已同步到最新数据' })
}

function openDetail(row: MerchantOrder) {
  currentOrder.value = row
  detailVisible.value = true
}

async function loadBoundMerchantOrders() {
  const twentyMallBindings = getMerchantBindings().filter((item) => item.platformCode === 'TWENTY_MALL' && item.accountNo)
  if (!twentyMallBindings.length) {
    return loadListWithFallback(() => loadOrders(), fallbackOrders)
  }
  const result = await Promise.all(twentyMallBindings.map(async (binding) => {
    try {
      const list = await loadTwentyMallMerchantOrders(binding.accountNo as string) as MerchantOrder[]
      return list.map((item) => ({
        ...item,
        buyerMaskedName: item.buyerMaskedName || '20商城买家'
      }))
    } catch {
      return []
    }
  }))
  return result.flat()
}
</script>
