<template>
  <div class="material-analysis">
    <div class="page-header">
      <div class="header-left">
        <h2>素材衰减分析</h2>
        <span class="subtitle">追踪素材CTR/CVR/CPA趋势，预判素材疲劳信号</span>
      </div>
      <el-select v-model="selectedMaterialId" placeholder="选择素材分析" filterable style="width: 300px" @change="loadAnalysis">
        <el-option
          v-for="m in materials"
          :key="m.id"
          :label="`${m.name} (${m.code})`"
          :value="m.id"
        >
          <div class="option-item">
            <span>{{ m.name }}</span>
            <el-tag :type="m.status === 1 ? 'success' : 'info'" size="small" effect="plain">
              {{ statusLabel(m.status) }}
            </el-tag>
          </div>
        </el-option>
      </el-select>
    </div>

    <div v-if="!selectedMaterial" class="empty-state">
      <el-empty description="请选择一个素材查看衰减分析" />
    </div>

    <div v-else-if="loaded && decayData.length === 0" class="empty-state">
      <el-empty description="该素材暂无衰减数据（可能未关联任何投放策略）" />
    </div>

    <template v-else-if="selectedMaterial">
      <el-row :gutter="16" class="info-row">
        <el-col :span="8">
          <el-card shadow="hover">
            <div class="material-header">
              <span class="material-name">{{ selectedMaterial.name }}</span>
              <el-tag :type="materialTypeTag" size="small">{{ materialTypeLabel }}</el-tag>
            </div>
            <div class="material-meta">
              <span>编码: {{ selectedMaterial.code }}</span>
              <span v-if="selectedMaterial.duration"> | 时长: {{ selectedMaterial.duration }}s</span>
            </div>
          </el-card>
        </el-col>
        <el-col :span="4">
          <el-card shadow="hover" :body-style="{ textAlign: 'center', padding: '16px' }">
            <div class="metric-label">综合评分</div>
            <div class="metric-value" :style="{ color: scoreColor }">{{ selectedMaterial.score || '-' }}</div>
          </el-card>
        </el-col>
        <el-col :span="4">
          <el-card shadow="hover" :body-style="{ textAlign: 'center', padding: '16px' }">
            <div class="metric-label">平均CTR</div>
            <div class="metric-value" :style="{ color: avgCtrColor }">{{ avgCTR }}</div>
          </el-card>
        </el-col>
        <el-col :span="4">
          <el-card shadow="hover" :body-style="{ textAlign: 'center', padding: '16px' }">
            <div class="metric-label">平均CVR</div>
            <div class="metric-value" :style="{ color: avgCvrColor }">{{ avgCVR }}</div>
          </el-card>
        </el-col>
        <el-col :span="4">
          <el-card shadow="hover" :body-style="{ textAlign: 'center', padding: '16px' }">
            <div class="metric-label">平均CPA</div>
            <div class="metric-value" :style="{ color: avgCpaColor }">{{ avgCPA }}</div>
          </el-card>
        </el-col>
      </el-row>

      <el-card shadow="hover" class="chart-card">
        <template #header>
          <div class="card-header">
            <span>衰减曲线</span>
            <div class="card-header-right">
              <div class="legend-tags">
                <el-tag size="small" color="#409eff">CTR (%)</el-tag>
                <el-tag size="small" color="#67c23a">CVR (%)</el-tag>
                <el-tag size="small" color="#e6a23c">CPA (¥)</el-tag>
              </div>
              <el-button size="small" type="primary" plain :loading="aiLoading" @click="fetchAIAdvice">
                AI 策略推荐
              </el-button>
            </div>
          </div>
        </template>
        <div v-if="aiAdvice" class="ai-advice-banner">
          <span class="ai-badge">AI</span>
          <span class="ai-text">{{ aiAdvice }}</span>
        </div>
        <div v-else-if="aiLoading" class="ai-advice-banner ai-loading">
          策略分析中...
        </div>
        <div ref="decayChartRef" style="width: 100%; height: 400px"></div>
      </el-card>

    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick, onBeforeUnmount } from 'vue'
