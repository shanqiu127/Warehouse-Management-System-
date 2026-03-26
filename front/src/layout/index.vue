<template>
  <el-container class="layout-container">
    <el-aside width="200px" style="background-color: #304156; color: white;">
      <h3 style="text-align: center; color: white; padding: 15px 0; margin: 0;">仓库管理系统</h3>
      <el-menu background-color="#304156" text-color="#fff" router :default-active="$route.path">
        <el-menu-item index="/home">首页</el-menu-item>
        
        <!-- 业务菜单对 superadmin 隐藏，避免干扰超管专注于系统治理 -->
        <el-sub-menu index="/base" v-if="!onlySuperAdmin">
          <template #title>基础资料</template>
          <el-menu-item index="/base/supplier">供应商管理</el-menu-item>
          <el-menu-item index="/base/goods">商品资料管理</el-menu-item>
        </el-sub-menu>

        <el-sub-menu index="/purchase" v-if="!onlySuperAdmin">
          <template #title>进货</template>
          <el-menu-item index="/business/purchase">商品进货</el-menu-item>
          <el-menu-item index="/business/purchase-return">进货退货单</el-menu-item>
        </el-sub-menu>

        <el-sub-menu index="/sales" v-if="!onlySuperAdmin">
          <template #title>销售</template>
          <el-menu-item index="/business/sales">商品销售</el-menu-item>
          <el-menu-item index="/business/sales-return">销售退货单</el-menu-item>
        </el-sub-menu>

        <el-sub-menu index="/stock-warning" v-if="!onlySuperAdmin">
          <template #title>库存预警</template>
          <el-menu-item index="/business/stock-warning">预警中心</el-menu-item>
        </el-sub-menu>

        <!-- 统计报表仅管理员可见 -->
        <el-sub-menu index="/statistics" v-if="isAdminOnly">
          <template #title>统计报表</template>
          <el-menu-item index="/business/sales-chart">销售统计图表</el-menu-item>
        </el-sub-menu>

        <!-- 系统管理中的业务模块仅管理员可见 -->
        <el-sub-menu index="/system" v-if="isAdminOnly">
          <template #title>系统管理</template>
          <el-menu-item index="/system/notice">公告管理</el-menu-item>
          <el-menu-item index="/system/user">用户管理</el-menu-item>
          <el-menu-item index="/system/dept">部门管理</el-menu-item>
          <el-menu-item index="/system/employee">员工管理</el-menu-item>
        </el-sub-menu>

        <el-sub-menu index="/superadmin" v-if="onlySuperAdmin">
          <template #title>超管中心</template>
          <el-menu-item index="/system/super-admin">超管总览</el-menu-item>
          <el-menu-item index="/system/security-ip-policy">安全策略</el-menu-item>
          <el-menu-item index="/system/login-log">登录日志</el-menu-item>
          <el-menu-item index="/system/operation-log">操作日志</el-menu-item>
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
import { computed } from 'vue'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import { isSuperAdmin, normalizeRole } from '@/utils/auth'

const router = useRouter()
const userStore = useUserStore()

const isAdminOnly = computed(() => normalizeRole(userStore.role) === 'admin')
const onlySuperAdmin = computed(() => isSuperAdmin(userStore.role))

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