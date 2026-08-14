<template>
  <div ref="chartRef" style="width: 100%; height: 300px"></div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, watch } from 'vue'
import * as echarts from 'echarts'

export interface ChannelPieDataItem {
  channel: string
  cost: number
}

const props = defineProps<{
  data: ChannelPieDataItem[]
}>()

const chartRef = ref<HTMLElement>()
let chartInstance: echarts.ECharts | null = null

const channelNameMap: Record<string, string> = {
  DOUYIN: '巨量引擎',
  XIAOHONGSHU: '小红书',
  BILIBILI: 'B站',
  TENCENT: '腾讯广告',
  BAIDU_FEED: '百度信息流',
  BAIDU_SEARCH: '百度搜索',
}

function getChannelLabel(channel: string): string {
  return channelNameMap[channel] || channel
}

function renderChart() {
  if (!chartInstance) return
  const total = props.data.reduce((sum, d) => sum + d.cost, 0)
  const pieData = props.data.map((d) => ({
    name: getChannelLabel(d.channel),
    value: Math.round(d.cost * 100) / 100,
  }))

  chartInstance.setOption({
    tooltip: {
      trigger: 'item',
      formatter: (params: any) => {
        const pct = total > 0 ? ((params.value / total) * 100).toFixed(1) : '0.0'
        return `${params.name}<br/>消耗: ¥${params.value.toLocaleString()}<br/>占比: ${pct}%`
      },
    },
    series: [
      {
        type: 'pie',
        radius: ['40%', '70%'],
        center: ['50%', '50%'],
        avoidLabelOverlap: true,
        padAngle: 2,
        itemStyle: {
          borderRadius: 4,
        },
        label: {
          show: true,
          formatter: '{b}\n{d}%',
          fontSize: 12,
        },
        labelLine: {
          show: true,
        },
        emphasis: {
          label: {
            show: true,
            fontSize: 14,
            fontWeight: 'bold',
          },
        },
        data: pieData,
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
