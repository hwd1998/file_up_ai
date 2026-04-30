# Harness 工具权限规则
## 允许
- mvn clean compile / test / spring-boot:run
- git 查看、提交
- 执行 .harness/scripts/ 下脚本
- 读取项目源码与配置

## 禁止
- rm -rf、chmod 777 等高危命令
- 访问系统敏感文件
- 未授权第三方接口
- 静默执行危险操作