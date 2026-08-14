import request from '@/utils/request'
import type { CampaignDTO } from '@/types'

export function getCampaigns(params?: {
  page?: number
  pageSize?: number
  name?: string
  status?: number
  strategyId?: number
}): Promise<{ list: CampaignDTO[]; total: number; page: number; pageSize: number }> {
  return request.get('/campaigns', { params })
}

export function getCampaign(id: number): Promise<CampaignDTO> {
  return request.get(`/campaigns/${id}`)
}

export function createCampaign(data: Partial<CampaignDTO>): Promise<number> {
  return request.post('/campaigns', data)
}

export function updateCampaign(id: number, data: Partial<CampaignDTO>): Promise<void> {
  return request.put(`/campaigns/${id}`, data)
}

export function deleteCampaign(id: number): Promise<void> {
  return request.delete(`/campaigns/${id}`)
}

export function updateCampaignStatus(id: number, status: number): Promise<void> {
  return request.patch(`/campaigns/${id}/status`, { status })
}

export function batchUpdateStatus(ids: number[], status: number): Promise<void> {
  return request.patch('/campaigns/batch-status', { ids, status })
}
