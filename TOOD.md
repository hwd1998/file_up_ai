# 项目任务清单 & 技术债务追踪

> 每次对话开始前必须读取本文件，实现功能后同步更新。

---

## ✅ 已完成

### 设计阶段
- [x] 需求摘要文档（doc/需求摘要.md）
- [x] 产品方案文档（doc/产品方案-文件上传系统.md）
- [x] 技术方案文档（doc/技术方案-文件上传系统.md）
- [x] 数据库设计文档（doc/数据库设计.md）
- [x] UI 方案文档（doc/UI方案-文件上传系统.md，Tailwind CSS + Alpine.js）
- [x] Claude Design Prompts（doc/claude-design-prompts.md，10个页面）
- [x] UI Demo 静态页面（UIdemo/prompt_01 登录页、prompt_03 上传页、prompt_04 历史页、prompt_06_5 向导Step5）

---

## 🔄 已绕过 / 临时方案（⚠️ 必须后续处理）

| 编号 | 描述 | 临时方案 | 目标方案 | 优先级 |
|------|------|----------|----------|--------|
| — | 暂无绕过项 | — | — | — |

---

## 📋 待完成

### 第一阶段：基础框架 ✅ 全部完成

- [x] Spring Boot 3.2 工程初始化（pom.xml、包结构、统一响应 Result、全局异常）
- [x] application.properties 配置（端口、MySQL、飞书、上传参数）
- [x] 数据库初始化 SQL（全部表结构 + 索引，参考 doc/数据库设计.md）
- [x] 飞书 OAuth2.0 登录集成（/auth/feishu/login → 回调 → Session 写入）
- [x] 目录树管理 CRUD（directories + directory_permissions）
- [x] 分片上传核心接口（/upload/init、/upload/chunk、/upload/merge）
- [x] 断点续传查询接口（/upload/chunks）
- [x] 同步快速校验（表头匹配 + 必填列，QuickValidationUtil）
- [x] 上传历史列表接口（分页、过滤、任务详情、错误明细）
- [x] 前端页面：登录页（分栏布局，渐变左栏 + 飞书按钮登录）
- [x] 前端页面：目录首页（目录树展开/折叠，侧边栏 + 内容区）
- [x] 前端页面：上传文件页（拖拽 + 分片 + 进度条 + 断点续传）
- [x] 前端页面：上传历史列表（筛选 + 分页 + 详情侧滑面板）
- [x] 前端页面：管理员模板页（Phase 2 占位）

### 第二阶段：模板系统

- [ ] 模板 CRUD 接口（列表、创建、停用、复制、版本历史）
- [ ] 样例文件解析（Apache POI SXSSFWorkbook + OpenCSV，自动推断字段类型）
- [ ] DDL 生成工具（DdlGenerateUtil，表名/列名白名单过滤，防注入）
- [ ] 动态建表执行（通过 JDBC 执行生成的 DDL）
- [ ] 字段映射配置存储（source_column → target_column，含常量映射、跳过字段）
- [ ] 模板版本自动递增逻辑（V1.0 → V1.1 → V2.0）
- [ ] 校验引擎开发（责任链模式，10 类规则，参考技术方案 3.1.3）
- [ ] RabbitMQ 异步任务队列集成（queue.validation + 死信队列 DLQ）
- [ ] 异步完整校验 Worker（ValidationWorker，自动重试 3 次）
- [ ] 批量写入业务表（JDBC 批量插入，每批 1000 行）
- [ ] 原始文件上传 OSS（处理成功后写入，设置 90 天过期）
- [ ] 飞书消息通知（成功/失败/校验异常，NotificationWorker）
- [ ] 无数据申报接口（/upload/no-data）
- [ ] 前端页面：模板列表页（基于 prompt_05）
- [ ] 前端页面：模板创建向导 Step1-5（基于 UIdemo/prompt_06_5，含字段映射 SVG 画布）

### 第三阶段：监控与告警

- [ ] 监控看板接口（目录树 + 各周期用户上报状态统计）
- [ ] 监控状态定时计算任务（每小时，更新 monitor_status 表）
- [ ] 导出监控报表（EasyExcel，/monitor/dashboard/export）
- [ ] 定时提醒任务（截止前 N 天，飞书推送给业务用户及上级）
- [ ] 飞书系统告警 Webhook（失败率/队列/并发等阈值触发）
- [ ] 告警规则后台可配置 CRUD（alert_rules 表）
- [ ] 操作审计日志（AOP 切面自动记录模板变更/权限变更/用户登录）
- [ ] 过期文件自动清理定时任务（每日 02:00，90 天）
- [ ] 前端页面：监控看板（基于 prompt_09，树形状态色块）
- [ ] 前端页面：告警配置页（基于 prompt_10）
- [ ] 前端页面：目录管理页（基于 prompt，含授权侧滑面板）
- [ ] 前端页面：用户管理页（飞书部门批量同步）

### 第四阶段：稳定性与上线

- [ ] 全流程 UAT 测试（参考需求摘要五、验收标准全部场景）
- [ ] 性能测试（50 并发用户 + 2GB 文件，JMeter）
- [ ] 安全扫描（OWASP Top10 自查，重点：动态 DDL 注入、目录越权）
- [ ] Actuator + Micrometer 指标接入
- [ ] 慢查询优化（upload_tasks 复合索引验证）
- [ ] 生产环境部署文档

---

## 📝 变更日志

### 2026-04-30
- 完成全部设计文档（需求/产品/技术/数据库/UI 方案）
- 生成 Claude Design Prompts（10个页面），完成 4 个页面 UI Demo
- UI 技术栈从 Ant Design 调整为 Tailwind CSS + Alpine.js
- TOOD.md 初始化，进入第一阶段开发准备
- **第一阶段全部完成**：50 个 Java 文件 + 6 个 Thymeleaf 模板，mvn clean compile 通过
  - pom.xml 修复：annotationProcessorPath 中 lombok 增加显式版本号 1.18.32
  - SecurityConfig 调整为 permitAll，auth 由 controller 层 HttpSession 属性校验
  - PageResult 字段 records 与前端对齐（history.html 使用 json.data.records）
