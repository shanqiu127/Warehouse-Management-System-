import { createRouter, createWebHistory } from "vue-router"
import { getRole, getToken, hasRole } from "@/utils/auth"

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: "/login",
      name: "Login",
      component: () => import("../views/LoginView.vue")
    },
    {
      path: "/register",
      name: "Register",
      component: () => import("../views/RegisterView.vue")
    },
    {
      path: "/",
      component: () => import("../layout/index.vue"),
      redirect: "/home",
      children: [
        {
          path: "home",
          name: "Home",
          component: () => import("../views/HomeView.vue")
        },
        // 基础资料页面 (由于普通员工只能查看，故不使用角色锁，在页面中利用 v-permission 控制操作按钮即可)
        {
          path: "base/supplier",
          name: "BaseSupplier",
          component: () => import("../views/base/SupplierView.vue")
        },
        {
          path: "base/goods",
          name: "BaseGoods",
          component: () => import("../views/base/GoodsView.vue")
        },
        // 业务部分页面
        {
          path: "business/purchase",
          name: "BusinessPurchase",
          component: () => import("../views/business/PurchaseView.vue")
        },
        {
          path: "business/purchase-return",
          name: "BusinessPurchaseReturn",
          component: () => import("../views/business/PurchaseReturnView.vue")
        },
        {
          path: "business/sales",
          name: "BusinessSales",
          component: () => import("../views/business/SalesView.vue")
        },
        {
          path: "business/sales-return",
          name: "BusinessSalesReturn",
          component: () => import("../views/business/SalesReturnView.vue")
        },
        // 统计图表页面（仅管理员可用）
        {
          path: "business/sales-chart",
          name: "BusinessSalesChart",
          component: () => import("../views/business/SalesChartView.vue"),
          meta: { roles: ['admin', 'superadmin'] } 
        },
        // 以下为动态权限测试页面（仅管理员可用）
        {
          path: "system/notice",
          name: "SystemNotice",
          component: () => import("../views/system/NoticeView.vue"),
          meta: { roles: ['admin', 'superadmin'] } 
        },
        {
          path: "system/user",
          name: "SystemUser",
          component: () => import("../views/system/UserView.vue"),
          meta: { roles: ['admin', 'superadmin'] } 
        },
        {
          path: "system/dept",
          name: "SystemDept",
          component: () => import("../views/system/DeptView.vue"),
          meta: { roles: ['admin', 'superadmin'] } 
        },
        {
          path: "system/employee",
          name: "SystemEmployee",
          component: () => import("../views/system/EmployeeView.vue"),
          meta: { roles: ['admin', 'superadmin'] } 
        }
      ]
    },
    // 将无权限页面重定向
    {
      path: "/403",
      name: "Forbidden",
      component: () => import("../views/HomeView.vue") // 暂时使用首页替代演示，或者你可以单独建一个 403 页面
    }
  ]
})

// 添加前置路由全局守卫：处理登录拦截与角色权限
router.beforeEach((to, from, next) => {
  const token = getToken()
  const role = getRole()

  if (to.path !== '/login' && to.path !== '/register') {
    if (!token) {
      // 1. 未登录拦截至登录页
      return next('/login')
    } else {
      // 2. 鉴权：页面是否有角色限制
      if (to.meta && to.meta.roles) {
        const allowRoles = to.meta.roles
        if (!hasRole(role, allowRoles)) {
          // 角色不匹配，无权限访问该页面
          return next('/403') // 实际开发可结合 ElMessage 提示并跳转无权限页
        }
      }
      return next()
    }
  } else {
    next()
  }
})

export default router