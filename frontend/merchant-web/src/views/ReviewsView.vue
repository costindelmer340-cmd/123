<template>
  <div class="panel">
    <div class="toolbar">
      <el-segmented v-model="risk" :options="riskOptions" />
      <el-button type="primary" @click="batchAnalyze">批量分析</el-button>
    </div>
    <el-table v-loading="loading" :data="filteredReviews">
      <el-table-column prop="platformCode" label="平台" width="100" />
      <el-table-column prop="orderNo" label="订单号" min-width="160" />
      <el-table-column prop="merchantName" label="商家" min-width="150" />
      <el-table-column prop="productName" label="商品" min-width="180" />
      <el-table-column prop="productScore" label="商品分" width="100" />
      <el-table-column prop="serviceScore" label="服务分" width="100" />
      <el-table-column prop="content" label="评价内容" min-width="320" />
      <el-table-column label="情感" width="120">
        <template #default="{ row }">{{ sentimentText(row.sentiment) }}</template>
      </el-table-column>
      <el-table-column label="风险" width="120">
        <template #default="{ row }">{{ riskText(row.riskLevel) }}</template>
      </el-table-column>
      <el-table-column prop="keywords" label="关键词" min-width="150" />
      <el-table-column prop="analysisSummary" label="分析摘要" min-width="240" />
      <el-table-column prop="suggestion" label="处理建议" min-width="260" />
      <el-table-column label="操作" width="130">
        <template #default="{ row }">
          <el-button link type="primary" @click="analyzeReview(row.id)">AI 分析</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { loadTwentyMallMerchantReviews } from '../api'
import { ElMessage } from 'element-plus'
import { getMerchantBindings } from '../utils/auth'

type ReviewRow = {
  id: number
  platformCode: string
  productScore: number
  serviceScore: number
  content: string
  sentiment: string
  riskLevel: string
  keywords: string
  analysisSummary: string
  suggestion: string
  orderNo?: string
  productName?: string
  merchantName?: string
}

const risk = ref('全部')
const riskOptions = [
  { label: '全部', value: '全部' },
  { label: '高风险', value: 'HIGH' },
  { label: '中风险', value: 'MEDIUM' },
  { label: '低风险', value: 'LOW' }
]
const reviewData = ref<ReviewRow[]>([])
const loading = ref(false)

onMounted(async () => {
  loading.value = true
  reviewData.value = await loadBoundTwentyMallReviews()
  loading.value = false
})

const filteredReviews = computed(() => {
  if (risk.value === '全部') {
    return reviewData.value
  }
  return reviewData.value.filter((item) => item.riskLevel === risk.value)
})

function batchAnalyze() {
  reviewData.value = reviewData.value.map((item) => analyzeReviewItem(item))
  ElMessage({ type: 'success', message: '已完成当前评价列表分析' })
}

function analyzeReview(reviewId: number) {
  reviewData.value = reviewData.value.map((item) => (item.id === reviewId ? analyzeReviewItem(item) : item))
  ElMessage({ type: 'success', message: '已生成该评价的分析结果' })
}

function analyzeReviewItem(item: ReviewRow) {
  const negative = item.content.includes('划痕') || item.content.includes('问题') || item.productScore <= 2
  const logistics = item.content.includes('物流')
  const serviceGood = item.content.includes('客服') || item.serviceScore >= 4
  const keywords = [
    item.content.includes('划痕') ? '商品划痕' : '',
    logistics ? '物流体验' : '',
    serviceGood ? '客服服务' : '',
    negative ? '质量风险' : '正向反馈'
  ].filter(Boolean).join('、')

  return {
    ...item,
    sentiment: negative ? 'NEGATIVE' : 'POSITIVE',
    riskLevel: negative ? 'MEDIUM' : 'LOW',
    keywords,
    analysisSummary: negative
      ? '评价包含商品质量或体验风险，需要客服主动跟进。'
      : '评价整体正向，可用于服务质量复盘和优秀案例沉淀。',
    suggestion: negative
      ? '建议在 24 小时内联系用户，核实问题并提供换货、补偿或质检处理方案。'
      : '建议标记为低风险评价，后续用于客服服务质量样本。'
  }
}

async function loadBoundTwentyMallReviews() {
  const twentyMallBindings = getMerchantBindings().filter((item) => item.platformCode === 'TWENTY_MALL' && item.accountNo)
  if (!twentyMallBindings.length) {
    return []
  }
  const result = await Promise.all(twentyMallBindings.map(async (binding) => {
    try {
      return await loadTwentyMallMerchantReviews(binding.accountNo as string) as ReviewRow[]
    } catch {
      return []
    }
  }))
  return result.flat()
}

function sentimentText(value: string) {
  const map: Record<string, string> = {
    POSITIVE: '正向',
    NEGATIVE: '负向',
    NEUTRAL: '中性',
    MIXED: '混合'
  }
  return map[value] || value
}

function riskText(value: string) {
  const map: Record<string, string> = {
    HIGH: '高风险',
    MEDIUM: '中风险',
    LOW: '低风险',
    NONE: '无风险'
  }
  return map[value] || value
}
</script>
