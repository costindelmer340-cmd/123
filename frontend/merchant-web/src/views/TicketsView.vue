<template>
  <div class="panel">
    <div class="toolbar">
      <el-input v-model="keyword" placeholder="搜索工单" style="max-width: 320px" />
      <el-button type="primary" @click="dialogVisible = true">新建工单</el-button>
    </div>
    <el-table v-loading="loading" :data="filteredTickets">
      <el-table-column prop="ticketNo" label="工单号" min-width="170" />
      <el-table-column prop="title" label="标题" min-width="220" />
      <el-table-column prop="ticketType" label="类型" width="130" />
      <el-table-column prop="status" label="状态" width="130" />
      <el-table-column prop="priority" label="优先级" width="100" />
      <el-table-column prop="assignee" label="处理人" width="120" />
      <el-table-column prop="flowRemark" label="流转备注" min-width="200" />
      <el-table-column prop="dueAt" label="截止时间" min-width="160" />
      <el-table-column label="操作" width="150">
        <template #default="{ row }">
          <el-button link type="primary" @click="transferTicket(row.id)">流转</el-button>
          <el-button link @click="closeTicket(row.id)">关闭</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-dialog v-model="dialogVisible" title="新建工单" width="520px">
      <el-form label-width="88px">
        <el-form-item label="标题">
          <el-input v-model="ticketForm.title" placeholder="请输入工单标题" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="ticketForm.ticketType" style="width: 100%">
            <el-option label="售后问题" value="AFTER_SALE" />
            <el-option label="咨询问题" value="CONSULT" />
            <el-option label="评价风险" value="REVIEW_RISK" />
          </el-select>
        </el-form-item>
        <el-form-item label="优先级">
          <el-select v-model="ticketForm.priority" style="width: 100%">
            <el-option label="普通" value="NORMAL" />
            <el-option label="高" value="HIGH" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="createTicket">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { tickets } from '../data/mock'
import { loadTickets } from '../api'
import { loadListWithFallback } from '../api/normalize'
import { ElMessage } from 'element-plus'

const ticketData = ref<typeof tickets>([])
const keyword = ref('')
const loading = ref(false)
const dialogVisible = ref(false)
const ticketForm = ref({ title: '', ticketType: 'AFTER_SALE', priority: 'NORMAL' })

onMounted(async () => {
  loading.value = true
  ticketData.value = await loadListWithFallback(() => loadTickets(), tickets)
  loading.value = false
})

const filteredTickets = computed(() => {
  const value = keyword.value.trim()
  if (!value) {
    return ticketData.value
  }
  return ticketData.value.filter((item) => item.ticketNo.includes(value) || item.title.includes(value))
})

function createTicket() {
  const title = ticketForm.value.title.trim()
  if (!title) {
    ElMessage({ type: 'warning', message: '请输入工单标题' })
    return
  }
  ticketData.value = [
    {
      id: Date.now(),
      ticketNo: `TK${new Date().toISOString().slice(0, 10).replace(/-/g, '')}${String(ticketData.value.length + 1).padStart(4, '0')}`,
      title,
      ticketType: ticketForm.value.ticketType,
      status: 'OPEN',
      priority: ticketForm.value.priority,
      assignee: '客服一组',
      flowRemark: '新建工单，待分派处理',
      dueAt: '2026-06-26 18:00:00'
    },
    ...ticketData.value
  ]
  ticketForm.value = { title: '', ticketType: 'AFTER_SALE', priority: 'NORMAL' }
  dialogVisible.value = false
  ElMessage({ type: 'success', message: '工单已创建' })
}

function transferTicket(ticketId: number) {
  ticketData.value = ticketData.value.map((item) => {
    if (item.id !== ticketId) {
      return item
    }
    const next = getNextTicketFlow(item.status)
    return {
      ...item,
      status: next.status,
      assignee: next.assignee,
      flowRemark: next.remark
    }
  })
  ElMessage({ type: 'success', message: '工单已流转到下一处理节点' })
}

function closeTicket(ticketId: number) {
  ticketData.value = ticketData.value.map((item) => {
    if (item.id !== ticketId) {
      return item
    }
    return {
      ...item,
      status: 'CLOSED',
      assignee: '客服主管',
      flowRemark: '工单已关闭，处理结果已归档'
    }
  })
  ElMessage({ type: 'success', message: '工单已关闭' })
}

function getNextTicketFlow(status: string) {
  if (status === 'OPEN') {
    return { status: 'IN_PROGRESS', assignee: '客服专员', remark: '已分派客服专员处理' }
  }
  if (status === 'IN_PROGRESS') {
    return { status: 'PENDING', assignee: '售后主管', remark: '复杂问题升级至售后主管复核' }
  }
  if (status === 'PENDING') {
    return { status: 'RESOLVED', assignee: '客服主管', remark: '处理方案已确认，等待关闭' }
  }
  return { status, assignee: '客服主管', remark: '当前工单已在最终节点' }
}
</script>
