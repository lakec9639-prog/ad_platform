<template>
  <div class="audience-list">
    <div class="page-header">
      <h2>人群管理</h2>
      <el-button size="small" type="primary" plain :loading="aiLoading" @click="fetchAIAdvice">
        AI 人群分析
      </el-button>
    </div>
    <div v-if="aiAdvice" class="ai-advice-banner">
      <span class="ai-badge">AI</span>
      <span class="ai-text">{{ aiAdvice }}</span>
    </div>
    <div v-else-if="aiLoading" class="ai-advice-banner ai-loading">
      人群分析中...
    </div>

    <el-card shadow="hover">
      <el-table :data="audiences" stripe style="width: 100%">
        <el-table-column type="index" label="序号" width="64" align="center" />
        <el-table-column prop="code" label="人群编码" align="center">
          <template #default="{ row }">
            {{ (row as any).code || row.id || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="name" label="人群名称" min-width="140" align="center" show-overflow-tooltip />
        <el-table-column label="来源" width="120" align="center">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ sourceLabel(row.source) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="预估规模" sortable :sort-method="(a, b) => (a.sizeEstimate || 0) - (b.sizeEstimate || 0)" align="center">
          <template #default="{ row }">
            {{ (row.sizeEstimate || 0).toLocaleString() }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '使用中' : '待使用' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper" v-if="total > 0">
        <el-pagination
          v-model:current-page="currentPage"
          :page-size="pageSize"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="loadAudiences"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getAudiences } from '@/api/audience'
import type { AudienceDTO } from '@/types'

const audiences = ref<AudienceDTO[]>([])
const currentPage = ref(1)
const pageSize = 20
const total = ref(0)

const sourceMap: Record<string, string> = {
  DMP: 'DMP人群',
  LOOKALIKE: '相似扩展',
  RETARGET: '重定向',
}

function sourceLabel(type?: string): string {
  return type ? sourceMap[type] || type : '-'
}

async function loadAudiences() {
  try {
    const result = await getAudiences()
    audiences.value = result
  } catch (err) {
    console.error('Failed to load audiences:', err)
  }
}

onMounted(loadAudiences)

const aiAdvice = ref('')
const aiLoading = ref(false)

async function fetchAIAdvice() {
  if (audiences.value.length === 0) return
  aiLoading.value = true
  try {
    const summary = audiences.value.map(a => ({
      name: a.name,
      source: a.source,
      size: a.sizeEstimate || 0,
    }))

    const systemPrompt = `你是一个程序化广告投放策略分析师。根据人群数据，推荐应该加大哪个策略的力度。

6个策略：
S1=高价值人群, S2=新品破圈, S3=竞品截流, S4=弃单重定向, S5=智能通投, S6=兜底

分析规则：
- 重定向人群大→加大S4弃单重定向
- DMP人群大→加大S1高价值转化
- 相似扩展人群大→加大S2新品破圈
- 总人群量大→整体扩量，优先S1/S3
- 人群量小→建议S5通投探索

输出规则：
1. 必须≤20个汉字
2. 格式如：人群量大,加S1S3预算
3. 只输出结果，不加解释`

    const userPrompt = `当前人群数据：
${summary.map(a => `${a.name} 来源:${a.source} 规模:${a.size.toLocaleString()}`).join('\n')}

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
</script>

<style scoped>
.audience-list {
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
.pagination-wrapper {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
