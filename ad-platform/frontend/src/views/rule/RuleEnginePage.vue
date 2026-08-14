<template>
  <div class="rule-engine-page">
    <div class="page-header">
      <h2>规则引擎</h2>
      <el-button type="primary" @click="openCreate">
        <el-icon><Plus /></el-icon>
        新建规则
      </el-button>
    </div>

    <el-alert
      title="系统内置规则（灰锁）不可删除或禁用，仅可调整阈值"
      type="warning"
      show-icon
      :closable="false"
      class="system-alert"
    />

    <el-card shadow="hover">
      <el-table :data="rules" stripe style="width: 100%">
        <el-table-column prop="name" label="规则名称" min-width="160" show-overflow-tooltip />
        <el-table-column label="触发条件" min-width="200">
          <template #default="{ row }">
            {{ formatTriggerCondition(row) }}
          </template>
        </el-table-column>
        <el-table-column label="执行动作" width="120">
          <template #default="{ row }">
            {{ actionTypeLabel(row.actionType) }}
          </template>
        </el-table-column>
        <el-table-column prop="priority" label="优先级" width="80" sortable />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-switch
              :model-value="row.status === 1"
              :disabled="isSystemRule(row)"
              @change="(val: boolean) => handleStatusToggle(row, val)"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="openEdit(row)">
              编辑
            </el-button>
            <el-button type="warning" link size="small" @click="openSandbox(row)">
              沙箱测试
            </el-button>
            <el-button
              type="danger"
              link
              size="small"
              :disabled="isSystemRule(row)"
              @click="handleDelete(row)"
            >
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
          @current-change="loadRules"
        />
      </div>
    </el-card>

    <RuleFormDialog
      v-model:visible="dialogVisible"
      :rule="editingRule"
      @saved="handleSaved"
    />

    <SandboxDialog
      v-model:visible="sandboxVisible"
      :rule-id="sandboxRuleId"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getRules, updateRuleStatus, deleteRule } from '@/api/rule'
import type { RuleDTO } from '@/types'
import RuleFormDialog from './RuleFormDialog.vue'
import SandboxDialog from './SandboxDialog.vue'

const rules = ref<RuleDTO[]>([])
const currentPage = ref(1)
const pageSize = 20
const total = ref(0)

const dialogVisible = ref(false)
const editingRule = ref<RuleDTO | null>(null)

const sandboxVisible = ref(false)
const sandboxRuleId = ref<number>(0)

function isSystemRule(rule: RuleDTO): boolean {
  return rule.isSystem === true
}

const triggerMetricLabels: Record<string, string> = {
  CPA: 'CPA',
  CTR: '点击率',
  CVR: '转化率',
  CONSUME: '消耗金额',
}

const operatorLabels: Record<string, string> = {
  GT: '>', LT: '<', GTE: '≥', LTE: '≤',
}

function formatTriggerCondition(rule: RuleDTO): string {
  const metric = triggerMetricLabels[rule.triggerMetric || ''] || rule.triggerMetric
  const op = operatorLabels[rule.triggerOperator || ''] || rule.triggerOperator
  const threshold = rule.triggerThreshold || ''
  const window = rule.triggerWindowHours ? `${rule.triggerWindowHours}h` : ''
  return [metric, op, threshold, window].filter(Boolean).join(' ')
}

function actionTypeLabel(type?: string): string {
  switch (type) {
    case 'PAUSE_CAMPAIGN': return '暂停计划'
    default: return type || '-'
  }
}

async function loadRules() {
  try {
    const result = await getRules()
    rules.value = result
  } catch (err) {
    console.error('Failed to load rules:', err)
  }
}

function openCreate() {
  editingRule.value = null
  dialogVisible.value = true
}

function openEdit(rule: RuleDTO) {
  editingRule.value = { ...rule }
  dialogVisible.value = true
}

async function handleStatusToggle(rule: RuleDTO, val: boolean) {
  if (!rule.id) return
  try {
    await updateRuleStatus(rule.id, val ? 1 : 0)
    rule.status = val ? 1 : 0
    ElMessage.success(val ? '规则已启用' : '规则已禁用')
  } catch (err) {
    console.error('Failed to toggle rule status:', err)
  }
}

function openSandbox(rule: RuleDTO) {
  sandboxRuleId.value = rule.id!
  sandboxVisible.value = true
}

async function handleDelete(rule: RuleDTO) {
  if (!rule.id) return
  try {
    await ElMessageBox.confirm('确定删除该规则？删除后不可恢复。', '确认删除', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await deleteRule(rule.id)
    ElMessage.success('规则已删除')
    await loadRules()
  } catch (err) {
    if (err !== 'cancel') {
      console.error('Failed to delete rule:', err)
    }
  }
}

function handleSaved() {
  dialogVisible.value = false
  editingRule.value = null
  loadRules()
}

onMounted(loadRules)
</script>

<style scoped>
.rule-engine-page {
  padding: 0;
}
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}
.page-header h2 {
  margin: 0;
  font-size: 22px;
  font-weight: 600;
  color: #303133;
}
.system-alert {
  margin-bottom: 16px;
}
.pagination-wrapper {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
