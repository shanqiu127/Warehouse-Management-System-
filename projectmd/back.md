# 仓库管理系统后端总结（back）

## 一、项目结构与技术栈

| 维度 | 内容 |
|---|---|
| 技术栈 | JDK 17、Spring Boot 3.3.5、MyBatis-Plus 3.5.5、Sa-Token 1.37.0 |
| 接口规范 | 统一返回 `{code, msg, data}`，上下文前缀 `/api` |
| 核心目录 | `controller`、`service`、`mapper`、`entity`、`dto`、`vo`、`common`、`config` |
| 认证约定 | `Authorization: Bearer token` |
| 文档能力 | OpenAPI + Knife4j (`/api/doc.html`) |

## 二、当前阶段概述

### 1.  版本定位
- `role` 仍保持 `superadmin/admin/employee` 三值语义。
- 部门差异由 `dept_id + dept_code` 承载。
- 公告分发由单状态模型升级为“受众角色 + 目标部门”双维度模型。
- 历史作废/作废并红冲从前端直接执行，收口为采购/销售提交、仓储审批执行的业务流。

### 2. 当前后端范围
- 已覆盖：认证授权、部门化权限、公告分发、系统管理 CRUD、基础资料 CRUD、四类业务单据、库存预警、审批流、统计图表、首页汇总、员工工作台聚合与员工联系方式自助维护、工作要求模块、图片上传与工作要求超时提醒、项目 AI 助手、多模型路由、知识库检索与会话留痕。


## 三、当前后端实现（核心）

| 模块 | 当前能力 | 关键文件 | 说明 |
|---|---|---|---|
| 数据模型 | `sys_user` 增加 `dept_id`，`sys_notice` 增加 `target_role/target_dept_id` | `db.sql`、`SysUser`、`SysNotice` | 角色不扩枚举，部门能力通过字段建模承载 |
| 初始化数据 | 固定 5 个部门、11 个账号、3 类公告样例 | `db.sql` | superadmin + 5 个部门管理员 + 5 个部门员工 |
| 登录态上下文 | Session 写入 `role/deptId/deptCode`，`/auth/userinfo` 返回部门信息 | `AuthService`、`LoginResponse` | 为 Service 层部门判断与前端权限矩阵提供统一上下文 |
| 权限抽象 | 提供当前用户、部门管理员、同部门访问、超管访问等统一能力 | `AuthzService` | 取代散落的角色字符串判断 |
| 员工工作台 | `employee` 角色可获取聚合首页数据并保存联系方式 | `HomeController`、`HomeService`、`EmployeeWorkbenchVO`、`EmployeeContactUpdateDTO` | 聚合 summary/profile/deptContact/workRequirements，并同步更新用户表与员工表 |
| 工作要求主链路 | 管理员发布工作要求、员工接受/拒收/提交、管理员审核通过或驳回 | `WorkRequirementController`、`WorkRequirementService`、`WorkRequirement*DTO/VO` | 已形成“通知 -> 工作要求”完整流程，按部门隔离数据范围 |
| 工作要求数据模型 | 主表、员工分配表、附件表及超时字段 | `db.sql`、`WorkRequirement`、`WorkRequirementAssign`、`WorkRequirementAttachment` | 超时不进入主状态机，而是通过 `overdue_flag / submitted_on_time` 等字段独立建模 |
| 文件上传 | 支持员工提交执行结果时上传图片，路径落库存储 | `FileUploadController`、`WebConfig`、`application.properties` | 文件写入本地磁盘，数据库仅存相对路径，静态资源通过 `/uploads/**` 映射访问 |
| AI 助手主链路 | 提供提问、知识检索、角色收口、模型选择与自动回退 | `ProjectAssistantController`、`ProjectAssistantService`、`ProjectKnowledgeBaseService`、`ProjectKnowledgeRetrievalService` | 问答先做问题分类与知识召回，再拼装提示词调用大模型，最终以项目口径返回答案 |
| AI 会话与审计 | 保存会话、消息、模型调用日志与治理结果 | `AiConversationController`、`AiConversationService`、`LlmRoutingService`、`LlmGovernanceService`、`LlmAuditService` | 已记录 `providerCode/modelCode/fallbackUsed` 与模型调用日志，`latencyMs` 当前落在审计日志而非 `ai_message` |
| 站内消息 | 已登录用户统一使用站内邮箱；工作要求超时可推送给员工与部门管理员 | `MessageController`、`MessageService`、`sys_message` | 在既有消息中心基础上扩展工作要求超时提醒 |
| 超时定时任务 | 每 10 分钟扫描一次已过截止时间且未完成的工作要求 | `WorkRequirementOverdueScheduler` | 首次超时时落库、计数并发送站内消息 |
| 注册链路 | 支持公开部门下拉与带部门注册 | `AuthController`、`DeptService`、`RegisterRequest`、`SaTokenConfig` | 修复注册页无部门数据问题 |
| 公告分发 | superadmin 可发管理员/全员公告；部门管理员仅发本部门员工公告 | `NoticeService`、`Notice*DTO/VO` | 公告查询、详情、管理均按可见范围过滤，并已修复全员公告空部门 ID 的分页空指针 |
| 用户与组织管理 | superadmin 管理全量，部门管理员仅能管理本部门员工账号 | `UserManageService`、`DeptService`、`EmployeeService` | 用户/部门/员工接口都已收口到部门边界 |
| 基础资料 | 供应商、商品资料管理仅仓储管理员可访问；预警中心对仓储/采购/销售管理员开放 | `SupplierService`、`GoodsService` | warning-only 场景单独放开访问范围 |
| 业务单据 | 采购/销售与对应退货模块只允许所属部门管理员访问 | `PurchaseService`、`PurchaseReturnService`、`SalesService`、`SalesReturnService` | 页面和服务边界已对齐 |
| 作废审批流 | 采购/销售管理员可提交历史单据作废申请，仓储管理员审批执行 | `ApprovalService` | 提交端与审批端部门职责已明确分离 |
| 审批状态回传 | 四类单据 VO 统一回传 `isDeleted/approvalStatus/approvalRequestAction` | `PurchaseVO`、`PurchaseReturnVO`、`SalesVO`、`SalesReturnVO` | 前端可直接显示已删除、待审批、驳回、作废成功等状态 |
| 统计图表 | 销售图表按财务部门收口，毛利分析仅财务管理员可访问 | `SalesChartService` | 与前端财务菜单与毛利入口保持一致 |

