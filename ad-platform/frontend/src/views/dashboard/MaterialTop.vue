<template>
  <div class="material-top">
    <div class="material-top-title">素材消耗排行 TOP5</div>
    <el-table :data="data" stripe style="width: 100%" size="small">
      <el-table-column type="index" label="排名" width="60" />
      <el-table-column label="素材名称" min-width="160" show-overflow-tooltip>
        <template #default="{ row }">
          {{ row.name || '未知' }}
        </template>
      </el-table-column>
      <el-table-column prop="cost" label="消耗 (¥)" sortable>
        <template #default="{ row }">
          {{ row.cost.toLocaleString() }}
        </template>
      </el-table-column>
      <el-table-column prop="conversions" label="转化数" sortable />
      <el-table-column prop="cpa" label="CPA (¥)" sortable>
        <template #default="{ row }">
          {{ row.cpa.toFixed(2) }}
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
export interface MaterialTopDataItem {
  id: number
  name?: string
  cost: number
  conversions: number
  cpa: number
}

defineProps<{
  data: MaterialTopDataItem[]
}>()
</script>

<style scoped>
.material-top-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid #ebeef5;
}
</style>
