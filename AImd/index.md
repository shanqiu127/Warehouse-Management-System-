# AImd 总索引

## 1. 当前知识库目标

当前知识库完全基于各角色的前端页面功能来组织，聚焦每个角色能看到的页面、能做的操作、完整的业务流程。

核心原则：

- 以前端页面功能为主线，覆盖每个角色的所有模块和操作
- 每个角色只回答自己职责范围内的问题，不混入其他部门内容
- 工作流程、表单字段、操作规则等均基于实际页面功能描述

## 2. 文档使用方式

### 2.1 角色主文档

每个角色一份主文档，按页面维度详细描述该角色可用的所有功能。

- `AImd/superadmin.md` — 超级管理员：超管总览、部门审批、安全策略、登录日志、操作日志、公告管理、用户管理
- `AImd/hr_admin.md` — 人事管理员：全部门管理、全员工管理、员工分布图表、工作要求、公告管理、用户部门管理
- `AImd/purchase_admin.md` — 采购管理员：商品进货、进货退货、预警中心、工作要求、公告管理、用户部门管理
- `AImd/sales_admin.md` — 销售管理员：商品销售、销售退货、预警中心、工作要求、公告管理、用户部门管理
- `AImd/warehouse_admin.md` — 仓储管理员：供应商管理、商品资料管理、预警中心、作废审批、工作要求、公告管理、用户部门管理
- `AImd/finance_admin.md` — 财务管理员：销售统计图表（含毛利视角）、工作要求、公告管理、用户部门管理
- `AImd/employee.md` — 普通员工：员工工作台、工作要求详情、个人信息维护、公告查看、站内消息

### 2.2 end 补充文档

`AImd/end` 仅用于补充技术层面的问答，不作为业务问答的主来源：

- `AImd/end/project.md` — 项目结构与目录职责
- `AImd/end/front.md` — 前端页面与入口
- `AImd/end/back.md` — 后端接口与服务链路

## 3. 各角色加载策略

### 3.1 超级管理员

- 默认主文档：`AImd/superadmin.md`
- 补充文档：`AImd/end/project.md`、`AImd/end/front.md`、`AImd/end/back.md`
- 如果问题指向某个部门业务，再补对应部门文档

### 3.2 各部门管理员

- 默认只加载本部门文档
- 只有明确问到页面结构、接口、启动等技术问题时，才补 `AImd/end`
- 不默认混入其他部门管理员文档

### 3.3 普通员工

- 默认只加载 `AImd/employee.md`
- 聚焦工作要求流程、个人信息维护、公告与消息查看
- 不加载管理员文档和技术结构文档

## 4. 角色文档映射

| 当前角色 | 部门编码 | 主文档 |
|---|---|---|
| superadmin | 任意 | AImd/superadmin.md |
| admin | hr | AImd/hr_admin.md |
| admin | purchase | AImd/purchase_admin.md |
| admin | sales | AImd/sales_admin.md |
| admin | warehouse | AImd/warehouse_admin.md |
| admin | finance | AImd/finance_admin.md |
| employee | 任意部门 | AImd/employee.md |

## 5. 推荐问题策略

每个角色配置 6 条推荐问题，基于该角色最常用的页面功能和操作流程生成，帮助用户快速找到所需信息。

## 6. 不建议的加载方式

- 不建议所有角色默认加载全部 AImd 文档
- 不建议把开发记录文档加入知识库
- 不建议让普通员工默认加载管理员文档
