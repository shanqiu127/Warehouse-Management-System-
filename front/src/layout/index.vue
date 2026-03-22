<template>
  <el-container class="layout-container">
    <el-aside width="200px" style="background-color: #304156; color: white;">
      <h3 style="text-align: center; color: white; padding: 15px 0; margin: 0;">仓库管理系统</h3>
      <el-menu background-color="#304156" text-color="#fff" router :default-active="$route.path">
        <el-menu-item index="/home">首页</el-menu-item>
        
        <!-- 基础资料菜单，包含供应商和商品，所有角色都可见 -->
        <el-sub-menu index="/base">
          <template #title>基础资料</template>
          <el-menu-item index="/base/supplier">供应商管理</el-menu-item>
          <el-menu-item index="/base/goods">商品资料管理</el-menu-item>
        </el-sub-menu>

        <!-- 进货菜单，所有角色可见 -->
        <el-sub-menu index="/purchase">
          <template #title>进货</template>
          <el-menu-item index="/business/purchase">商品进货</el-menu-item>
          <el-menu-item index="/business/purchase-return">进货退货单</el-menu-item>
        </el-sub-menu>

        <!-- 销售菜单，所有角色可见 -->
        <el-sub-menu index="/sales">
          <template #title>销售</template>
          <el-menu-item index="/business/sales">商品销售</el-menu-item>
          <el-menu-item index="/business/sales-return">销售退货单</el-menu-item>
        </el-sub-menu>

        <!-- 统计报表菜单，仅管理员/超级管理员可见 -->
        <el-sub-menu index="/statistics" v-if="userStore.role === 'admin' || userStore.role === 'superadmin'">
          <template #title>统计报表</template>
          <el-menu-item index="/business/sales-chart">销售统计图表</el-menu-item>
        </el-sub-menu>

        <!-- 管理员/超级管理员专属菜单，根据 role 控制 -->
        <el-sub-menu index="/system" v-if="userStore.role === 'admin' || userStore.role === 'superadmin'">
          <template #title>系统管理</template>
          <el-menu-item index="/system/notice">公告管理</el-menu-item>
          <el-menu-item index="/system/user">用户管理</el-menu-item>
          <el-menu-item index="/system/dept">部门管理</el-menu-item>
          <el-menu-item index="/system/employee">员工管理</el-menu-item>
        </el-sub-menu>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header style="display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid #eaeaea;">
        <div style="font-weight: bold;">后台数据管理系统</div>
        <div>
          <el-button type="danger" text @click="handleLogout">退出登录</el-button>
        </div>
      </el-header>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()

const handleLogout = () => {
  userStore.clearToken()
  ElMessage.success('已安全退出')
  router.push('/login')
}
</script>

<style scoped>
.layout-container {
  height: 100vh;
}
.el-menu {
  border-right: none;
}
</style>