## 四、当前接口与业务口径

### 1. 认证与部门上下文
- `/api/auth/login`
- `/api/auth/register`
- `/api/auth/userinfo`
- `/api/auth/logout`
- `/api/auth/check`
- `/api/auth/depts`

### 2. 系统管理
- `/api/system/notices/*`
- `/api/system/users/*`
- `/api/system/depts/*`
- `/api/system/employees/*`
- `/api/system/approval-orders/*`
- `/api/system/work-requirements/*`
- `/api/system/messages/*`

### 3. 基础资料与业务模块
- `/api/base/suppliers/*`
- `/api/base/goods/*`
- `/api/business/purchases/*`
- `/api/business/purchase-returns/*`
- `/api/business/sales/*`
- `/api/business/sales-returns/*`
- `/api/business/purchases/options/returnable`
- `/api/business/sales/options/returnable`
- `/api/upload/image`

### 4. 图表与首页
- `/api/business/charts/overview`
- `/api/business/charts/top5`
- `/api/business/charts/brand-ratio`
- `/api/business/charts/daily-trend`
- `/api/business/charts/profit-overview`
- `/api/business/charts/profit-brand-top`
- `/api/business/charts/profit-daily-trend`
- `/api/home/summary`
- `/api/home/employee-workbench`
- `/api/home/employee-workbench/contact`
- `/api/home/work-requirements/*`

### 5. AI 助手接口
- `/api/assistant/project/ask`
- `/api/assistant/project/models`
- `/api/assistant/project/conversations`
- `/api/assistant/project/conversations/{conversationId}/messages`
- `/api/assistant/project/conversations/{conversationId}`

## 五、角色与权限说明（后端口径）

| 角色 | 当前能力 | 关键约束 |
|---|---|---|
| superadmin | 全局系统治理、公告管理、用户管理、审计相关能力 | `dept_id` 允许为空，公告可发管理员或全员 |
| hr admin | 部门管理、员工管理、公告管理、本部门员工账号管理 | 不可管理其他部门账号 |
| purchase admin | 进货、进货退货、预警中心、公告管理、本部门员工账号管理 | 历史作废/红冲需提交仓储审批 |
| sales admin | 销售、销售退货、预警中心、公告管理、本部门员工账号管理 | 历史作废/红冲需提交仓储审批 |
| warehouse admin | 供应商、商品资料、预警中心、作废审批、公告管理、本部门员工账号管理 | 审批模块执行人固定为仓储管理员 |
| finance admin | 销售统计图表、公告管理、本部门员工账号管理 | 毛利视角仅财务管理员可访问 |
| employee | 员工工作台、公告查看、手机号邮箱自助维护、工作要求接收与执行、站内邮箱、项目内 AI 助手问答 | 工作要求仅允许访问本人分配记录，AI 助手仅回答项目内部制度/流程/模块相关问题，不开放通用闲聊 |

