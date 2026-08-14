# LUMI 投放中台 — Phase 3: 前端核心层

> **For agentic workers:** This is Phase 3 of the implementation plan. Build the Vue 3 foundation then all page components.

**Goal:** Complete frontend SPA with router, API layer, Pinia stores, layout, and all 7 page modules.

**Architecture:** Vue 3 SPA with Composition API + `<script setup>`. Element Plus for UI components, ECharts for charts. Pinia for state management. Axios with request/response interceptors.

---

### Task 3.a: Vue 3 Project Scaffold

**Files:**
- Create: `ad-platform/frontend/package.json`
- Create: `ad-platform/frontend/vite.config.ts`
- Create: `ad-platform/frontend/tsconfig.json`
- Create: `ad-platform/frontend/tsconfig.node.json`
- Create: `ad-platform/frontend/index.html`
- Create: `ad-platform/frontend/src/main.ts`
- Create: `ad-platform/frontend/src/App.vue`
- Create: `ad-platform/frontend/src/env.d.ts`
- Create: `ad-platform/frontend/.env`

**Interfaces:**
- Consumes: nothing (standalone scaffold)
- Produces: Empty Vue 3 SPA with Vite dev server

**package.json:**
```json
{
  "name": "ad-platform-frontend",
  "version": "1.0.0",
  "type": "module",
  "scripts": {
    "dev": "vite",
    "build": "vue-tsc --noEmit && vite build",
    "preview": "vite preview"
  },
  "dependencies": {
    "vue": "^3.4.21",
    "vue-router": "^4.3.0",
    "pinia": "^2.1.7",
    "element-plus": "^2.7.0",
    "@element-plus/icons-vue": "^2.3.1",
    "axios": "^1.6.8",
    "echarts": "^5.5.0",
    "vue-echarts": "^6.7.2",
    "dayjs": "^1.11.10"
  },
  "devDependencies": {
    "@vitejs/plugin-vue": "^5.0.4",
    "typescript": "^5.4.3",
    "vite": "^5.2.6",
    "vue-tsc": "^2.0.7",
    "unplugin-auto-import": "^0.17.5",
    "unplugin-vue-components": "^0.27.0"
  }
}
```

**vite.config.ts:**
```typescript
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'
import { resolve } from 'path'

export default defineConfig({
  plugins: [
    vue(),
    AutoImport({ resolvers: [ElementPlusResolver()] }),
    Components({ resolvers: [ElementPlusResolver()] }),
  ],
  resolve: {
    alias: { '@': resolve(__dirname, 'src') },
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
```

**src/main.ts:**
```typescript
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import App from './App.vue'
import router from './router'

const app = createApp(App)
app.use(createPinia())
app.use(router)
app.use(ElementPlus)
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}
app.mount('#app')
```

