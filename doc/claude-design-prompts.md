# Claude Design Prompts — 文件上传系统

每个 Prompt 独立投喂，一次一个页面。
技术栈：Tailwind CSS + Alpine.js，前后端不分离，PC Web Only（min-width: 1280px）。

---

## Prompt 01 · 登录页

Design a clean login page for an enterprise file upload system called "文件上传管理系统".

Full-screen layout split into two halves side by side:

LEFT half (60% width): a rich decorative background panel using a deep blue-to-indigo gradient (from-blue-700 to-indigo-900). Inside:
- Large bold headline text in white: "企业数据上报平台" (text-4xl font-bold)
- Subtitle in blue-200: "统一上传 · 规范校验 · 实时监控"
- A decorative illustration or abstract geometric shapes (overlapping translucent circles, soft glowing blobs, or a subtle grid pattern) in lighter blue tones to fill the visual space

RIGHT half (40% width): white background, vertically centered content:
- Small logo icon + system name "文件上传管理系统" (text-lg font-semibold text-gray-800) at top center of this panel
- Spacer
- Heading "欢迎回来" (text-2xl font-bold text-gray-900)
- Sub-text "请使用企业飞书账号登录" (text-sm text-gray-400)
- A prominent login button (full width, h-12, rounded-lg, bg-blue-600 hover:bg-blue-700) with Feishu logo icon on left + "使用飞书账号登录" in white
- Helper text: "点击后将跳转至飞书授权页面" (text-xs text-gray-400, text-center, mt-2)
- Footer copyright "© 2026 企业数据中台" at the very bottom of the right panel (text-xs text-gray-300)

Use Tailwind CSS. The overall feel should be modern, professional, enterprise-grade.

---

## Prompt 02 · 业务用户首页（目录树）

Design a PC web dashboard page for business users of a file upload system.

Page layout (full screen, min-width 1280px):
- Top header bar (white, border-b, h-16): left side has logo + system name, right side has user avatar + name + dropdown arrow
- Left sidebar (w-56, bg-white, border-r, full height): collapsible directory tree navigation
- Main content area (flex-1, bg-gray-100, p-6)

Left sidebar tree structure:
- Search input at top with magnifier icon
- Tree nodes with expand/collapse chevron icons
- Leaf nodes (lowest level directories) show a folder icon + directory name + two small action buttons on hover: "上传" (blue) and "历史" (gray)
- Leaf nodes have colored status badges on the right: green "已上传", red "已逾期", gray "待上传"
- Indentation per level: pl-4

Main content area:
- Welcome message: "欢迎，张三" with today's date

Use Tailwind CSS. Clean, minimal enterprise style.

---

## Prompt 03 · 文件上传页

Design a file upload page for an enterprise data reporting system. PC web, Tailwind CSS.

Page header: breadcrumb navigation "首页 / 产品营销 / 某品牌 / 月度费用报表", plus a "查看上传历史" button (text-sm, outlined) aligned right. Below breadcrumb: template info line "模板：月度费用报表 V1.2" with a "下载模板" link.

Main content: two-column layout (gap-6):

LEFT column (40%, white card, rounded-xl, p-6):
- Section title "填写上传信息"
- Form fields with labels:
  - "费用发生月份 *" → month picker input
  - "投放工具 *" → radio button group (品牌投放 / 渠道投放 / 自营)
  - "备注" → textarea (optional)
- At the bottom: a secondary ghost button "本周期无数据申报" (full width, border-dashed)
- Primary button "确认上传" (bg-blue-600, full width)

RIGHT column (60%, white card, rounded-xl, p-6):
- Section title "上传文件区"
- Large drag-and-drop zone (border-2 border-dashed border-gray-300, rounded-xl, h-48, bg-gray-50): center icon + "拖拽文件到此处，或 点击选择文件" + hint text "支持 .xls / .xlsx / .csv，单文件 ≤ 2GB，单次最多 40 个文件"
- Below the drop zone: file list table (appears after file selection) with columns: file name, size, status badge (pending/checking/success/error), delete icon
- Overall progress bar at bottom (bg-blue-600, rounded-full, with percentage label)