import * as echarts from 'echarts'
import { getMaterials } from '@/api/material'
import type { MaterialDTO } from '@/types'

interface DecayPoint {
  statDate: string
  impressions: number
  clicks: number
  conversions: number
  cost: number
  ctr: number
  cvr: number
  cpa: number
}

const materials = ref<MaterialDTO[]>([])
const selectedMaterialId = ref<number | undefined>(undefined)
const selectedMaterial = computed(() =>
  materials.value.find((m) => m.id === selectedMaterialId.value)
)
const decayData = ref<DecayPoint[]>([])
const loaded = ref(false)
const decayChartRef = ref<HTMLElement>()
let chartInstance: echarts.ECharts | null = null

function statusLabel(status?: number): string {
  switch (status) {
    case 0: return '审核中'
    case 1: return '生效中'
    case 2: return '衰退中'
    case 3: return '已停止'
    default: return '未知'
  }
}

const materialTypeTag = computed(() => {
  const t = selectedMaterial.value?.type
  switch (t) {
    case 'video': return 'success'
    case 'image': return 'warning'
    case 'image_text': return 'info'
    default: return 'info'
  }
})

const materialTypeLabel = computed(() => {
  const t = selectedMaterial.value?.type
  switch (t) {
    case 'video': return '视频'
    case 'image': return '图片'
    case 'image_text': return '图文'
    default: return t || '-'
  }
})

const scoreColor = computed(() => {
  const s = selectedMaterial.value?.score || 0
  if (s >= 85) return '#67c23a'
  if (s >= 70) return '#409eff'
  if (s >= 60) return '#e6a23c'
  return '#f56c6c'
})

const avgCTR = computed(() => {
  if (decayData.value.length === 0) return '-'
  const avg = decayData.value.reduce((s, d) => s + d.ctr, 0) / decayData.value.length
  return avg.toFixed(2) + '%'
})

const avgCVR = computed(() => {
  if (decayData.value.length === 0) return '-'
  const avg = decayData.value.reduce((s, d) => s + d.cvr, 0) / decayData.value.length
  return avg.toFixed(2) + '%'
})

const avgCPA = computed(() => {
  if (decayData.value.length === 0) return '-'
  const avg = decayData.value.reduce((s, d) => s + d.cpa, 0) / decayData.value.length
  return '¥' + avg.toFixed(2)
})

const avgCtrColor = computed(() => {
  const v = parseFloat(avgCTR.value)
  if (isNaN(v)) return '#909399'
  if (v >= 3) return '#67c23a'
  if (v >= 1.5) return '#409eff'
  return '#e6a23c'
})

const avgCvrColor = computed(() => {
  const v = parseFloat(avgCVR.value)
  if (isNaN(v)) return '#909399'
  if (v >= 1) return '#67c23a'
  if (v >= 0.5) return '#409eff'
  return '#e6a23c'
})

const avgCpaColor = computed(() => {
  const v = parseFloat(avgCPA.value.replace('¥', ''))
  if (isNaN(v)) return '#909399'
  if (v <= 200) return '#67c23a'
  if (v <= 400) return '#e6a23c'
  return '#f56c6c'
})

const aiAdvice = ref('')
const aiLoading = ref(false)

