# 企业协同运营管理系统项目目录结构

## 1. 项目概览

本项目采用前后端分离结构：
- front：Vue 3 前端工程
- back：Spring Boot 后端工程
- 根目录：数据库脚本、README 与 doxc 文档目录；当前已新增项目 AI 助手专题文档，用于沉淀模型、能力边界与落地方案。


### 1.1 当前技术栈与版本

| 层 | 技术 |
|---|---|
| 前端 | Vue 3 + Vite + Element Plus + Pinia + Vue Router + Axios + ECharts |
| 后端 | Spring Boot 3.3.5 + MyBatis-Plus 3.5.5 + Sa-Token 1.37.0 |
| 数据库 | MySQL 8.0 |
| 运行环境 | JDK 17、Node.js 16+（建议 18+） |

### 1.2 当前角色模型

- superadmin：超级管理员，负责系统治理、安全审计、全局公告与用户管理。
- admin：部门管理员，仍使用统一 `admin` 角色值，但通过 `dept_id/dept_code` 区分为财务、销售、仓储、采购、人事五类管理员。
- employee：部门员工，仍使用统一 `employee` 角色值，但同样绑定部门归属。

### 1.3 2026-04-06 当前版本主题

- 在既有部门化权限与审批体系之上，新增“工作要求”模块，挂在管理员侧“通知”父菜单下。
- 员工通过首页进入工作要求详情，形成“接受/拒收 -> 执行 -> 提交 -> 审核”的闭环。
- 工作要求超时能力已正式落库：超时作为独立维度，不改变原主状态机。
- 首页提醒体系已升级为会话级提醒：员工待接受/超时提醒、管理员待审核/超时提醒、仓储审批提醒。
- 在业务系统内部补齐“项目 AI 助手”模块，形成“项目知识问答 + 多模型路由 + 历史会话 + 审计留痕”的辅助能力。

## 2. 当前目录结构（精简主干）

- Enterprise Collaboration Operations System (ECS)/
- db.sql
- README.md
- back/
	- pom.xml
	- mvnw / mvnw.cmd
	- src/main/java/org/example/back/
		- common/
		- config/
		- controller/
			- FileUploadController.java
			- ProjectAssistantController.java
			- AiConversationController.java
			- WorkRequirementController.java
		- dto/
		- entity/
		- mapper/
		- scheduler/
			- WorkRequirementOverdueScheduler.java
		- service/
			- AuthzService.java
			- ProjectAssistantService.java
			- ProjectKnowledgeBaseService.java
			- ProjectKnowledgeRetrievalService.java
		- llm/service/
			- LlmRoutingService.java
			- LlmGovernanceService.java
			- LlmAuditService.java
		- vo/
	- src/main/resources/
	- src/test/java/
	- target/（后端构建输出）
- front/
	- package.json
	- vite.config.js
	- src/
		- api/
		- assets/
		- components/
			- assistant/
				- AssistantLauncher.vue
			- MessageCenter.vue
		- layout/
		- router/
		- stores/
		- utils/
			- auth.js
			- permission.js
			- bizDocumentState.js
		- views/
			- HomeView.vue
			- LoginView.vue
			- RegisterView.vue
			- WorkRequirementDetailView.vue
			- home/components/
				- AdminHome.vue
				- EmployeeHome.vue
				- SuperAdminHome.vue
			- base/
				- SupplierView.vue
				- GoodsView.vue
			- business/
				- PurchaseView.vue
				- PurchaseReturnView.vue
				- SalesView.vue
				- SalesReturnView.vue
				- SalesChartView.vue
				- StockWarningView.vue
			- system/
				- NoticeView.vue
				- ProjectAssistantView.vue
				- UserView.vue
				- DeptView.vue
				- EmployeeView.vue
				- WorkRequirementView.vue
				- VoidApprovalView.vue
	- dist/（前端构建输出）
	- node_modules/（前端依赖）
- doxc/
	- plan.md
	- TUDO.md
	- log.md
	- front.md
	- back.md
	- project.md
	- bug.md
	- idea.md
	- later.md

## 3. 目录职责说明

