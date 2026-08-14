import request from '@/utils/request'
import type { Result, RuleDTO } from '@/types'

export function getRules(params?: {
  name?: string
  status?: number
}): Promise<RuleDTO[]> {
  return request.get('/rules', { params })
}

export function createRule(data: RuleDTO): Promise<number> {
  return request.post('/rules', data)
}

export function updateRule(id: number, data: RuleDTO): Promise<void> {
  return request.put(`/rules/${id}`, data)
}

export function updateRuleStatus(id: number, status: number): Promise<void> {
  return request.patch(`/rules/${id}/status`, { status })
}

export function deleteRule(id: number): Promise<void> {
  return request.delete(`/rules/${id}`)
}

export function testRule(id: number, data?: { startDate: string; endDate: string }): Promise<any> {
  return request.post(`/rules/${id}/test`, data)
}

export function getRuleLogs(ruleId: number, params?: {
  page?: number
  pageSize?: number
}): Promise<{ list: any[]; total: number }> {
  return request.get(`/rules/${ruleId}/logs`, { params })
}
