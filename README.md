# Warehouse Management System

一个基于前后端分离架构的仓库管理系统，覆盖认证授权、系统管理、基础资料、进销退存业务与销售统计分析。

## 项目亮点

- 前后端分离：Vue 3 + Spring Boot + MySQL
- 角色权限：superadmin / admin / employee
- 核心业务：进货、进货退货、销售、销售退货
- 数据可视化：销售概览、Top5、品牌占比、趋势图
- 接口文档：内置 OpenAPI + Knife4j

## 在线演示

- 演示网站：https://wmsystem01.pages.dev/
- 演示网站体验可能不够完整，建议本地部署后体验完整功能。(注册功能在演示网站无法使用)

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

### 配置文件说明

- `back/src/main/resources/application.properties` 中数据库密码请按本机环境修改。



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


## 默认账号（可按需修改）

- `superadmin`
- `admin`
- `employee`

默认密码:123456

## 角色说明

- `superadmin`：超级管理员，具备管理员全部能力
- `admin`：管理员，负责系统与业务管理
- `employee`：普通用户，以查看权限为主

## 接口文档

- Knife4j：`http://localhost:8080/api/doc.html`
- OpenAPI JSON：`http://localhost:8080/api/v3/api-docs`

---

如果这个项目对你有帮助，欢迎 Star。
