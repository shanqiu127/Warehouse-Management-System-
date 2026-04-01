<template>
  <el-container class="layout-container">
    <el-aside width="200px" style="background-color: #304156; color: white;">
      <h3 style="text-align: center; color: white; padding: 15px 0; margin: 0;">仓库管理系统</h3>
      <el-menu background-color="#304156" text-color="#fff" router :default-active="$route.path">
        <el-menu-item index="/home">首页</el-menu-item>

        <template v-if="isHrAdmin">
          <el-menu-item index="/system/dept">全部门管理</el-menu-item>
          <el-menu-item index="/system/employee">全员工管理</el-menu-item>
          <el-menu-item index="/system/hr-chart">员工分布图表</el-menu-item>
          <el-menu-item index="/system/notice">公告管理</el-menu-item>
          <el-menu-item index="/system/user">用户部门管理</el-menu-item>
        </template>

        <template v-else-if="isPurchaseAdmin">
          <el-menu-item index="/business/purchase">商品进货</el-menu-item>
          <el-menu-item index="/business/purchase-return">进货退货</el-menu-item>
          <el-menu-item index="/business/stock-warning">预警中心</el-menu-item>
          <el-menu-item index="/system/notice">公告管理</el-menu-item>
          <el-menu-item index="/system/user">用户部门管理</el-menu-item>
        </template>

        <template v-else-if="isSalesAdmin">
          <el-menu-item index="/business/sales">商品销售</el-menu-item>
          <el-menu-item index="/business/sales-return">销售退货</el-menu-item>
          <el-menu-item index="/business/stock-warning">预警中心</el-menu-item>
          <el-menu-item index="/system/notice">公告管理</el-menu-item>
          <el-menu-item index="/system/user">用户部门管理</el-menu-item>
        </template>

        <template v-else-if="isWarehouseAdmin">
          <el-menu-item index="/base/supplier">供应商管理</el-menu-item>
          <el-menu-item index="/base/goods">商品资料管理</el-menu-item>
          <el-menu-item index="/business/stock-warning">预警中心</el-menu-item>
          <el-menu-item index="/system/void-approval">作废审批</el-menu-item>
          <el-menu-item index="/system/notice">公告管理</el-menu-item>
          <el-menu-item index="/system/user">用户部门管理</el-menu-item>
        </template>

        <template v-else-if="isFinanceAdmin">
          <el-menu-item index="/business/sales-chart">销售统计图表</el-menu-item>
          <el-menu-item index="/system/notice">公告管理</el-menu-item>
          <el-menu-item index="/system/user">用户部门管理</el-menu-item>
        </template>

        <template v-else-if="showSuperAdminCenter">
          <el-menu-item index="/system/notice">公告管理</el-menu-item>
          <el-menu-item index="/system/user">用户管理</el-menu-item>
          <el-menu-item index="/system/super-admin">超管总览</el-menu-item>
          <el-menu-item index="/system/dept-approval">部门审批</el-menu-item>
        </template>

        <el-sub-menu index="/superadmin-audit" v-if="showSuperAdminCenter">
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
        <div class="header-actions">
          <MessageCenter />
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
import MessageCenter from '@/components/MessageCenter.vue'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import { isAdminRole, isSuperAdmin, normalizeDeptCode } from '@/utils/auth'
import { logoutAPI } from '@/api/user'

const router = useRouter()
const userStore = useUserStore()

const currentDeptCode = computed(() => normalizeDeptCode(userStore.deptCode))
const isDeptAdminRole = computed(() => isAdminRole(userStore.role))
const showSuperAdminCenter = computed(() => isSuperAdmin(userStore.role))
const isFinanceAdmin = computed(() => isDeptAdminRole.value && currentDeptCode.value === 'finance')
const isSalesAdmin = computed(() => isDeptAdminRole.value && currentDeptCode.value === 'sales')
const isWarehouseAdmin = computed(() => isDeptAdminRole.value && currentDeptCode.value === 'warehouse')
const isPurchaseAdmin = computed(() => isDeptAdminRole.value && currentDeptCode.value === 'purchase')
const isHrAdmin = computed(() => isDeptAdminRole.value && currentDeptCode.value === 'hr')

const handleLogout = async () => {
  try {
    await logoutAPI()
  } catch {
    // 本地登录态仍需清理，避免后端会话异常时阻塞退出。
  }
  userStore.clearToken()
  ElMessage.success('已安全退出')
  router.push('/login')
}
</script>

<style scoped>
.layout-container {
  height: 100vh;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.el-menu {
  border-right: none;
}
</style>