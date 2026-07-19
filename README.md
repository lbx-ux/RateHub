# RateHub

RateHub 是一个前后端分离的**本地生活探店社交与高并发优惠券秒杀平台**。该项目基于 Spring Boot 2.x、Vue 3 和 Redis 构建，旨在为用户提供商铺检索、探店分享、社交互动，以及高并发场景下的秒杀抢券服务。

---

## 🛠️ 技术栈

### 前端 (Frontend)
- **核心框架**：Vue 3 (Composition API)
- **构建工具**：Vite
- **状态管理**：Pinia
- **路由管理**：Vue Router
- **UI 组件库**：Element Plus
- **网络请求**：Axios

### 后端 (Backend)
- **核心框架**：Spring Boot 2.3.12.RELEASE
- **持久层框架**：MyBatis 
- **数据库**：MySQL
- **缓存与高并发**：Redis (Lettuce 连接池)
- **第三方集成**：阿里云号码认证服务 V2.0 SDK (短信服务及图形验证码)
- **实用工具**：Hutool, Lombok, PageHelper

---

## 🌟 核心功能模块

1. **用户与安全模块**
   - 阿里云短信验证码快速登录与注册。
   - 图形验证码防御攻击。
   - 基于 Redis 存储 Session/Token 实现分布式单点登录。

2. **商铺与地理定位 (GEO) 模块**
   - 商铺分类多条件检索（按评价、评分等排序）。
   - 利用 Redis GEO 实现附近商铺的经纬度定位及距离排序。

3. **高并发秒杀与优惠券模块**
   - 商家代金券发布。
   - 秒杀优惠券的高并发抢购（一人一单限制）。
   - 基于 Redis 缓存、Lua 脚本实现原子性库存扣减。
   - 基于 Redis 消息队列/阻塞队列实现异步下单，降低数据库瞬时压力。

4. **探店社区（Blog）模块**
   - 探店日记（发布图文笔记）。
   - 基于 Redis Sorted Set 实现的点赞排行榜。
   - 基于 Redis Bitmap 实现的每日签到与连续签到统计。

5. **社交关系与 Feed 流模块**
   - 用户关注/粉丝系统。
   - 基于 Redis Feed 流（推送模式/拉取模式）实现的好友动态时间线订阅。

---

## 📁 项目结构

```text
RateHub/
├── backend/             # 后端 Spring Boot 工程
│   ├── src/
│   └── pom.xml          # Maven 依赖配置
├── frontend/            # 前端 Vue3 工程
│   ├── src/
│   ├── package.json     # 前端依赖与构建配置
│   └── vite.config.js   # Vite 配置文件
├── api.md               # 前后端交互 API 接口规范文档
└── README.md            # 项目自述文档
```

---

## 🚀 快速开始

### 环境依赖
- **Java**: JDK 1.8+
- **Build**: Maven 3.x+
- **Node.js**: 18+ (推荐)
- **Database**: MySQL 5.7+ / 8.0+
- **Cache**: Redis 6.x+

---

### 后端启动步骤

1. **准备数据库**
   - 创建 MySQL 数据库（如 `hmdp`），并导入项目配套的 SQL 脚本初始化表结构和基础数据。

2. **配置服务**
   - 进入 `backend/src/main/resources/` 目录。
   - 复制示例配置文件：
     ```bash
     cp application.yaml.example application.yaml
     ```
   - 打开并编辑 `application.yaml`，填入你的本地数据库、Redis 连接信息以及阿里云短信/验证码服务的 AK 等凭证。

3. **编译并运行**
   - 导入 IDE (如 IntelliJ IDEA)，等待 Maven 依赖下载完毕。
   - 运行主类 `com.hmdp.HmDianPingApplication` 启动后端服务。
   - 服务默认运行在 `http://localhost:8081`（详见具体配置）。

---

### 前端启动步骤

1. **安装依赖**
   - 打开终端并进入 `frontend` 目录：
     ```bash
     cd frontend
     ```
   - 安装依赖包：
     ```bash
     npm install
     ```

2. **本地开发运行**
   - 运行本地开发服务器：
     ```bash
     npm run dev
     ```
   - 打开浏览器访问输出的本地地址（默认通常为 `http://localhost:5173`）即可进入系统。
