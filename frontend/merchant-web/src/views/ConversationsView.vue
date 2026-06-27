<template>
  <div class="split-grid">
    <div class="panel">
      <h2 class="section-title">会话列表</h2>
      <div
        v-for="item in conversationData"
        :key="item.id"
        class="status-line clickable-line"
        :class="{ active: selectedConversation?.id === item.id }"
        @click="selectedConversation = item"
      >
        <div>
          <strong>{{ item.conversationNo }}</strong>
          <div class="page-kicker">{{ item.orderNo }} · {{ item.productName }}</div>
          <div class="page-kicker">{{ item.aiIntent }} · {{ formatStatus(item.status) }} · {{ item.lastMessageAt }}</div>
        </div>
        <el-tag>{{ formatStatus(item.status) }}</el-tag>
      </div>
    </div>
    <div class="panel">
      <h2 class="section-title">聊天窗口</h2>
      <div class="page-kicker">
        {{ selectedConversation?.conversationNo || '请选择会话' }}
        <template v-if="selectedConversation">
          ｜{{ selectedConversation.orderNo }}｜{{ selectedConversation.merchantName }}
        </template>
      </div>
      <div class="chat-box">
        <div
          v-for="message in chatMessages"
          :key="message.id"
          class="message-row"
          :class="{ right: message.senderType === 'STAFF' }"
        >
          <div class="speaker">{{ speakerText(message.senderType) }}</div>
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
  </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, ref, watch } from 'vue'
import { conversations } from '../data/mock'
import { ElMessage } from 'element-plus'

type DemoConversation = {
  id: number
  conversationNo: string
  orderNo: string
  productName: string
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

watch(selectedConversation, () => {
  replyContent.value = ''
  loadMessages()
})

async function loadConversations(showError = true) {
  try {
    const response = await fetch('/api/demo-chat/conversations')
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
.chat-box {
  height: 330px;
  overflow: auto;
  border: 1px solid #e4e8f0;
  border-radius: 8px;
  padding: 14px;
  background: #f8fafc;
}

.bubble {
  max-width: 76%;
  padding: 10px 12px;
  border-radius: 8px;
  margin-bottom: 10px;
  line-height: 1.6;
}

.message-row {
  margin-bottom: 12px;
}

.message-row.right {
  text-align: right;
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
  margin-left: auto;
  display: inline-block;
  text-align: left;
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
</style>
