<template>
  <div class="conversation-grid">
    <div class="panel">
      <h2 class="section-title">会话列表</h2>
      <div
        v-for="item in conversationData"
        :key="item.id"
        class="status-line clickable-line"
        :class="{ active: selectedConversation?.id === item.id }"
        @click="selectConversation(item)"
      >
        <div>
          <strong>{{ conversationTitle(item) }}</strong>
          <div class="page-kicker">{{ item.orderNo }} · {{ cleanProductName(item.productName) }}</div>
          <div class="page-kicker">{{ item.aiIntent }} · {{ formatStatus(item.status) }} · {{ item.lastMessageAt }}</div>
        </div>
        <div class="conversation-actions">
          <el-tag>{{ formatStatus(item.status) }}</el-tag>
          <el-button link type="primary" @click.stop="openOrderDetail(item)">详细</el-button>
        </div>
      </div>
    </div>
    <div class="panel">
      <h2 class="section-title">聊天窗口</h2>
      <div class="page-kicker">
        <template v-if="selectedConversation">
          {{ conversationTitle(selectedConversation) }}｜{{ selectedConversation.orderNo }}｜{{ cleanProductName(selectedConversation.productName) }}
        </template>
        <template v-else>请选择会话</template>
      </div>
      <div class="chat-box">
        <div
          v-for="message in chatMessages"
          :key="message.id"
          class="message-row"
          :class="{ right: message.senderType === 'STAFF' }"
        >
          <div class="speaker">{{ speakerText(message.senderType) }} {{ formatMessageTime(message.createdAt) }}</div>
          <div class="bubble" :class="message.senderType.toLowerCase()">
            {{ message.content }}
          </div>
        </div>
      </div>
      <div class="chat-input">
        <el-input v-model="replyContent" placeholder="输入回复内容" @keyup.enter="sendReply" />
        <el-button type="primary" @click="sendReply">发送</el-button>
      </div>
    </div>
    <el-dialog v-model="orderDetailVisible" title="订单详情" width="620px">
      <el-descriptions v-if="detailConversation" :column="2" border>
        <el-descriptions-item label="订单编号">{{ detailConversation.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="所属平台">{{ detailConversation.platformName || '20商城' }}</el-descriptions-item>
        <el-descriptions-item label="商家名称">{{ detailConversation.merchantName }}</el-descriptions-item>
        <el-descriptions-item label="商品名称">{{ cleanProductName(detailConversation.productName) }}</el-descriptions-item>
        <el-descriptions-item label="售后状态">{{ detailConversation.afterSaleStatus }}</el-descriptions-item>
        <el-descriptions-item label="接待状态">{{ formatStatus(detailConversation.status) }}</el-descriptions-item>
        <el-descriptions-item label="咨询意图">{{ detailConversation.aiIntent }}</el-descriptions-item>
        <el-descriptions-item label="最近消息时间">{{ detailConversation.lastMessageAt }}</el-descriptions-item>
        <el-descriptions-item label="最近消息" :span="2">{{ detailConversation.lastMessage || '暂无' }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button type="primary" @click="orderDetailVisible = false">知道了</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, ref, watch } from 'vue'
import { conversations } from '../data/mock'
import { ElMessage } from 'element-plus'
import { getMerchantBindings } from '../utils/auth'

type DemoConversation = {
  id: number
  conversationNo: string
  orderNo: string
  productName: string
  platformName?: string
  merchantAccountNo?: string
  merchantName: string
  afterSaleStatus: string
  status: string
  aiIntent: string
  lastMessage: string
  lastMessageAt: string
}

type DemoMessage = {
  id: string
  orderNo: string
  senderType: string
  speaker: string
  content: string
  createdAt: string
}

const conversationData = ref<DemoConversation[]>([])
const selectedConversation = ref<DemoConversation | null>(null)
const replyContent = ref('')
const chatMessages = ref<DemoMessage[]>([])
const orderDetailVisible = ref(false)
const detailConversation = ref<DemoConversation | null>(null)
let pollingTimer = 0

onMounted(async () => {
  await loadConversations()
  selectedConversation.value = conversationData.value[0] || null
  await loadMessages()
  pollingTimer = window.setInterval(async () => {
    await loadConversations(false)
    await loadMessages(false)
  }, 2500)
})

onUnmounted(() => {
  window.clearInterval(pollingTimer)
})

watch(selectedConversation, (next, previous) => {
  if (next?.orderNo !== previous?.orderNo) {
    replyContent.value = ''
  }
  loadMessages()
})

function selectConversation(conversation: DemoConversation) {
  selectedConversation.value = conversation
}

function openOrderDetail(conversation: DemoConversation) {
  detailConversation.value = conversation
  orderDetailVisible.value = true
}

async function loadConversations(showError = true) {
  try {
    const accounts = getMerchantBindings()
      .filter((item) => item.platformCode === 'TWENTY_MALL' && item.accountNo)
      .map((item) => `merchantAccounts=${encodeURIComponent(item.accountNo as string)}`)
      .join('&')
    const response = await fetch(`/api/demo-chat/conversations${accounts ? `?${accounts}` : ''}`)
    const payload = await response.json()
    conversationData.value = payload.data || []
    if (selectedConversation.value) {
      const latestSelected = conversationData.value.find((item) => item.orderNo === selectedConversation.value?.orderNo)
      selectedConversation.value = latestSelected || conversationData.value[0] || null
    }
  } catch {
    conversationData.value = conversations.map((item) => ({
      id: item.id,
      conversationNo: item.conversationNo,
      orderNo: item.orderNo || 'DY202606250001',
      productName: item.productName || 'Aurora X1 智能手机',
      platformName: '抖音商城',
      merchantName: item.merchantName || '星链数码旗舰店',
      afterSaleStatus: item.afterSaleStatus || '处理中',
      status: item.status,
      aiIntent: item.aiIntent,
      lastMessage: item.lastMessage,
      lastMessageAt: item.lastMessageAt
    }))
    if (showError) {
      ElMessage({ type: 'warning', message: '后端演示对话服务未启动，当前显示本地会话' })
    }
  }
}

async function loadMessages(showError = true) {
  const conversation = selectedConversation.value
  if (!conversation) {
    chatMessages.value = [{ id: 'empty', orderNo: '', senderType: 'AI', speaker: 'AI客服', content: '请选择左侧会话', createdAt: '' }]
    return
  }
  try {
    const response = await fetch(`/api/demo-chat/conversations/${conversation.orderNo}/messages`)
    const payload = await response.json()
    chatMessages.value = payload.data || []
  } catch {
    chatMessages.value = [
      { id: 'user-last', orderNo: conversation.orderNo, senderType: 'CONSUMER', speaker: '用户', content: conversation.lastMessage || '暂无用户消息', createdAt: '' },
      { id: 'ai-tip', orderNo: conversation.orderNo, senderType: 'AI', speaker: 'AI客服', content: '请根据订单、售后规则和知识库给出准确回复。', createdAt: '' }
    ]
    if (showError) {
      ElMessage({ type: 'warning', message: '暂时无法读取共享消息' })
    }
  }
}

async function sendReply() {
  const conversation = selectedConversation.value
  const content = replyContent.value.trim()
  if (!conversation) {
    ElMessage({ type: 'warning', message: '请先选择会话' })
    return
  }
  if (!content) {
    ElMessage({ type: 'warning', message: '请输入回复内容' })
    return
  }
  try {
    await fetch(`/api/demo-chat/conversations/${conversation.orderNo}/messages`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ senderType: 'STAFF', content })
    })
    replyContent.value = ''
    await loadConversations(false)
    await loadMessages(false)
    ElMessage({ type: 'success', message: '回复已发送，用户端会自动刷新显示' })
  } catch {
    ElMessage({ type: 'error', message: '发送失败，请确认后端服务已启动' })
  }
}

