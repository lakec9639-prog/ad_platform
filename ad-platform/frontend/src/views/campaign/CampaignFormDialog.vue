<template>
  <el-dialog
    :model-value="visible"
    :title="isEditing ? '编辑广告组' : '新建广告组'"
    width="600px"
    @update:model-value="$emit('update:visible', $event)"
    @close="handleClose"
    :close-on-click-modal="false"
  >
    <el-form
      ref="formRef"
      :model="form"
      :rules="formRules"
      label-width="120px"
      label-position="left"
    >
      <el-form-item label="广告组名称" prop="name">
        <el-input v-model="form.name" placeholder="例: S1-爆款重定向-高活" />
      </el-form-item>

      <el-form-item label="所属策略" prop="strategyId">
        <el-select v-model="form.strategyId" placeholder="选择策略" style="width: 100%" filterable>
          <el-option
            v-for="s in strategies"
            :key="s.id"
            :label="`${s.name} (${s.code})`"
            :value="s.id"
          />
        </el-select>
      </el-form-item>

      <el-form-item label="投放渠道" prop="channel">
        <el-select v-model="form.channel" placeholder="选择渠道" style="width: 100%">
          <el-option label="巨量引擎 (DOUYIN)" value="DOUYIN" />
          <el-option label="小红书 (XIAOHONGSHU)" value="XIAOHONGSHU" />
          <el-option label="B站 (BILIBILI)" value="BILIBILI" />
          <el-option label="腾讯广告 (TENCENT)" value="TENCENT" />
          <el-option label="百度信息流 (BAIDU_FEED)" value="BAIDU_FEED" />
          <el-option label="百度搜索 (BAIDU_SEARCH)" value="BAIDU_SEARCH" />
        </el-select>
      </el-form-item>

      <el-form-item label="日预算 (¥)" prop="budgetDaily">
        <el-input-number v-model="form.budgetDaily" :min="100" :step="500" style="width: 100%" />
      </el-form-item>

      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="出价方式" prop="bidType">
            <el-select v-model="form.bidType" placeholder="选择" style="width: 100%">
              <el-option label="OCPM" value="OCPM" />
              <el-option label="CPM" value="CPM" />
              <el-option label="CPC" value="CPC" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="出价 (¥)" prop="bidPrice">
            <el-input-number v-model="form.bidPrice" :min="0.1" :step="1" :precision="2" style="width: 100%" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-form-item label="平台计划ID">
        <el-input v-model="form.platformCampaignId" placeholder="选填，第三方平台ID" />
      </el-form-item>

      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="投放开始">
            <el-date-picker
              v-model="form.launchAt"
              type="datetime"
              placeholder="选择时间"
              value-format="YYYY-MM-DD HH:mm:ss"
              style="width: 100%"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="投放结束">
            <el-date-picker
              v-model="form.stopAt"
              type="datetime"
              placeholder="选择时间"
              value-format="YYYY-MM-DD HH:mm:ss"
              style="width: 100%"
            />
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>

    <template #footer>
      <el-button @click="handleClose">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="handleSubmit">
        {{ isEditing ? '保存修改' : '创建广告组' }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getStrategies } from '@/api/strategy'
import { createCampaign, updateCampaign } from '@/api/campaign'
import type { StrategyDTO } from '@/types'

const props = defineProps<{
  visible: boolean
  campaign?: any | null // null = create mode
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  saved: []
}>()

const formRef = ref()
const submitting = ref(false)
const strategies = ref<StrategyDTO[]>([])

const form = reactive({
  name: '',
  strategyId: undefined as number | undefined,
  channel: '',
  budgetDaily: 1000,
  bidType: 'OCPM',
  bidPrice: 10,
  platformCampaignId: '',
  launchAt: undefined as string | undefined,
  stopAt: undefined as string | undefined,
})

const isEditing = ref(false)

const formRules = {
  name: [{ required: true, message: '请输入广告组名称', trigger: 'blur' }],
  strategyId: [{ required: true, message: '请选择所属策略', trigger: 'change' }],
  channel: [{ required: true, message: '请选择投放渠道', trigger: 'change' }],
  budgetDaily: [{ required: true, message: '请输入日预算', trigger: 'blur' }],
}

async function loadStrategies() {
  try {
    strategies.value = await getStrategies()
  } catch {
    // silently fail
  }
}

watch(() => props.visible, (val) => {
  if (val) {
    loadStrategies()
    isEditing.value = !!props.campaign
    if (props.campaign) {
      form.name = props.campaign.name || ''
      form.strategyId = props.campaign.strategyId
      form.channel = props.campaign.channel || ''
      form.budgetDaily = props.campaign.budgetDaily || 1000
      form.bidType = props.campaign.bidType || 'OCPM'
      form.bidPrice = props.campaign.bidPrice || 10
      form.platformCampaignId = props.campaign.platformCampaignId || ''
      form.launchAt = props.campaign.launchAt || undefined
      form.stopAt = props.campaign.stopAt || undefined
    } else {
      form.name = ''
      form.strategyId = undefined
      form.channel = ''
      form.budgetDaily = 1000
      form.bidType = 'OCPM'
      form.bidPrice = 10
      form.platformCampaignId = ''
      form.launchAt = undefined
      form.stopAt = undefined
    }
  }
})

function handleClose() {
  emit('update:visible', false)
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    if (isEditing.value && props.campaign?.id) {
      await updateCampaign(props.campaign.id, { ...form })
      ElMessage.success('广告组已更新')
    } else {
      await createCampaign({ ...form })
      ElMessage.success('广告组创建成功')
    }
    emit('saved')
    handleClose()
  } catch (err: any) {
    ElMessage.error(err?.message || '操作失败')
  } finally {
    submitting.value = false
  }
}
</script>
