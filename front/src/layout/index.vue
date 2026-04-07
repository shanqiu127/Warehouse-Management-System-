<template>
  <el-container class="layout-container">
    <el-aside v-if="showSidebar" :width="sidebarWidth" class="app-aside">
      <div :class="['brand-block', { 'brand-block--collapsed': isSidebarCollapsed }]">
        <h3 class="brand-title">{{ isSidebarCollapsed ? 'WMS' : '仓库管理系统' }}</h3>
      </div>
      <el-menu
        class="app-menu"
        background-color="#304156"
        text-color="#fff"
        router
        :collapse="isSidebarCollapsed"
        :collapse-transition="false"
        :default-active="route.path"
        :default-openeds="defaultOpeneds"
      >
        <el-menu-item index="/home"><el-icon><HomeFilled /></el-icon><span>首页</span></el-menu-item>

        <template v-if="isHrAdmin">
          <el-menu-item index="/system/dept"><el-icon><OfficeBuilding /></el-icon><span>全部门管理</span></el-menu-item>
          <el-menu-item index="/system/employee"><el-icon><User /></el-icon><span>全员工管理</span></el-menu-item>
          <el-menu-item index="/system/hr-chart"><el-icon><PieChart /></el-icon><span>员工分布图表</span></el-menu-item>
          <el-sub-menu index="/notification">
            <template #title><el-icon><Promotion /></el-icon><span class="menu-title-text">发布</span></template>
            <el-menu-item index="/system/work-requirement"><el-icon><Tickets /></el-icon><span>工作要求</span></el-menu-item>
            <el-menu-item index="/system/notice"><el-icon><Bell /></el-icon><span>公告管理</span></el-menu-item>
          </el-sub-menu>
          <el-menu-item index="/system/user"><el-icon><UserFilled /></el-icon><span>用户部门管理</span></el-menu-item>
        </template>

        <template v-else-if="isPurchaseAdmin">
          <el-menu-item index="/business/purchase"><el-icon><ShoppingCart /></el-icon><span>商品进货</span></el-menu-item>
          <el-menu-item index="/business/purchase-return"><el-icon><RefreshLeft /></el-icon><span>进货退货</span></el-menu-item>
          <el-menu-item index="/business/stock-warning"><el-icon><WarningFilled /></el-icon><span>预警中心</span></el-menu-item>
          <el-sub-menu index="/notification">
            <template #title><el-icon><Promotion /></el-icon><span class="menu-title-text">发布</span></template>
            <el-menu-item index="/system/work-requirement"><el-icon><Tickets /></el-icon><span>工作要求</span></el-menu-item>
            <el-menu-item index="/system/notice"><el-icon><Bell /></el-icon><span>公告管理</span></el-menu-item>
          </el-sub-menu>
          <el-menu-item index="/system/user"><el-icon><UserFilled /></el-icon><span>用户部门管理</span></el-menu-item>
        </template>

        <template v-else-if="isSalesAdmin">
          <el-menu-item index="/business/sales"><el-icon><Sell /></el-icon><span>商品销售</span></el-menu-item>
          <el-menu-item index="/business/sales-return"><el-icon><RefreshRight /></el-icon><span>销售退货</span></el-menu-item>
          <el-menu-item index="/business/stock-warning"><el-icon><WarningFilled /></el-icon><span>预警中心</span></el-menu-item>
          <el-sub-menu index="/notification">
            <template #title><el-icon><Promotion /></el-icon><span class="menu-title-text">发布</span></template>
            <el-menu-item index="/system/work-requirement"><el-icon><Tickets /></el-icon><span>工作要求</span></el-menu-item>
            <el-menu-item index="/system/notice"><el-icon><Bell /></el-icon><span>公告管理</span></el-menu-item>
          </el-sub-menu>
          <el-menu-item index="/system/user"><el-icon><UserFilled /></el-icon><span>用户部门管理</span></el-menu-item>
        </template>

        <template v-else-if="isWarehouseAdmin">
          <el-menu-item index="/base/supplier"><el-icon><Van /></el-icon><span>供应商管理</span></el-menu-item>
          <el-menu-item index="/base/goods"><el-icon><GoodsFilled /></el-icon><span>商品资料管理</span></el-menu-item>
          <el-menu-item index="/business/stock-warning"><el-icon><WarningFilled /></el-icon><span>预警中心</span></el-menu-item>
          <el-menu-item index="/system/void-approval"><el-icon><DocumentChecked /></el-icon><span>作废审批</span></el-menu-item>
          <el-sub-menu index="/notification">
            <template #title><el-icon><Promotion /></el-icon><span class="menu-title-text">发布</span></template>
            <el-menu-item index="/system/work-requirement"><el-icon><Tickets /></el-icon><span>工作要求</span></el-menu-item>
            <el-menu-item index="/system/notice"><el-icon><Bell /></el-icon><span>公告管理</span></el-menu-item>
          </el-sub-menu>
          <el-menu-item index="/system/user"><el-icon><UserFilled /></el-icon><span>用户部门管理</span></el-menu-item>
        </template>

        <template v-else-if="isFinanceAdmin">
          <el-menu-item index="/business/sales-chart"><el-icon><DataAnalysis /></el-icon><span>销售统计图表</span></el-menu-item>
          <el-sub-menu index="/notification">
            <template #title><el-icon><Promotion /></el-icon><span class="menu-title-text">发布</span></template>
            <el-menu-item index="/system/work-requirement"><el-icon><Tickets /></el-icon><span>工作要求</span></el-menu-item>
            <el-menu-item index="/system/notice"><el-icon><Bell /></el-icon><span>公告管理</span></el-menu-item>
          </el-sub-menu>
          <el-menu-item index="/system/user"><el-icon><UserFilled /></el-icon><span>用户部门管理</span></el-menu-item>
        </template>

        <template v-else-if="showSuperAdminCenter">
          <el-sub-menu index="/system/super-admin-center">
            <template #title>
              <el-icon><Monitor /></el-icon>
              <span class="submenu-title-link" @click="goSuperAdminOverview">超管中心</span>
            </template>
            <el-menu-item index="/system/super-admin"><el-icon><Monitor /></el-icon><span>超管总览</span></el-menu-item>
            <el-menu-item index="/system/security-ip-policy"><el-icon><Lock /></el-icon><span>安全策略</span></el-menu-item>
            <el-menu-item index="/system/login-log"><el-icon><Notebook /></el-icon><span>登录日志</span></el-menu-item>
            <el-menu-item index="/system/operation-log"><el-icon><Document /></el-icon><span>操作日志</span></el-menu-item>
            <el-menu-item index="/system/dept-approval"><el-icon><Stamp /></el-icon><span>部门审批</span></el-menu-item>
          </el-sub-menu>
          <el-menu-item index="/system/user"><el-icon><UserFilled /></el-icon><span>用户管理</span></el-menu-item>
          <el-menu-item index="/system/notice"><el-icon><Bell /></el-icon><span>公告管理</span></el-menu-item>
        </template>
      </el-menu>
    </el-aside>
    <el-container class="content-container">
      <el-header class="app-header">
        <div class="header-left">
          <el-button
            v-if="showSidebar"
            text
            class="sidebar-toggle"
            :aria-label="isSidebarCollapsed ? '展开菜单栏' : '收起菜单栏'"
            @click="toggleSidebar"
          >
            <el-icon>
              <component :is="isSidebarCollapsed ? Expand : Fold" />
            </el-icon>
          </el-button>
          <div class="header-title">后台数据管理系统</div>
        </div>
        <div class="header-actions">
          <MessageCenter v-if="showMessageCenter" />
          <el-button type="danger" text @click="handleLogout"><el-icon><SwitchButton /></el-icon>退出登录</el-button>
        </div>
      </el-header>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { useRoute, useRouter } from 'vue-router'
