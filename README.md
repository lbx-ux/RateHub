# RateHub - 本地生活社交与高并发秒杀平台

[![Vue3](https://img.shields.io/badge/Vue.js-3.x-4FC08D?style=flat&logo=vue.js&logoColor=white)](https://vuejs.org/)
[![SpringBoot](https://img.shields.io/badge/Spring%20Boot-2.3+-6DB33F?style=flat&logo=spring&logoColor=white)](https://spring.io/projects/spring-boot)
[![Redis](https://img.shields.io/badge/Redis-6.x-DC382D?style=flat&logo=redis&logoColor=white)](https://redis.io/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?style=flat&logo=mysql&logoColor=white)](https://www.mysql.com/)

RateHub 是一款前后端分离的**企业级本地生活探店与社交平台**。项目深度整合了 Spring Boot 与 Redis，不仅提供商铺检索、探店分享、好友关注等丰富的社交功能，更**攻克了千万级流量下的秒杀抢券难题**，是一个绝佳的高并发架构最佳实践项目。

---

## 🔥 项目核心亮点 (Highlights)

本项目不仅是业务的堆砌，更是针对真实复杂场景给出的优雅技术解决方案：

### 1. 🚀 高并发秒杀抢券架构
- **超卖危机化解**：基于 Redis + Lua 脚本实现原子级别的库存预扣减，彻底告别并发超卖。
- **一人一单防刷**：引入 **Redisson 分布式锁**，在集群环境下精准控制用户购买行为，防范恶意刷单。
- **异步下单削峰**：采用 Redis 消息队列（Stream/阻塞队列）将订单入库异步化，使得接口响应时间降至毫秒级，轻松应对峰值流量。

### 2. 🧠 Redis 的深度与极致应用
- **Feed 流推送（达人动态）**：基于 Redis **Sorted Set (ZSet)** 构建千万粉丝级的推拉结合（Push/Pull）动态时间线，实现类似朋友圈的高性能刷新。
- **海量用户签到**：采用 Redis **Bitmap (位图)**，在百万日活场景下仅用极小的内存开销（极省空间）完成签到记录与连续签到天数统计。
- **LBS 附近商铺**：利用 Redis **GEO** 数据结构实现毫秒级的地理位置检索、经纬度计算与按距离排序。
- **点赞排行榜**：运用 ZSet 记录并展示探店日记的最早点赞 Top N 达人。

### 3. 🛡️ 稳健的基础支撑设计
- **无状态分布式 Session**：基于 Redis Token 机制与 Spring 拦截器链，结合 `ThreadLocal` 优雅地实现用户信息在线程上下文中的无损透传。
- **缓存穿透与雪崩防御**：针对热点商铺查询，设计了空对象缓存、互斥锁重建缓存等防线，保障底层 MySQL 的绝对安全。

---

## 🛠️ 技术栈全景

### 前端 (Frontend)
- **核心框架**：Vue 3 (Composition API) + Vite
- **全家桶**：Pinia (状态管理) + Vue Router (路由)
- **UI & 网络**：Element Plus 组件库 + Axios 异步请求

### 后端 (Backend)
- **核心框架**：Spring Boot 2.3.12.RELEASE + MyBatis
- **数据层**：MySQL (关系型存储) + Redis (Lettuce 连接池)
- **第三方基建**：阿里云号码认证服务（手机验证码防刷防御）
- **工具链**：Hutool 工具箱, Lombok 代码简化, PageHelper 分页插件

---

## 📁 源码导航

```text
RateHub/
├── backend/             # 后端 Spring Boot 核心工程
├── frontend/            # 前端 Vue3 交互工程
├── api.md               # 📖 前后端 RESTful API 标准接口文档（极其详尽，已全部补全）
└── README.md            # 项目自述文档
```

---

## 🚀 快速启动指南

### 环境依赖清单
- **Java**: JDK 1.8+
- **构建工具**: Maven 3.x+
- **Node.js**: v18+ (推荐)
- **数据库**: MySQL 5.7+ / 8.0+
- **缓存**: Redis 6.x+

### 🛠️ 后端启动步骤

1. **初始化数据库**
   - 在 MySQL 中创建 `hmdp` 数据库，并导入项目中配套的初始化 SQL 脚本。
2. **配置关键密钥**
   - 进入 `backend/src/main/resources/`，将 `application.yaml.example` 复制为 `application.yaml`。
   - 填入你的 MySQL 密码、Redis 密码，以及阿里云服务的 AccessKey。
3. **点火启动**
   - 导入 IntelliJ IDEA 等开发工具，待 Maven 依赖就绪后，运行 `com.hmdp.HmDianPingApplication` 主类。
   - 后端服务默认监听于 `http://localhost:8081`。

### 🎨 前端启动步骤

1. **装载依赖**
   ```bash
   cd frontend
   npm install
   ```
2. **极速热更启动**
   ```bash
   npm run dev
   ```
3. 打开浏览器访问控制台输出的地址（默认 `http://localhost:5173`），即可体验流畅的探店世界！
