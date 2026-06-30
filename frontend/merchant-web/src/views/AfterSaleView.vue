<template>
  <div class="panel">
    <div class="toolbar">
      <el-segmented v-model="status" :options="filterOptions" />
      <el-button v-if="status === '全部' && filteredAfterSales.length" type="primary" @click="writeBackAllAfterSales">统一回写处理结果</el-button>
    </div>
    <div v-loading="loading">
      <el-empty v-if="!groupedAfterSales.length" description="暂无售后单" />
      <section v-for="platform in groupedAfterSales" :key="platform.key" class="platform-group">
        <div class="platform-heading">
          <img :src="platform.icon" :alt="platform.name" />
          <div>
            <strong>{{ platform.name }} - {{ platform.shopNames }}</strong>
            <span>共 {{ platform.total }} 条售后单</span>
          </div>
        </div>
        <div v-for="shop in platform.shops" :key="shop.key" class="shop-group">
          <div class="shop-heading">
            <span>{{ shop.name }}</span>
            <em>{{ shop.items.length }} 条</em>
          </div>
          <el-table :data="shop.items">
            <el-table-column prop="orderNo" label="订单编号" min-width="170" />
            <el-table-column label="类型" width="140">
              <template #default="{ row }">{{ labelText.afterSaleType[row.afterSaleType] || row.afterSaleType }}</template>
            </el-table-column>
            <el-table-column prop="productName" label="商品名称" min-width="220" />
            <el-table-column label="状态" width="140">
              <template #default="{ row }">{{ labelText.status[row.status] || row.status }}</template>
            </el-table-column>
            <el-table-column prop="createdAt" label="创建时间" min-width="160" />
            <el-table-column label="操作" width="190">
              <template #default="{ row }">
                <span class="row-actions">
                  <span v-if="hasUnreadMark(row)" class="unread-dot" />
                  <el-button v-if="canReview(row)" link type="primary" @click="openReview(row)">审核</el-button>
                  <el-button v-else link type="primary" @click="openDetail(row)">详细</el-button>
                </span>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </section>
    </div>
    <el-dialog v-model="detailVisible" title="售后详情" width="860px">
      <el-descriptions v-if="selectedAfterSale" class="after-sale-descriptions" :column="2" border>
        <el-descriptions-item label="订单编号">{{ selectedAfterSale.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="商品名称">{{ selectedAfterSale.productName }}</el-descriptions-item>
        <el-descriptions-item label="所属平台">{{ selectedAfterSale.platformName }}</el-descriptions-item>
        <el-descriptions-item label="所属店铺">{{ selectedAfterSale.shopName }}</el-descriptions-item>
        <el-descriptions-item label="类型">{{ labelText.afterSaleType[selectedAfterSale.afterSaleType] || selectedAfterSale.afterSaleType }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ labelText.status[selectedAfterSale.status] || selectedAfterSale.status }}</el-descriptions-item>
        <el-descriptions-item label="申请原因">{{ selectedAfterSale.description || labelText.reason[selectedAfterSale.reasonType] || selectedAfterSale.reasonType }}</el-descriptions-item>
        <el-descriptions-item label="申请金额">{{ selectedAfterSale.requestedAmount }}</el-descriptions-item>
        <el-descriptions-item label="优先级">{{ labelText.priority[selectedAfterSale.priority] || selectedAfterSale.priority }}</el-descriptions-item>
        <el-descriptions-item label="回写状态">{{ labelText.writeBack[selectedAfterSale.writeBackStatus] || selectedAfterSale.writeBackStatus }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ selectedAfterSale.createdAt }}</el-descriptions-item>
        <el-descriptions-item label="审核意见" :span="2">{{ selectedAfterSale.reviewOpinion || '暂无' }}</el-descriptions-item>
        <el-descriptions-item label="凭证照片" :span="2">
          <div v-if="selectedAfterSale.evidenceImages?.length" class="evidence-list">
            <el-image
              v-for="(image, index) in selectedAfterSale.evidenceImages"
              :key="index"
              :src="image"
              :preview-src-list="selectedAfterSale.evidenceImages"
              fit="cover"
              class="evidence-image"
            />
          </div>
          <span v-else>暂无</span>
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button v-if="selectedAfterSale && canReview(selectedAfterSale)" type="danger" @click="openRejectDialog">拒绝</el-button>
        <el-button v-if="selectedAfterSale && canReview(selectedAfterSale)" type="primary" @click="approveSelectedAfterSale">同意</el-button>
      </template>
    </el-dialog>
    <el-dialog v-model="reviewVisible" title="拒绝售后申请" width="520px">
      <el-form label-width="88px">
        <el-form-item label="拒绝原因">
          <el-input
            v-model="reviewForm.reason"
            type="textarea"
            :rows="4"
            placeholder="请输入拒绝售后申请的具体原因"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reviewVisible = false">取消</el-button>
        <el-button type="primary" @click="submitReview">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { afterSales } from '../data/mock'
