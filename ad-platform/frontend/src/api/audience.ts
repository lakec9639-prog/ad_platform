import request from '@/utils/request'
import type { Result, AudienceDTO } from '@/types'

export function getAudiences(params?: {
  name?: string
}): Promise<AudienceDTO[]> {
  return request.get('/audiences', { params })
}

export function createAudience(data: AudienceDTO): Promise<number> {
  return request.post('/audiences', data)
}