### 3.1 front
- 承载页面、路由、菜单、按钮权限、登录态持久化与后端接口联调。
- 当前最关键的前端权限文件为 `router/index.js`、`layout/index.vue`、`utils/auth.js`、`utils/permission.js`。
- 当前最关键的前端业务状态文件为 `utils/bizDocumentState.js`，用于统一解释删除与审批结果。
- 当前新增的通知与任务入口包括 `views/system/WorkRequirementView.vue`、`views/WorkRequirementDetailView.vue`、`components/MessageCenter.vue`，以及首页提醒实现 `views/home/components/AdminHome.vue`、`EmployeeHome.vue`。
- 当前新增的 AI 助手入口包括独立页面 `views/system/ProjectAssistantView.vue` 与首页悬浮入口 `components/assistant/AssistantLauncher.vue`，承担项目内问答与历史会话体验。

### 3.2 back
- 承载认证授权、部门权限边界、公告分发、业务流程、库存联动、审批流、统计聚合与异常处理。
- 当前最关键的权限内核文件为 `AuthService`、`AuthzService`、`RequireAdminAspect`、`StpInterfaceImpl`。
- 当前新增的工作要求后端主链路包括 `WorkRequirementController`、`WorkRequirementService`、`WorkRequirementOverdueScheduler`、`FileUploadController` 与消息扩展 `MessageService`。
- 当前新增的 AI 助手后端主链路包括 `ProjectAssistantController`、`AiConversationController`、`ProjectAssistantService`、`ProjectKnowledgeBaseService`、`ProjectKnowledgeRetrievalService` 以及 LLM 路由/治理/审计服务。

### 3.3 根目录与 doxc
- `db.sql` 是当前唯一数据库初始化脚本，已固化 5 部门、11 账号、公告样例、审批相关表结构，以及工作要求/工作要求分配/工作要求附件与超时字段。
- `log.md` 记录按时间归档的开发日志。

## 4. 实现要点

### 4.1 数据与权限模型
- 后端统一前缀仍为 `/api`。
- `sys_user` 已增加 `dept_id`，admin 与 employee 都必须绑定部门，superadmin 允许为空。
- `sys_notice` 已增加 `target_role` 与 `target_dept_id`，公告分发模型从单一状态升级为受众模型。
- 登录态和前端本地存储都已统一承载 `role + deptId + deptCode + deptName`。
- 工作要求模块新增三张表：`work_requirement`、`work_requirement_assign`、`work_requirement_attachment`。
- 工作要求超时采用独立维度建模：`overdue_flag`、`overdue_at`、`submitted_on_time`、`overdue_remind_count`、`last_remind_time`、`completed_at`。

### 4.2 当前菜单与页面口径
- superadmin：仅首页、公告管理、用户管理、超管总览、项目 AI 助手、安全审计。
- hr admin：首页、全部门管理、全员工管理、通知（工作要求/公告管理）、项目 AI 助手、用户部门管理。
- purchase admin：首页、商品进货、进货退货、预警中心、通知（工作要求/公告管理）、项目 AI 助手、用户部门管理。
- sales admin：首页、商品销售、销售退货、预警中心、通知（工作要求/公告管理）、项目 AI 助手、用户部门管理。
- warehouse admin：首页、供应商管理、商品资料管理、预警中心、作废审批、通知（工作要求/公告管理）、项目 AI 助手、用户部门管理。
- finance admin：首页、销售统计图表、通知（工作要求/公告管理）、项目 AI 助手、用户部门管理。
- employee：当前不再显示左侧菜单，直接使用单页员工工作台，承载档案、联系方式、部门联络信息、工作要求、公告、提醒卡片与项目 AI 助手入口。

### 4.3 当前业务与审批口径
- 进货退货必须关联来源进货单，销售退货必须关联来源销售单。
- 当天单据可删除，历史单据只能提交作废/作废并红冲审批。
- 历史采购/销售及其退货单的作废审批由采购或销售管理员提交，仓储管理员审批执行。
- 四类单据返回值已统一附带删除状态与审批状态，前端当前在操作列展示彩色结果文本。
- 工作要求主状态机为：待接受、执行中、待审核、已完成、拒收、已驳回。
- 工作要求超时不改变主状态；若超过截止时间仍未接受或未提交，则额外标记为“超时中”；若截止后提交，则标记“逾期提交/逾期完成”。
- 工作要求执行结果支持文本 + 本地图片上传；附件只保存路径，不保存二进制内容。

