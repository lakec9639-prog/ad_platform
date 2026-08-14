<template>
  <div class="campaign-detail">
    <div class="page-header">
      <div class="header-left">
        <el-button @click="goBack">
          <el-icon><ArrowLeft /></el-icon>
          返回
        </el-button>
        <h2>广告组详情</h2>
      </div>
      <div class="header-actions">
        <el-button
          :type="campaign?.status === 1 ? 'warning' : 'success'"
          size="small"
          :disabled="!campaign || campaign.status === 0 || campaign.status === 3"
          @click="toggleStatus"
        >
          {{ campaign?.status === 1 ? '暂停投放' : '恢复投放' }}
        </el-button>
        <el-button type="primary" size="small" @click="handleEdit">
          编辑
        </el-button>
        <el-button size="small" @click="handleClone" :disabled="!campaign">
          克隆
        </el-button>
      </div>
    </div>

    <div v-if="loading" class="loading-state">
      <el-skeleton :rows="5" animated />
    </div>

    <template v-else-if="campaign">
      <el-card shadow="hover" class="info-card">
        <template #header>
          <div class="card-header">
            <span>广告组信息</span>
            <el-tag :type="statusType" size="small">{{ statusLabel }}</el-tag>
          </div>
        </template>
        <el-descriptions :column="3" border>
          <el-descriptions-item label="名称" :span="3">
            {{ campaign.name }}
          </el-descriptions-item>
          <el-descriptions-item label="所属策略">
            {{ campaign.strategyName || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="渠道">
            <el-tag size="small" effect="plain">{{ channelLabel }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="出价类型">
            {{ campaign.bidType || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="日预算">
            ¥{{ (campaign.budgetDaily || 0).toLocaleString() }}
          </el-descriptions-item>
          <el-descriptions-item label="出价">
            ¥{{ (campaign.bidPrice || 0).toFixed(2) }}
          </el-descriptions-item>
          <el-descriptions-item label="平台ID">
            {{ campaign.platformCampaignId || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="投放开始">
            {{ campaign.launchAt || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="投放结束">
            {{ campaign.stopAt || '-' }}
          </el-descriptions-item>
        </el-descriptions>
      </el-card>

      <el-card shadow="hover" class="stats-card">
        <template #header>
          <span>数据概览（近30天）</span>
        </template>
        <el-row :gutter="16">
          <el-col :span="6">
            <div class="stat-item">
              <div class="stat-label">消耗</div>
              <div class="stat-value">¥{{ (campaign.currentCost || 0).toLocaleString() }}</div>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="stat-item">
              <div class="stat-label">转化数</div>
              <div class="stat-value">{{ campaign.currentConversions || 0 }}</div>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="stat-item">
              <div class="stat-label">CPA</div>
              <div class="stat-value" :class="cpaClass">
                ¥{{ computedCPA }}
              </div>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="stat-item">
              <div class="stat-label">ROAS</div>
              <div class="stat-value" :class="roasClass">
                {{ computedROAS }}
              </div>
            </div>
          </el-col>
        </el-row>
      </el-card>

      <el-card shadow="hover" class="chart-card">
        <template #header>
          <span>消耗趋势</span>
        </template>
        <div ref="chartRef" style="width: 100%; height: 300px"></div>
      </el-card>
    </template>

    <CampaignFormDialog
      v-model:visible="dialogVisible"
      :campaign="editingCampaign"
      @saved="handleSaved"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { getCampaign, updateCampaignStatus, createCampaign } from '@/api/campaign'
import { getTrends } from '@/api/dashboard'
import type { CampaignDTO } from '@/types'
import type { TrendItem } from '@/api/dashboard'
import CampaignFormDialog from './CampaignFormDialog.vue'

const route = useRoute()
const router = useRouter()

const campaign = ref<CampaignDTO | null>(null)
const loading = ref(false)
const trendData = ref<TrendItem[]>([])
const dialogVisible = ref(false)
const editingCampaign = ref<any>(null)
const chartRef = ref<HTMLElement>()
let chartInstance: echarts.ECharts | null = null

const channelNameMap: Record<string, string> = {
  DOUYIN: '巨量引擎', XIAOHONGSHU: '小红书', BILIBILI: 'B站',
  TENCENT: '腾讯广告', BAIDU_FEED: '百度信息流', BAIDU_SEARCH: '百度搜索',
}

const channelLabel = computed(() => {
  const ch = campaign.value?.channel
  return ch ? channelNameMap[ch] || ch : '-'
})

const statusType = computed(() => {
  switch (campaign.value?.status) {
    case 0: return 'info'
    case 1: return 'success'
    case 2: return 'warning'
    case 3: return 'danger'
    default: return 'info'
  }
})

const statusLabel = computed(() => {
  switch (campaign.value?.status) {
    case 0: return '搭建中'
    case 1: return '投放中'
    case 2: return '已暂停'
    case 3: return '已停止'
    default: return '未知'
  }
})

const computedCPA = computed(() => {
  const conv = campaign.value?.currentConversions || 0
  const cost = campaign.value?.currentCost || 0
  if (conv <= 0) return '-'
  return (cost / conv).toFixed(2)
})

const computedROAS = computed(() => {
  const conv = campaign.value?.currentConversions || 0
  const cost = campaign.value?.currentCost || 0
  if (cost <= 0) return '-'
  return (conv / cost).toFixed(2)
})

const cpaClass = computed(() => {
  const cpa = parseFloat(computedCPA.value)
  if (isNaN(cpa)) return ''
  if (cpa > 500) return 'text-danger'
  if (cpa > 250) return 'text-warning'
  return 'text-success'
})

const roasClass = computed(() => {
  const roas = parseFloat(computedROAS.value)
  if (isNaN(roas)) return ''
  if (roas < 0.5) return 'text-danger'
  if (roas < 1) return 'text-warning'
  return 'text-success'
})

function renderChart() {
  if (!chartRef.value || trendData.value.length === 0) return
  if (chartInstance) chartInstance.dispose()
  chartInstance = echarts.init(chartRef.value)

  const dates = trendData.value.map((d) => {
    const parts = d.date.split('-')
    return parts.length >= 3 ? `${parts[1]}-${parts[2]}` : d.date
  })
  const costs = trendData.value.map((d) => d.cost)
  const conversions = trendData.value.map((d) => d.conversions || 1)
  const cpas = trendData.value.map((d, i) =>
    d.conversions > 0 ? Math.round((d.cost / d.conversions) * 100) / 100 : 0
  )

  chartInstance.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'cross' } },
    legend: { data: ['消耗', 'CPA'], top: 0 },
    grid: { left: '3%', right: '4%', bottom: '3%', top: '40px', containLabel: true },
    xAxis: { type: 'category', data: dates, boundaryGap: true, axisLabel: { fontSize: 11 } },
    yAxis: [
      { type: 'value', name: '消耗 (¥)', nameTextStyle: { fontSize: 12 },
        axisLabel: { formatter: (v: number) => v >= 10000 ? `${(v / 10000).toFixed(1)}万` : `${v}` } },
      { type: 'value', name: 'CPA (¥)', nameTextStyle: { fontSize: 12 },
        axisLabel: { formatter: (v: number) => `${v.toFixed(0)}` } },
    ],
    series: [
      { name: '消耗', type: 'bar', data: costs,
        itemStyle: { color: '#409eff', borderRadius: [2, 2, 0, 0] }, barMaxWidth: 24 },
      { name: 'CPA', type: 'line', yAxisIndex: 1, data: cpas, smooth: true,
        symbol: 'circle', symbolSize: 6,
        lineStyle: { color: '#e6a23c', width: 2 }, itemStyle: { color: '#e6a23c' } },
    ],
  })
}

async function loadDetail() {
  const id = Number(route.params.id)
  if (isNaN(id)) {
    ElMessage.error('无效的广告组ID')
    return
  }
  loading.value = true
  try {
    const [camp, rawTrends] = await Promise.all([
      getCampaign(id),
      getTrends({ campaignId: id }).catch(() => []),
    ])
    campaign.value = camp
    trendData.value = (rawTrends as any[]).map((t: any) => ({
      date: typeof t.statDate === 'string' ? t.statDate : '',
      impressions: t.impressions || 0,
      clicks: t.clicks || 0,
      cost: t.cost || 0,
      conversions: t.conversions || 0,
    }))
    await nextTick()
    renderChart()
  } catch (err) {
    console.error('Failed to load campaign detail:', err)
    ElMessage.error('加载广告组详情失败')
  } finally {
    loading.value = false
  }
}

async function toggleStatus() {
  if (!campaign.value) return
  const newStatus = campaign.value.status === 1 ? 2 : 1
  const label = newStatus === 2 ? '暂停' : '启用'
  try {
    await ElMessageBox.confirm(
      `确定${label}广告组「${campaign.value.name}」吗？`,
      '确认操作',
      { confirmButtonText: label, cancelButtonText: '取消', type: 'info' }
    )
    await updateCampaignStatus(campaign.value.id!, newStatus)
    ElMessage.success(`已${label}`)
    await loadDetail()
  } catch { /* cancelled */ }
}

function handleEdit() {
  editingCampaign.value = campaign.value ? { ...campaign.value } : null
  dialogVisible.value = true
}

async function handleClone() {
  if (!campaign.value) return
  try {
    const cloneData = {
      strategyId: campaign.value.strategyId,
      name: campaign.value.name + '_复制',
      channel: campaign.value.channel,
      budgetDaily: campaign.value.budgetDaily,
      bidType: campaign.value.bidType,
      bidPrice: campaign.value.bidPrice,
      platformCampaignId: campaign.value.platformCampaignId
        ? campaign.value.platformCampaignId + '_COPY'
        : undefined,
      launchAt: undefined,
      stopAt: undefined,
    }
    const newId = await createCampaign(cloneData)
    ElMessage.success('克隆成功，已创建新广告组')
    router.push(`/campaign/${newId}`)
  } catch (err: any) {
    ElMessage.error(err?.message || '克隆失败')
  }
}

function handleSaved() {
  loadDetail()
}

function goBack() {
  router.push('/campaign/list')
}

onMounted(loadDetail)

onBeforeUnmount(() => {
  chartInstance?.dispose()
})
</script>

<style scoped>
.campaign-detail { padding: 0; }
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
  flex-wrap: wrap;
  gap: 12px;
}
.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.header-left h2 { margin: 0; font-size: 22px; font-weight: 600; color: #303133; }
.header-actions { display: flex; gap: 8px; }
.loading-state { padding: 40px; }
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.info-card, .stats-card { margin-bottom: 16px; }
.stats-card .stat-item {
  text-align: center;
  padding: 12px 0;
}
.stat-label {
  font-size: 13px;
  color: #909399;
  margin-bottom: 8px;
}
.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: #303133;
}
.text-success { color: #67c23a !important; }
.text-warning { color: #e6a23c !important; }
.text-danger { color: #f56c6c !important; }
.chart-card { margin-bottom: 20px; }
</style>
