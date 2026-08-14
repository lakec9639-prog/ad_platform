/** Generic API response wrapper */
export interface Result<T = any> {
  code: number
  message: string
  data: T
}

/** Paginated response */
export interface PageResult<T = any> {
  list: T[]
  total: number
  page: number
  pageSize: number
}

/** Strategy (投放策略) */
export interface StrategyDTO {
  id?: number
  name: string
  code?: string
  status?: number // 0=草稿 1=启用 2=暂停 3=结束
  objective?: string // CONVERT, BRAND, RETARGET
  description?: string
  budget?: number
  targetCpa?: number
  targetCvr?: number
  expectedRoas?: number
  budgetRatio?: number
  sortOrder?: number
  channelAllocations?: { channel: string; budgetRatio: number }[]
  audienceIds?: number[]
  materialIds?: number[]
  currentCost?: number
  currentCpa?: number
  currentRoas?: number
  createdAt?: string
  updatedAt?: string
}

/** Campaign / Ad Group (广告组) */
export interface CampaignDTO {
  id?: number
  name: string
  strategyId?: number
  strategyName?: string
  channel?: string
  platformCampaignId?: string
  budgetDaily?: number
  bidPrice?: number
  bidType?: string
  status?: number // 0=搭建中 1=投放中 2=已暂停 3=已停止
  launchAt?: string
  stopAt?: string
  currentCost?: number
  currentCpa?: number
  currentRoas?: number
  currentConversions?: number
  createdAt?: string
  updatedAt?: string
}

/** Audience (人群) */
export interface AudienceDTO {
  id?: number
  name: string
  code?: string
  source?: string // dmp, lookalike, retarget
  sizeEstimate?: number
  status?: number
  createdAt?: string
  updatedAt?: string
}

/** Material (素材) */
export interface MaterialDTO {
  id?: number
  name: string
  code?: string
  type?: string // video, image, image_text
  duration?: number
  status?: number // 0=审核中 1=生效中 2=衰退中 3=已停止
  score?: number
  createdAt?: string
  updatedAt?: string
}

/** Rule (规则) */
export interface RuleDTO {
  id?: number
  name: string
  triggerMetric?: string   // CPA, CTR, CVR, CONSUME
  triggerOperator?: string // GT, LT, GTE, LTE
  triggerThreshold?: string
  triggerWindowHours?: number
  actionType?: string      // PAUSE_CAMPAIGN, etc.
  actionParams?: string    // JSON string
  scopeType?: string       // STRATEGY, CAMPAIGN
  scopeValue?: string
  priority?: number
  cooldownMinutes?: number
  isSystem?: boolean
  status?: number
  createdAt?: string
  updatedAt?: string
}

/** Channel Account (渠道账号) */
export interface ChannelAccountDTO {
  id?: number
  name: string
  channel: string
  appId?: string
  appSecret?: string
  status?: number
  createdAt?: string
  updatedAt?: string
}
