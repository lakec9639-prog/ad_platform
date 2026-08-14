<template>
  <div class="material-list">
    <div class="page-header">
      <h2>素材管理</h2>
      <el-button type="primary" @click="router.push('/material/analysis')">
        <el-icon><TrendCharts /></el-icon>
        素材衰减分析
      </el-button>
    </div>

    <el-card shadow="hover">
      <el-table :data="materials" stripe style="width: 100%">
        <el-table-column prop="code" label="素材编码" width="140">
          <template #default="{ row }">
            {{ (row as any).code || row.id || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="name" label="素材名称" min-width="160" show-overflow-tooltip />
        <el-table-column label="类型" width="100">
          <template #default="{ row }">
            {{ typeLabel(row.type) }}
          </template>
        </el-table-column>
        <el-table-column label="时长" width="80">
          <template #default="{ row }">
            {{ row.duration ? `${row.duration}秒` : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">
              {{ statusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="评分" width="160">
          <template #default="{ row }">
            <el-progress
              :percentage="(row as any).score || 0"
              :status="scoreStatus((row as any).score)"
              :stroke-width="12"
              style="width: 120px"
            />
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="180">
          <template #default="{ row }">
            {{ row.createdAt || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="router.push('/material/analysis')">
              衰减分析
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper" v-if="total > 0">
        <el-pagination
          v-model:current-page="currentPage"
          :page-size="pageSize"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="loadMaterials"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { TrendCharts } from '@element-plus/icons-vue'
import { getMaterials } from '@/api/material'
import type { MaterialDTO } from '@/types'

const router = useRouter()
const materials = ref<MaterialDTO[]>([])
const currentPage = ref(1)
const pageSize = 20
const total = ref(0)

function typeLabel(type?: string): string {
  switch (type) {
    case 'video': return '视频'
    case 'image': return '图片'
    case 'image_text': return '图文'
    default: return type || '-'
  }
}

function statusType(status?: number): 'info' | 'success' | 'warning' | 'danger' {
  switch (status) {
    case 0: return 'info'    // 待审核
    case 1: return 'success' // 可用
    case 2: return 'warning' // 衰减
    case 3: return 'danger'  // 停用
    default: return 'info'
  }
}

function statusLabel(status?: number): string {
  switch (status) {
    case 0: return '待审核'
    case 1: return '可用'
    case 2: return '衰减'
    case 3: return '停用'
    default: return '未知'
  }
}

function scoreStatus(score?: number): 'success' | 'warning' | 'exception' {
  if (!score) return 'exception'
  if (score >= 80) return 'success'
  if (score >= 60) return 'warning'
  return 'exception'
}

async function loadMaterials() {
  try {
    const result = await getMaterials()
    materials.value = result
  } catch (err) {
    console.error('Failed to load materials:', err)
  }
}

onMounted(loadMaterials)
</script>

<style scoped>
.material-list {
  padding: 0;
}
.page-header {
  margin-bottom: 20px;
}
.page-header h2 {
  margin: 0;
  font-size: 22px;
  font-weight: 600;
  color: #303133;
}
.pagination-wrapper {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
