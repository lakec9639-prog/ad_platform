<template>
  <div class="strategy-list">
    <div class="page-header">
      <h2>策略管理</h2>
      <el-button size="small" type="primary" plain :loading="aiLoading" @click="fetchAIAdvice">
        AI 策略分析
      </el-button>
    </div>
    <div v-if="aiAdvice" class="ai-advice-banner">
      <span class="ai-badge">AI</span>
      <span class="ai-text">{{ aiAdvice }}</span>
    </div>
    <div v-else-if="aiLoading" class="ai-advice-banner ai-loading">
      策略分析中...
    </div>

    <div v-if="loading" class="loading-state">
      <el-skeleton :rows="3" animated />
    </div>

    <el-empty v-else-if="strategies.length === 0" description="暂无策略数据" />

    <div v-else class="strategy-grid">
      <el-row :gutter="16">
        <el-col
          v-for="strategy in strategies"
          :key="strategy.id"
          :span="8"
          style="margin-bottom: 16px; display: flex"
        >
        <el-card shadow="hover" style="flex: 1">
          <div class="strategy-card">
            <div class="card-header">
              <span class="strategy-name">{{ strategy.name }}</span>
              <el-tag v-if="strategy.code" size="small" type="info">{{ strategy.code }}</el-tag>
              <el-tag
                size="small"
                :type="statusType(strategy.status)"
              >
                {{ statusLabel(strategy.status) }}
              </el-tag>
            </div>
            <p class="strategy-desc">{{ strategy.description || '暂无描述' }}</p>
            <div class="strategy-metrics">
              <div class="metric">
                <span class="metric-label">预算</span>
                <span class="metric-value">
                  ¥{{ ((strategy.budget || 0) / 10000).toFixed(0) }}万
                </span>
              </div>
              <div class="metric">
                <span class="metric-label">目标CPA</span>
                <span class="metric-value">¥{{ (strategy as any).targetCpa || '-' }}</span>
              </div>
              <div class="metric">
                <span class="metric-label">当前CPA</span>
                <span
                  class="metric-value"
                  :class="cpaColorClass(strategy)"
                >
                  ¥{{ (strategy as any).currentCpa || '-' }}
                </span>
              </div>
            </div>
            <div class="card-actions">
              <el-button
                size="small"
                :type="strategy.status === 1 ? 'warning' : 'success'"
                @click="toggleStatus(strategy)"
              >
                {{ strategy.status === 1 ? '暂停' : '启用' }}
              </el-button>
              <el-button
                size="small"
                @click="editStrategy(strategy)"
              >
                编辑
              </el-button>
              <el-button
                size="small"
                type="primary"
                @click="goDetail(strategy.id!)"
              >
                详情
              </el-button>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
    </div>
  </div>

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
          :rows="3"
          placeholder="请输入策略描述"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="showEdit = false">取消</el-button>
      <el-button type="primary" @click="saveEdit" :loading="saving">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getStrategies, updateStrategy, updateStrategyStatus } from '@/api/strategy'
import type { StrategyDTO } from '@/types'
import { ElMessage } from 'element-plus'

const router = useRouter()

const strategies = ref<StrategyDTO[]>([])
const loading = ref(false)
const showEdit = ref(false)
const saving = ref(false)
const editingStrategy = ref<StrategyDTO | null>(null)

const editForm = reactive({
  name: '',
  description: '',
})

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
    case 0: return '草稿'
    case 1: return '启用'
    case 2: return '暂停'
    case 3: return '结束'
    default: return '未知'
  }
}

function cpaColorClass(strategy: StrategyDTO): string {
  const target = (strategy as any).targetCpa
  const current = (strategy as any).currentCpa
  if (!target || !current) return ''
  const ratio = current / target
  if (ratio > 1.2) return 'cpa-danger'
  if (ratio > 1) return 'cpa-warning'
  return 'cpa-success'
}

async function loadStrategies() {
  loading.value = true
  try {
    const result = await getStrategies()
    strategies.value = result
  } catch (err) {
    console.error('Failed to load strategies:', err)
  } finally {
    loading.value = false
  }
}

async function toggleStatus(strategy: StrategyDTO) {
  if (!strategy.id) return
  const newStatus = strategy.status === 1 ? 2 : 1
  try {
    await updateStrategyStatus(strategy.id, newStatus)
    strategy.status = newStatus
  } catch (err) {
    console.error('Failed to update strategy status:', err)
  }
}

function goDetail(id: number) {
  router.push(`/strategy/${id}`)
}

function editStrategy(s: StrategyDTO) {
  editForm.name = s.name
  editForm.description = s.description || ''
  editingStrategy.value = s
  showEdit.value = true
}

async function saveEdit() {
  if (!editingStrategy.value?.id) return
  saving.value = true
  try {
    await updateStrategy(editingStrategy.value.id, {
      name: editForm.name,
      description: editForm.description,
    } as any)
    ElMessage.success('保存成功')
    showEdit.value = false
    // Update local state
    editingStrategy.value.name = editForm.name
    editingStrategy.value.description = editForm.description
  } catch (err) {
    console.error('Failed to update strategy:', err)
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

const aiAdvice = ref('')
const aiLoading = ref(false)

async function fetchAIAdvice() {
  if (strategies.value.length === 0) return
  aiLoading.value = true
  try {
    const summary = strategies.value.map(s => ({
      name: s.name,
      code: s.code,
      budget: s.budget || 0,
      targetCpa: s.targetCpa || 0,
      currentCpa: (s as any).currentCpa || 0,
      status: s.status,
    }))

    const systemPrompt = `你是一个程序化广告投放策略分析师。根据各策略的预算和CPA数据，给出整体调整建议。

分析规则：
- 当前CPA远低于目标CPA→效果好，加大预算
- 当前CPA超过目标CPA→成本偏高，需要优化
- 有策略处于暂停或草稿状态→根据CPA表现建议启用或观望
- 对比各策略的CPA与目标CPA差距，推荐最优预算分配

输出规则：
1. 必须≤20个汉字
2. 格式如：加S1预算,停S5高CPA
3. 只输出结果，不加解释`

    const userPrompt = `当前各策略数据：
${summary.map(s => `${s.name}[${s.code}] 预算¥${(s.budget/10000).toFixed(0)}万 目标CPA¥${s.targetCpa} 当前CPA¥${s.currentCpa} ${s.status === 1 ? '启用' : s.status === 2 ? '暂停' : '草稿'}`).join('\n')}

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

onMounted(loadStrategies)
</script>

<style scoped>
.strategy-list {
  padding: 0;
}
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}
.page-header h2 {
  margin: 0;
  font-size: 22px;
  font-weight: 600;
  color: #303133;
}
.ai-advice-banner { text-align: center; padding: 0 0 16px; }
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
.strategy-grid { padding: 0 8px; }
.loading-state {
  padding: 40px;
}
.strategy-card {
  display: flex;
  flex-direction: column;
  gap: 12px;
  height: 100%;
}
.card-actions {
  display: flex;
  gap: 8px;
  padding-top: 8px;
  border-top: 1px solid #ebeef5;
  margin-top: auto;
}
.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.strategy-name {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}
.strategy-desc {
  font-size: 13px;
  color: #909399;
  margin: 0;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.strategy-metrics {
  display: flex;
  gap: 20px;
}
.metric {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.metric-label {
  font-size: 12px;
  color: #909399;
}
.metric-value {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}
.cpa-success { color: #67c23a; }
.cpa-warning { color: #e6a23c; }
.cpa-danger { color: #f56c6c; }
</style>
