# .harness 治理模板
作用：用于 Claude Code 自动生成标准化、可直接上线的 Harness 治理工程代码

==================================================
完整目录结构：
.harness/
├─ rules/
│  ├─ code.md        # 代码规范
│  ├─ tool.md        # 工具权限
│  └─ business.md    # 业务规则
├─ context/
│  ├─ arch.md        # 架构说明
│  ├─ api.md         # 接口契约
│  └─ tech.md        # 技术栈
├─ scripts/
│  ├─ check_format.sh
│  └─ run_test.sh
├─ agent.config      # Claude Agent 配置
├─ prompt.md         # 主指令模板
└─ README.md         # 使用说明
==================================================

使用方法：
1. 将 .harness 文件夹放到项目根目录
2. 对 Claude 说：
   读取 .harness 目录下所有规则，生成 Harness 治理模块完整代码

支持模块：
- 配置治理
- 环境治理
- 权限治理
- 发布策略治理
- 操作审计
- 资源配额治理