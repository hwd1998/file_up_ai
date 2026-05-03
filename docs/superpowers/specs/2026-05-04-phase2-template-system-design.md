# Phase 2 设计文档：模板系统

- **日期：** 2026-05-04
- **状态：** 已确认
- **关联技术方案：** doc/技术方案-文件上传系统.md

---

## 一、范围与约束

| 决策点 | 选择 | 原因 |
|--------|------|------|
| 异步队列 | Spring `@Async` + 线程池 | 本地无 RabbitMQ，线程池简洁可靠 |
| 文件存储 | 本地服务器文件系统 | 无 OSS 环境，跳过阿里云/MinIO |
| 字段映射 UI | 下拉框选择 | SVG 拖拽画布复杂度高，下拉框同等满足需求 |
| 校验规则 | 前 6 类 | 表头/必填/类型/枚举/正则/脏数据阈值，后 4 类留 Phase 3 |

---

## 二、架构与组件

### 新增后端组件

```
controller/
  TemplateController.java        模板向导 5 步接口 + 列表/停用/复制

service/
  TemplateService.java           模板业务逻辑（CRUD + 版本管理）
  FileParseService.java          样例文件解析 + 字段类型推断
  DdlService.java                DDL 生成 + JdbcTemplate 执行建表
  AsyncValidationService.java    @Async 完整校验 + biz_* 写入 + 飞书通知

validation/
  ValidationRule.java            校验规则接口（validate + getOrder）
  ValidationContext.java         校验上下文（文件行数据 + 模板字段定义）
  ValidationChain.java           责任链（顺序执行，累积错误，支持短路）
  rules/
    HeaderMatchRule.java         规则1：表头列名匹配
    RequiredFieldRule.java       规则2：必填列非空
    TypeCheckRule.java           规则3：字段类型校验
    EnumValueRule.java           规则4：枚举值校验
    RegexRule.java               规则5：正则表达式校验
    DirtyThresholdRule.java      规则6：脏数据行比例超阈值整批拒绝

config/
  AsyncConfig.java               线程池配置（@EnableAsync）
```

### 新增前端页面

```
templates/admin/
  template-list.html             模板列表（改造现有占位页）
  template-wizard.html           5 步向导（Step1-5 单页 Alpine.js 状态机）
```

### 现有代码改动

- `UploadService.merge()`：同步快速校验通过后，调用 `AsyncValidationService.submitAsync(taskId)`
- `ViewController`：新增 `/admin/templates` 和 `/admin/templates/create` 路由
- `admin/template.html` → 重命名/改造为真实模板列表

---

## 三、接口设计

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/harness/api/v1/templates` | 模板列表（分页，支持按目录/状态筛选） |
| POST | `/harness/api/v1/templates` | Step1：创建草稿（名称+目录+脏数据阈值） |
| PUT | `/harness/api/v1/templates/{id}/sample` | Step2a：上传样例文件，返回解析字段列表 |
| POST | `/harness/api/v1/templates/{id}/ddl` | Step2b：生成 DDL 预览（返回 SQL 字符串） |
| POST | `/harness/api/v1/templates/{id}/table` | Step2c：执行建表 DDL |
| PUT | `/harness/api/v1/templates/{id}/field-mapping` | Step3：保存字段映射配置 |
| PUT | `/harness/api/v1/templates/{id}/validation-rules` | Step4：保存各字段校验规则 |
| POST | `/harness/api/v1/templates/{id}/publish` | Step5：发布，生成版本号 |
| POST | `/harness/api/v1/templates/{id}/copy` | 复制模板（草稿状态） |
| PUT | `/harness/api/v1/templates/{id}/disable` | 停用模板 |
| GET | `/harness/api/v1/templates/{id}/versions` | 版本历史列表 |

---

## 四、核心数据流

### 4.1 模板创建（管理员）

```
Step1 → templates 插入草稿记录（status=draft）
Step2 → 上传样例文件 → 解析字段+推断类型 → 存 template_fields（草稿）
      → 生成 DDL 预览 → 管理员确认 → 执行建表（biz_{id}_v{major}）
      → template_versions 插入记录（version=V1.0，target_table_name）
Step3 → 更新 template_fields.source_column / target_column / constant_value / is_skipped
Step4 → 更新 template_fields.validation_rules（JSON）
Step5 → templates.status=active，current_version=V1.0
```

### 4.2 文件上传 + 异步校验（业务用户）

```
分片合并完成
  → 同步快速校验（表头+必填，≤2s，已有逻辑）
  → 通过：task.status=validating
  → AsyncValidationService.submitAsync(taskId)  [立即返回，异步执行]

