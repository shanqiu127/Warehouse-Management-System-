# Warehouse Management System

一个基于前后端分离架构的仓库管理系统，覆盖认证授权、系统管理、基础资料、进销退存业务与销售统计分析。

## 项目亮点

这是一个面向多部门协作场景的仓库管理系统，核心特色是角色 + 部门双维权限、历史单据审批流、公告定向分发和安全审计闭环。

| 维度 | 亮点 | 价值 |
|---|---|---|
| 权限模型 | 角色 + 部门双维权限控制 | 不再停留在简单管理员/员工二分，更接近真实企业组织结构 |
| 部门协同 | 人事、采购、销售、仓储、财务拥有独立工作台 | 不同部门看到不同菜单、首页指标和业务入口 |
| 历史单据治理 | 历史单据不能直接处理，需提交仓储审批后作废或红冲 | 审计追溯更完整，业务风险更可控 |
| 信息投放 | 公告支持管理员、全员、部门员工定向分发 | 信息按角色和部门自动收口，避免越权可见 |
| 经营分析 | 销售统计和毛利看板分层开放 | 支持经营分析，同时保证财务数据边界 |
| 安全治理 | IP 策略、登录日志、操作日志、超管总览形成闭环 | 兼顾业务管理与系统治理能力 |

### 历史单据审批流速览

```mermaid
flowchart LR
	A[采购管理员 / 销售管理员] --> B[提交历史单据作废或红冲申请]
	B --> C[仓储管理员审批]
	C -->|通过| D[执行作废或作废并红冲]
	C -->|驳回| E[保留原单据并回写驳回状态]
	D --> F[业务列表展示审批结果]
	E --> F
```

### 角色权限矩阵速览

| 身份 | 代表账号 | 权限范围 | 主要入口 |
|---|---|---|---|
| 超级管理员 | `superadmin` | 系统治理与安全审计 | 首页、公告管理、用户管理、超管总览、安全策略、登录日志、操作日志 |
| 人事管理员 | `hr_admin` | 人事与组织管理 | 首页、部门管理、员工管理、公告管理、用户部门管理 |
| 采购管理员 | `purchase_admin` | 采购业务与库存协同 | 首页、商品进货、进货退货、预警中心、公告管理、用户部门管理 |
| 销售管理员 | `sales_admin` | 销售业务与库存协同 | 首页、商品销售、销售退货、预警中心、公告管理、用户部门管理 |
| 仓储管理员 | `warehouse_admin` | 仓储资料与审批中心 | 首页、供应商管理、商品资料管理、预警中心、作废审批、公告管理、用户部门管理 |
| 财务管理员 | `finance_admin` | 经营分析与报表查看 | 首页、销售统计图表、公告管理、用户部门管理 |
| 部门员工 | `*_employee` | 本部门公告查看 | 统一仅保留首页，公告按所属部门可见范围展示 |

### 角色层级关系图

```mermaid
flowchart TD
	Root[仓库管理系统权限模型]
	Root --> SA[超级管理员]
	Root --> DA[部门管理员]
	Root --> DE[部门员工]

	DA --> HR[人事]
	DA --> Purchase[采购]
	DA --> Sales[销售]
	DA --> Warehouse[仓储]
	DA --> Finance[财务]

	SA --> SA1[系统治理]
	SA --> SA2[安全审计]
	DE --> DE1[首页]
	DE --> DE2[部门公告]
```



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