- [ ] **Step 1:** Create `package.json`, `vite.config.ts`, `tsconfig.json`, `tsconfig.node.json`, `.env`
- [ ] **Step 2:** Create `index.html` with `<div id="app">` mount point
- [ ] **Step 3:** Create `src/main.ts`, `src/App.vue`, `src/env.d.ts`
- [ ] **Step 4:** Install deps: `cd ad-platform/frontend && npm install`
- [ ] **Step 5:** Verify dev server starts: `npm run dev` (visit http://localhost:5173)

---

### Task 3.b: Router + Layout + Axios Setup

**Files:**
- Create: `ad-platform/frontend/src/router/index.ts`
- Create: `ad-platform/frontend/src/layout/AppLayout.vue`
- Create: `ad-platform/frontend/src/layout/SideMenu.vue`
- Create: `ad-platform/frontend/src/utils/request.ts`

**Interfaces:**
- Consumes: main.ts from 3.a
- Produces: App layout with sidebar navigation and Axios instance

**Router:**
```typescript
// src/router/index.ts
import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory('/ad-platform'),
  routes: [
    {
      path: '/',
      component: () => import('@/layout/AppLayout.vue'),
      redirect: '/dashboard',
      children: [
        { path: 'dashboard', name: 'Dashboard', component: () => import('@/views/dashboard/DashboardPage.vue'), meta: { title: '总览看板', icon: 'DataAnalysis' } },
        { path: 'strategy', name: 'Strategy', redirect: '/strategy/list', children: [
          { path: 'list', name: 'StrategyList', component: () => import('@/views/strategy/StrategyList.vue'), meta: { title: '策略列表' } },
          { path: ':id', name: 'StrategyDetail', component: () => import('@/views/strategy/StrategyDetail.vue'), meta: { title: '策略详情' } },
        ]},
        { path: 'campaign', name: 'Campaign', redirect: '/campaign/list', children: [
          { path: 'list', name: 'CampaignList', component: () => import('@/views/campaign/CampaignList.vue'), meta: { title: '计划列表' } },
          { path: ':id', name: 'CampaignDetail', component: () => import('@/views/campaign/CampaignDetail.vue'), meta: { title: '计划详情' } },
        ]},
        { path: 'audience', name: 'Audience', component: () => import('@/views/audience/AudienceList.vue'), meta: { title: '人群管理', icon: 'User' } },
        { path: 'material', name: 'Material', component: () => import('@/views/material/MaterialList.vue'), meta: { title: '素材管理', icon: 'Picture' } },
        { path: 'rule-engine', name: 'RuleEngine', component: () => import('@/views/rule/RuleEnginePage.vue'), meta: { title: '规则引擎', icon: 'SetUp' } },
        { path: 'settings', name: 'Settings', component: () => import('@/views/settings/SettingsPage.vue'), meta: { title: '系统设置', icon: 'Setting' } },
      ],
    },
  ],
})

export default router
```

**Axios request utility:**
```typescript
// src/utils/request.ts
import axios from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({ baseURL: '/api/v1', timeout: 15000 })

request.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code !== 0) {
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message))
    }
    return res.data
  },
  (error) => {
    ElMessage.error(error.message || '网络错误')
    return Promise.reject(error)
  }
)

export default request
```

**AppLayout.vue:**
```vue
<script setup lang="ts">
import SideMenu from './SideMenu.vue'
</script>

<template>
  <el-container style="min-height: 100vh">
    <SideMenu />
    <el-container>
      <el-header style="background: #fff; border-bottom: 1px solid #eee; display: flex; align-items: center; padding: 0 20px;">
        <h2 style="margin: 0; font-size: 18px;">LUMI 程序化广告智能投放中台</h2>
      </el-header>
      <el-main style="background: #f5f7fa;">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>
```

- [ ] **Step 1:** Create `src/router/index.ts` with all routes (7 pages)
- [ ] **Step 2:** Create `src/layout/AppLayout.vue` (sidebar + header + main content)
- [ ] **Step 3:** Create `src/layout/SideMenu.vue` (el-menu with router links, expanded by default)
- [ ] **Step 4:** Create `src/utils/request.ts` (Axios with response interceptor)
- [ ] **Step 5:** Verify: dev server running, navigate to http://localhost:5173/ad-platform/dashboard shows layout

---

### Task 3.c: API Layer + Types + Stores

**Files:**
- Create: `ad-platform/frontend/src/types/index.ts`
- Create: `ad-platform/frontend/src/api/strategy.ts`
- Create: `ad-platform/frontend/src/api/campaign.ts`
- Create: `ad-platform/frontend/src/api/audience.ts`
- Create: `ad-platform/frontend/src/api/material.ts`
- Create: `ad-platform/frontend/src/api/rule.ts`
- Create: `ad-platform/frontend/src/api/dashboard.ts`
- Create: `ad-platform/frontend/src/stores/app.ts`

**Interfaces:**
- Consumes: request.ts from 3.b
- Produces: All API functions used by views

**Types:**
```typescript
// src/types/index.ts
export interface Result<T> {
  code: number
  message: string
  data: T
}

export interface PageResult<T> {
  list: T[]
  total: number
  page: number
  size: number
}

export interface StrategyDTO {
  id: number
  name: string
  code: string
  status: number
  objective: string
  description: string
  budget: number
  budgetRatio: number
  targetCpa: number
  targetCvr: number
  expectedRoas: number
  sortOrder: number
  channelAllocations?: { channel: string; budgetRatio: number }[]
  audienceIds?: number[]
  materialIds?: number[]
  currentCost?: number
  currentCpa?: number
  currentRoas?: number
}

export interface CampaignDTO {
  id: number
  strategyId: number
  strategyName: string
  name: string
  channel: string
  platformCampaignId: string
  budgetDaily: number
  bidType: string
  bidPrice: number
  status: number
  launchAt: string
  stopAt: string
  currentCost: number
  currentConversions: number
  currentCpa: number
  currentRoas: number
}
```

**API modules — one per domain:**
```typescript
// src/api/strategy.ts
import request from '@/utils/request'
import type { StrategyDTO } from '@/types'

export function getStrategies(): Promise<StrategyDTO[]> {
  return request.get('/strategies')
}

export function getStrategy(id: number): Promise<StrategyDTO> {
  return request.get(`/strategies/${id}`)
}

export function createStrategy(data: Partial<StrategyDTO>): Promise<number> {
  return request.post('/strategies', data)
}

export function updateStrategy(id: number, data: Partial<StrategyDTO>): Promise<void> {
  return request.put(`/strategies/${id}`, data)
}

export function updateStrategyStatus(id: number, status: number): Promise<void> {
  return request.patch(`/strategies/${id}/status`, { status })
}
```

**Pinia store:**
```typescript
// src/stores/app.ts
import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useAppStore = defineStore('app', () => {
  const dateRange = ref<string[]>([dayjs().subtract(30, 'day').format('YYYY-MM-DD'), dayjs().format('YYYY-MM-DD')])
  function setDateRange(range: string[]) { dateRange.value = range }
  return { dateRange, setDateRange }
})
```

- [ ] **Step 1:** Create `src/types/index.ts` with all TypeScript interfaces
- [ ] **Step 2:** Create all 6 API modules (`src/api/strategy.ts`, `campaign.ts`, `audience.ts`, `material.ts`, `rule.ts`, `dashboard.ts`)
- [ ] **Step 3:** Create `src/stores/app.ts`
- [ ] **Step 4:** Verify compilation: `npx vue-tsc --noEmit` (should pass with empty pages)

---

### Task 3.d: Dashboard Page

**Files:**
- Create: `ad-platform/frontend/src/views/dashboard/DashboardPage.vue`
- Create: `ad-platform/frontend/src/views/dashboard/StatsCard.vue`
- Create: `ad-platform/frontend/src/views/dashboard/TrendChart.vue`
- Create: `ad-platform/frontend/src/views/dashboard/ChannelPie.vue`
- Create: `ad-platform/frontend/src/views/dashboard/MaterialTop.vue`

**Interfaces:**
- Consumes: dashboard API, app store (date range)

**DashboardPage.vue** — main layout matching spec §9.1:
```vue
<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { getOverview, getTrends, getChannelDist, getMaterialTop } from '@/api/dashboard'
import { useAppStore } from '@/stores/app'
import StatsCard from './StatsCard.vue'
import TrendChart from './TrendChart.vue'
import ChannelPie from './ChannelPie.vue'
import MaterialTop from './MaterialTop.vue'

const appStore = useAppStore()
const overview = ref<any>({})
const trends = ref<any[]>([])
const channelDist = ref<any[]>([])
const materialTop = ref<any[]>([])

async function loadData() {
  const [start, end] = appStore.dateRange
  overview.value = await getOverview(start, end)
  trends.value = await getTrends(start, end)
  channelDist.value = await getChannelDist(start, end)
  materialTop.value = await getMaterialTop(start, end, 5)
}

onMounted(loadData)
watch(() => appStore.dateRange, loadData)
</script>

<template>
  <div class="dashboard-page">
    <!-- Header with budget progress -->
    <el-card class="mb-4">
      <div class="flex items-center justify-between">
        <h2 style="margin:0">数据总览</h2>
        <div class="flex items-center gap-4">
          <el-date-picker
            v-model="appStore.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            @change="loadData"
          />
          <div>
            <span>预算进度:</span>
            <el-progress
              :percentage="Number((overview.budgetProgress || 0) * 100).toFixed(1)"
              :status="overview.budgetProgress > 0.8 ? 'warning' : 'success'"
              style="width:200px;display:inline-block;margin-left:8px"
            />
            <span style="margin-left:8px;color:#999">
              ¥{{ (overview.totalCost || 0).toLocaleString() }} / ¥800,000
            </span>
          </div>
        </div>
      </div>
    </el-card>

    <!-- 4 Stats Cards -->
    <el-row :gutter="16" class="mb-4">
      <el-col :span="6"><StatsCard title="总消耗" :value="overview.totalCost" prefix="¥" :precision="0" /></el-col>
      <el-col :span="6"><StatsCard title="新客数" :value="overview.totalNewUsers" :precision="0" /></el-col>
      <el-col :span="6"><StatsCard title="CPA" :value="overview.cpa" prefix="¥" :precision="2" :status="overview.cpa > 250 ? 'danger' : 'success'" /></el-col>
      <el-col :span="6"><StatsCard title="ROAS" :value="overview.roas" :precision="2" :status="overview.roas < 1.5 ? 'warning' : 'success'" /></el-col>
    </el-row>

    <!-- Trend Chart -->
    <el-card class="mb-4">
      <TrendChart :data="trends" />
    </el-card>

    <!-- Channel Distribution + Material Top -->
    <el-row :gutter="16">
      <el-col :span="12"><el-card><ChannelPie :data="channelDist" /></el-card></el-col>
      <el-col :span="12"><el-card><MaterialTop :data="materialTop" /></el-card></el-col>
    </el-row>
  </div>
</template>
```

**StatsCard.vue:**
```vue
<script setup lang="ts">
defineProps<{
  title: string
  value?: number
  prefix?: string
  suffix?: string
  precision?: number
  status?: 'success' | 'warning' | 'danger'
}>()
</script>
<template>
  <el-card :shadow="'hover'" :class="status ? 'stat-' + status : ''">
    <div class="stat-label">{{ title }}</div>
    <div class="stat-value" :style="status ? { color: status === 'danger' ? '#f56c6c' : status === 'warning' ? '#e6a23c' : '#67c23a' } : {}">
      {{ prefix || '' }}{{ value != null ? value.toLocaleString(undefined, { minimumFractionDigits: precision ?? 0, maximumFractionDigits: precision ?? 0 }) : '--' }}{{ suffix || '' }}
    </div>
  </el-card>
</template>
<style scoped>
.stat-label { font-size: 14px; color: #909399; margin-bottom: 8px; }
.stat-value { font-size: 28px; font-weight: bold; }
</style>
```

**TrendChart.vue** — ECharts dual-Y axis chart:
```vue
<script setup lang="ts">
import * as echarts from 'echarts'
import { ref, onMounted, watch } from 'vue'

const props = defineProps<{ data: any[] }>()
const chartRef = ref<HTMLDivElement>()
let chart: echarts.ECharts | null = null

function render() {
  if (!chartRef.value || !props.data.length) return
  chart = echarts.init(chartRef.value)
  chart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['消耗', 'CPA'] },
    xAxis: { type: 'category', data: props.data.map((d: any) => d.stat_date) },
    yAxis: [{ type: 'value', name: '消耗(元)' }, { type: 'value', name: 'CPA(元)' }],
    series: [
      { name: '消耗', type: 'bar', data: props.data.map((d: any) => d.cost), itemStyle: { color: '#409eff' } },
      { name: 'CPA', type: 'line', yAxisIndex: 1, data: props.data.map((d: any) => d.conversions ? (d.cost / d.conversions).toFixed(2) : 0), itemStyle: { color: '#e6a23c' } },
    ],
  })
}

onMounted(render)
watch(() => props.data, render, { deep: true })
</script>
<template>
  <div ref="chartRef" style="height: 350px"></div>
</template>
```

- [ ] **Step 1:** Create `StatsCard.vue` — reusable metric display card
- [ ] **Step 2:** Create `TrendChart.vue` — ECharts dual-axis (bar + line)
- [ ] **Step 3:** Create `ChannelPie.vue` — ECharts pie chart, shows channel cost distribution
- [ ] **Step 4:** Create `MaterialTop.vue` — table listing top materials by CTR/Cost
- [ ] **Step 5:** Create `DashboardPage.vue` — assemble all components
- [ ] **Step 6:** Verify: navigate to /dashboard, all charts render with data

---

### Task 3.e: Strategy Pages

**Files:**
- Create: `ad-platform/frontend/src/views/strategy/StrategyList.vue`
- Create: `ad-platform/frontend/src/views/strategy/StrategyDetail.vue`
- Create: `ad-platform/frontend/src/views/strategy/StrategyForm.vue` (dialog)

**StrategyList.vue:**
```vue
<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getStrategies, updateStrategyStatus } from '@/api/strategy'
import type { StrategyDTO } from '@/types'

const router = useRouter()
const strategies = ref<StrategyDTO[]>([])

onMounted(async () => {
  strategies.value = await getStrategies()
})

const statusMap: Record<number, string> = { 0: '草稿', 1: '启用', 2: '暂停', 3: '结束' }
const statusType: Record<number, string> = { 0: 'info', 1: 'success', 2: 'warning', 3: 'danger' }

async function toggleStatus(s: StrategyDTO) {
  const newStatus = s.status === 1 ? 2 : 1
  await updateStrategyStatus(s.id!, newStatus)
  s.status = newStatus
}

function viewDetail(id: number) {
  router.push(`/strategy/${id}`)
}

function getCpaStatus(cpa: number, target: number): string {
  if (!cpa || !target) return ''
  const ratio = cpa / target
  return ratio > 1.5 ? 'danger' : ratio > 1.0 ? 'warning' : 'success'
}
</script>

<template>
  <div>
    <div class="mb-4 flex justify-between items-center">
      <h2 style="margin:0">策略管理</h2>
      <el-button type="primary" @click="router.push('/strategy/edit')">新建策略</el-button>
    </div>
    <el-row :gutter="16">
      <el-col :span="8" v-for="s in strategies" :key="s.id" class="mb-4">
        <el-card :shadow="'hover'" @click="viewDetail(s.id!)" style="cursor:pointer">
          <div class="flex justify-between items-start">
            <div>
              <span class="text-lg font-bold">{{ s.name }}</span>
              <el-tag :type="statusType[s.status]" size="small" class="ml-2">{{ statusMap[s.status] }}</el-tag>
            </div>
            <el-tag>{{ s.code }}</el-tag>
          </div>
          <div class="mt-2 text-sm text-gray-400">{{ s.description }}</div>
          <el-divider style="margin:12px 0" />
          <div class="grid grid-cols-3 gap-2 text-center">
            <div>
              <div class="text-xs text-gray-400">预算</div>
              <div class="font-bold">¥{{ (s.budget / 10000).toFixed(1) }}万</div>
            </div>
            <div>
              <div class="text-xs text-gray-400">目标CPA</div>
              <div class="font-bold">¥{{ s.targetCpa }}</div>
            </div>
            <div>
              <div class="text-xs text-gray-400">当前CPA</div>
              <div :class="'font-bold ' + (getCpaStatus(s.currentCpa!, s.targetCpa) === 'danger' ? 'text-red-500' : getCpaStatus(s.currentCpa!, s.targetCpa) === 'warning' ? 'text-yellow-500' : 'text-green-500')">
                {{ s.currentCpa ? '¥' + s.currentCpa.toFixed(2) : '--' }}
              </div>
            </div>
          </div>
          <el-divider style="margin:12px 0" />
          <div class="flex justify-between">
            <el-button size="small" :type="s.status === 1 ? 'warning' : 'success'" @click.stop="toggleStatus(s)">
              {{ s.status === 1 ? '暂停' : '启用' }}
            </el-button>
            <el-button size="small" @click.stop="viewDetail(s.id!)">详情</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>
```

- [ ] **Step 1:** Create `StrategyList.vue` — card-based layout with 3-column grid, shows budget/targetCPA/currentCPA per card, status toggle, click-to-detail
- [ ] **Step 2:** Create `StrategyDetail.vue` — full strategy info, channel allocation breakdown, associated plans list (embed CampaignList filtered by strategyId)
- [ ] **Step 3:** Verify: navigate to /strategy/list, see 7 strategy cards with data from seed

---

### Task 3.f: Campaign Page

**Files:**
- Create: `ad-platform/frontend/src/views/campaign/CampaignList.vue`
- Create: `ad-platform/frontend/src/views/campaign/CampaignDetail.vue`

**CampaignList.vue** — table with filters and batch operations:
```vue
<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getCampaigns, batchUpdateStatus } from '@/api/campaign'
import type { CampaignDTO } from '@/types'

const campaigns = ref<CampaignDTO[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const strategyId = ref<number>()
const channel = ref<string>()
const keyword = ref<string>()
const selectedIds = ref<number[]>([])
const loading = ref(false)

async function loadData() {
  loading.value = true
  try {
    const result = await getCampaigns({ page: page.value, size: size.value, strategyId: strategyId.value, channel: channel.value, keyword: keyword.value })
    campaigns.value = result.list
    total.value = result.total
  } finally {
    loading.value = false
  }
}

async function handleBatchStatus(status: number) {
  if (!selectedIds.value.length) return
  await batchUpdateStatus(selectedIds.value, status)
  selectedIds.value = []
  await loadData()
}

onMounted(loadData)

const statusMap: Record<number, string> = { 0: '搭建中', 1: '投放中', 2: '暂停', 3: '关停' }
const statusType: Record<number, string> = { 0: 'info', 1: 'success', 2: 'warning', 3: 'danger' }
const channelMap: Record<string, string> = { DOUYIN: '巨量引擎', XIAOHONGSHU: '小红书', BILIBILI: 'B站', TENCENT: '腾讯广告', BAIDU_FEED: '百度信息流', BAIDU_SEARCH: '百度搜索' }
</script>

<template>
  <div>
    <div class="mb-4 flex justify-between items-center">
      <h2 style="margin:0">广告计划</h2>
      <el-button type="primary">新建计划</el-button>
    </div>

    <!-- Filters -->
    <el-card class="mb-4">
      <el-form :inline="true" class="flex flex-wrap gap-2">
        <el-form-item label="策略">
          <el-input v-model="strategyId" placeholder="策略ID" style="width:120px" />
        </el-form-item>
        <el-form-item label="渠道">
          <el-select v-model="channel" clearable placeholder="全部渠道" style="width:130px">
            <el-option v-for="(label, key) in channelMap" :key="key" :label="label" :value="key" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="keyword" placeholder="计划名称" style="width:160px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">查询</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- Batch operations -->
    <div class="mb-2">
      <el-button size="small" @click="handleBatchStatus(1)" :disabled="!selectedIds.length">批量启用</el-button>
      <el-button size="small" @click="handleBatchStatus(2)" :disabled="!selectedIds.length">批量暂停</el-button>
      <span style="margin-left:8px;color:#999">已选 {{ selectedIds.length }} 项</span>
    </div>

    <!-- Table -->
    <el-table :data="campaigns" v-model:selection="selectedIds" row-key="id" v-loading="loading" stripe>
      <el-table-column type="selection" width="40" />
      <el-table-column prop="name" label="计划名称" min-width="180" show-overflow-tooltip />
      <el-table-column prop="channel" label="渠道" width="110">
        <template #default="{ row }">{{ channelMap[row.channel] || row.channel }}</template>
      </el-table-column>
      <el-table-column prop="strategyName" label="归属策略" width="180" />
      <el-table-column prop="budgetDaily" label="日预算" width="100">
        <template #default="{ row }">¥{{ row.budgetDaily?.toLocaleString() }}</template>
      </el-table-column>
      <el-table-column label="当前消耗" width="110">
        <template #default="{ row }">¥{{ (row.currentCost || 0).toLocaleString() }}</template>
      </el-table-column>
      <el-table-column label="转化数" width="80" prop="currentConversions" />
      <el-table-column label="CPA" width="100">
        <template #default="{ row }">{{ row.currentCpa ? '¥' + row.currentCpa.toFixed(2) : '--' }}</template>
      </el-table-column>
      <el-table-column label="状态" width="80">
        <template #default="{ row }"><el-tag :type="statusType[row.status]" size="small">{{ statusMap[row.status] }}</el-tag></template>
      </el-table-column>
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="$router.push('/campaign/' + row.id)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- Pagination -->
    <div class="mt-4 flex justify-end">
      <el-pagination v-model:page="page" v-model:size="size" :total="total" layout="total,prev,pager,next" @change="loadData" />
    </div>
  </div>
</template>
```

- [ ] **Step 1:** Create `CampaignList.vue` with filters, batch select, table, pagination
- [ ] **Step 2:** Create `CampaignDetail.vue` with campaign info + hourly stats mini-chart
- [ ] **Step 3:** Verify: /campaign/list shows table, filters work, batch select works

---

### Task 3.g: Audience + Material Pages

**Files:**
- Create: `ad-platform/frontend/src/views/audience/AudienceList.vue`
- Create: `ad-platform/frontend/src/views/material/MaterialList.vue`

**AudienceList.vue:**
```vue
<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getAudiences } from '@/api/audience'
import type { AudienceDTO } from '@/types'

const audiences = ref<AudienceDTO[]>([])
onMounted(async () => { audiences.value = await getAudiences() })
const sourceMap: Record<string, string> = { DMP: 'DMP人群', LOOKALIKE: '相似扩展', RETARGET: '重定向' }
</script>
<template>
  <div>
    <div class="mb-4 flex justify-between items-center">
      <h2 style="margin:0">人群管理</h2>
      <el-button type="primary">新建人群包</el-button>
    </div>
    <el-table :data="audiences" stripe>
      <el-table-column prop="code" label="编码" width="100" />
      <el-table-column prop="name" label="人群包名称" min-width="160" />
      <el-table-column prop="source" label="来源" width="120">
        <template #default="{ row }">{{ sourceMap[row.source] || row.source }}</template>
      </el-table-column>
      <el-table-column prop="sizeEstimate" label="预估规模" width="120">
        <template #default="{ row }">{{ row.sizeEstimate?.toLocaleString() }}</template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }"><el-tag :type="row.status === 0 ? 'success' : 'info'" size="small">{{ row.status === 0 ? '可用' : '暂停' }}</el-tag></template>
      </el-table-column>
    </el-table>
  </div>
</template>
```

- [ ] **Step 1:** Create `AudienceList.vue` — table with 13 audience records by code/name/source/size
- [ ] **Step 2:** Create `MaterialList.vue` — table with CTR/CPA columns, status tags, score progress bar
- [ ] **Step 3:** Verify: /audience shows 13 records, /material shows 12 records

---

### Task 3.h: Rule Engine Page

**Files:**
- Create: `ad-platform/frontend/src/views/rule/RuleEnginePage.vue`
- Create: `ad-platform/frontend/src/views/rule/RuleFormDialog.vue`
- Create: `ad-platform/frontend/src/views/rule/SandboxDialog.vue`

**RuleEnginePage.vue:**
```vue
<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getRules, updateRuleStatus, deleteRule } from '@/api/rule'
import type { RuleDTO } from '@/types'

const rules = ref<RuleDTO[]>([])
const ruleFormVisible = ref(false)
const sandboxVisible = ref(false)
const selectedRuleId = ref<number>()
const editingRule = ref<RuleDTO>()

onMounted(async () => { rules.value = await getRules() })

async function toggleStatus(rule: RuleDTO) {
  await updateRuleStatus(rule.id!, rule.status === 1 ? 0 : 1)
  rule.status = rule.status === 1 ? 0 : 1
}

function openSandbox(id: number) {
  selectedRuleId.value = id
  sandboxVisible.value = true
}

function editRule(rule: RuleDTO) {
  editingRule.value = { ...rule }
  ruleFormVisible.value = true
}

async function handleDelete(rule: RuleDTO) {
  if (rule.isSystem) { ElMessage.warning('系统内置规则不可删除'); return }
  await deleteRule(rule.id!)
  rules.value = rules.value.filter(r => r.id !== rule.id)
}
</script>
<template>
  <div>
    <div class="mb-4 flex justify-between items-center">
      <h2 style="margin:0">自动化规则引擎</h2>
      <el-button type="primary" @click="editingRule = undefined; ruleFormVisible = true">新建规则</el-button>
    </div>
    <el-alert title="系统内置规则（灰锁）不可删除或禁用，仅可调整阈值" type="warning" :closable="false" class="mb-4" />
    <el-table :data="rules" stripe>
      <el-table-column prop="name" label="规则名称" min-width="160" />
      <el-table-column label="触发条件" min-width="200">
        <template #default="{ row }">{{ row.triggerMetric }} {{ row.triggerOperator }} {{ row.triggerThreshold }} ({{ row.triggerWindowHours }}h)</template>
      </el-table-column>
      <el-table-column prop="actionType" label="执行动作" width="140" />
      <el-table-column prop="priority" label="优先级" width="70" />
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-switch :model-value="row.status === 1" :disabled="row.isSystem === 1" @change="toggleStatus(row)" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="editRule(row)">编辑</el-button>
          <el-button size="small" @click="openSandbox(row.id!)">沙箱测试</el-button>
          <el-button size="small" type="danger" :disabled="row.isSystem === 1" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <RuleFormDialog v-model:visible="ruleFormVisible" :rule="editingRule" @saved="onMounted" />
    <SandboxDialog v-model:visible="sandboxVisible" :rule-id="selectedRuleId" />
  </div>
</template>
```

- [ ] **Step 1:** Create `RuleEnginePage.vue` — rule table with switch toggle, edit/sandbox/delete buttons
- [ ] **Step 2:** Create `RuleFormDialog.vue` — form with trigger metric/operator/threshold/window dropdowns, action type selector, scope picker, priority input, cooldown input
- [ ] **Step 3:** Create `SandboxDialog.vue` — date range picker + simulate button + result display (trigger count, affected campaigns, estimated budget saved, trigger log list)
- [ ] **Step 4:** Verify: /rule-engine shows 2 rules (system + CPA), try sandbox test

---

### Task 3.i: Settings Page + Nginx Config

**Files:**
- Create: `ad-platform/frontend/src/views/settings/SettingsPage.vue`
- Create: `ad-platform/nginx.conf`

**SettingsPage.vue:**
- Budget config form (total budget display, per-strategy budget adjustment)
- Channel account config (placeholder for future integration)
- System info display

**nginx.conf:**
```nginx
server {
    listen 80;
    server_name localhost;

    # Frontend static files
    location / {
        root /app/frontend/dist;
        index index.html;
        try_files $uri $uri/ /index.html;
    }

    # API reverse proxy
    location /api/ {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
}
```

- [ ] **Step 1:** Create `SettingsPage.vue` with budget summary card + strategy budget list
- [ ] **Step 2:** Create `nginx.conf` for production deployment
- [ ] **Step 3:** Verify production build: `cd frontend && npm run build` generates dist/

---

### Self-Review Checklist

After writing all files, verify against the spec:

- [ ] **All 10 database tables exist** (ad_strategy, ad_strategy_channel, ad_audience, ad_material, ad_strategy_audience, ad_strategy_material, ad_campaign, ad_rule, ad_rule_execution_log, ad_stats_hourly)
- [ ] **All 7 seed strategies** (S1-S7) with correct budget, CPA, channel allocations
- [ ] **13 audiences** (AUD001-AUD013) and **12 materials** (C001-C012) seeded
- [ ] **System rule** (test campaign detection) with is_system=1 exists in seed data
- [ ] **All 35+ API endpoints** from spec §5 are implemented
- [ ] **Dashboard Redis caching** with per-day key pattern is implemented
- [ ] **Frontend has all 7 routes** (/dashboard, /strategy, /campaign, /audience, /material, /rule-engine, /settings)
- [ ] **Sandbox test** can simulate rule execution against historical data
- [ ] **Budget total = 800,000** summed from all strategies (12+10+8+8+12+16+14=80万)
- [ ] **No placeholder code** — all implementations are complete
