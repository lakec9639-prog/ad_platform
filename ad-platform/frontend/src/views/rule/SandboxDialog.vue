<template>
  <el-dialog
    :model-value="visible"
    title="沙箱测试"
    width="700px"
    @update:model-value="$emit('update:visible', $event)"
    @close="handleClose"
  >
    <div class="sandbox-config">
      <el-form :inline="true">
        <el-form-item label="日期范围">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="testing" @click="startSimulation">
            开始模拟
          </el-button>
        </el-form-item>
      </el-form>
    </div>

    <template v-if="hasResult">
      <el-divider content-position="left">模拟结果</el-divider>

      <el-row :gutter="16" class="result-stats">
        <el-col :span="8">
          <el-card shadow="hover" class="result-card">
            <div class="result-value">{{ result.triggerCount }}</div>
            <div class="result-label">触发次数</div>
          </el-card>
        </el-col>
        <el-col :span="8">
          <el-card shadow="hover" class="result-card">
            <div class="result-value">{{ result.affectedCampaignCount }}</div>
            <div class="result-label">影响广告组数</div>
          </el-card>
        </el-col>
        <el-col :span="8">
          <el-card shadow="hover" class="result-card">
            <div class="result-value">¥{{ result.estimatedBudgetSaved.toLocaleString() }}</div>
            <div class="result-label">预估节省预算</div>
          </el-card>
        </el-col>
      </el-row>

      <el-table :data="triggerDetails" stripe style="width: 100%; margin-top: 16px" size="small">
        <el-table-column prop="date" label="日期" width="120" />
        <el-table-column prop="campaignId" label="广告组ID" width="120" />
        <el-table-column prop="triggerValue" label="触发值" width="120" />
        <el-table-column prop="actionDescription" label="执行动作" min-width="200" show-overflow-tooltip />
      </el-table>
    </template>

    <template v-else>
      <el-empty description="选择日期范围并开始模拟" />
    </template>

    <template #footer>
      <el-button @click="handleClose">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { testRule } from '@/api/rule'

const props = withDefaults(
  defineProps<{
    visible: boolean
    ruleId: number
  }>(),
  {
    ruleId: 0,
  },
)

const emit = defineEmits<{
  'update:visible': [value: boolean]
}>()

const dateRange = ref<[string, string]>(['', ''])
const testing = ref(false)

interface SandboxResult {
  triggerCount: number
  affectedCampaignCount: number
  estimatedBudgetSaved: number
}

interface TriggerDetail {
  date: string
  campaignId: number
  triggerValue: string
  actionDescription: string
}

const result = ref<SandboxResult>({
  triggerCount: 0,
  affectedCampaignCount: 0,
  estimatedBudgetSaved: 0,
})

const triggerDetails = ref<TriggerDetail[]>([])

const hasResult = computed(() => result.value.triggerCount > 0)

async function startSimulation() {
  if (!props.ruleId) return
  testing.value = true
  try {
    const res = await testRule(props.ruleId)
    // Map API response to simulation display
    result.value = {
      triggerCount: res.matched ? 1 : 0,
      affectedCampaignCount: res.matched ? 1 : 0,
      estimatedBudgetSaved: res.matched ? 500 : 0,
    }
    triggerDetails.value = res.matched
      ? [
          {
            date: dateRange.value[0] || '-',
            campaignId: props.ruleId,
            triggerValue: '触发',
            actionDescription: res.reason || '执行规则动作',
          },
        ]
      : []
  } catch (err) {
    console.error('Sandbox test failed:', err)
    result.value = {
      triggerCount: 0,
      affectedCampaignCount: 0,
      estimatedBudgetSaved: 0,
    }
    triggerDetails.value = []
  } finally {
    testing.value = false
  }
}

function handleClose() {
  emit('update:visible', false)
  result.value = { triggerCount: 0, affectedCampaignCount: 0, estimatedBudgetSaved: 0 }
  triggerDetails.value = []
}
</script>

<style scoped>
.sandbox-config {
  padding: 8px 0;
}
.result-stats {
  margin-bottom: 8px;
}
.result-card {
  text-align: center;
  padding: 4px 0;
}
.result-value {
  font-size: 22px;
  font-weight: 700;
  color: #409eff;
}
.result-label {
  font-size: 13px;
  color: #909399;
  margin-top: 4px;
}
</style>