Status badges: gray "等待中", blue "校验中", green "通过", red "失败"

---

## Prompt 04 · 上传历史列表页

Design an upload history list page for an enterprise file upload system. PC web, Tailwind CSS.

Page header: breadcrumb "首页 / 产品营销 / 月度费用报表 / 上传历史"

Filter bar (white card, p-4, rounded-xl, mb-4):
- Date range picker, status dropdown (全部/成功/失败/校验失败/申报), file name search input, and a blue "查询" button — all in one horizontal row

Results table (white card, rounded-xl):
- Table columns: 任务ID, 文件名, 上传时间, 状态, 操作
- Status cells use colored badges: green "成功", red "校验失败", orange "处理中", gray "已申报"
- Operations column: text links "详情" (blue) and "下载" (gray), conditionally shown
- Striped rows (even rows bg-gray-50)
- Pagination bar at bottom right: page numbers + "共 48 条" total count

Right side-panel (slide-over, fixed, w-[640px], white, shadow-xl) — shown when clicking "详情":
- Header: "任务详情 #1022" + close X button
- Info grid: file name, upload time, status badge, processing duration
- Error detail table below: row number, column name, error description

---

## Prompt 05 · 模板列表页（管理员）

Design an admin template management list page. PC web, Tailwind CSS.

Admin layout:
- Top header (white, border-b): logo left, "管理后台" nav tag, user avatar right
- Left sidebar (w-56): vertical nav menu with active highlight on "模板管理"; other items: 目录管理, 用户管理, 监控看板, 告警配置

Main content (bg-gray-100, p-6):
- Page title "模板管理" (text-xl font-semibold)
- Action bar: "+ 新建模板" button (bg-blue-600) on the left, search input + status dropdown (全部/已发布/停用/草稿) on the right
- Table (white card, rounded-xl):
  - Columns: 模板名称, 所属目录, 当前版本, 状态, 最后更新, 操作
  - Status badges: green "已发布", gray "停用", yellow "草稿"
  - Operations: text link buttons "新建版本" "查看历史" "复制" "停用" (each in different muted colors, small text)
  - Rows are separated by light gray borders

---

## Prompt 06 · 模板创建向导（5步）

Design a 5-step wizard page for creating an upload template. PC web, Tailwind CSS.

Top stepper (white card, p-4, rounded-xl, mb-6):
Horizontal step indicators connected by lines:
Step 1 "基础信息" (active, blue circle), Step 2 "样例上传与建表", Step 3 "上传表单配置", Step 4 "校验规则", Step 5 "发布确认"
Active step: filled blue-600 circle with white number. Completed steps: filled gray-400 with checkmark. Future steps: white circle with gray border.

Step 1 content (white card, rounded-xl, p-6):
- Form field "模板名称 *": text input with helper text "同一目录内不可重名"
- Form field "所属目录 *": cascading select showing "产品营销 > 某品牌 > 月度费用报表" with a warning note "已绑定模板的目录不可再次绑定" in amber
- Form field "模板描述": textarea, optional
- Bottom navigation bar: "下一步 →" button right-aligned (bg-blue-600)

The overall page should feel like a clean enterprise form wizard, with generous whitespace and clear visual hierarchy.

---

## Prompt 07 · 字段映射画布（Step 2）

Design a field mapping canvas UI for a data template wizard step. PC web, Tailwind CSS.

Top section (white card, p-4, rounded-xl, mb-4):
- File upload area for sample file (compact, h-24, dashed border): "上传样例文件（≤100行）支持 .xls/.xlsx/.csv"
- Radio group below: "○ 一键建表" / "○ 选择已有表"

Main mapping canvas (white card, rounded-xl, p-6):
Three-column layout:
- LEFT column (30%): "文件字段" header, list of field cards (white, border, rounded-lg, p-3, shadow-sm): each shows field name + detected type tag (e.g., VARCHAR, DATE, DECIMAL) as a small gray badge
- CENTER column (40%): SVG connection lines drawn between connected fields; unconnected fields show dashed gray lines; connected fields show solid blue-600 lines with arrow tips
- RIGHT column (30%): "目标表字段" header, list of target table field cards in same card style