import { loadTwentyMallMerchantAfterSales, reviewTwentyMallAfterSale } from '../api'
import { ElMessage } from 'element-plus'
import twentyMallIcon from '../assets/platforms/twenty-mall.png'
import { getMerchantBindings } from '../utils/auth'

type AfterSaleRow = typeof afterSales[number] & {
  orderNo: string
  productName: string
  platformCode: string
  platformName: string
  platformIcon: string
  shopName: string
  description?: string
  evidenceImages?: string[]
}

const status = ref('全部')
const READ_AFTER_SALE_KEY = 'merchant_read_after_sale_ids'
const afterSalesData = ref<AfterSaleRow[]>([])
const loading = ref(false)
const reviewVisible = ref(false)
const detailVisible = ref(false)
const selectedAfterSale = ref<AfterSaleRow | null>(null)
const reviewingId = ref<number | null>(null)
const readAfterSaleIds = ref<Set<number>>(readStoredAfterSaleIds())
const reviewForm = ref({ reason: '' })
const filterOptions = [
  { label: '全部', value: '全部' },
  { label: '待审核', value: 'PENDING_REVIEW' },
  { label: '处理中', value: 'PROCESSING' },
  { label: '已结束', value: 'FINISHED' }
]
const labelText = {
  afterSaleType: { RETURN_REFUND: '退货退款', REFUND_ONLY: '仅退款', PRICE_PROTECTION: '价保' } as Record<string, string>,
  reason: { PRODUCT_QUALITY: '商品质量问题', LOGISTICS_DELAY: '物流延迟', WRONG_GOODS: '错发/漏发', PRICE_PROTECTION: '价格保护', OTHER: '其他原因' } as Record<string, string>,
  status: { PENDING_REVIEW: '待审核', PROCESSING: '处理中', REJECTED: '已拒绝', COMPLETED: '已完成', CLOSED: '已关闭' } as Record<string, string>,
  priority: { HIGH: '高', NORMAL: '普通', LOW: '低' } as Record<string, string>,
  writeBack: { PENDING: '待回写', WAITING: '等待中', SUCCESS: '已回写', FAILED: '回写失败' } as Record<string, string>
}

onMounted(async () => {
  loading.value = true
  const loaded = await loadBoundAfterSales()
  afterSalesData.value = loaded.map(normalizeAfterSaleRow)
  loading.value = false
})

async function loadBoundAfterSales() {
  const bindings = getMerchantBindings().filter((item) => item.platformCode === 'TWENTY_MALL' && item.accountNo)
  if (!bindings.length) {
    return []
  }
  try {
    const result = await Promise.all(bindings.map((binding) => loadTwentyMallMerchantAfterSales(binding.accountNo as string)))
    return result.flat() as typeof afterSales
  } catch {
    ElMessage({ type: 'error', message: '暂时无法读取后端售后申请，请确认后端服务和数据库已启动' })
    return []
  }
}

const filteredAfterSales = computed(() => {
  if (status.value === '全部') {
    return afterSalesData.value
  }
  if (status.value === 'FINISHED') {
    return afterSalesData.value.filter((item) => ['COMPLETED', 'REJECTED', 'CLOSED'].includes(item.status))
  }
  return afterSalesData.value.filter((item) => item.status === status.value)
})

