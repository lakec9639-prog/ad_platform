<template>
  <el-dialog
    :model-value="visible"
    :title="isEditing ? '编辑规则' : '新建规则'"
    width="600px"
    @update:model-value="$emit('update:visible', $event)"
    @close="handleClose"
    :close-on-click-modal="false"
  >
    <el-form
      ref="formRef"
      :model="form"
      :rules="formRules"
      label-width="140px"
      label-position="left"
    >
      <el-form-item label="规则名称" prop="name">
        <el-input v-model="form.name" placeholder="请输入规则名称" />
      </el-form-item>

      <el-form-item label="触发指标" prop="triggerMetric">
        <el-select v-model="form.triggerMetric" placeholder="选择指标" style="width: 100%">
          <el-option label="消耗" value="cost" />
          <el-option label="CPA" value="cpa" />
          <el-option label="CVR" value="cvr" />
          <el-option label="CTR" value="ctr" />
          <el-option label="转化数" value="conversions" />
          <el-option label="展示量" value="impressions" />
        </el-select>
      </el-form-item>

      <el-form-item label="触发条件" prop="triggerOperator">
        <el-select v-model="form.triggerOperator" placeholder="选择条件" style="width: 100%">
          <el-option label="大于" value="gt" />
          <el-option label="大于等于" value="gte" />
          <el-option label="小于" value="lt" />
          <el-option label="小于等于" value="lte" />
          <el-option label="等于" value="eq" />
        </el-select>
      </el-form-item>

      <el-form-item label="触发阈值" prop="triggerThreshold">
        <el-input-number
          v-model="form.triggerThreshold"
          :min="0"
          :precision="2"
          style="width: 100%"
        />
      </el-form-item>

      <el-form-item label="统计窗口 (小时)" prop="triggerWindowHours">
        <el-input-number
          v-model="form.triggerWindowHours"
          :min="1"
          :max="168"
          style="width: 100%"
        />
      </el-form-item>

      <el-form-item label="执行动作" prop="actionType">
        <el-select v-model="form.actionType" placeholder="选择动作" style="width: 100%">
          <el-option label="暂停计划" value="PAUSE_CAMPAIGN" />
          <el-option label="启用计划" value="ACTIVATE" />
          <el-option label="提价" value="RAISE_BID" />
          <el-option label="降价" value="LOWER_BID" />
          <el-option label="替换素材" value="SWAP_MATERIAL" />
          <el-option label="调整预算" value="ADJUST_BUDGET" />
          <el-option label="发送告警" value="SEND_ALERT" />
        </el-select>
      </el-form-item>

      <el-form-item label="作用范围类型" prop="scopeType">
        <el-select v-model="form.scopeType" placeholder="选择范围" style="width: 100%">
          <el-option label="全局" value="GLOBAL" />
          <el-option label="策略" value="STRATEGY" />
          <el-option label="广告组" value="CAMPAIGN" />
          <el-option label="素材" value="MATERIAL" />
        </el-select>
      </el-form-item>

      <el-form-item label="作用范围值" prop="scopeValue">
        <el-input v-model="form.scopeValue" placeholder="输入ID，多个用逗号分隔" />
      </el-form-item>

      <el-form-item label="优先级" prop="priority">
        <el-input-number v-model="form.priority" :min="1" :max="999" style="width: 100%" />
      </el-form-item>

      <el-form-item label="冷却时间 (分钟)" prop="cooldownMinutes">
        <el-input-number
          v-model="form.cooldownMinutes"
          :min="0"
          :max="1440"
          style="width: 100%"
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="handleClose">取消</el-button>
      <el-button type="primary" :loading="saving" @click="handleSave">
        保存
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, watch, computed } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { createRule, updateRule } from '@/api/rule'
import type { RuleDTO } from '@/types'

const props = withDefaults(
  defineProps<{
    visible: boolean
    rule?: RuleDTO | null
  }>(),
  {
    rule: null,
  },
)

const emit = defineEmits<{
  'update:visible': [value: boolean]
  saved: []
}>()