Below each unmatched field: small action links "+ 常量映射" and "跳过此列" in text-xs text-blue-500

Bottom navigation: "← 上一步" (outlined) and "下一步 →" (bg-blue-600), right-aligned

---

## Prompt 08 · 校验规则配置（Step 4）

Design a validation rules configuration step for a template wizard. PC web, Tailwind CSS.

Page content (white card, rounded-xl, p-6):
- Section header "校验规则配置" + right-aligned dropdown button "从已有模板导入规则集"

Two rule groups:

Group 1 — "必须校验项（不可关闭）" (gray-100 bg, rounded-lg, p-4):
Each rule row: lock icon + rule name + description + optional inline config. Rules:
- 表头字段名称匹配 — toggle "大小写敏感" (disabled-looking toggle, default off)
- 必填列校验 — shows tag list of required columns
- 字段类型校验 — no extra config

Group 2 — "可选校验项" (white):
Each rule row has: toggle switch (Alpine.js x-model style) + rule name + inline config that expands when toggle is ON:
- 枚举值校验 → shows chip list of allowed values when enabled
- 正则表达式校验 → shows regex input field
- 数值范围校验 → shows min/max number inputs
- 跨列依赖校验 → shows two column selectors + operator
- 文件内唯一性 → shows multi-column selector
- 唯一键去重策略 → shows radio group: 跳过/覆盖更新/整批拒绝
- 脏数据阈值 → shows percentage number input

Bottom nav: "← 上一步" + "下一步 →"

---

## Prompt 09 · 监控看板（管理员）

Design an upload monitoring dashboard page for IT admins. PC web, Tailwind CSS.

Admin sidebar layout (same as template list page).

Main content (bg-gray-100, p-6):
- Page title "上报监控看板"
- Filter bar: month picker "2026-04" + "导出报表" button (outlined, with Excel icon)

Top stats row — 4 stat cards (white, rounded-xl, p-4, shadow-sm):
- "总目录数 28" (gray)
- "已上传 18" (green-50 bg, green-600 text, green dot)
- "未上传 7" (red-50 bg, red-600 text, red dot)
- "上传失败 3" (amber-50 bg, amber-600 text, amber dot)

Main table (white card, rounded-xl):
- Tree-structured rows: group rows (directory names, bg-gray-50, bold) expand to show user rows
- Columns: 目录/用户, 本期上报状态, 上传时间, 操作
- Status cells: green badge "已上传", red badge "未上传", amber badge "上传失败", blue badge "无数据申报"
- Operations: "查看" link (blue) only
- Tree expand/collapse via chevron icons on group rows

Color legend row at page bottom: small colored dots with labels.

---

## Prompt 10 · 告警配置页（管理员）

Design an alert rules configuration page for IT admins. PC web, Tailwind CSS.

Admin sidebar layout. Main content (bg-gray-100, p-6):

Page title "告警配置" + "保存配置" button (bg-blue-600) top right.

Section 1 — "告警阈值配置" (white card, rounded-xl, p-6, mb-4):
Table with columns: 监控指标, 阈值, 告警级别, 通知对象
Rows (each fully editable inline):
- 上传失败率 | [10] % | 严重 (red badge) | IT管理员飞书群
- 单文件解析耗时 | [30] 秒 | 警告 (amber badge) | IT管理员
- 队列堆积数 | [500] 条 | 警告 | IT管理员
- 并发上传用户数 | [60] 人 | 提示 (blue badge) | IT管理员
- 存储连续失败次数 | [3] 次 | 严重 | IT管理员飞书群
Input fields in table cells: small number inputs (w-20, border, rounded, text-center)

Section 2 — "飞书通知配置" (white card, rounded-xl, p-6):
- Label "告警接收群 Webhook" + full-width text input for URL
- "测试发送" button (outlined, blue) inline right