async function fetchAIAdvice() {
  if (decayData.value.length === 0) return
  aiLoading.value = true
  try {
    const trend = decayData.value.map(d => ({
      statDate: d.statDate,
      ctr: d.ctr,
      cvr: d.cvr,
      cpa: d.cpa,
    }))

    const systemPrompt = `你是一个程序化广告投放策略分析师。根据素材的CTR(点击率)、CVR(转化率)、CPA(获客成本)趋势，推荐最应该调整预算的策略。

现有6个策略及特点：
S1=高价值人群精准转化，ROI导向，适合CTR和CVR都好的素材
S2=新品破圈拉新，拉新导向，适合CTR好但CVR低时拓展新用户
S3=竞品截流抢夺，截流导向，适合CTR高但CVR低的素材
S4=弃单重定向强转化，召回导向，适合CPA飙升时加强老客召回
S5=智能通投探索，探索导向，适合稳定期补充预算消耗
S6=兜底保量，填充导向，适合其他策略消耗完后自动补量

分析规则：
- CTR上升且CVR稳定 → 素材吸引力强，加大S1/S3预算
- CTR高但CVR下降 → 点击多转化少，加大S2/S3拉新截流
- CTR下降 → 素材疲劳，减少当前渠道，加大S2拓新或S5探索
- CVR上升CPA下降 → 转化效率好，加大S1/S4最大化ROI
- CPA飙升 → 成本失控，加大S4重定向降本，减S5/S6探索
- 所有指标平稳 → 保持S5维持消耗

输出规则：
1. 必须≤15个汉字
2. 格式如：CTR太高,加大S3力度
3. 可组合如：CTR降CVR升,加S1减S5
4. 只输出结果，不加解释`

    const recent = trend.slice(-14)
    const userPrompt = `该素材最近${recent.length}天CTR/CVR/CPA数据：

日期\tCTR(%)\tCVR(%)\tCPA(¥)
${recent.map(p => `${p.statDate}\t${p.ctr.toFixed(2)}\t${p.cvr.toFixed(2)}\t${p.cpa.toFixed(2)}`).join('\n')}

请输出投放建议：`

    const apiKey = import.meta.env.VITE_AI_API_KEY
    const baseUrl = import.meta.env.VITE_AI_BASE_URL || 'https://api.deepseek.com'
    const model = import.meta.env.VITE_AI_MODEL || 'deepseek-chat'

    console.log('[AI] calling API at:', baseUrl + '/v1/chat/completions')

    const res = await fetch(baseUrl + '/v1/chat/completions', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': 'Bearer ' + apiKey,
      },
      body: JSON.stringify({
        model: model,
        max_tokens: 50,
        temperature: 0.1,
        messages: [
          { role: 'system', content: systemPrompt },
          { role: 'user', content: userPrompt },
        ],
      }),
    })

    const json = await res.json()
    console.log('[AI] response:', json)

    if (json.choices && json.choices.length > 0) {
      aiAdvice.value = json.choices[0].message.content || ''
    } else {
      aiAdvice.value = ''
    }
  } catch (e) {
    console.error('[AI] error:', e)
    aiAdvice.value = ''
  } finally {
    aiLoading.value = false
  }
}

function renderChart() {
  if (!decayChartRef.value || decayData.value.length === 0) return
  if (chartInstance) chartInstance.dispose()
  chartInstance = echarts.init(decayChartRef.value)

  const dates = decayData.value.map((d) => {
    const parts = d.statDate.split('-')
    return parts.length >= 3 ? `${parts[1]}-${parts[2]}` : d.statDate
  })
  const ctrData = decayData.value.map((d) => Number(d.ctr.toFixed(2)))
  const cvrData = decayData.value.map((d) => Number(d.cvr.toFixed(2)))
  const cpaData = decayData.value.map((d) => Number(d.cpa.toFixed(2)))

  chartInstance.setOption({
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'cross' },
      formatter: (params: any[]) => {
        let html = `<div style="font-weight:bold;margin-bottom:6px">${params[0].axisValue}</div>`
        params.forEach((p: any) => {
          html += `<div style="display:flex;justify-content:space-between;gap:20px">
            <span>${p.marker} ${p.seriesName}</span>
            <strong>${p.value}${p.seriesName === 'CPA (¥)' ? '' : '%'}</strong>
          </div>`
        })
        return html
      },
    },
    legend: { data: ['CTR (%)', 'CVR (%)', 'CPA (¥)'], top: 0 },
    grid: { left: '3%', right: '4%', bottom: '3%', top: '40px', containLabel: true },
    xAxis: { type: 'category', data: dates, boundaryGap: false, axisLabel: { fontSize: 11 } },
    yAxis: [
      {
        type: 'value', name: 'CTR / CVR (%)',
        axisLabel: { formatter: '{value}%' },
        splitLine: { lineStyle: { type: 'dashed', opacity: 0.3 } },
      },
      {
        type: 'value', name: 'CPA (¥)',
        axisLabel: { formatter: '¥{value}' },
        splitLine: { show: false },
      },
    ],
    series: [
      {
        name: 'CTR (%)', type: 'line', smooth: true, symbol: 'circle', symbolSize: 6,
        data: ctrData,
        lineStyle: { color: '#409eff', width: 2 },
        itemStyle: { color: '#409eff' },
        areaStyle: { color: 'rgba(64,158,255,0.08)' },
      },
      {
        name: 'CVR (%)', type: 'line', smooth: true, symbol: 'diamond', symbolSize: 6,
        data: cvrData,
        lineStyle: { color: '#67c23a', width: 2 },
        itemStyle: { color: '#67c23a' },
        areaStyle: { color: 'rgba(103,194,58,0.08)' },
      },
      {
        name: 'CPA (¥)', type: 'line', smooth: true, symbol: 'triangle', symbolSize: 6,
        yAxisIndex: 1,
        data: cpaData,
        lineStyle: { color: '#e6a23c', width: 2 },
        itemStyle: { color: '#e6a23c' },
        areaStyle: { color: 'rgba(230,162,60,0.08)' },
      },
    ],
  })
}

