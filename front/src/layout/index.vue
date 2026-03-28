<template>
  <el-container class="layout-container">
    <el-aside width="200px" style="background-color: #304156; color: white;">
      <h3 style="text-align: center; color: white; padding: 15px 0; margin: 0;">仓库管理系统</h3>
      <el-menu background-color="#304156" text-color="#fff" router :default-active="$route.path">
        <el-menu-item index="/home">首页</el-menu-item>
        
        <!-- 角色分工调整：基础资料/进货/销售仅对普通员工显示 -->
        <el-sub-menu index="/base" v-if="isEmployeeOnly">
          <template #title>基础资料</template>
          <el-menu-item index="/base/supplier">供应商管理</el-menu-item>
          <el-menu-item index="/base/goods">商品资料管理</el-menu-item>
        </el-sub-menu>

        <el-sub-menu index="/purchase" v-if="isEmployeeOnly">
          <template #title>进货</template>
          <el-menu-item index="/business/purchase">商品进货</el-menu-item>
          <el-menu-item index="/business/purchase-return">进货退货单</el-menu-item>
        </el-sub-menu>

        <el-sub-menu index="/sales" v-if="isEmployeeOnly">
          <template #title>销售</template>
          <el-menu-item index="/business/sales">商品销售</el-menu-item>
          <el-menu-item index="/business/sales-return">销售退货单</el-menu-item>
        </el-sub-menu>

        <el-sub-menu index="/stock-warning" v-if="isEmployeeOnly">
          <template #title>库存预警</template>
          <el-menu-item index="/business/stock-warning">预警中心</el-menu-item>
        </el-sub-menu>

        <!-- 统计报表仅管理员可见 -->
        <el-sub-menu index="/statistics" v-if="isAdminOnly">
          <template #title>统计报表</template>
          <el-menu-item index="/business/sales-chart">销售统计图表</el-menu-item>
        </el-sub-menu>

        <el-menu-item index="/system/void-approval" v-if="isAdminOnly">作废审批</el-menu-item>
        <el-menu-item index="/system/notice" v-if="isAdminOnly">公告管理</el-menu-item>
        <el-sub-menu index="/personnel" v-if="isAdminOnly">
          <template #title>人事管理</template>
          <el-menu-item index="/system/dept">部门管理</el-menu-item>
          <el-menu-item index="/system/employee">员工管理</el-menu-item>
        </el-sub-menu>

        <el-menu-item index="/system/super-admin" v-if="onlySuperAdmin">超管总览</el-menu-item>
        <el-menu-item index="/system/user" v-if="onlySuperAdmin">用户管理</el-menu-item>
        <el-sub-menu index="/superadmin-audit" v-if="onlySuperAdmin">
          <template #title>安全审计</template>
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
const isEmployeeOnly = computed(() => normalizeRole(userStore.role) === 'employee')
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