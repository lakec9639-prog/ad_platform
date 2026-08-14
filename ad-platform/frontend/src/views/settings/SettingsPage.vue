<template>
  <div class="settings-page">
    <div class="page-header">
      <h2>系统设置</h2>
    </div>

    <el-card shadow="hover" class="settings-section">
      <template #header>
        <span>预算总览</span>
      </template>
      <div class="budget-overview">
        <div class="budget-item">
          <span class="budget-item-label">总预算</span>
          <span class="budget-item-value">¥800,000</span>
        </div>
        <div class="budget-item">
          <span class="budget-item-label">已消耗</span>
          <span class="budget-item-value consumed">¥{{ spent.toLocaleString() }}</span>
        </div>
        <div class="budget-item">
          <span class="budget-item-label">剩余</span>
          <span class="budget-item-value remaining">¥{{ remaining.toLocaleString() }}</span>
        </div>
      </div>
      <el-progress
        :percentage="progressPercentage"
        :status="progressPercentage >= 90 ? 'exception' : progressPercentage >= 70 ? 'warning' : 'success'"
        :stroke-width="16"
      />
    </el-card>

    <el-card shadow="hover" class="settings-section">
      <template #header>
        <span>策略预算分配</span>
      </template>
      <el-table :data="strategyBudgets" stripe style="width: 100%">
        <el-table-column prop="name" label="策略名称" min-width="160" />
        <el-table-column label="分配预算" width="140">
          <template #default="{ row }">
            ¥{{ (row.allocation || 0).toFixed(0) }}万
          </template>
        </el-table-column>
        <el-table-column prop="spent" label="已消耗 (万)" width="140">
          <template #default="{ row }">
            ¥{{ (row.spent || 0).toFixed(0) }}万
          </template>
        </el-table-column>
        <el-table-column prop="remaining" label="剩余 (万)" width="140">
          <template #default="{ row }">
            ¥{{ (row.remaining || 0).toFixed(0) }}万
          </template>
        </el-table-column>
        <el-table-column label="进度" width="160">
          <template #default="{ row }">
            <el-progress
              :percentage="row.allocation > 0 ? Math.min(Math.round((row.spent / row.allocation) * 100), 100) : 0"
              :stroke-width="12"
              style="width: 120px"
            />
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card shadow="hover" class="settings-section">
      <template #header>
        <div class="card-header">
          <span>渠道账号</span>
          <el-button size="small" type="primary" @click="openAdd">添加账号</el-button>
        </div>
      </template>
      <el-table v-loading="loading" :data="accounts" stripe style="width: 100%">
        <el-table-column prop="name" label="账号名称" min-width="140" />
        <el-table-column label="渠道" width="120">
          <template #default="{ row }">
            <el-tag size="small">{{ channelLabel(row.channel) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="appId" label="App ID" min-width="160" />
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openEdit(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card shadow="hover" class="settings-section">
      <template #header>
        <span>系统信息</span>
      </template>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="系统版本">V1.0</el-descriptions-item>
        <el-descriptions-item label="运行环境">{{ environment }}</el-descriptions-item>
        <el-descriptions-item label="框架版本">Vue 3 + Element Plus</el-descriptions-item>
        <el-descriptions-item label="构建工具">Vite</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- Add/Edit Dialog -->
    <el-dialog v-model="showDialog" :title="isEditing ? '编辑渠道账号' : '添加渠道账号'" width="500px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="账号名称">
          <el-input v-model="form.name" placeholder="例如: 巨量引擎-主账户" />
        </el-form-item>
        <el-form-item label="渠道">
          <el-select v-model="form.channel" style="width:100%">
            <el-option label="巨量引擎(抖音)" value="DOUYIN" />
            <el-option label="腾讯广告" value="TENCENT" />
            <el-option label="百度信息流" value="BAIDU_FEED" />
            <el-option label="小红书" value="XIAOHONGSHU" />
            <el-option label="B站" value="BILIBILI" />
          </el-select>
        </el-form-item>
        <el-form-item label="App ID">
          <el-input v-model="form.appId" placeholder="渠道分配的App ID" />
        </el-form-item>
        <el-form-item label="App Secret">
          <el-input v-model="form.appSecret" type="password" show-password placeholder="渠道分配的密钥" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="禁用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" @click="save" :loading="saving">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, reactive, onMounted } from 'vue'
import { getChannelAccounts, createChannelAccount, updateChannelAccount, deleteChannelAccount } from '@/api/channelAccount'
import type { ChannelAccountDTO } from '@/types'
import { ElMessage, ElMessageBox } from 'element-plus'

const CHANNEL_LABELS: Record<string, string> = {
  DOUYIN: '巨量引擎',
  TENCENT: '腾讯广告',
  BAIDU_FEED: '百度信息流',
  XIAOHONGSHU: '小红书',
  BILIBILI: 'B站',
}

const totalBudget = 800000
const spent = ref(350000)
const remaining = computed(() => Math.max(totalBudget - spent.value, 0))
const progressPercentage = computed(() => Math.round((spent.value / totalBudget) * 100))
const environment = ref(import.meta.env.MODE || 'production')
const loading = ref(false)
const accounts = ref<ChannelAccountDTO[]>([])
const showDialog = ref(false)
const saving = ref(false)
const isEditing = ref(false)
const editingId = ref<number | null>(null)

const form = reactive({
  name: '',
  channel: '',
  appId: '',
  appSecret: '',
  status: 1,
})

const strategyBudgets = ref([
  { name: '高价值人群精准转化', allocation: 25, spent: 12, remaining: 13 },
  { name: '新品破圈拉新', allocation: 15, spent: 8, remaining: 7 },
  { name: '竞品截流抢夺', allocation: 20, spent: 15, remaining: 5 },
  { name: '弃单重定向强转化', allocation: 15, spent: 10, remaining: 5 },
  { name: '智能通投探索', allocation: 25, spent: 5, remaining: 20 },
])

function channelLabel(channel: string): string {
  return CHANNEL_LABELS[channel] || channel
}

async function loadAccounts() {
  loading.value = true
  try {
    accounts.value = await getChannelAccounts()
  } catch (err) {
    console.error('Failed to load channel accounts:', err)
  } finally {
    loading.value = false
  }
}

function resetForm() {
  form.name = ''
  form.channel = ''
  form.appId = ''
  form.appSecret = ''
  form.status = 1
  editingId.value = null
  isEditing.value = false
}

function openAdd() {
  resetForm()
  showDialog.value = true
}

function openEdit(row: ChannelAccountDTO) {
  resetForm()
  isEditing.value = true
  editingId.value = row.id!
  form.name = row.name
  form.channel = row.channel
  form.appId = row.appId || ''
  form.appSecret = row.appSecret || ''
  form.status = row.status ?? 1
  showDialog.value = true
}

async function save() {
  if (!form.name || !form.channel) {
    ElMessage.warning('请填写账号名称和渠道')
    return
  }
  saving.value = true
  try {
    if (isEditing.value && editingId.value) {
      await updateChannelAccount(editingId.value, { ...form })
      ElMessage.success('更新成功')
    } else {
      await createChannelAccount({ ...form })
      ElMessage.success('创建成功')
    }
    showDialog.value = false
    await loadAccounts()
  } catch (err) {
    console.error('Failed to save channel account:', err)
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

async function handleDelete(row: ChannelAccountDTO) {
  try {
    await ElMessageBox.confirm(`确认删除渠道账号「${row.name}」？`, '提示', { type: 'warning' })
    await deleteChannelAccount(row.id!)
    ElMessage.success('删除成功')
    await loadAccounts()
  } catch {
    // cancelled
  }
}

onMounted(loadAccounts)
</script>

<style scoped>
.settings-page {
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
.settings-section {
  margin-bottom: 16px;
}
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.budget-overview {
  display: flex;
  gap: 40px;
  margin-bottom: 16px;
}
.budget-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.budget-item-label {
  font-size: 13px;
  color: #909399;
}
.budget-item-value {
  font-size: 24px;
  font-weight: 700;
  color: #303133;
}
.budget-item-value.consumed {
  color: #f56c6c;
}
.budget-item-value.remaining {
  color: #67c23a;
}
</style>
