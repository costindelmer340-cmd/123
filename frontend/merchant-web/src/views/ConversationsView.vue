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
          <div class="page-kicker">{{ item.aiIntent }} · {{ item.lastMessageAt }}</div>
        </div>
        <el-tag>{{ item.status }}</el-tag>
      </div>
    </div>
    <div class="panel">
      <h2 class="section-title">聊天窗口</h2>
      <div class="page-kicker">{{ selectedConversation?.conversationNo || '请选择会话' }}</div>
      <div class="chat-box">
        <div v-for="message in chatMessages" :key="message.id" class="bubble" :class="message.sender">
          {{ message.content }}
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
import { computed, onMounted, ref, watch } from 'vue'
import { conversations } from '../data/mock'
import { loadConversations } from '../api'
import { loadListWithFallback } from '../api/normalize'
import { ElMessage } from 'element-plus'

const conversationData = ref<typeof conversations>([])
const selectedConversation = ref<(typeof conversations)[number] | null>(null)
const replyContent = ref('')
const localReplies = ref<Record<number, { id: string; sender: string; content: string }[]>>({})

onMounted(async () => {
  conversationData.value = await loadListWithFallback(() => loadConversations(), conversations)
  selectedConversation.value = conversationData.value[0] || null
})

watch(selectedConversation, () => {
  replyContent.value = ''
})

const chatMessages = computed(() => {
  const conversation = selectedConversation.value
  if (!conversation) {
    return [{ id: 'empty', sender: 'ai', content: '请选择左侧会话' }]
  }
  return [
    { id: 'user-last', sender: 'user', content: conversation.lastMessage || '暂无用户消息' },
    { id: 'ai-tip', sender: 'ai', content: '请根据订单、售后规则和知识库给出准确回复。' },
    ...(localReplies.value[conversation.id] || [])
  ]
})

function sendReply() {
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
  const nextMessage = { id: `staff-${Date.now()}`, sender: 'staff', content }
  localReplies.value = {
    ...localReplies.value,
    [conversation.id]: [...(localReplies.value[conversation.id] || []), nextMessage]
  }
  replyContent.value = ''
  ElMessage({ type: 'success', message: '回复已添加到当前会话' })
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
}

.user {
  background: #fff;
}

.ai {
  background: #e8f4ff;
}

.staff {
  background: #dcfce7;
  margin-left: auto;
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