const formRef = ref<FormInstance>()
const saving = ref(false)

const isEditing = computed(() => !!props.rule?.id)

const metricMap: Record<string, string> = {
  cost: 'CONSUME',
  cpa: 'CPA',
  cvr: 'CVR',
  ctr: 'CTR',
  conversions: 'CONVERSIONS',
  impressions: 'IMPRESSIONS',
}
const metricReverse: Record<string, string> = {
  CONSUME: 'cost', CPA: 'cpa', CVR: 'cvr', CTR: 'ctr',
}

const operatorMap: Record<string, string> = {
  gt: 'GT', gte: 'GTE', lt: 'LT', lte: 'LTE', eq: 'EQ',
}
const operatorReverse: Record<string, string> = {
  GT: 'gt', GTE: 'gte', LT: 'lt', LTE: 'lte',
}

const form = reactive({
  name: '',
  triggerMetric: '',
  triggerOperator: 'GT',
  triggerThreshold: '',
  triggerWindowHours: 24,
  actionType: '',
  scopeType: '',
  scopeValue: '',
  priority: 50,
  cooldownMinutes: 60,
  status: 1,
})

const formRules: FormRules = {
  name: [{ required: true, message: '请输入规则名称', trigger: 'blur' }],
  triggerMetric: [{ required: true, message: '请选择触发指标', trigger: 'change' }],
  triggerOperator: [{ required: true, message: '请选择触发条件', trigger: 'change' }],
  triggerThreshold: [{ required: true, message: '请输入触发阈值', trigger: 'blur' }],
  actionType: [{ required: true, message: '请选择执行动作', trigger: 'change' }],
}

function resetForm() {
  form.name = ''
  form.triggerMetric = ''
  form.triggerOperator = 'GT'
  form.triggerThreshold = ''
  form.triggerWindowHours = 24
  form.actionType = ''
  form.scopeType = ''
  form.scopeValue = ''
  form.priority = 50
  form.cooldownMinutes = 60
  form.status = 1
}

watch(
  () => props.visible,
  (val) => {
    if (val) {
      if (props.rule) {
        form.name = props.rule.name
        form.triggerMetric = metricReverse[props.rule.triggerMetric || ''] || props.rule.triggerMetric || ''
        form.triggerOperator = operatorReverse[props.rule.triggerOperator || ''] || props.rule.triggerOperator || 'GT'
        form.triggerThreshold = props.rule.triggerThreshold || ''
        form.triggerWindowHours = props.rule.triggerWindowHours ?? 24
        form.actionType = props.rule.actionType || ''
        form.scopeType = props.rule.scopeType || ''
        form.scopeValue = props.rule.scopeValue || ''
        form.priority = props.rule.priority ?? 50
        form.cooldownMinutes = props.rule.cooldownMinutes ?? 60
        form.status = props.rule.status ?? 1
      } else {
        resetForm()
      }
    }
  },
)

function buildDTO(): Record<string, any> {
  return {
    name: form.name,
    triggerMetric: metricMap[form.triggerMetric] || form.triggerMetric,
    triggerOperator: operatorMap[form.triggerOperator] || form.triggerOperator,
    triggerThreshold: String(form.triggerThreshold),
    triggerWindowHours: form.triggerWindowHours,
    actionType: form.actionType,
    actionParams: '{}',
    scopeType: form.scopeType || 'CAMPAIGN',
    scopeValue: form.scopeValue || null,
    priority: form.priority,
    cooldownMinutes: form.cooldownMinutes,
    status: form.status,
  }
}

async function handleSave() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    if (isEditing.value && props.rule?.id) {
      await updateRule(props.rule.id, buildDTO() as any)
      ElMessage.success('规则更新成功')
    } else {
      await createRule(buildDTO() as any)
      ElMessage.success('规则创建成功')
    }
    emit('saved')
  } catch (err) {
    console.error('Failed to save rule:', err)
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

function handleClose() {
  emit('update:visible', false)
  resetForm()
}
</script>
