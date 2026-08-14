<template>
  <el-container class="app-container">
    <el-aside width="220px">
      <SideMenu />
    </el-aside>
    <el-container>
      <el-header class="app-header">
        <span class="app-title">LUMI 程序化广告智能投放中台</span>
      </el-header>
      <el-main class="app-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import SideMenu from './SideMenu.vue'
</script>

<style scoped>
.app-container {
  height: 100vh;
}

.app-header {
  display: flex;
  align-items: center;
  background-color: #fff;
  border-bottom: 1px solid #e6e6e6;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
}

.app-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  letter-spacing: 1px;
}

.app-main {
  background-color: #f0f2f5;
  padding: 20px;
  overflow-y: auto;
}
</style>