async function loadMaterials() {
  try {
    materials.value = await getMaterials()
    // Pick the first active material with strategy links (not the pending one)
    const active = materials.value.find((m) => m.status === 1)
    if (active?.id) {
      selectedMaterialId.value = active.id
      await loadAnalysis()
    } else if (materials.value.length > 0) {
      selectedMaterialId.value = materials.value[0].id
      await loadAnalysis()
    }
  } catch { /* ignore */ }
}

async function loadAnalysis() {
  if (!selectedMaterialId.value) return

  const id = selectedMaterialId.value
  const endDate = new Date().toISOString().split('T')[0]
  const startDate = new Date(Date.now() - 30 * 86400000).toISOString().split('T')[0]

  loaded.value = false
  try {
    const res = await fetch(`/api/v1/materials/${id}/decay?startDate=${startDate}&endDate=${endDate}`)
    const json = await res.json()
    if (json.code === 0 && json.data) {
      decayData.value = json.data as DecayPoint[]
    } else {
      decayData.value = []
    }
  } catch {
    decayData.value = []
  }

  aiAdvice.value = ''
  loaded.value = true
  await nextTick()
  renderChart()
}

onMounted(loadMaterials)

onBeforeUnmount(() => {
  chartInstance?.dispose()
})
</script>

<style scoped>
.material-analysis { padding: 0; }
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
  flex-wrap: wrap;
  gap: 12px;
}
.header-left h2 { margin: 0 0 4px 0; font-size: 22px; font-weight: 600; color: #303133; }
.subtitle { font-size: 13px; color: #909399; }
.info-row { margin-bottom: 16px; }
.material-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}
.material-name { font-weight: 600; font-size: 16px; }
.material-meta { font-size: 13px; color: #909399; }
.metric-label { font-size: 12px; color: #909399; margin-bottom: 4px; }
.metric-value { font-size: 22px; font-weight: 700; }
.chart-card { margin-bottom: 16px; }
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.card-header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}
.legend-tags { display: flex; gap: 6px; }
.option-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
}
.empty-state { padding: 60px 0; }
.ai-advice-banner { text-align: center; padding: 8px 0 4px; }
.ai-advice-banner.ai-loading { font-size: 13px; color: #909399; }
.ai-badge {
  display: inline-block;
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: #fff;
  font-size: 11px;
  font-weight: 700;
  padding: 2px 7px;
  border-radius: 4px;
  margin-right: 8px;
  vertical-align: middle;
}
.ai-text { font-size: 16px; font-weight: 600; color: #303133; vertical-align: middle; }
</style>
