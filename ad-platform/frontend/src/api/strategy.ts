import request from '@/utils/request'
import type { StrategyDTO } from '@/types'

export function getStrategies(params?: {
  name?: string
  status?: number
}): Promise<StrategyDTO[]> {
  return request.get('/strategies', { params })
}

export function getStrategy(id: number): Promise<StrategyDTO> {
  return request.get(`/strategies/${id}`)
}

export function createStrategy(data: StrategyDTO): Promise<StrategyDTO> {
  return request.post('/strategies', data)
}

export function updateStrategy(id: number, data: StrategyDTO): Promise<StrategyDTO> {
  return request.put(`/strategies/${id}`, data)
}

export function updateStrategyStatus(id: number, status: number): Promise<void> {
  return request.patch(`/strategies/${id}/status`, { status })
}