const groupedAfterSales = computed(() => {
  const platformMap = new Map<string, {
    key: string
    name: string
    icon: string
    total: number
    shopNames: Set<string>
    shops: Map<string, { key: string; name: string; items: AfterSaleRow[] }>
  }>()

  filteredAfterSales.value.forEach((item) => {
    if (!platformMap.has(item.platformCode)) {
      platformMap.set(item.platformCode, {
        key: item.platformCode,
        name: item.platformName,
        icon: item.platformIcon,
        total: 0,
        shopNames: new Set(),
        shops: new Map()
      })
    }
    const platform = platformMap.get(item.platformCode)!
    platform.total += 1
    platform.shopNames.add(item.shopName)
    const shopKey = `${item.platformCode}:${item.shopName}`
    if (!platform.shops.has(shopKey)) {
      platform.shops.set(shopKey, { key: shopKey, name: item.shopName, items: [] })
    }
    platform.shops.get(shopKey)!.items.push(item)
  })

  return Array.from(platformMap.values()).map((platform) => ({
    ...platform,
    shopNames: Array.from(platform.shopNames).join('、'),
    shops: Array.from(platform.shops.values())
  }))
})

function openDetail(row: AfterSaleRow) {
  markAfterSaleRead(row)
  selectedAfterSale.value = row
  detailVisible.value = true
}

function openReview(row: AfterSaleRow) {
  markAfterSaleRead(row)
  selectedAfterSale.value = row
  detailVisible.value = true
}

function submitReview() {
  if (reviewingId.value === null) {
    return
  }
  if (!reviewForm.value.reason.trim()) {
    ElMessage({ type: 'warning', message: '请输入拒绝原因' })
    return
  }
  approveAfterSale(reviewingId.value, 'REJECT', reviewForm.value.reason.trim())
  reviewVisible.value = false
}

function approveSelectedAfterSale() {
  if (!selectedAfterSale.value) {
    return
  }
  approveAfterSale(selectedAfterSale.value.id, 'APPROVE')
}

function openRejectDialog() {
  if (!selectedAfterSale.value) {
    return
  }
  reviewingId.value = selectedAfterSale.value.id
  reviewForm.value = { reason: '' }
  reviewVisible.value = true
}

async function approveAfterSale(afterSaleId: number, result = 'APPROVE', reason = '') {
  try {
    const updated = await reviewTwentyMallAfterSale(afterSaleId, result as 'APPROVE' | 'REJECT', reason) as AfterSaleRow
    afterSalesData.value = afterSalesData.value.map((item) => (
      item.id === afterSaleId ? normalizeAfterSaleRow({ ...item, ...updated }) : item
    ))
    ElMessage({
      type: 'success',
      message: result === 'REJECT' ? '售后申请已拒绝并写入数据库' : '售后审核已通过并写入数据库'
    })
    syncSelectedAfterSale(afterSaleId)
    detailVisible.value = false
  } catch {
    ElMessage({ type: 'error', message: '审核失败，请确认后端服务和数据库已启动' })
  }
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
  syncSelectedAfterSale(afterSaleId)
}

function writeBackAllAfterSales() {
  const writableStatuses = ['PROCESSING', 'COMPLETED', 'REJECTED', 'CLOSED']
  let changed = 0
  afterSalesData.value = afterSalesData.value.map((item) => {
    if (!writableStatuses.includes(item.status) || item.writeBackStatus === 'SUCCESS') {
      return item
    }
    changed += 1
    return {
      ...item,
      writeBackStatus: 'SUCCESS',
      reviewOpinion: item.reviewOpinion || '处理结果已同步外部平台'
    }
  })
  if (selectedAfterSale.value) {
    syncSelectedAfterSale(selectedAfterSale.value.id)
  }
  ElMessage({
    type: changed ? 'success' : 'info',
    message: changed ? `已统一回写 ${changed} 条售后处理结果` : '当前没有需要回写的售后单'
  })
}

function canReview(row: AfterSaleRow) {
  return row.status === 'PENDING_REVIEW'
}

