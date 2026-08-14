import request from '@/utils/request'
import type { Result, MaterialDTO } from '@/types'

export function getMaterials(params?: {
  name?: string
  type?: string
  status?: number
}): Promise<MaterialDTO[]> {
  return request.get('/materials', { params })
}

export function createMaterial(data: MaterialDTO): Promise<number> {
  return request.post('/materials', data)
}
