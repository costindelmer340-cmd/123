<template>
  <div class="panel">
    <div class="toolbar">
      <el-tabs v-model="active">
        <el-tab-pane label="知识文章" name="articles" />
        <el-tab-pane label="FAQ" name="faq" />
        <el-tab-pane label="售后规则" name="rules" />
      </el-tabs>
      <el-button type="primary" @click="dialogVisible = true">新增</el-button>
    </div>
    <el-table v-loading="loading" :data="tableData">
      <el-table-column :prop="mainColumn.prop" :label="mainColumn.label" min-width="260" />
      <el-table-column prop="category" label="分类" width="180" v-if="active !== 'rules'" />
      <el-table-column prop="ruleType" label="规则类型" width="180" v-if="active === 'rules'" />
      <el-table-column prop="status" label="状态" width="130" v-if="active === 'articles'" />
      <el-table-column label="启用" width="100" v-if="active !== 'articles'">
        <template #default="{ row }">
          <el-tag :type="row.enabled ? 'success' : 'info'">{{ row.enabled ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" min-width="160" />
      <el-table-column label="操作" width="150">
        <template #default>
          <el-button link type="primary" @click="toastTodo('编辑知识')">编辑</el-button>
          <el-button link type="danger" @click="toastTodo('删除知识')">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-dialog v-model="dialogVisible" title="新增知识" width="560px">
      <el-form label-width="88px">
        <el-form-item :label="mainColumn.label">
          <el-input v-model="knowledgeForm.title" />
        </el-form-item>
        <el-form-item label="分类">
          <el-input v-model="knowledgeForm.category" />
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model="knowledgeForm.content" type="textarea" :rows="4" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="createKnowledge">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { articles, faqs, rules } from '../data/mock'
import { loadArticles, loadFaqs, loadRules } from '../api'
import { loadListWithFallback } from '../api/normalize'
import { toastTodo } from '../utils/feedback'
import { ElMessage } from 'element-plus'

const active = ref('articles')
const articleData = ref<typeof articles>(articles)
const faqData = ref<typeof faqs>(faqs)
const ruleData = ref<typeof rules>(rules)
const loading = ref(false)
const dialogVisible = ref(false)
const knowledgeForm = ref({ title: '', category: '', content: '' })

async function loadCurrentTab() {
  loading.value = true
  if (active.value === 'articles') {
    articleData.value = await loadListWithFallback(() => loadArticles(), articles)
  } else if (active.value === 'faq') {
    faqData.value = await loadListWithFallback(() => loadFaqs(), faqs)
  } else {
    ruleData.value = await loadListWithFallback(() => loadRules(), rules)
  }
  loading.value = false
}

onMounted(loadCurrentTab)
watch(active, loadCurrentTab)

const tableData = computed(() => {
  if (active.value === 'faq') {
    return faqData.value
  }
  if (active.value === 'rules') {
    return ruleData.value
  }
  return articleData.value
})

const mainColumn = computed(() => {
  if (active.value === 'faq') {
    return { prop: 'question', label: '问题' }
  }
  if (active.value === 'rules') {
    return { prop: 'ruleName', label: '规则名称' }
  }
  return { prop: 'title', label: '标题' }
})

function createKnowledge() {
  const title = knowledgeForm.value.title.trim()
  if (!title) {
    ElMessage({ type: 'warning', message: `请输入${mainColumn.value.label}` })
    return
  }
  const createdAt = '2026-06-25 19:30:00'
  if (active.value === 'faq') {
    faqData.value = [{ id: Date.now(), question: title, answer: knowledgeForm.value.content, category: knowledgeForm.value.category || 'GENERAL', priority: 0, enabled: true, createdAt }, ...faqData.value]
  } else if (active.value === 'rules') {
    ruleData.value = [{ id: Date.now(), ruleName: title, ruleType: knowledgeForm.value.category || 'CUSTOM', content: knowledgeForm.value.content, enabled: true, createdAt }, ...ruleData.value]
  } else {
    articleData.value = [{ id: Date.now(), title, category: knowledgeForm.value.category || 'GENERAL', status: 'PUBLISHED', createdAt }, ...articleData.value]
  }
  knowledgeForm.value = { title: '', category: '', content: '' }
  dialogVisible.value = false
  ElMessage({ type: 'success', message: '知识已新增' })
}
</script>
