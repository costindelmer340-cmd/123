<template>
  <el-container class="shell">
    <el-aside width="230px" class="aside">
      <div class="brand">平台管理后台</div>
      <button v-for="item in sections" :key="item" :class="{ active: active === item }" @click="active = item">{{ item }}</button>
    </el-aside>
    <el-container>
      <el-header class="header">
        <h1>{{ active }}</h1>
        <el-tag type="success">系统运行中</el-tag>
      </el-header>
      <el-main>
        <section v-if="active === '系统概览'" class="grid">
          <div v-for="item in metrics" :key="item.label" class="card">
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
          </div>
        </section>
        <section v-else-if="active === '外部平台'" class="card">
          <el-table :data="platforms">
            <el-table-column label="图标" width="88">
              <template #default="{ row }">
                <img class="platform-icon" :src="row.icon" :alt="row.name" />
              </template>
            </el-table-column>
            <el-table-column prop="code" label="平台编码" />
            <el-table-column prop="name" label="平台名称" />
            <el-table-column prop="description" label="说明" />
            <el-table-column prop="status" label="状态" />
            <el-table-column prop="shops" label="绑定店铺数" />
          </el-table>
        </section>
        <section v-else-if="active === '同步监控'" class="card">
          <el-table :data="syncLogs">
            <el-table-column prop="task" label="任务" />
            <el-table-column prop="status" label="状态" />
            <el-table-column prop="count" label="数量" />
            <el-table-column prop="time" label="时间" />
          </el-table>
        </section>
        <section v-else-if="active === 'AI 配置'" class="card">
          <el-descriptions border :column="1">
            <el-descriptions-item label="默认服务">FastAPI 规则版 AI</el-descriptions-item>
            <el-descriptions-item label="服务地址">http://localhost:9000</el-descriptions-item>
            <el-descriptions-item label="模型路线">规则版 -> 专用小模型 -> RAG</el-descriptions-item>
          </el-descriptions>
        </section>
        <section v-else class="card">
          <h2>{{ active }}</h2>
          <p>该模块已预留管理入口，后续接入真实接口。</p>
        </section>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import twentyMallIcon from './assets/platforms/twenty-mall.png'

const sections = ['系统概览', '用户管理', '商家管理', '外部平台', '同步监控', '知识库', '规则配置', '评价分析', 'AI 配置']
const active = ref('系统概览')
const metrics = [
  { label: '商家数', value: '36' },
  { label: '绑定店铺', value: '42' },
  { label: '今日同步', value: '3,482' },
  { label: 'AI 调用', value: '1,260' }
]
const platforms = [
  { code: 'DOUYIN', name: '抖音电商', description: '真实抖店开放平台接入', status: '启用', shops: 18, icon: '' },
  { code: 'TAOBAO', name: '淘宝', description: '预留淘宝开放平台接入', status: '规划中', shops: 0, icon: '' },
  { code: 'PDD', name: '拼多多', description: '预留拼多多开放平台接入', status: '规划中', shops: 0, icon: '' },
  { code: 'JD', name: '京东', description: '预留京东开放平台接入', status: '规划中', shops: 0, icon: '' },
  { code: 'TWENTY_MALL', name: '20商城', description: '自建数据库模拟真实电商平台，提供订单、售后、评价等演示数据', status: '启用', shops: 1, icon: twentyMallIcon }
]
const syncLogs = [
  { task: '抖音订单同步', status: 'SUCCESS', count: 286, time: '2026-06-25 14:00:00' },
  { task: '抖音售后同步', status: 'SUCCESS', count: 31, time: '2026-06-25 14:10:00' }
]
</script>