### 4.4 当前预警中心口径
- 仓储、采购、销售管理员都可访问预警中心。
- 商品资料与供应商资料管理仍仅仓储管理员可访问。
- 为避免无关提示，销售与采购进入预警中心时不再加载供应商下拉接口。

### 4.5 当前提醒与消息口径
- 员工首页：待接受任务提醒、超时任务提醒。
- 管理员首页：工作要求待审核提醒、工作要求超时提醒；仓储管理员额外有作废/红冲审批提醒。
- 所有首页提醒关闭都按当前 token 写入 `sessionStorage`，仅对当前登录会话生效。
- 站内邮箱已对所有登录用户开放，工作要求超时通过消息中心统一接收。

### 4.6 当前 AI 助手口径
- 当前 AI 助手定位为“项目内助手”而非通用聊天工具，答案范围聚焦于系统角色职责、业务流程、页面入口、审批规则、模块说明等项目知识。
- superadmin 与部门管理员可在前端选择模型；employee 走默认模型策略，不开放管理侧模型控制。
- 后端当前启用的主模型与回退模型体系已切换到通义千问、智谱 GLM、Kimi、DeepSeek，不再保留豆包作为当前文档口径。
- 历史会话、消息记录与模型调用日志已经形成基础闭环，但 `ai_message` 当前不存 `latencyMs`，`ai_model_call_log.assistantMessageId` 仍处于预留未回填状态。



## 5. 快速启动与联调入口

### 5.1 后端启动
1. 进入 `back` 目录。
2. 编译：`mvnw.cmd -q -DskipTests compile`
3. 测试：`mvnw.cmd -q test`
4. 启动：`mvnw.cmd spring-boot:run`

### 5.2 前端启动
1. 进入 `front` 目录。
2. 安装依赖：`npm install`
3. 开发模式：`npm run dev`
4. 构建打包：`npm run build`

### 5.3 数据库初始化
1. 新库执行根目录 `db.sql`。
2. 初始化后可直接使用 固化的 11 个默认账号，默认密码均为 `123456`。

### 5.4 调试入口
- Knife4j：`http://localhost:8080/api/doc.html`
- OpenAPI：`http://localhost:8080/api/v3/api-docs`

## 6. 核心业务链路（简要）

- 认证链路：登录/注册 -> Session 写入部门上下文 -> 前端持久化权限上下文 -> 路由守卫与接口鉴权。
- 员工首页链路：登录进入 `/home` -> `HomeView` 分发 `EmployeeHome` -> 调用 `/api/home/employee-workbench` 聚合员工工作台，再并行加载公告分页与详情。
- 工作要求链路：管理员进入“通知 -> 工作要求”发布任务 -> 后端按员工生成分配记录 -> 员工从首页卡片或提醒进入详情页接受/执行/提交 -> 管理员审核通过或驳回。
- 工作要求超时链路：截止时间到达后由 `WorkRequirementOverdueScheduler` 定时扫描 -> 写入超时字段 -> 站内邮箱通知员工与部门管理员 -> 前端列表/详情/首页提醒统一显示超时状态。
- AI 助手链路：前端页面或悬浮入口发起提问 -> 后端执行问题分类、知识召回、角色约束与提示词拼装 -> `LlmRoutingService` 选择实际模型并按需回退 -> 返回答案并写入会话与审计日志。
- 系统管理链路：用户、部门、员工、公告按“角色 + 部门”边界访问与管理。
- 基础资料链路：仓储管理员维护供应商与商品主数据，采购/销售共享预警中心只读能力。
- 业务链路：进货、进货退货、销售、销售退货统一遵循“当天可删、历史走审批”的规则。
- 审批链路：采购/销售管理员提交历史作废申请 -> 仓储管理员审批 -> 结果回流业务单据列表。
- 统计链路：销售统计与毛利统计由财务管理员消费。

