import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    component: () => import('@/layout/AppLayout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/DashboardPage.vue'),
        meta: { title: '仪表盘', icon: 'DataAnalysis' },
      },
      {
        path: 'strategy',
        redirect: '/strategy/list',
      },
      {
        path: 'strategy/list',
        name: 'StrategyList',
        component: () => import('@/views/strategy/StrategyList.vue'),
        meta: { title: '策略列表', icon: 'List' },
      },
      {
        path: 'strategy/:id',
        name: 'StrategyDetail',
        component: () => import('@/views/strategy/StrategyDetail.vue'),
        meta: { title: '策略详情', icon: 'List', hidden: true },
      },
      {
        path: 'campaign',
        redirect: '/campaign/list',
      },
      {
        path: 'campaign/list',
        name: 'CampaignList',
        component: () => import('@/views/campaign/CampaignList.vue'),
        meta: { title: '广告组列表', icon: 'Document' },
      },
      {
        path: 'campaign/:id',
        name: 'CampaignDetail',
        component: () => import('@/views/campaign/CampaignDetail.vue'),
        meta: { title: '广告组详情', icon: 'Document', hidden: true },
      },
      {
        path: 'audience',
        name: 'Audience',
        component: () => import('@/views/audience/AudienceList.vue'),
        meta: { title: '人群管理', icon: 'User' },
      },
      {
        path: 'material',
        redirect: '/material/list',
      },
      {
        path: 'material/list',
        name: 'MaterialList',
        component: () => import('@/views/material/MaterialList.vue'),
        meta: { title: '素材列表', icon: 'Picture' },
      },
      {
        path: 'material/analysis',
        name: 'MaterialAnalysis',
        component: () => import('@/views/material/MaterialAnalysis.vue'),
        meta: { title: '素材衰减分析', icon: 'TrendCharts' },
      },
      {
        path: 'rule-engine',
        name: 'RuleEngine',
        component: () => import('@/views/rule/RuleEnginePage.vue'),
        meta: { title: '规则引擎', icon: 'SetUp' },
      },
      {
        path: 'settings',
        name: 'Settings',
        component: () => import('@/views/settings/SettingsPage.vue'),
        meta: { title: '系统设置', icon: 'Setting' },
      },
    ],
  },
]

const router = createRouter({
  history: createWebHistory('/ad-platform'),
  routes,
})

export default router