import { computed, ref } from 'vue'
import MessageCenter from '@/components/MessageCenter.vue'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import { isAdminRole, isEmployeeRole, isSuperAdmin, normalizeDeptCode } from '@/utils/auth'
import { logoutAPI } from '@/api/user'
import {
  HomeFilled, OfficeBuilding, User, PieChart, Promotion, Tickets, Bell, UserFilled,
  ShoppingCart, RefreshLeft, Sell, RefreshRight, WarningFilled, Van, GoodsFilled,
  DocumentChecked, DataAnalysis, Monitor, Lock, Notebook, Document, Stamp, SwitchButton,
  Fold, Expand
} from '@element-plus/icons-vue'

const SIDEBAR_COLLAPSE_STORAGE_KEY = 'layout-sidebar-collapsed'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const isSidebarCollapsed = ref(localStorage.getItem(SIDEBAR_COLLAPSE_STORAGE_KEY) === '1')

const currentDeptCode = computed(() => normalizeDeptCode(userStore.deptCode))
const isDeptAdminRole = computed(() => isAdminRole(userStore.role))
const isEmployee = computed(() => isEmployeeRole(userStore.role))
const showSidebar = computed(() => !isEmployee.value)
const sidebarWidth = computed(() => (isSidebarCollapsed.value ? '64px' : '220px'))
const showSuperAdminCenter = computed(() => isSuperAdmin(userStore.role))
const showMessageCenter = computed(() => !showSuperAdminCenter.value)
const defaultOpeneds = computed(() => {
  if (!showSuperAdminCenter.value) {
    return []
  }

  return route.path.startsWith('/system/super-admin')
    || route.path === '/system/security-ip-policy'
    || route.path === '/system/login-log'
    || route.path === '/system/operation-log'
    || route.path === '/system/dept-approval'
    ? ['/system/super-admin-center']
    : []
})
const isFinanceAdmin = computed(() => isDeptAdminRole.value && currentDeptCode.value === 'finance')
const isSalesAdmin = computed(() => isDeptAdminRole.value && currentDeptCode.value === 'sales')
const isWarehouseAdmin = computed(() => isDeptAdminRole.value && currentDeptCode.value === 'warehouse')
const isPurchaseAdmin = computed(() => isDeptAdminRole.value && currentDeptCode.value === 'purchase')
const isHrAdmin = computed(() => isDeptAdminRole.value && currentDeptCode.value === 'hr')