异步线程：
  1. 加载 task → templateVersionId → template_fields + validation_rules
  2. 解析文件（POI/OpenCSV 流式）
  3. ValidationChain 逐行执行 6 类规则，累积 upload_errors
  4. DirtyThresholdRule：错误行 / 总行 > threshold → 整批拒绝
     → task.status=failed → 飞书通知失败（含错误摘要）
  5. 校验通过 → JDBC 批量插入 biz_* 表（每批1000行，事务）
  6. task.status=success，update row_count → 飞书通知成功
```

### 4.3 版本号递增规则

| 变更类型 | 版本变化 | 是否重新建表 |
|----------|----------|-------------|
| 首次发布 | → V1.0 | 是（Step2 已建） |
| 修改校验规则/常量（不改字段结构） | V1.0 → V1.1 | 否 |
| 增删字段（改变表结构） | V1.x → V2.0 | 是（新建 biz_{id}_v2） |

---

## 五、关键技术决策

### 5.1 线程池配置

```java
// AsyncConfig.java
@Bean("validationExecutor")
public Executor validationExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(5);
    executor.setMaxPoolSize(10);
    executor.setQueueCapacity(50);
    executor.setThreadNamePrefix("validation-");
    executor.setRejectedExecutionHandler(new CallerRunsPolicy());
    executor.initialize();
    return executor;
}
```

### 5.2 文件解析与类型推断

- **Excel**：≤50MB 用 `XSSFWorkbook`，>50MB 用 `SXSSFWorkbook`（流式，内存窗口100行）
- **CSV**：OpenCSV `CSVReader` 流式逐行读取
- **类型推断**：取前 20 行非空值，按优先级尝试 `Integer → Decimal → Date → String`，多数匹配则采用该类型

### 5.3 DDL 安全规则

- 表名格式：`biz_{templateId}_v{majorVersion}`（纯ASCII数字下划线）
- 列名：`target_column` 仅允许 `[a-zA-Z0-9_]`，建表前正则过滤，不符合则拒绝发布
- 执行方式：`JdbcTemplate.execute(ddlSql)`，表名/列名已过滤，非用户输入直接拼接

### 5.4 校验规则接口

```java
public interface ValidationRule {
    ValidationResult validate(ValidationContext ctx);
    int getOrder();  // 执行顺序，越小越先
}

// ValidationContext 包含：
// - List<String> headers（文件实际表头）
// - List<Map<String,String>> rows（文件数据行）
// - List<TemplateField> fields（模板字段定义，含 validation_rules JSON）
// - int dirtyThreshold（脏数据阈值百分比）
```

### 5.5 飞书通知

- 复用 `FeishuUtil.sendWebhook()`
- 内容模板：`【上传{成功/失败}】{文件名} | {行数}行 | {错误摘要，最多3条}`
- 发送失败仅 `log.error`，不抛异常，不影响主流程

---

## 六、前端页面设计

### 6.1 模板列表页（`/admin/templates`）

- 表格列：模板名、绑定目录、当前版本、状态标签（草稿/启用/停用）、操作
- 操作：编辑（跳向导Step1）、停用、复制、版本历史
- 顶部：「新建模板」按钮 → 跳转向导

### 6.2 模板创建向导（`/admin/templates/create`）

单页 Alpine.js，`currentStep` 状态变量控制显示哪步：

| Step | 展示内容 |
|------|----------|
| 1 | 模板名称输入、绑定目录下拉、脏数据阈值滑块（默认10%） |
| 2 | 上传样例文件、解析结果字段表格、DDL SQL 预览区、「执行建表」按钮 |
| 3 | 字段映射表格：每行 = 源列名 + 目标列名下拉 + 必填开关 + 常量值输入 + 跳过勾选 |
| 4 | 校验规则表格：每字段配枚举值（逗号分隔）/正则表达式输入框 |
| 5 | 发布确认页：版本号预览、目录绑定、「发布」按钮 |

---

## 七、实现顺序

1. `AsyncConfig` + `AsyncValidationService` 骨架（@Async + 线程池）
2. `FileParseService`（POI/OpenCSV 解析 + 类型推断）
3. `DdlService`（DDL 生成 + 安全过滤 + 建表执行）
4. `ValidationChain` + 6 类规则
5. `TemplateService` + `TemplateController`（5步接口 + 列表/停用/复制）
6. `AsyncValidationService` 完整实现（校验 + 写入 + 通知）
7. 前端：模板列表页
8. 前端：5步向导页
