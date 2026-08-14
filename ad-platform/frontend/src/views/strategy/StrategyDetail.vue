<template>
  <div class="strategy-detail">
    <div class="page-header">
      <el-button @click="goBack">
        <el-icon><ArrowLeft /></el-icon>
        返回
      </el-button>
      <h2>策略详情</h2>
      <el-button type="primary" @click="openEdit" style="margin-left:auto">编辑</el-button>
    </div>

    <div v-if="loading" class="loading-state">
      <el-skeleton :rows="5" animated />
    </div>

    <template v-else-if="strategy">
      <el-card shadow="hover" class="info-card">
        <template #header>
          <span>基本信息</span>
        </template>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="策略名称" :span="2">
            {{ strategy.name }}
          </el-descriptions-item>
          <el-descriptions-item label="策略代码">
            {{ strategy.code || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="statusType" size="small">{{ statusLabel }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="策略描述" :span="2">
            {{ strategy.description || '暂无描述' }}
          </el-descriptions-item>
          <el-descriptions-item label="投放目标">
            {{ strategy.objective || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="预算">
            ¥{{ ((strategy.budget || 0) / 10000).toFixed(0) }}万
          </el-descriptions-item>
          <el-descriptions-item label="目标CPA">
            ¥{{ strategy.targetCpa || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="目标CVR">
            {{ strategy.targetCvr || '-' }}%
          </el-descriptions-item>
          <el-descriptions-item label="预期ROAS">
            {{ strategy.expectedRoas || '-' }}
          </el-descriptions-item>
        </el-descriptions>
      </el-card>

      <el-card shadow="hover" class="channel-card" v-if="channelAllocations.length > 0">
        <template #header>
          <span>渠道分配</span>
        </template>
        <el-table :data="channelAllocations" stripe>
          <el-table-column label="渠道">
            <template #default="{ row }">
              {{ getChannelLabel(row.channel) }}
            </template>
          </el-table-column>
          <el-table-column prop="budgetRatio" label="预算占比 (%)">
            <template #default="{ row }">
              {{ row.budgetRatio }}%
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </template>

    <!-- Edit Dialog -->
    <el-dialog v-model="showEdit" title="编辑策略" width="500px">
      <el-form :model="editForm" label-width="100px">
        <el-form-item label="策略名称">
          <el-input v-model="editForm.name" />
        </el-form-item>
        <el-form-item label="策略描述">
          <el-input
            v-model="editForm.description"
            type="textarea"
            :rows="4"
            placeholder="请输入策略描述"
          />
        </el-form-item>
        <el-form-item label="投放目标">
          <el-input v-model="editForm.objective" />
        </el-form-item>
        <el-form-item label="预算(万)">
          <el-input-number v-model="editForm.budgetWan" :min="0" :step="10" style="width:100%" />
        </el-form-item>
        <el-form-item label="目标CPA">
          <el-input-number v-model="editForm.targetCpa" :min="0" style="width:100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEdit = false">取消</el-button>
        <el-button type="primary" @click="saveEdit" :loading="saving">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getStrategy, updateStrategy } from '@/api/strategy'
import type { StrategyDTO } from '@/types'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()

const strategy = ref<StrategyDTO | null>(null)
const loading = ref(false)
const showEdit = ref(false)
const saving = ref(false)

const channelNameMap: Record<string, string> = {
  DOUYIN: '巨量引擎',
  XIAOHONGSHU: '小红书',
  BILIBILI: 'B站',
  TENCENT: '腾讯广告',
  BAIDU_FEED: '百度信息流',
  BAIDU_SEARCH: '百度搜索',
}

interface ChannelAllocation {
  channel: string
  budgetRatio: number
}

const channelAllocations = ref<ChannelAllocation[]>([])

const editForm = reactive({
  name: '',
  description: '',
  objective: '',
  budgetWan: 0,
  targetCpa: 0,
})

function getChannelLabel(channel: string): string {
  return channelNameMap[channel] || channel
}

const statusType = computed(() => {
  switch (strategy.value?.status) {
    case 0: return 'info'
    case 1: return 'success'
    case 2: return 'warning'
    case 3: return 'danger'
    default: return 'info'
  }
})

const statusLabel = computed(() => {
  switch (strategy.value?.status) {
    case 0: return '草稿'
    case 1: return '启用'
    case 2: return '暂停'
    case 3: return '结束'
    default: return '未知'
  }
})

async function loadDetail() {
  const id = Number(route.params.id)
  if (isNaN(id)) return
  loading.value = true
  try {
    const data = await getStrategy(id)
    strategy.value = data
    const allocations = (data as any).channelAllocations
    if (Array.isArray(allocations)) {
      channelAllocations.value = allocations
    }
  } catch (err) {
    console.error('Failed to load strategy detail:', err)
  } finally {
    loading.value = false
  }
}

function openEdit() {
  if (!strategy.value) return
  editForm.name = strategy.value.name
  editForm.description = strategy.value.description || ''
  editForm.objective = strategy.value.objective || ''
  editForm.budgetWan = ((strategy.value.budget || 0) / 10000)
  editForm.targetCpa = strategy.value.targetCpa || 0
  showEdit.value = true
}

async function saveEdit() {
  if (!strategy.value?.id) return
  saving.value = true
  try {
    await updateStrategy(strategy.value.id, {
      name: editForm.name,
      description: editForm.description,
      objective: editForm.objective,
      budget: editForm.budgetWan * 10000,
      targetCpa: editForm.targetCpa,
    } as any)
    ElMessage.success('保存成功')
    showEdit.value = false
    await loadDetail()
  } catch (err) {
    console.error('Failed to update strategy:', err)
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

function goBack() {
  router.push('/strategy/list')
}

onMounted(loadDetail)
</script>

<style scoped>
.strategy-detail {
  padding: 0;
}
.page-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
}
.page-header h2 {
  margin: 0;
  font-size: 22px;
  font-weight: 600;
  color: #303133;
}
.loading-state {
  padding: 40px;
}
.info-card {
  margin-bottom: 20px;
}
.channel-card {
  margin-bottom: 20px;
}
</style>
