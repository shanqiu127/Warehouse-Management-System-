# Warehouse Management System

一个基于前后端分离架构的仓库管理系统，覆盖认证授权、系统管理、基础资料、进销退存业务与销售统计分析。

## 项目亮点

- 前后端分离：Vue 3 + Spring Boot + MySQL
- 角色与部门双维权限模型：支持超级管理员、部门管理员、部门员工三大类角色，并通过部门归属细分权限边界。
- 部门化工作台：人事、采购、销售、仓储、财务五类管理员拥有各自独立菜单、首页指标与业务边界
- 核心业务：进货、进货退货、销售、销售退货、库存预警
- 退货闭环能力：进货退货/销售退货均支持来源单关联与可退数量校验
- 历史单据治理：当天单据可直接删除，历史单据支持“仅作废”与“作废并红冲”双路径，兼顾审计追溯与业务可控
- 作废审批流：采购/销售历史单据的作废与作废红冲需提交仓储审批，由仓储管理员审批通过后执行
- 审批状态透出：业务列表可直接显示已删除、待审批、驳回、作废成功、作废并红冲成功等状态
- 公告分发模型：支持管理员公告、全员公告、部门员工公告，公告可见范围与账号所属部门自动联动
- 预警中心共享访问：仓储、采购、销售管理员共用库存预警中心，但按权限收口筛选项与操作入口
- 安全治理闭环：IP 白名单策略 + 登录日志 + 操作审计日志，关键访问与关键动作可追踪
- 超级管理员中心能力：提供总览、安全策略、登录日志、操作日志四个治理入口，聚焦系统治理而非业务操作
- 权限体验完善：独立 403 无权限页 + 路由拦截提示，访问反馈更清晰
- 数据可视化：销售概览、Top5、品牌占比、趋势图
- 业务毛利看板：仅财务部门管理员可访问，支持销售额、成本、毛利、毛利率总览，含品牌毛利 Top 与毛利日趋势
- 成本口径治理：已落地成本快照字段与快照聚合口径，减少历史价格变动造成的统计漂移
- 管理可观测性：角色首页按职责展示公告、库存预警、系统错误、数据库状态等关键摘要，便于快速定位问题
- 接口文档：内置 OpenAPI + Knife4j



## 在线演示

- 演示网站：https://wmsfront.pages.dev/
- 演示网站体验可能不够完整，建议本地部署后体验完整功能。


## 默认账号（可按需修改）

- `superadmin`：超级管理员，聚焦系统治理与安全审计管理
- `hr_admin`：人事管理员
- `purchase_admin`：采购管理员
- `sales_admin`：销售管理员
- `warehouse_admin`：仓储管理员
- `finance_admin`：财务管理员
- `*_employee`：对应部门员工账号

默认密码:123456



## 技术栈

- 前端：Vue 3、Vite、Element Plus、Pinia、Vue Router、Axios、ECharts
- 后端：Spring Boot 3.3.5、MyBatis-Plus 3.5.5、Sa-Token 1.37.0
- 数据库：MySQL 8.0
- 运行环境：JDK 17、Node.js 16+

## 环境准备

- JDK 17：后端基于 Spring Boot 3，必须有 Java 17 环境。
- Maven（MVN）：用于编译和启动后端。项目自带 Maven Wrapper（`mvnw` / `mvnw.cmd`）(即使未全局安装 Maven 也通常可以直接运行)。
- Node.js（建议 18+）和 npm：前端基于 Vue 3 + Vite，需要用 npm 安装依赖并启动前端。
- MySQL 8.0：项目数据存储在 MySQL，需先执行初始化 SQL 脚本。

## 注意事项
### 必须更改：
- back/src/main/resources/application.properties 中数据库密码请按本机环境修改。
### 可选更改：
- db.sql 中初始化的默认账号和密码（当前为 `superadmin`、`hr_admin`、`purchase_admin`、`sales_admin`、`warehouse_admin`、`finance_admin` 及各部门 `*_employee`，默认密码均为 `123456`）可按需调整。（应用于登录系统时的默认账号密码）
- back/src/main/java/org/example/back/service/UserManageService.java 中的默认密码为 `123456`。（应用于新建用户时的默认密码）
- 这两个是不一样的，前者是数据库初始化时的默认账号密码，后者是通过用户管理界面新建用户时的默认密码。


## 目录结构

```text
.
├─front/                 # Vue 前端
├─back/                  # Spring Boot 后端
└─db.sql                 # 数据库初始化脚本
```

## 快速开始

### 1. 拉取项目

```bash
git clone <你的仓库地址>
cd "Warehouse Management System"
```


### 2. 初始化数据库

1. 创建数据库（建议字符集 utf8mb4）。
2. 执行根目录数据库脚本：`db.sql`。
3. `db.sql` 已包含毛利看板所需成本快照字段与回填逻辑，可直接用于全新环境初始化。



### 3. 启动后端

```bash
cd back
.\mvnw.cmd -DskipTests compile
.\mvnw.cmd spring-boot:run
```

后端默认地址：`http://localhost:8080`

### 4. 启动前端

```bash
cd front
npm install
npm run dev
```

前端默认地址：`http://localhost:5173`



## 接口文档

- Knife4j：`http://localhost:8080/api/doc.html#/home`
- Swagger UI：`http://localhost:8080/api/swagger-ui/index.html`

---

如果这个项目对你有帮助，欢迎 Star。
