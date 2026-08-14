<template>
  <div ref="chartRef" style="width: 100%; height: 300px"></div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, watch } from 'vue'
import * as echarts from 'echarts'

export interface TrendDataItem {
  stat_date: string
  cost: number
  conversions: number
}

const props = defineProps<{
  data: TrendDataItem[]
}>()

const chartRef = ref<HTMLElement>()
let chartInstance: echarts.ECharts | null = null

function formatDate(dateStr: string): string {
  const parts = dateStr.split('-')
  if (parts.length >= 3) {
    return `${parts[1]}-${parts[2]}`
  }
  return dateStr
}

function renderChart() {
  if (!chartInstance) return
  const dates = props.data.map((d) => formatDate(d.stat_date))
  const costs = props.data.map((d) => d.cost)
  const cpas = props.data.map((d) =>
    d.conversions > 0 ? Math.round((d.cost / d.conversions) * 100) / 100 : 0,
  )

  chartInstance.setOption({
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'cross' },
    },
    legend: {
      data: ['消耗', 'CPA'],
      top: 0,
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      top: '40px',
      containLabel: true,
    },
    xAxis: {
      type: 'category',
      data: dates,
      boundaryGap: true,
      axisLabel: { fontSize: 11 },
    },
    yAxis: [
      {
        type: 'value',
        name: '消耗 (¥)',
        nameTextStyle: { fontSize: 12 },
        axisLabel: {
          formatter: (v: number) => (v >= 10000 ? `${(v / 10000).toFixed(0)}万` : `${v}`),
        },
      },
      {
        type: 'value',
        name: 'CPA (¥)',
        nameTextStyle: { fontSize: 12 },
        axisLabel: {
          formatter: (v: number) => `${v.toFixed(0)}`,
        },
      },
    ],
    series: [
      {
        name: '消耗',
        type: 'bar',
        data: costs,
        itemStyle: { color: '#409eff', borderRadius: [2, 2, 0, 0] },
        barMaxWidth: 30,
      },
      {
        name: 'CPA',
        type: 'line',
        yAxisIndex: 1,
        data: cpas,
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        lineStyle: { color: '#e6a23c', width: 2 },
        itemStyle: { color: '#e6a23c' },
      },
    ],
  })
}

function initChart() {
  if (!chartRef.value) return
  if (chartInstance) {
    chartInstance.dispose()
  }
  chartInstance = echarts.init(chartRef.value)
  renderChart()
}

watch(
  () => props.data,
  () => renderChart(),
  { deep: true },
)

onMounted(initChart)

onBeforeUnmount(() => {
  chartInstance?.dispose()
})
</script>
