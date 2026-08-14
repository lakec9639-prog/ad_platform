<template>
  <el-card shadow="hover">
    <div class="stat-card" :class="status ? `stat-${status}` : ''">
      <div class="stat-label">{{ title }}</div>
      <div class="stat-value">{{ prefix }}{{ formattedValue }}</div>
    </div>
  </el-card>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(defineProps<{
  title: string
  value: number
  prefix?: string
  precision?: number
  status?: 'success' | 'warning' | 'danger' | ''
}>(), {
  prefix: '',
  precision: 0,
  status: '',
})

const formattedValue = computed(() => {
  return props.value.toLocaleString(undefined, {
    minimumFractionDigits: props.precision,
    maximumFractionDigits: props.precision,
  })
})
</script>

<style scoped>
.stat-card {
  text-align: center;
  padding: 8px 0;
}
.stat-label {
  font-size: 14px;
  color: #909399;
  margin-bottom: 8px;
}
.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #303133;
}
.stat-success .stat-value {
  color: #67c23a;
}
.stat-warning .stat-value {
  color: #e6a23c;
}
.stat-danger .stat-value {
  color: #f56c6c;
}
</style>