function hasUnreadMark(row: AfterSaleRow) {
  return row.status === 'PENDING_REVIEW' && !readAfterSaleIds.value.has(row.id)
}

function markAfterSaleRead(row: AfterSaleRow) {
  if (row.status !== 'PENDING_REVIEW') {
    return
  }
  readAfterSaleIds.value.add(row.id)
  localStorage.setItem(READ_AFTER_SALE_KEY, JSON.stringify(Array.from(readAfterSaleIds.value)))
}

function readStoredAfterSaleIds() {
  const raw = localStorage.getItem(READ_AFTER_SALE_KEY)
  if (!raw) {
    return new Set<number>()
  }
  try {
    return new Set((JSON.parse(raw) as number[]).map(Number))
  } catch {
    return new Set<number>()
  }
}

function syncSelectedAfterSale(afterSaleId: number) {
  if (selectedAfterSale.value?.id === afterSaleId) {
    selectedAfterSale.value = afterSalesData.value.find((item) => item.id === afterSaleId) || null
  }
}

function normalizeAfterSaleRow(item: typeof afterSales[number]) {
  const productNameMap: Record<number, string> = {
    1: '青轴机械键盘',
    2: '便携保温杯'
  }
  const productNameByNo: Record<string, string> = {
    AS202606250001: '青轴机械键盘',
    AS202606250002: '便携保温杯'
  }
  const shopNameMap: Record<number, string> = {
    1: '极光外设旗舰店',
    2: '极光外设旗舰店'
  }
  const productName = productNameByNo[item.afterSaleNo] || (
    'productName' in item && typeof item.productName === 'string'
      ? removePlatformPrefix(item.productName)
      : productNameMap[item.id] || '售后商品'
  )
  return {
    ...item,
    orderNo: 'orderNo' in item && typeof item.orderNo === 'string'
      ? item.orderNo
      : fallbackOrderNo(item.afterSaleNo),
    productName,
    platformCode: 'TWENTY_MALL',
    platformName: '20商城',
    platformIcon: twentyMallIcon,
    shopName: 'shopName' in item && typeof item.shopName === 'string'
      ? item.shopName
      : shopNameMap[item.id] || '20商城店铺'
  }
}

function removePlatformPrefix(productName: string) {
  return productName.replace(/^20商城\s*/, '').trim()
}

function fallbackOrderNo(afterSaleNo: string) {
  const map: Record<string, string> = {
    TMAS202606270001: 'TM202606270001',
    TMAS202606270002: 'TM202606270004',
    AS202606250001: 'TM202606270001',
    AS202606250002: 'TM202606270004'
  }
  return map[afterSaleNo] || afterSaleNo.replace(/^TMAS/, 'TM')
}
</script>

<style scoped>
.platform-group {
  margin-top: 18px;
  border: 1px solid #e4e8f0;
  border-radius: 8px;
  overflow: hidden;
  background: #fff;
}

.platform-heading {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 14px;
  background: #f8fafc;
  border-bottom: 1px solid #e4e8f0;
}

.platform-heading img {
  width: 28px;
  height: 28px;
  border-radius: 6px;
  object-fit: cover;
}

.platform-heading strong,
.platform-heading span {
  display: block;
}

.platform-heading span {
  margin-top: 2px;
  color: #64748b;
  font-size: 12px;
}

.shop-group + .shop-group {
  border-top: 1px solid #e4e8f0;
}

.shop-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 14px;
  color: #334155;
  font-weight: 600;
  background: #fff;
}

.shop-heading em {
  color: #64748b;
  font-size: 12px;
  font-style: normal;
  font-weight: 400;
}

.row-actions {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.unread-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #ef4444;
  box-shadow: 0 0 0 2px #fee2e2;
}

.evidence-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.evidence-image {
  width: 88px;
  height: 88px;
  border-radius: 6px;
  overflow: hidden;
  border: 1px solid #e4e8f0;
}

.after-sale-descriptions :deep(.el-descriptions__label) {
  width: 108px;
  min-width: 108px;
  white-space: nowrap;
}

.after-sale-descriptions :deep(.el-descriptions__content) {
  min-width: 230px;
  word-break: break-word;
}
</style>
