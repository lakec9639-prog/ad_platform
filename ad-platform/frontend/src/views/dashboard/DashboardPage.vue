<template>
  <div class="dashboard-page">
    <div class="dashboard-header">
      <h2 class="dashboard-title">仪表盘</h2>
      <div class="dashboard-actions">
        <el-date-picker
          v-model="dateRangeStr"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
          @change="loadAllData"
        />
        <el-button type="primary" @click="loadAllData">
          <el-icon><Search /></el-icon>
          查询
        </el-button>
        <div class="budget-progress">
          <span class="budget-label">预算进度</span>
          <el-progress
            :percentage="budgetPercentage"
            :status="budgetPercentage >= 90 ? 'exception' : budgetPercentage >= 70 ? 'warning' : 'success'"
            :stroke-width="14"
            style="width: 200px"
          />
          <span class="budget-text">
            ¥{{ overviewData.cost.toLocaleString() }} / ¥800,000
          </span>
        </div>
      </div>
    </div>

    <el-row :gutter="16" class="stat-row">
      <el-col :span="6">
        <StatsCard
          title="总消耗"
          :value="overviewData.cost"
          prefix="¥"
        />
      </el-col>
      <el-col :span="6">
        <StatsCard
          title="新增用户"
          :value="overviewData.newUsers"
        />
      </el-col>
      <el-col :span="6">
        <StatsCard
          title="CPA"
          :value="cpaValue"
          prefix="¥"
          :precision="2"
          :status="cpaStatus"
        />
      </el-col>
      <el-col :span="6">
        <StatsCard
          title="ROAS"
          :value="roasValue"
          :precision="2"
          :status="roasStatus"
        />
      </el-col>
    </el-row>

    <el-row :gutter="16" class="chart-row">
      <el-col :span="16">
        <el-card shadow="hover">
          <template #header>
            <span>消耗趋势</span>
          </template>
          <TrendChart :data="trendChartData" />
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header>
            <span>渠道分布</span>
          </template>
          <ChannelPie :data="channelPieData" />
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="hover" class="top-card" style="margin-top: 16px">
      <MaterialTop :data="materialTopData" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { getOverview, getTrends, getChannelDist, getMaterialTop } from '@/api/dashboard'
import type { OverviewData, TrendItem, ChannelDistItem, MaterialTopItem } from '@/api/dashboard'
import StatsCard from './StatsCard.vue'
import TrendChart from './TrendChart.vue'
import type { TrendDataItem } from './TrendChart.vue'
import ChannelPie from './ChannelPie.vue'
import type { ChannelPieDataItem } from './ChannelPie.vue'
import MaterialTop from './MaterialTop.vue'
import type { MaterialTopDataItem } from './MaterialTop.vue'

const dateRangeStr = ref<string[]>([
  new Date(Date.now() - 30 * 86400000).toISOString().split('T')[0],
  new Date().toISOString().split('T')[0],
])

const overviewData = ref<OverviewData>({
  totalCost: 0, totalNewUsers: 0, totalConversions: 0, totalGmv: 0,
  totalImpressions: 0, totalClicks: 0,
  cpa: 0, roas: 0, ctr: 0, cvr: 0,
  budgetTotal: 0, budgetProgress: 0, budgetRemaining: 0,
  impressions: 0, clicks: 0, cost: 0, conversions: 0, newUsers: 0,
})

const trendData = ref<TrendItem[]>([])
const channelDistData = ref<ChannelDistItem[]>([])
const materialTopRaw = ref<MaterialTopItem[]>([])

const totalBudget = 800000

const budgetPercentage = computed(() => {
  return Math.min(Math.round((overviewData.value.cost / totalBudget) * 100), 100)
})

const cpaValue = computed(() => {
  if (overviewData.value.conversions <= 0) return 0
  return Math.round((overviewData.value.cost / overviewData.value.conversions) * 100) / 100
})

const roasValue = computed(() => {
  if (overviewData.value.cost <= 0) return 0
  return Math.round((overviewData.value.conversions / overviewData.value.cost) * 10000) / 100
})

const cpaStatus = computed(() => {
  if (cpaValue.value > 150) return 'danger'
  if (cpaValue.value > 100) return 'warning'
  return 'success'
})

const roasStatus = computed(() => {
  if (roasValue.value < 0.5) return 'danger'
  if (roasValue.value < 1) return 'warning'
  return 'success'
})

const trendChartData = computed<TrendDataItem[]>(() => {
  return trendData.value.map((item) => ({
    stat_date: item.date,
    cost: item.cost,
    conversions: item.conversions,
  }))
})

const channelPieData = computed<ChannelPieDataItem[]>(() => {
  return channelDistData.value.map((item) => ({
    channel: item.channel,
    cost: item.value,
  }))
})

const materialTopData = computed<MaterialTopDataItem[]>(() => {
  return materialTopRaw.value.map((item) => ({
    id: item.id,
    name: item.name,
    cost: item.cost,
    conversions: item.clicks,
    cpa: item.clicks > 0 ? Math.round((item.cost / item.clicks) * 100) / 100 : 0,
  }))
})

function getDateParams() {
  if (!dateRangeStr.value || dateRangeStr.value.length < 2) return {}
  return {
    startDate: dateRangeStr.value[0],
    endDate: dateRangeStr.value[1],
  }
}

async function loadAllData() {
  const params = getDateParams()
  try {
    const [overview, trends, channelDist, materialTop] = await Promise.all([
      getOverview(params),
      getTrends(params),
      getChannelDist(params),
      getMaterialTop({ ...params, limit: 5 }),
    ])
    overviewData.value.impressions = overview.totalImpressions
    overviewData.value.clicks = overview.totalClicks
    overviewData.value.cost = overview.totalCost
    overviewData.value.conversions = overview.totalConversions
    overviewData.value.ctr = overview.ctr
    overviewData.value.cvr = overview.cvr
    overviewData.value.newUsers = overview.totalNewUsers
    trendData.value = trends.map((t: any) => ({
      date: typeof t.statDate === 'string' ? t.statDate : '',
      impressions: t.impressions || 0,
      clicks: t.clicks || 0,
      cost: t.cost || 0,
      conversions: t.conversions || 0,
    }))
    channelDistData.value = channelDist.map((c: any) => ({
      channel: c.channel || '',
      value: c.cost || 0,
      percentage: 0,
    }))
    materialTopRaw.value = materialTop.map((m: any) => ({
      id: m.material_id || 0,
      name: m.material_name || '',
      impressions: m.impressions || 0,
      clicks: m.clicks || 0,
      cost: m.cost || 0,
    }))
  } catch (err) {
    console.error('Failed to load dashboard data:', err)
  }
}

onMounted(() => {
  loadAllData()
})
</script>

<style scoped>
.dashboard-page {
  padding: 0;
}
.dashboard-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
  flex-wrap: wrap;
  gap: 12px;
}
.dashboard-title {
  margin: 0;
  font-size: 22px;
  font-weight: 600;
  color: #303133;
}
.dashboard-actions {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}
.budget-progress {
  display: flex;
  align-items: center;
  gap: 10px;
}
.budget-label {
  font-size: 13px;
  color: #606266;
  white-space: nowrap;
}
.budget-text {
  font-size: 12px;
  color: #909399;
  white-space: nowrap;
}
.stat-row {
  margin-bottom: 16px;
}
.chart-row {
  margin-bottom: 0;
}
</style>
