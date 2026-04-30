# 数据上报平台 — 启动指南

## 环境要求

| 依赖 | 版本要求 |
|------|----------|
| JDK | 21+ |
| Maven | 3.6+ |
| MySQL | 8.0+ |

---

## 第一步：飞书开放平台配置

登录 [飞书开放平台](https://open.feishu.cn/app) 创建/进入你的应用，获取以下三项：

| 配置项 | 位置 |
|--------|------|
| `App ID` | 应用凭证 → App ID |
| `App Secret` | 应用凭证 → App Secret |
| 重定向 URL | 安全设置 → 重定向 URL，**必须**添加 `http://localhost:8080/auth/feishu/callback` |

**应用权限**（权限管理中勾选）：
- `contact:user.base:readonly` — 获取用户基本信息（姓名、邮箱、open_id）

> OAuth 登录能力无需单独勾选权限，启用「网页应用」并配置重定向 URL 即可。

> 本地开发填 `http://localhost:8080/auth/feishu/callback`，生产环境替换为实际域名。

---

## 第二步：初始化数据库

```sql
-- 1. 创建数据库
CREATE DATABASE file_upload CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 2. 执行初始化脚本（包含全部建表语句 + 默认告警规则）
source src/main/resources/db/init.sql
```

或通过命令行一次性执行：

```bash
mysql -u root -p -e "CREATE DATABASE file_upload CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
mysql -u root -p file_upload < src/main/resources/db/init.sql
```

**初始化后包含的表（12 张）：**

```
sys_user              — 系统用户（飞书 OAuth 自动写入）
directories           — 目录树
directory_permissions — 目录授权
templates             — 数据模板（Phase 2 使用）
template_versions     — 模板版本（Phase 2 使用）
template_fields       — 模板字段（Phase 2 使用）
upload_tasks          — 上传任务记录
upload_errors         — 校验错误明细
upload_chunk_records  — 分片上传进度
monitor_status        — 监控状态快照（Phase 3 使用）
alert_rules           — 告警规则（含 5 条默认规则）
audit_logs            — 操作审计日志（Phase 3 使用）
```

---

## 第三步：配置环境变量

在系统环境变量或启动命令中设置以下变量，**飞书三项为必填**：

### 必填

| 环境变量 | 说明 | 示例 |
|----------|------|------|
| `FEISHU_APP_ID` | 飞书应用 App ID | `cli_xxxxxx` |
| `FEISHU_APP_SECRET` | 飞书应用 App Secret | `xxxxxx` |
| `FEISHU_REDIRECT_URI` | 飞书 OAuth 回调地址 | `http://localhost:8080/auth/feishu/callback` |

### 可选（有默认值）

| 环境变量 | 默认值 | 说明 |
|----------|--------|------|
| `DB_USERNAME` | `root` | 数据库用户名 |
| `DB_PASSWORD` | `root` | 数据库密码 |
| `UPLOAD_TMP_DIR` | `./upload-tmp` | 分片临时文件存储目录 |
| `FEISHU_WEBHOOK_URL` | 空 | 飞书群机器人 Webhook，留空则不发通知 |

### 设置方式（选其一）

**方式 A — 命令行参数（推荐本地开发）：**

```bash
mvn spring-boot:run \
  -Dspring-boot.run.jvmArguments="\
    -DFEISHU_APP_ID=cli_xxxxxx \
    -DFEISHU_APP_SECRET=your_secret \
    -DFEISHU_REDIRECT_URI=http://localhost:8080/auth/feishu/callback \
    -DDB_USERNAME=root \
    -DDB_PASSWORD=yourpassword"
```

**方式 B — 系统环境变量（推荐 CI/生产）：**

```bash
export FEISHU_APP_ID=cli_xxxxxx
export FEISHU_APP_SECRET=your_secret
export FEISHU_REDIRECT_URI=http://localhost:8080/auth/feishu/callback
export DB_USERNAME=root
export DB_PASSWORD=yourpassword
mvn spring-boot:run
```

**方式 C — 本地覆盖配置文件（最简单）：**

在项目根目录创建 `src/main/resources/application-local.properties`，内容：

```properties
feishu.app-id=cli_xxxxxx
feishu.app-secret=your_secret
feishu.redirect-uri=http://localhost:8080/auth/feishu/callback
spring.datasource.username=root
spring.datasource.password=yourpassword
```

然后启动时激活 local profile：

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

---

## 第四步：设置管理员账号

系统首次登录时会通过飞书 OAuth 自动在 `sys_user` 表中创建用户记录，默认角色为 `user`。

**登录一次后**，将自己设为管理员：

```sql
-- 将指定用户升级为管理员（替换 your_name）
UPDATE sys_user SET role = 'admin' WHERE user_name = 'your_name';

-- 或者通过飞书 open_id（登录日志中可看到）
UPDATE sys_user SET role = 'admin' WHERE open_id = 'ou_xxxxxx';
```

管理员登录后会自动跳转到 `/admin/template` 页面，普通用户跳转到 `/home`。

---

## 第五步：启动应用

```bash
# 编译
mvn clean compile

# 启动（激活 local profile，对应方式 C）
mvn spring-boot:run -Dspring-boot.run.profiles=local

# 或直接启动（通过系统环境变量注入配置）
mvn spring-boot:run
```

启动成功日志：
```
Started UploadApplication in x.xxx seconds
```

---

## 访问地址

| 地址 | 说明 |
|------|------|
| `http://localhost:8080/login` | 登录页（飞书按钮） |
| `http://localhost:8080/home` | 目录首页 |
| `http://localhost:8080/admin/template` | 管理员模板页 |
| `http://localhost:8080/swagger-ui.html` | API 文档 |
| `http://localhost:8080/actuator/health` | 健康检查 |

---

## 常见问题

**Q: 飞书回调报错 `redirect_uri_mismatch`**
— 检查飞书开放平台「安全设置 → 重定向 URL」是否已添加完整回调地址（含 `/auth/feishu/callback`），且与 `FEISHU_REDIRECT_URI` 完全一致。

**Q: 数据库连接失败**
— 检查 MySQL 服务是否运行，数据库名是否为 `file_upload`，用户名/密码是否正确。

**Q: 上传临时目录权限报错**
— 确认 `UPLOAD_TMP_DIR` 指向的目录存在且当前用户有读写权限；默认 `./upload-tmp` 会在工作目录下自动创建。

**Q: 登录成功但页面一直跳回登录**
— Session 未生效，检查浏览器是否禁用了 Cookie。

---

## 当前阶段说明

**Phase 1（已完成）**：飞书登录 + 目录管理 + 分片上传 + 快速校验 + 上传历史。

**Phase 2（待开发）**：模板系统（字段映射、版本管理、异步完整校验）。目前 `/admin/template` 页面为占位状态，模板相关接口尚未实现。