function formatStatus(status: string) {
  const statusMap: Record<string, string> = {
    AI_SERVING: 'AI接待中',
    AGENT_SERVING: '人工接待中',
    CLOSED: '已关闭'
  }
  return statusMap[status] || status
}

function conversationTitle(conversation: DemoConversation) {
  return `${conversation.platformName || '20商城'} · ${conversation.merchantName}`
}

function cleanProductName(productName: string) {
  return productName.replace(/^20商城\s*/, '').trim()
}

function formatMessageTime(value?: string) {
  if (!value) return ''
  const text = String(value).replace('T', ' ').replace(/\.\d+$/, '')
  const match = text.match(/^(\d{4})[-/.](\d{1,2})[-/.](\d{1,2})\s+(\d{1,2}):(\d{1,2})(?::(\d{1,2}))?/)
  if (!match) return text
  const [, year, month, day, hour, minute, second = '00'] = match
  return `${year}.${Number(month)}.${Number(day)} ${hour.padStart(2, '0')}:${minute.padStart(2, '0')}:${second.padStart(2, '0')}`
}

function speakerText(senderType: string) {
  const speakerMap: Record<string, string> = {
    CONSUMER: '用户',
    AI: 'AI客服',
    STAFF: '人工客服'
  }
  return speakerMap[senderType] || senderType
}
</script>

<style scoped>
.conversation-grid {
  display: grid;
  grid-template-columns: 360px minmax(0, 1fr);
  gap: 16px;
  margin-top: 16px;
}

.chat-box {
  height: 460px;
  overflow: auto;
  border: 1px solid #e4e8f0;
  border-radius: 8px;
  padding: 14px;
  background: #f8fafc;
}

.bubble {
  display: inline-block;
  width: fit-content;
  max-width: 76%;
  padding: 10px 12px;
  border-radius: 8px;
  margin-bottom: 10px;
  line-height: 1.6;
  text-align: left;
  white-space: pre-wrap;
  word-break: break-word;
}

.message-row {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  margin-bottom: 12px;
}

.message-row.right {
  align-items: flex-end;
}

.speaker {
  color: #64748b;
  font-size: 12px;
  margin-bottom: 4px;
}

.consumer {
  background: #fff;
}

.ai {
  background: #e8f4ff;
}

.staff {
  background: #dcfce7;
}

.chat-input {
  display: flex;
  gap: 10px;
  margin-top: 12px;
}

.clickable-line {
  cursor: pointer;
}

.clickable-line.active {
  background: #eef6ff;
  border-color: #b7d8ff;
}

.conversation-actions {
  display: inline-flex;
  flex-shrink: 0;
  align-items: center;
  gap: 8px;
}

@media (max-width: 1200px) {
  .conversation-grid {
    grid-template-columns: 300px minmax(0, 1fr);
  }
}
</style>
