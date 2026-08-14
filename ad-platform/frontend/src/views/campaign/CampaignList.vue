<template>
  <div class="campaign-list">
    <div class="page-header">
      <h2>广告组管理</h2>
      <el-button type="primary" @click="handleCreate">
        <el-icon><Plus /></el-icon>
        新建广告组
      </el-button>
    </div>

    <el-card shadow="hover" class="filter-card">
      <el-form :inline="true" :model="filters">
        <el-form-item label="策略">
          <el-select v-model="filters.strategyId" placeholder="选择策略" clearable style="width: 160px">
            <el-option
              v-for="s in strategies"
              :key="s.id"
              :label="s.name"
              :value="s.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="渠道">
          <el-select v-model="filters.channel" placeholder="选择渠道" clearable style="width: 140px">
            <el-option label="巨量引擎" value="DOUYIN" />
            <el-option label="小红书" value="XIAOHONGSHU" />
            <el-option label="B站" value="BILIBILI" />
            <el-option label="腾讯广告" value="TENCENT" />
            <el-option label="百度信息流" value="BAIDU_FEED" />
            <el-option label="百度搜索" value="BAIDU_SEARCH" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filters.status" placeholder="选择状态" clearable style="width: 120px">
            <el-option label="搭建中" :value="0" />
            <el-option label="投放中" :value="1" />
            <el-option label="已暂停" :value="2" />
            <el-option label="已停止" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input
            v-model="filters.keyword"
            placeholder="名称搜索"
            style="width: 160px"
            clearable
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>
            查询
          </el-button>
          <el-button @click="resetFilters">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="hover" class="table-card">
      <template #header>
        <div class="card-header">
          <span>广告组列表</span>
          <el-button size="small" type="primary" plain :loading="aiLoading" @click="fetchAIAdvice">
            AI 投放建议
          </el-button>
        </div>
      </template>
      <div v-if="aiAdvice" class="ai-advice-banner">
        <span class="ai-badge">AI</span>
        <span class="ai-text">{{ aiAdvice }}</span>
      </div>
      <div v-else-if="aiLoading" class="ai-advice-banner ai-loading">
        投放分析中...
      </div>
      <div class="batch-actions">
        <el-button
          type="success"
          size="small"
          :disabled="selectedIds.length === 0"
          @click="batchUpdate(1)"
        >
          批量启用
        </el-button>
        <el-button
          type="warning"
          size="small"
          :disabled="selectedIds.length === 0"
          @click="batchUpdate(2)"
        >
          批量暂停
        </el-button>
        <span class="batch-hint" v-if="selectedIds.length > 0">
          已选择 {{ selectedIds.length }} 项
        </span>
      </div>

      <el-table
        :data="campaigns"
        stripe
        style="width: 100%"
        @selection-change="handleSelectionChange"
        v-loading="loading"
      >
        <el-table-column type="selection" width="45" />
        <el-table-column prop="name" label="名称" min-width="180" show-overflow-tooltip />
        <el-table-column label="渠道" width="100">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ channelLabel(row.channel) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="strategyName" label="所属策略" width="150" show-overflow-tooltip />
        <el-table-column label="日预算" width="110" align="right" sortable :sort-method="(a, b) => (a.budgetDaily || 0) - (b.budgetDaily || 0)">
          <template #default="{ row }">
            ¥{{ (row.budgetDaily || 0).toLocaleString() }}
          </template>
        </el-table-column>
        <el-table-column label="消耗" width="110" align="right" sortable :sort-method="(a, b) => (a.currentCost || 0) - (b.currentCost || 0)">
          <template #default="{ row }">
            ¥{{ (row.currentCost || 0).toLocaleString() }}
          </template>
        </el-table-column>
        <el-table-column label="CPA" width="100" align="right" sortable :sort-method="(a, b) => (a.currentCpa || 0) - (b.currentCpa || 0)">
          <template #default="{ row }">
            <span :style="cpaStyle(row)">{{ formatCPA(row) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="转化" width="80" align="right" sortable :sort-method="(a, b) => (a.currentConversions || 0) - (b.currentConversions || 0)">
          <template #default="{ row }">
            {{ row.currentConversions || 0 }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">
              {{ statusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="goDetail(row.id!)">
              详情
            </el-button>
            <el-button type="warning" link size="small" @click="handleEdit(row)">
              编辑
            </el-button>
            <el-button
              :type="row.status === 1 ? 'warning' : 'success'"
              link
              size="small"
              @click="toggleStatus(row)"
            >
              {{ row.status === 1 ? '暂停' : row.status === 2 ? '启用' : '-' }}
            </el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row)">
              删除
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
          @current-change="loadCampaigns"
        />
      </div>
    </el-card>

    <CampaignFormDialog
      v-model:visible="dialogVisible"
      :campaign="editingCampaign"
      @saved="handleSaved"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import { getStrategies } from '@/api/strategy'
import { getCampaigns, batchUpdateStatus, deleteCampaign, updateCampaignStatus } from '@/api/campaign'
import type { CampaignDTO, StrategyDTO } from '@/types'
import CampaignFormDialog from './CampaignFormDialog.vue'

const router = useRouter()

const strategies = ref<StrategyDTO[]>([])

const filters = reactive({
  strategyId: undefined as number | undefined,
  channel: '',
  status: undefined as number | undefined,
  keyword: '',
})

const campaigns = ref<CampaignDTO[]>([])
const selectedIds = ref<number[]>([])
const currentPage = ref(1)
const pageSize = 20
const total = ref(0)
const loading = ref(false)

const dialogVisible = ref(false)
const editingCampaign = ref<any>(null)

const channelNameMap: Record<string, string> = {
  DOUYIN: '巨量引擎',
  XIAOHONGSHU: '小红书',
  BILIBILI: 'B站',
  TENCENT: '腾讯广告',
  BAIDU_FEED: '百度信息流',
  BAIDU_SEARCH: '百度搜索',
}

function channelLabel(channel?: string): string {
  return channel ? channelNameMap[channel] || channel : '-'
}

function statusType(status?: number): 'info' | 'success' | 'warning' | 'danger' {
  switch (status) {
    case 0: return 'info'
    case 1: return 'success'
    case 2: return 'warning'
    case 3: return 'danger'
    default: return 'info'
  }
}

function statusLabel(status?: number): string {
  switch (status) {
    case 0: return '搭建中'
    case 1: return '投放中'
    case 2: return '已暂停'
    case 3: return '已停止'
    default: return '未知'
  }
}

function formatCPA(row: CampaignDTO): string {
  if (!row.currentConversions || row.currentConversions <= 0) return '-'
  return '¥' + ((row.currentCost || 0) / row.currentConversions).toFixed(2)
}

function cpaStyle(row: CampaignDTO): Record<string, string> {
  const cpa = row.currentConversions ? (row.currentCost || 0) / row.currentConversions : 0
  if (cpa <= 0) return {}
  if (cpa > 500) return { color: '#f56c6c', fontWeight: 'bold' }
  if (cpa > 250) return { color: '#e6a23c' }
  return { color: '#67c23a' }
}

function handleSelectionChange(rows: CampaignDTO[]) {
  selectedIds.value = rows.map((r) => r.id!).filter(Boolean)
}

async function loadStrategies() {
  try {
    strategies.value = await getStrategies()
  } catch { /* ignore */ }
}

async function loadCampaigns() {
  loading.value = true
  try {
    const result = await getCampaigns({
      page: currentPage.value,
      pageSize,
      strategyId: filters.strategyId,
      channel: filters.channel || undefined,
      keyword: filters.keyword || undefined,
    })
    campaigns.value = result.list
    total.value = result.total
  } catch (err) {
    console.error('Failed to load campaigns:', err)
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  currentPage.value = 1
  loadCampaigns()
}

function resetFilters() {
  filters.strategyId = undefined
  filters.channel = ''
  filters.status = undefined
  filters.keyword = ''
  handleSearch()
}

function handleCreate() {
  editingCampaign.value = null
  dialogVisible.value = true
}

function handleEdit(row: CampaignDTO) {
  editingCampaign.value = { ...row }
  dialogVisible.value = true
}

function handleSaved() {
  loadCampaigns()
}

async function handleDelete(row: CampaignDTO) {
  try {
    await ElMessageBox.confirm(
      `确定删除广告组「${row.name}」吗？此操作不可恢复。`,
      '确认删除',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' }
    )
    await deleteCampaign(row.id!)
    ElMessage.success('广告组已删除')
    loadCampaigns()
  } catch { /* cancelled */ }
}

async function toggleStatus(row: CampaignDTO) {
  const newStatus = row.status === 1 ? 2 : 1
  const label = newStatus === 1 ? '启用' : '暂停'
  try {
    await ElMessageBox.confirm(
      `确定${label}广告组「${row.name}」吗？`,
      '确认操作',
      { confirmButtonText: label, cancelButtonText: '取消', type: 'info' }
    )
    await updateCampaignStatus(row.id!, newStatus)
    ElMessage.success(`已${label}`)
    loadCampaigns()
  } catch { /* cancelled */ }
}

async function batchUpdate(status: number) {
  if (selectedIds.value.length === 0) return
  const label = status === 1 ? '启用' : '暂停'
  try {
    await ElMessageBox.confirm(
      `确定批量${label}选中的 ${selectedIds.value.length} 个广告组吗？`,
      '确认操作',
      { confirmButtonText: label, cancelButtonText: '取消', type: 'info' }
    )
    await batchUpdateStatus(selectedIds.value, status)
    ElMessage.success(`批量${label}成功`)
    selectedIds.value = []
    await loadCampaigns()
  } catch { /* cancelled */ }
}

function goDetail(id: number) {
  router.push(`/campaign/${id}`)
}

const aiAdvice = ref('')
const aiLoading = ref(false)

async function fetchAIAdvice() {
  if (campaigns.value.length === 0) return
  aiLoading.value = true
  try {
    const summary = campaigns.value.filter(c => c.status === 1).map(c => ({
      name: c.name,
      channel: c.channel,
      strategy: c.strategyName,
      budgetDaily: c.budgetDaily || 0,
      cost: c.currentCost || 0,
      cpa: c.currentConversions ? ((c.currentCost || 0) / c.currentConversions).toFixed(1) : '-',
      conversions: c.currentConversions || 0,
      roas: c.currentRoas || 0,
    }))

    const systemPrompt = `你是一个程序化广告投放优化师。根据广告组投放数据，给出整体投放建议。

分析规则：
- CPA低且转化多→效果好，加大预算
- CPA高且消耗大→成本失控，暂停该业务
- ROAS高→回报好，优先加量
- 消耗少但CPA合理→正常投放无需操作
- 多组CPA都高→整体策略需要调整

输出规则：
1. 必须≤20个汉字
2. 格式如：加S1预算,停CPA超500组
3. 只输出结果，不加解释`

    const userPrompt = `当前投放中的广告组数据：
${summary.map(c => `${c.name}[${c.channel}/${c.strategy}] 日预算¥${c.budgetDaily} 消耗¥${c.cost} CPA${c.cpa} 转化${c.conversions} ROAS${c.roas}`).join('\n')}

请输出投放建议：`

    const apiKey = import.meta.env.VITE_AI_API_KEY
    const baseUrl = import.meta.env.VITE_AI_BASE_URL || 'https://api.deepseek.com'
    const model = import.meta.env.VITE_AI_MODEL || 'deepseek-chat'

    const res = await fetch(baseUrl + '/v1/chat/completions', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'Authorization': 'Bearer ' + apiKey },
      body: JSON.stringify({
        model, max_tokens: 60, temperature: 0.1,
        messages: [{ role: 'system', content: systemPrompt }, { role: 'user', content: userPrompt }],
      }),
    })

    const json = await res.json()
    aiAdvice.value = json.choices?.[0]?.message?.content || ''
  } catch (e) {
    console.error('[AI] error:', e)
    aiAdvice.value = ''
  } finally {
    aiLoading.value = false
  }
}

onMounted(() => {
  loadStrategies()
  loadCampaigns()
})
</script>

<style scoped>
.campaign-list { padding: 0; }
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}
.page-header h2 { margin: 0; font-size: 22px; font-weight: 600; color: #303133; }
.filter-card { margin-bottom: 16px; }
.batch-actions {
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  gap: 8px;
}
.batch-hint {
  font-size: 13px;
  color: #909399;
}
.pagination-wrapper {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.ai-advice-banner { text-align: center; padding: 8px 0 12px; }
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
