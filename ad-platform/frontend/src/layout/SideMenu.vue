<template>
  <div class="side-menu-wrapper">
    <div class="logo">
      <span class="logo-text">LUMI AD</span>
    </div>
    <el-menu
      :default-active="activeMenu"
      router
      background-color="#304156"
      text-color="#bfcbd9"
      active-text-color="#409eff"
      class="side-menu"
    >
      <el-menu-item index="/dashboard">
        <el-icon><DataAnalysis /></el-icon>
        <span>仪表盘</span>
      </el-menu-item>
      <el-menu-item index="/strategy/list">
        <el-icon><List /></el-icon>
        <span>策略管理</span>
      </el-menu-item>
      <el-menu-item index="/campaign/list">
        <el-icon><Document /></el-icon>
        <span>广告组管理</span>
      </el-menu-item>
      <el-menu-item index="/audience">
        <el-icon><User /></el-icon>
        <span>人群管理</span>
      </el-menu-item>
      <el-sub-menu index="/material">
        <template #title>
          <el-icon><Picture /></el-icon>
          <span>素材管理</span>
        </template>
        <el-menu-item index="/material/list">
          <span>素材列表</span>
        </el-menu-item>
        <el-menu-item index="/material/analysis">
          <el-icon><TrendCharts /></el-icon>
          <span>素材衰减分析</span>
        </el-menu-item>
      </el-sub-menu>
      <el-menu-item index="/rule-engine">
        <el-icon><SetUp /></el-icon>
        <span>规则引擎</span>
      </el-menu-item>
      <el-menu-item index="/settings">
        <el-icon><Setting /></el-icon>
        <span>系统设置</span>
      </el-menu-item>
    </el-menu>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { TrendCharts } from '@element-plus/icons-vue'

const route = useRoute()

const activeMenu = computed(() => {
  const { path } = route
  // If the route has a parent path like /strategy/:id, highlight the list path
  if (path.startsWith('/strategy')) {
    return '/strategy/list'
  }
  if (path.startsWith('/campaign')) {
    return '/campaign/list'
  }
  if (path.startsWith('/material')) {
    return path
  }
  return path
})
</script>

<style scoped>
.side-menu-wrapper {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #2b3648;
}

.logo-text {
  font-size: 20px;
  font-weight: bold;
  color: #fff;
  letter-spacing: 2px;
}

.side-menu {
  flex: 1;
  border-right: none;
}
</style>