## 六、关键流程说明

### 1. 登录与注册
- 登录成功后，Session 会同步写入 `role/deptId/deptCode`。
- 注册时必须提交 `deptId`，默认创建为 `employee`。
- `/auth/depts` 已加入白名单，注册页面可在未登录状态下加载部门下拉。

### 2. 公告分发
- superadmin：可发 `admin` 或 `all`，可选目标部门。
- 部门管理员：强制发给本部门 `employee`。
- 查询时按当前账号部门与角色动态过滤，不能越权查看不属于自己的公告。
- 全员公告 `targetDeptId = null` 已纳入空值安全处理，新员工登录首页不会再因公告分页触发空指针。

### 2.1 员工工作台
- `GET /api/home/employee-workbench`：返回员工首页聚合数据，拆分为 `summary/profile/deptContact/workRequirements` 四块。
- `PUT /api/home/employee-workbench/contact`：允许员工保存本人手机号与邮箱，并同步更新 `sys_user` 与 `sys_employee`。
- 采购、销售、仓储员工会附带低库存与零库存摘要；人事、财务员工返回档案完成状态所需信息。

### 2.2 工作要求模块
- 管理员通过 `/api/system/work-requirements` 发布工作要求，支持“全员”与“指定员工”两种下达范围，并自动按员工生成 `work_requirement_assign` 分配记录。
- 员工通过 `/api/home/work-requirements/{assignId}` 查看详情，状态流转为：待接受 -> 执行中 -> 待审核 -> 已完成；也支持“拒收”和“已驳回后重提”。
- 员工执行结果支持文本 + 图片附件，图片先经 `/api/upload/image` 上传，再将路径写入 `work_requirement_attachment`。
- 管理员审核时可通过或驳回；驳回会清空旧执行结果与附件，保留驳回次数，回到“已驳回”状态等待员工重提。

### 2.3 工作要求超时规则
- 超时不是新的主状态，而是独立维度：`overdue_flag=1` 表示已超时，`submitted_on_time=0` 表示逾期提交。
- 若截止时间已过但员工仍未接受或仍在执行/已驳回待重提，主状态保持原值，只额外显示“超时中”。
- 若员工在截止后提交，流程仍进入“待审核”，但会记录为“逾期提交”；审核通过后页面展示为“逾期完成”。
- `WorkRequirementOverdueScheduler` 每 10 分钟扫描一次超时记录，首次落库时写入 `overdue_at`、提醒次数和最近提醒时间，并通过站内消息提醒员工及本部门管理员。

### 2.4 站内消息中心
- 当前消息中心已不再局限管理员，所有已登录用户都可查看自己的站内消息。
- 既有员工账号管理提醒链路保留，工作要求超时消息新增 `sendWorkRequirementOverdueToEmployee` 与 `sendWorkRequirementOverdueToDeptAdmins` 两条发送方法。

### 3. 历史作废/作废并红冲
- 当天单据仍走删除。
- 历史采购与销售单据及其退货单，不再直接执行作废，而是由采购/销售管理员提交审批申请。
- 仓储管理员在审批模块中通过或驳回，审批结果与删除状态会同步回写到单据列表返回值中。

### 4. 库存预警中心
- warning-only 商品分页接口允许仓储、采购、销售管理员访问。
- 商品资料与供应商资料管理接口仍只允许仓储管理员访问。

### 5. 项目 AI 助手链路
- `POST /api/assistant/project/ask` 接收问题、可选会话 ID 与模型编码；superadmin/admin 可显式选模型，employee 使用系统默认策略。
- 服务层先做问题分类、角色约束与项目知识召回，再由 `LlmRoutingService` 按主模型和回退链路选择实际调用模型。
- 成功响应会写入会话消息；`ai_message` 当前保存 `providerCode/modelCode/fallbackUsed`，供历史会话和结果回显使用。
- 模型调用日志由 `LlmAuditService` 记录到 `ai_model_call_log`，包含请求模型、实际模型、问题类型、角色部门、耗时等治理字段；其中 `assistantMessageId` 字段已预留，但当前尚未回填真实消息 ID。

## 七、运行与验证

- 编译：`mvnw.cmd -q -DskipTests compile`
- 测试：`mvnw.cmd -q test`
- 启动：`mvnw.cmd spring-boot:run`
- 文档：`/api/doc.html`、`/api/v3/api-docs`
- 默认端口：`8080`，统一上下文前缀：`/api`