const toggleSidebar = () => {
  if (!showSidebar.value) {
    return
  }

  isSidebarCollapsed.value = !isSidebarCollapsed.value
  localStorage.setItem(SIDEBAR_COLLAPSE_STORAGE_KEY, isSidebarCollapsed.value ? '1' : '0')
}

const goSuperAdminOverview = () => {
  if (route.path !== '/system/super-admin') {
    router.push('/system/super-admin')
  }
}

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
  background: #f0f2f5;
}

.app-aside {
  overflow: hidden;
  background-color: #304156;
  color: white;
  transition: width 0.2s ease;
}

.brand-block {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 56px;
  padding: 12px 10px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.brand-block--collapsed {
  padding-inline: 6px;
}

.brand-title {
  margin: 0;
  color: white;
  font-size: 18px;
  font-weight: 700;
  letter-spacing: 1px;
  white-space: nowrap;
}

.brand-block--collapsed .brand-title {
  font-size: 14px;
  letter-spacing: 0.5px;
}

.app-menu {
  min-height: calc(100vh - 56px);
}

.content-container {
  min-width: 0;
}

.app-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  padding: 0 20px 0 12px;
  border-bottom: 1px solid #eaeaea;
  background: #fff;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.header-title {
  min-width: 0;
  font-weight: bold;
  color: #1f2937;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.sidebar-toggle {
  font-size: 18px;
  color: #1f2937;
}

.sidebar-toggle:hover {
  background: #f3f4f6;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.el-menu {
  border-right: none;
}

.submenu-title-link {
  display: inline-flex;
  align-items: center;
  width: 100%;
}

:deep(.el-menu--collapse .el-sub-menu__title .menu-title-text),
:deep(.el-menu--collapse .el-menu-item span),
:deep(.el-menu--collapse .el-sub-menu__title .submenu-title-link) {
  display: none;
}

@media screen and (max-width: 768px) {
  .app-header {
    padding-right: 12px;
  }

  .header-actions {
    gap: 6px;
  }
}
</style>