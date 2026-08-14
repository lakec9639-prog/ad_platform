import request from '@/utils/request'
import type { Result } from '@/types'

export interface OverviewData {
  totalCost: number
  totalNewUsers: number
  totalConversions: number
  totalGmv: number
  totalImpressions: number
  totalClicks: number
  cpa: number
  roas: number
  ctr: number
  cvr: number
  budgetTotal: number
  budgetProgress: number
  budgetRemaining: number
  // mapped fields
  impressions: number
  clicks: number
  cost: number
  conversions: number
  newUsers: number
}

export interface TrendItem {
  date: string
  impressions: number
  clicks: number
  cost: number
  conversions: number
}

export interface ChannelDistItem {
  channel: string
  value: number
  percentage: number
}

export interface MaterialTopItem {
  id: number
  name: string
  impressions: number
  clicks: number
  cost: number
}

export function getOverview(params?: {
  startDate?: string
  endDate?: string
}): Promise<OverviewData> {
  return request.get('/dashboard/overview', { params })
}

export function getTrends(params?: {
  startDate?: string
  endDate?: string
  campaignId?: number
}): Promise<TrendItem[]> {
  return request.get('/dashboard/trends', { params })
}

export function getChannelDist(params?: {
  startDate?: string
  endDate?: string
}): Promise<ChannelDistItem[]> {
  return request.get('/dashboard/channel-dist', { params })
}

export function getMaterialTop(params?: {
  startDate?: string
  endDate?: string
  limit?: number
}): Promise<MaterialTopItem[]> {
  return request.get('/dashboard/material-top', { params })
}
