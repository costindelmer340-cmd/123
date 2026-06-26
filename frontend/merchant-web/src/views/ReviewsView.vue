<template>
  <div class="panel">
    <div class="toolbar">
      <el-segmented v-model="risk" :options="['全部', 'HIGH', 'MEDIUM', 'LOW']" />
      <el-button type="primary" @click="batchAnalyze">批量分析</el-button>
    </div>
    <el-table v-loading="loading" :data="filteredReviews">
      <el-table-column prop="platformCode" label="平台" width="100" />
      <el-table-column prop="productScore" label="商品分" width="100" />
      <el-table-column prop="serviceScore" label="服务分" width="100" />
      <el-table-column prop="content" label="评价内容" min-width="320" />
      <el-table-column prop="sentiment" label="情感" width="120" />
      <el-table-column prop="riskLevel" label="风险" width="120" />
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
import { reviews } from '../data/mock'
import { loadReviews } from '../api'
import { loadListWithFallback } from '../api/normalize'
import { ElMessage } from 'element-plus'

const risk = ref('全部')
const reviewData = ref<typeof reviews>([])
const loading = ref(false)

onMounted(async () => {
  loading.value = true
  reviewData.value = await loadListWithFallback(() => loadReviews(), reviews)
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

function analyzeReviewItem(item: typeof reviews[number]) {
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
</script>
