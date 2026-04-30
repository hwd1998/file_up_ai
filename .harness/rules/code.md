# Harness 代码规范（AI 必须 100% 遵守）

## 后端规则

### 一、强制技术栈
- Java 21
- Spring Boot 3.2.x
- Spring Web
- Spring Security (如需权限控制)
- MybatisPlus
- Swagger (SpringDoc)
- Lombok
- Hutool
- MapStruct(简单对象转换可用Hutool的BeanUtil，针对复杂的对象转换通过 mapstruct + default 配合实现)
- Maven
- MySQL 8.0
- 编码遵循阿里巴巴 Java 开发手册

### 二、强制工程结构
```
com.xxx.harness
├─ controller    # 接口统一前缀 /harness/api/v1
├─ service       # 业务逻辑层，必须通过 Mapper 访问数据库，返回 DTO 而非实体类
│   └─ impl
├── model               # 实体模型
│   ├── entity          # MyBatisPlus 实体
│   ├── dto             # 数据传输对象
│   └── form            # 表单对象
├─ mapper        # 必须继承 BaseMapper，使用 MyBatis-Plus 提供的 CRUD 方法
├─ common
├─ exception
├─ config       # 配置类
└─ util         # 工具类
```
### 三、代码铁律
1. 统一返回 Result<T>：code/msg/data
2. 全局异常：GlobalExceptionHandler + BusinessException
3. 实体必须含：id、create_time、update_time、is_deleted
4. 日志统一 @Slf4j
5. 接口标准 RESTful：GET/POST/PUT/DELETE
7. 禁止硬编码敏感信息
8. 代码可直接编译运行
9. 严格遵循 SOLID、DRY、KISS、YAGNI 原则
10. 遵循 **OWASP 安全最佳实践**（如输入验证、SQL注入防护）

### 四、安全与性能规范
1. **输入校验**：使用 `@Valid` 注解 + JSR-303 校验注解
2. **事务管理**：`@Transactional` 注解仅标注在 Service 方法上
3. **性能优化**：使用 `QueryWrapper` 或 `LambdaQueryWrapper` 构建复杂查询条件
4. **密码处理**：使用 `BCrypt` 加密算法

### 五、代码风格规范
- 类名：`UpperCamelCase`（如 `UserServiceImpl`）
- 方法/变量名：`lowerCamelCase`（如 `saveUser`）
- 常量：`UPPER_SNAKE_CASE`（如 `MAX_LOGIN_ATTEMPTS`）
- 方法必须添加 Javadoc 注释

## 项目架构说明
本项目为前后端不分离架构，前后端代码在同一个项目中，不做前后端分离，不生成独立前端工程。