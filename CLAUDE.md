# project:java-spring-boot

##工具配置
- maven: 使用 ./mvnw 构建
- JDK: 版本 21
- Spring Boot: 版本 3.x

## 编码规范
- 使用 Lombok 简化代码
- 代码遵循 Alibaba Java 编码规范
- 项目结构严格遵循Maven规范
- 源码目录：src/main/java/com/example/demo
- 配置目录：src/main/resources
- 测试目录：src/test/java
- 包结构 主启动类：DemoApplication.java 放在 com.example.demo 包下
- 包结构 子包：controller、entity、repository、service
- 核心依赖：Spring Web、Spring Data JPA、MySQL Driver
- 配置文件生成 application.properties（不要用yml），配置服务器端口8080和MySQL连接基础信息
- 生成标准Maven项目文件：pom.xml（完整依赖配置，指定Spring Boot 3.x父依赖、JDK21）
- 前端样式使用 Tailwind CSS 3.x + Alpine.js
- 账号密码做数据库存储时默认MD5加密

# 项目架构说明
- 本项目为前后端不分离架构，前后端代码在同一个项目中，不做前后端分离，不生成独立前端工程。

## 常用命令
- 编译: ./mvnw clean compile 
- 测试: ./mvnw test 
- 运行: ./mvnw spring-boot:run



##产品方案设计
- 产品方案设计遵循product-design-template.md 模板要求

##技术方案设计
- 技术方案设计遵循tech-design-template.md 模板要求

## 核心指令
本项目已启用 .harness 治理体系，所有代码生成必须：
1. 优先读取并严格遵守 `.harness` 目录下全部规则
2. 技术栈、工程结构、代码风格、接口规范、业务约束 全部以 .harness 为准
3. 禁止重复定义、禁止自行扩展规范
4. 生成代码必须可直接编译、可直接运行


## 任务清单 & 技术债务

- 每次对话开始前，必须读取并理解 `TODO.md` 的完整内容
- 实现新功能后，更新 `## ✅ 已完成` 区域
- 采用临时方案绕过某个问题时，必须在 `## 🔄 已绕过` 表格中新增一行
- 禁止删除"已绕过"记录，除非对应的目标方案已实际落地