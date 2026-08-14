import request from '@/utils/request'
import type { ChannelAccountDTO } from '@/types'

export function getChannelAccounts(): Promise<ChannelAccountDTO[]> {
  return request.get('/channel-accounts')
}

export function getChannelAccount(id: number): Promise<ChannelAccountDTO> {
  return request.get(`/channel-accounts/${id}`)
}

export function createChannelAccount(data: Partial<ChannelAccountDTO>): Promise<number> {
  return request.post('/channel-accounts', data)
}

export function updateChannelAccount(id: number, data: Partial<ChannelAccountDTO>): Promise<void> {
  return request.put(`/channel-accounts/${id}`, data)
}

export function deleteChannelAccount(id: number): Promise<void> {
  return request.delete(`/channel-accounts/${id}`)
}
