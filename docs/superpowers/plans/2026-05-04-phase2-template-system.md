# Phase 2 模板系统 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现完整的模板系统，包括 5 步向导创建模板、异步完整校验（6类规则）、批量写入 biz_* 表、飞书通知、前端列表+向导页。

**Architecture:** Spring @Async 线程池替代 RabbitMQ 处理异步校验；文件解析用 POI/OpenCSV；责任链模式执行 6 类校验规则；JdbcTemplate 动态写入 biz_* 表。

**Tech Stack:** Spring Boot 3.2 / MyBatis-Plus / Apache POI 5.x / OpenCSV / JdbcTemplate / Thymeleaf + Alpine.js

---

## 文件清单

### 新建
```
src/main/java/com/example/upload/
  config/AsyncConfig.java
  validation/ValidationRule.java
  validation/ValidationResult.java
  validation/RowError.java
  validation/ValidationContext.java
  validation/ValidationChain.java
  validation/rules/HeaderMatchRule.java
  validation/rules/RequiredFieldRule.java
  validation/rules/TypeCheckRule.java
  validation/rules/EnumValueRule.java
  validation/rules/RegexRule.java
  validation/rules/DirtyThresholdRule.java
  service/FileParseService.java
  service/DdlService.java
  service/TemplateService.java
  service/AsyncValidationService.java
  service/impl/FileParseServiceImpl.java
  service/impl/DdlServiceImpl.java
  service/impl/TemplateServiceImpl.java
  service/impl/AsyncValidationServiceImpl.java
  controller/TemplateController.java
  model/dto/ParsedFieldDTO.java
  model/dto/DdlPreviewDTO.java
  model/dto/TemplateDTO.java
  model/dto/TemplateVersionDTO.java
src/main/resources/templates/admin/template-wizard.html
src/test/java/com/example/upload/validation/rules/HeaderMatchRuleTest.java
src/test/java/com/example/upload/validation/rules/TypeCheckRuleTest.java
src/test/java/com/example/upload/service/FileParseServiceTest.java
```

### 修改
```
src/main/java/com/example/upload/service/impl/UploadServiceImpl.java  (mergeAndValidate 接入 async)
src/main/java/com/example/upload/controller/ViewController.java        (新增 /admin/templates 路由)
src/main/resources/templates/admin/template.html                       (改造为真实列表页)
src/main/resources/application.properties                              (新增 async 配置)
```

---

## Task 1: AsyncConfig + AsyncValidationService 骨架

**Files:**
- Create: `src/main/java/com/example/upload/config/AsyncConfig.java`
- Create: `src/main/java/com/example/upload/service/AsyncValidationService.java`
- Create: `src/main/java/com/example/upload/service/impl/AsyncValidationServiceImpl.java`

- [ ] **Step 1: 创建 AsyncConfig**

```java
// src/main/java/com/example/upload/config/AsyncConfig.java
package com.example.upload.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@EnableAsync
@Configuration
public class AsyncConfig {

    @Bean("validationExecutor")
    public Executor validationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("validation-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
```

- [ ] **Step 2: 创建 AsyncValidationService 接口**

```java
// src/main/java/com/example/upload/service/AsyncValidationService.java
package com.example.upload.service;

public interface AsyncValidationService {
    void submitAsync(Long taskId);
}
```

- [ ] **Step 3: 创建 AsyncValidationServiceImpl 骨架**

```java
// src/main/java/com/example/upload/service/impl/AsyncValidationServiceImpl.java
package com.example.upload.service.impl;

import com.example.upload.common.enums.TaskStatus;
import com.example.upload.mapper.UploadTaskMapper;
import com.example.upload.model.entity.UploadTask;
import com.example.upload.service.AsyncValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncValidationServiceImpl implements AsyncValidationService {

    private final UploadTaskMapper uploadTaskMapper;

    @Override
    @Async("validationExecutor")
    public void submitAsync(Long taskId) {
        log.info("异步校验开始 taskId={}", taskId);
        UploadTask task = uploadTaskMapper.selectById(taskId);
        if (task == null) {
            log.error("任务不存在 taskId={}", taskId);
            return;
        }
        // Phase 2 Task 9 完整实现
        task.setStatus(TaskStatus.PROCESSING.getCode());
        uploadTaskMapper.updateById(task);
        log.info("异步校验骨架执行完成 taskId={}", taskId);
    }
}
```

- [ ] **Step 4: 修改 application.properties，追加异步配置**

在文件末尾追加：
```properties
## 异步校验线程池
spring.task.execution.pool.core-size=5
spring.task.execution.pool.max-size=10
spring.task.execution.pool.queue-capacity=50
```

- [ ] **Step 5: 编译验证**

```bash
mvn clean compile -q
```
预期：BUILD SUCCESS，无报错。

- [ ] **Step 6: 提交**

```bash
git add src/main/java/com/example/upload/config/AsyncConfig.java \
        src/main/java/com/example/upload/service/AsyncValidationService.java \
        src/main/java/com/example/upload/service/impl/AsyncValidationServiceImpl.java \
        src/main/resources/application.properties
git commit -m "feat: add async validation service skeleton with thread pool"
```

---

## Task 2: UploadService 接入异步服务

**Files:**
- Modify: `src/main/java/com/example/upload/service/impl/UploadServiceImpl.java`

- [ ] **Step 1: 注入 AsyncValidationService，替换 TODO**

在 `UploadServiceImpl` 中：

1. 在类字段区添加：
```java
private final AsyncValidationService asyncValidationService;
```

2. 将 `mergeAndValidate` 方法末尾的注释和 TODO 替换为：
```java
        // 快速校验通过，提交异步完整校验
        task.setStatus(TaskStatus.VALIDATING.getCode());
        uploadTaskMapper.updateById(task);
        asyncValidationService.submitAsync(taskId);
        log.info("任务 {} 快速校验通过，已提交异步完整校验", taskId);
```

完整替换范围（原第 153-157 行）：
```java
        // 快速校验通过，更新状态为处理中（等待异步完整校验，Phase2 实现）
        task.setStatus(TaskStatus.PROCESSING.getCode());
        uploadTaskMapper.updateById(task);
        log.info("任务 {} 快速校验通过，等待异步处理", taskId);
        // TODO Phase2: 投递 RabbitMQ 消息触发异步完整校验
```
替换为：
```java
        // 快速校验通过，提交异步完整校验
        task.setStatus(TaskStatus.VALIDATING.getCode());
        uploadTaskMapper.updateById(task);
        asyncValidationService.submitAsync(taskId);
        log.info("任务 {} 快速校验通过，已提交异步完整校验", taskId);
```

- [ ] **Step 2: 编译验证**

```bash
mvn clean compile -q
```
预期：BUILD SUCCESS。

- [ ] **Step 3: 提交**

```bash
git add src/main/java/com/example/upload/service/impl/UploadServiceImpl.java
git commit -m "feat: wire async validation service into upload merge flow"
```

---

## Task 3: FileParseService — 样例文件解析 + 类型推断

**Files:**
- Create: `src/main/java/com/example/upload/model/dto/ParsedFieldDTO.java`
- Create: `src/main/java/com/example/upload/service/FileParseService.java`
- Create: `src/main/java/com/example/upload/service/impl/FileParseServiceImpl.java`
- Test: `src/test/java/com/example/upload/service/FileParseServiceTest.java`

- [ ] **Step 1: 创建 ParsedFieldDTO**

```java
// src/main/java/com/example/upload/model/dto/ParsedFieldDTO.java
package com.example.upload.model.dto;

import lombok.Data;

@Data
public class ParsedFieldDTO {
    private String columnName;
    private String inferredType; // int / decimal / date / varchar
    private int fieldOrder;
}
```

- [ ] **Step 2: 创建 FileParseService 接口**

```java
// src/main/java/com/example/upload/service/FileParseService.java
package com.example.upload.service;

import com.example.upload.model.dto.ParsedFieldDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface FileParseService {
    /** 解析样例文件，返回字段列表（表头+推断类型） */
    List<ParsedFieldDTO> parseSample(MultipartFile file);

    /** 解析文件全量数据，返回 header + rows（用于异步校验） */
    ParsedResult parseAll(String filePath);

    record ParsedResult(List<String> headers, List<Map<String, String>> rows) {}
}
```

- [ ] **Step 3: 创建 FileParseServiceImpl**

```java
// src/main/java/com/example/upload/service/impl/FileParseServiceImpl.java
package com.example.upload.service.impl;

import com.example.upload.exception.BusinessException;
import com.example.upload.model.dto.ParsedFieldDTO;
import com.example.upload.service.FileParseService;
import com.opencsv.CSVReader;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@Slf4j
@Service
public class FileParseServiceImpl implements FileParseService {

    private static final int SAMPLE_ROWS = 20;
    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd"),
            DateTimeFormatter.ofPattern("yyyyMMdd")
    );

    @Override
    public List<ParsedFieldDTO> parseSample(MultipartFile file) {
        String name = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
        try {
            if (name.endsWith(".xlsx") || name.endsWith(".xls")) {
                return parseSampleExcel(file.getInputStream());
            } else if (name.endsWith(".csv")) {
                return parseSampleCsv(file.getInputStream());
            } else {
                throw new BusinessException("仅支持 .xlsx / .xls / .csv 格式");
            }
        } catch (IOException e) {
            throw new BusinessException("文件读取失败：" + e.getMessage());
        }
    }

    @Override
    public ParsedResult parseAll(String filePath) {
        File f = new File(filePath);
        if (!f.exists()) throw new BusinessException("文件不存在：" + filePath);
        String name = filePath.toLowerCase();
        try {
            if (name.endsWith(".xlsx") || name.endsWith(".xls")) {
                return parseAllExcel(new FileInputStream(f));
            } else {
                return parseAllCsv(new FileInputStream(f));
            }
        } catch (IOException e) {
            throw new BusinessException("文件解析失败：" + e.getMessage());
        }
    }

    // ── 私有方法 ────────────────────────────────────────────────

    private List<ParsedFieldDTO> parseSampleExcel(InputStream is) throws IOException {
        try (Workbook wb = new XSSFWorkbook(is)) {
            Sheet sheet = wb.getSheetAt(0);
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) throw new BusinessException("文件无表头行");

            int colCount = headerRow.getLastCellNum();
            List<String>[] samples = new ArrayList[colCount];
            for (int i = 0; i < colCount; i++) samples[i] = new ArrayList<>();

            int dataRows = 0;
            for (int r = 1; r <= sheet.getLastRowNum() && dataRows < SAMPLE_ROWS; r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                for (int c = 0; c < colCount; c++) {
                    Cell cell = row.getCell(c);
                    String val = cellToString(cell);
                    if (val != null && !val.isBlank()) samples[c].add(val);
                }
                dataRows++;
            }

            List<ParsedFieldDTO> result = new ArrayList<>();
            for (int c = 0; c < colCount; c++) {
                Cell hCell = headerRow.getCell(c);
                String colName = hCell != null ? hCell.toString().trim() : "column_" + c;
                ParsedFieldDTO dto = new ParsedFieldDTO();
                dto.setColumnName(colName);
                dto.setInferredType(inferType(samples[c]));
                dto.setFieldOrder(c);
                result.add(dto);
            }
            return result;
        }
    }

    private List<ParsedFieldDTO> parseSampleCsv(InputStream is) throws IOException {
        try (CSVReader reader = new CSVReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String[] headers = reader.readNext();
            if (headers == null) throw new BusinessException("文件无表头行");

            List<String>[] samples = new ArrayList[headers.length];
            for (int i = 0; i < headers.length; i++) samples[i] = new ArrayList<>();

            String[] line;
            int count = 0;
            while ((line = reader.readNext()) != null && count < SAMPLE_ROWS) {
                for (int c = 0; c < headers.length && c < line.length; c++) {
                    if (line[c] != null && !line[c].isBlank()) samples[c].add(line[c]);
                }
                count++;
            }

            List<ParsedFieldDTO> result = new ArrayList<>();
            for (int c = 0; c < headers.length; c++) {
                ParsedFieldDTO dto = new ParsedFieldDTO();
                dto.setColumnName(headers[c].trim());
                dto.setInferredType(inferType(samples[c]));
                dto.setFieldOrder(c);
                result.add(dto);
            }
            return result;
        }
    }

    private ParsedResult parseAllExcel(InputStream is) throws IOException {
        try (Workbook wb = new XSSFWorkbook(is)) {
            Sheet sheet = wb.getSheetAt(0);
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) return new ParsedResult(List.of(), List.of());

            List<String> headers = new ArrayList<>();
            for (int c = 0; c < headerRow.getLastCellNum(); c++) {
                Cell cell = headerRow.getCell(c);
                headers.add(cell != null ? cell.toString().trim() : "");
            }

            List<Map<String, String>> rows = new ArrayList<>();
            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                Map<String, String> map = new LinkedHashMap<>();
                for (int c = 0; c < headers.size(); c++) {
                    Cell cell = row.getCell(c);
                    map.put(headers.get(c), cellToString(cell) != null ? cellToString(cell) : "");
                }
                rows.add(map);
            }
            return new ParsedResult(headers, rows);
        }
    }

    private ParsedResult parseAllCsv(InputStream is) throws IOException {
        try (CSVReader reader = new CSVReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String[] headerArr = reader.readNext();
            if (headerArr == null) return new ParsedResult(List.of(), List.of());
            List<String> headers = Arrays.stream(headerArr).map(String::trim).toList();

            List<Map<String, String>> rows = new ArrayList<>();
            String[] line;
            while ((line = reader.readNext()) != null) {
                Map<String, String> map = new LinkedHashMap<>();
                for (int c = 0; c < headers.size(); c++) {
                    map.put(headers.get(c), c < line.length ? line[c] : "");
                }
                rows.add(map);
            }
            return new ParsedResult(headers, rows);
        }
    }

    private String inferType(List<String> samples) {
        if (samples.isEmpty()) return "varchar";
        long intCount = samples.stream().filter(this::isInteger).count();
        if (intCount >= samples.size() * 0.8) return "int";
        long decCount = samples.stream().filter(this::isDecimal).count();
        if (decCount >= samples.size() * 0.8) return "decimal";
        long dateCount = samples.stream().filter(this::isDate).count();
        if (dateCount >= samples.size() * 0.8) return "date";
        return "varchar";
    }

    private boolean isInteger(String s) {
        try { Long.parseLong(s.trim()); return true; } catch (NumberFormatException e) { return false; }
    }

    private boolean isDecimal(String s) {
        try { new java.math.BigDecimal(s.trim()); return true; } catch (NumberFormatException e) { return false; }
    }

    private boolean isDate(String s) {
        for (DateTimeFormatter fmt : DATE_FORMATS) {
            try { LocalDate.parse(s.trim(), fmt); return true; } catch (DateTimeParseException ignored) {}
        }
        return false;
    }

    private String cellToString(Cell cell) {
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> DateUtil.isCellDateFormatted(cell)
                    ? cell.getLocalDateTimeCellValue().toLocalDate().toString()
                    : String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCachedFormulaResultType() == CellType.NUMERIC
                    ? String.valueOf((long) cell.getNumericCellValue())
                    : cell.getStringCellValue();
            default -> null;
        };
    }
}
```

- [ ] **Step 4: 编译验证**

```bash
mvn clean compile -q
```
预期：BUILD SUCCESS。

- [ ] **Step 5: 提交**

```bash
git add src/main/java/com/example/upload/model/dto/ParsedFieldDTO.java \
        src/main/java/com/example/upload/service/FileParseService.java \
        src/main/java/com/example/upload/service/impl/FileParseServiceImpl.java
git commit -m "feat: add file parse service with type inference for Excel/CSV"
```

---

## Task 4: DdlService — DDL 生成 + 建表执行

**Files:**
- Create: `src/main/java/com/example/upload/model/dto/DdlPreviewDTO.java`
- Create: `src/main/java/com/example/upload/service/DdlService.java`
- Create: `src/main/java/com/example/upload/service/impl/DdlServiceImpl.java`

- [ ] **Step 1: 创建 DdlPreviewDTO**

```java
// src/main/java/com/example/upload/model/dto/DdlPreviewDTO.java
package com.example.upload.model.dto;

import lombok.Data;

@Data
public class DdlPreviewDTO {
    private String tableName;
    private String ddlSql;
}
```

- [ ] **Step 2: 创建 DdlService 接口**

```java
// src/main/java/com/example/upload/service/DdlService.java
package com.example.upload.service;

import com.example.upload.model.dto.DdlPreviewDTO;
import com.example.upload.model.entity.TemplateField;

import java.util.List;

public interface DdlService {
    /** 根据模板字段列表生成 DDL 预览（不执行） */
    DdlPreviewDTO generateDdl(Long templateId, int majorVersion, List<TemplateField> fields);

    /** 执行建表 DDL，返回实际表名 */
    String executeCreateTable(DdlPreviewDTO ddl);
}
```

- [ ] **Step 3: 创建 DdlServiceImpl**

```java
// src/main/java/com/example/upload/service/impl/DdlServiceImpl.java
package com.example.upload.service.impl;

import com.example.upload.exception.BusinessException;
import com.example.upload.model.dto.DdlPreviewDTO;
import com.example.upload.model.entity.TemplateField;
import com.example.upload.service.DdlService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DdlServiceImpl implements DdlService {

    private final JdbcTemplate jdbcTemplate;

    private static final java.util.regex.Pattern SAFE_NAME = java.util.regex.Pattern.compile("^[a-zA-Z0-9_]+$");

    @Override
    public DdlPreviewDTO generateDdl(Long templateId, int majorVersion, List<TemplateField> fields) {
        String tableName = "biz_" + templateId + "_v" + majorVersion;
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE TABLE IF NOT EXISTS `").append(tableName).append("` (\n");
        sb.append("  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,\n");
        sb.append("  `task_id` BIGINT NOT NULL COMMENT '关联上传任务ID',\n");
        sb.append("  `_version` VARCHAR(16) NOT NULL COMMENT '模板版本号',\n");

        List<TemplateField> active = fields.stream()
                .filter(f -> f.getIsSkipped() == null || f.getIsSkipped() == 0)
                .sorted(Comparator.comparing(TemplateField::getFieldOrder))
                .toList();

        for (TemplateField field : active) {
            String col = field.getTargetColumn();
            if (col == null || col.isBlank()) continue;
            if (!SAFE_NAME.matcher(col).matches()) {
                throw new BusinessException("列名包含非法字符：" + col + "（仅允许字母、数字、下划线）");
            }
            sb.append("  `").append(col).append("` ").append(toMysqlType(field.getFieldType())).append(",\n");
        }

        sb.append("  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP\n");
        sb.append(") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模板数据表';\n");

        DdlPreviewDTO dto = new DdlPreviewDTO();
        dto.setTableName(tableName);
        dto.setDdlSql(sb.toString());
        return dto;
    }

    @Override
    public String executeCreateTable(DdlPreviewDTO ddl) {
        log.info("执行建表 DDL: {}", ddl.getTableName());
        jdbcTemplate.execute(ddl.getDdlSql());
        log.info("建表成功: {}", ddl.getTableName());
        return ddl.getTableName();
    }

    private String toMysqlType(String fieldType) {
        if (fieldType == null) return "VARCHAR(512)";
        return switch (fieldType.toLowerCase()) {
            case "int" -> "BIGINT";
            case "decimal" -> "DECIMAL(18,4)";
            case "date" -> "DATE";
            case "datetime" -> "DATETIME";
            default -> "VARCHAR(512)";
        };
    }
}
```

- [ ] **Step 4: 编译验证**

```bash
mvn clean compile -q
```
预期：BUILD SUCCESS。

- [ ] **Step 5: 提交**

```bash
git add src/main/java/com/example/upload/model/dto/DdlPreviewDTO.java \
        src/main/java/com/example/upload/service/DdlService.java \
        src/main/java/com/example/upload/service/impl/DdlServiceImpl.java
git commit -m "feat: add DDL generation and table creation service"
```

---

## Task 5: 校验引擎基础设施

**Files:**
- Create: `src/main/java/com/example/upload/validation/RowError.java`
- Create: `src/main/java/com/example/upload/validation/ValidationResult.java`
- Create: `src/main/java/com/example/upload/validation/ValidationRule.java`
- Create: `src/main/java/com/example/upload/validation/ValidationContext.java`
- Create: `src/main/java/com/example/upload/validation/ValidationChain.java`

- [ ] **Step 1: 创建 RowError**

```java
// src/main/java/com/example/upload/validation/RowError.java
package com.example.upload.validation;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RowError {
    private int rowNumber;
    private String columnName;
    private String errorType;
    private String errorMessage;
}
```

- [ ] **Step 2: 创建 ValidationResult**

```java
// src/main/java/com/example/upload/validation/ValidationResult.java
package com.example.upload.validation;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class ValidationResult {
    private boolean aborted = false; // true = 整批拒绝，停止后续规则
    private final List<RowError> errors = new ArrayList<>();

    public void addError(int row, String col, String type, String msg) {
        errors.add(new RowError(row, col, type, msg));
    }

    public boolean hasErrors() { return !errors.isEmpty(); }
}
```

- [ ] **Step 3: 创建 ValidationRule 接口**

```java
// src/main/java/com/example/upload/validation/ValidationRule.java
package com.example.upload.validation;

public interface ValidationRule {
    ValidationResult validate(ValidationContext ctx);
    int getOrder();
}
```

- [ ] **Step 4: 创建 ValidationContext**

```java
// src/main/java/com/example/upload/validation/ValidationContext.java
package com.example.upload.validation;

import com.example.upload.model.entity.TemplateField;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class ValidationContext {
    private List<String> headers;
    private List<Map<String, String>> rows;
    private List<TemplateField> fields;
    private int dirtyThreshold; // 百分比，如 10 表示 10%
}
```

- [ ] **Step 5: 创建 ValidationChain**

```java
// src/main/java/com/example/upload/validation/ValidationChain.java
package com.example.upload.validation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ValidationChain {

    private final List<ValidationRule> rules = new ArrayList<>();

    public ValidationChain addRule(ValidationRule rule) {
        rules.add(rule);
        return this;
    }

    public ValidationResult execute(ValidationContext ctx) {
        ValidationResult merged = new ValidationResult();
        List<ValidationRule> sorted = rules.stream()
                .sorted(Comparator.comparingInt(ValidationRule::getOrder))
                .toList();

        for (ValidationRule rule : sorted) {
            ValidationResult r = rule.validate(ctx);
            merged.getErrors().addAll(r.getErrors());
            if (r.isAborted()) {
                merged.setAborted(true);
                break;
            }
        }
        return merged;
    }
}
```

- [ ] **Step 6: 编译验证**

```bash
mvn clean compile -q
```
预期：BUILD SUCCESS。

- [ ] **Step 7: 提交**

```bash
git add src/main/java/com/example/upload/validation/
git commit -m "feat: add validation engine infrastructure (Rule/Context/Chain)"
```

---

## Task 6: 6 类校验规则实现

**Files:**
- Create: `src/main/java/com/example/upload/validation/rules/HeaderMatchRule.java`
- Create: `src/main/java/com/example/upload/validation/rules/RequiredFieldRule.java`
- Create: `src/main/java/com/example/upload/validation/rules/TypeCheckRule.java`
- Create: `src/main/java/com/example/upload/validation/rules/EnumValueRule.java`
- Create: `src/main/java/com/example/upload/validation/rules/RegexRule.java`
- Create: `src/main/java/com/example/upload/validation/rules/DirtyThresholdRule.java`
- Test: `src/test/java/com/example/upload/validation/rules/HeaderMatchRuleTest.java`

- [ ] **Step 1: 创建 HeaderMatchRule（order=10，整批型）**

```java
// src/main/java/com/example/upload/validation/rules/HeaderMatchRule.java
package com.example.upload.validation.rules;

import com.example.upload.model.entity.TemplateField;
import com.example.upload.validation.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class HeaderMatchRule implements ValidationRule {

    @Override
    public ValidationResult validate(ValidationContext ctx) {
        ValidationResult result = new ValidationResult();
        Set<String> fileHeaders = ctx.getHeaders().stream()
                .map(String::trim).collect(Collectors.toSet());

        List<String> required = ctx.getFields().stream()
                .filter(f -> f.getIsSkipped() == null || f.getIsSkipped() == 0)
                .filter(f -> f.getConstantValue() == null || f.getConstantValue().isBlank())
                .map(TemplateField::getSourceColumn)
                .toList();

        for (String col : required) {
            if (!fileHeaders.contains(col)) {
                result.addError(1, col, "header_mismatch", "缺少必需列：" + col);
            }
        }
        if (result.hasErrors()) result.setAborted(true);
        return result;
    }

    @Override public int getOrder() { return 10; }
}
```

- [ ] **Step 2: 创建 RequiredFieldRule（order=20，行级）**

```java
// src/main/java/com/example/upload/validation/rules/RequiredFieldRule.java
package com.example.upload.validation.rules;

import com.example.upload.model.entity.TemplateField;
import com.example.upload.validation.*;

import java.util.List;

public class RequiredFieldRule implements ValidationRule {

    @Override
    public ValidationResult validate(ValidationContext ctx) {
        ValidationResult result = new ValidationResult();
        List<TemplateField> requiredFields = ctx.getFields().stream()
                .filter(f -> Integer.valueOf(1).equals(f.getIsRequired()))
                .filter(f -> f.getConstantValue() == null || f.getConstantValue().isBlank())
                .filter(f -> f.getIsSkipped() == null || f.getIsSkipped() == 0)
                .toList();

        int rowNum = 2;
        for (var row : ctx.getRows()) {
            for (TemplateField field : requiredFields) {
                String val = row.get(field.getSourceColumn());
                if (val == null || val.isBlank()) {
                    result.addError(rowNum, field.getSourceColumn(), "required_empty",
                            "第" + rowNum + "行「" + field.getSourceColumn() + "」不能为空");
                }
            }
            rowNum++;
        }
        return result;
    }

    @Override public int getOrder() { return 20; }
}
```

- [ ] **Step 3: 创建 TypeCheckRule（order=30，行级）**

```java
// src/main/java/com/example/upload/validation/rules/TypeCheckRule.java
package com.example.upload.validation.rules;

import com.example.upload.model.entity.TemplateField;
import com.example.upload.validation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

public class TypeCheckRule implements ValidationRule {

    private static final List<DateTimeFormatter> DATE_FMTS = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd"),
            DateTimeFormatter.ofPattern("yyyyMMdd")
    );

    @Override
    public ValidationResult validate(ValidationContext ctx) {
        ValidationResult result = new ValidationResult();
        List<TemplateField> typedFields = ctx.getFields().stream()
                .filter(f -> f.getIsSkipped() == null || f.getIsSkipped() == 0)
                .filter(f -> f.getConstantValue() == null || f.getConstantValue().isBlank())
                .filter(f -> f.getFieldType() != null && !f.getFieldType().equals("varchar"))
                .toList();

        int rowNum = 2;
        for (var row : ctx.getRows()) {
            for (TemplateField field : typedFields) {
                String val = row.get(field.getSourceColumn());
                if (val == null || val.isBlank()) { rowNum++; continue; }
                if (!matchesType(val, field.getFieldType())) {
                    result.addError(rowNum, field.getSourceColumn(), "type_mismatch",
                            "第" + rowNum + "行「" + field.getSourceColumn() + "」类型不匹配，期望" + field.getFieldType() + "，实际值：" + val);
                }
            }
            rowNum++;
        }
        return result;
    }

    private boolean matchesType(String val, String type) {
        return switch (type.toLowerCase()) {
            case "int" -> { try { Long.parseLong(val.trim()); yield true; } catch (Exception e) { yield false; } }
            case "decimal" -> { try { new java.math.BigDecimal(val.trim()); yield true; } catch (Exception e) { yield false; } }
            case "date" -> DATE_FMTS.stream().anyMatch(fmt -> { try { LocalDate.parse(val.trim(), fmt); return true; } catch (DateTimeParseException e) { return false; } });
            default -> true;
        };
    }

    @Override public int getOrder() { return 30; }
}
```

- [ ] **Step 4: 创建 EnumValueRule（order=40，行级）**

```java
// src/main/java/com/example/upload/validation/rules/EnumValueRule.java
package com.example.upload.validation.rules;

import cn.hutool.json.JSONUtil;
import com.example.upload.model.entity.TemplateField;
import com.example.upload.validation.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class EnumValueRule implements ValidationRule {

    @Override
    public ValidationResult validate(ValidationContext ctx) {
        ValidationResult result = new ValidationResult();

        List<TemplateField> enumFields = ctx.getFields().stream()
                .filter(f -> f.getIsSkipped() == null || f.getIsSkipped() == 0)
                .filter(f -> f.getValidationRules() != null && f.getValidationRules().contains("\"enum\""))
                .toList();

        int rowNum = 2;
        for (var row : ctx.getRows()) {
            for (TemplateField field : enumFields) {
                Set<String> allowed = parseEnum(field.getValidationRules());
                if (allowed.isEmpty()) continue;
                String val = row.get(field.getSourceColumn());
                if (val == null || val.isBlank()) continue;
                if (!allowed.contains(val.trim())) {
                    result.addError(rowNum, field.getSourceColumn(), "enum_invalid",
                            "第" + rowNum + "行「" + field.getSourceColumn() + "」值不在枚举范围，实际：" + val + "，允许：" + allowed);
                }
            }
            rowNum++;
        }
        return result;
    }

    private Set<String> parseEnum(String rulesJson) {
        try {
            cn.hutool.json.JSONObject obj = JSONUtil.parseObj(rulesJson);
            cn.hutool.json.JSONArray arr = obj.getJSONArray("enum");
            if (arr == null) return Set.of();
            return arr.stream().map(Object::toString).collect(Collectors.toSet());
        } catch (Exception e) {
            return Set.of();
        }
    }

    @Override public int getOrder() { return 40; }
}
```

- [ ] **Step 5: 创建 RegexRule（order=50，行级）**

```java
// src/main/java/com/example/upload/validation/rules/RegexRule.java
package com.example.upload.validation.rules;

import cn.hutool.json.JSONUtil;
import com.example.upload.model.entity.TemplateField;
import com.example.upload.validation.*;

import java.util.List;
import java.util.regex.Pattern;

public class RegexRule implements ValidationRule {

    @Override
    public ValidationResult validate(ValidationContext ctx) {
        ValidationResult result = new ValidationResult();

        List<TemplateField> regexFields = ctx.getFields().stream()
                .filter(f -> f.getIsSkipped() == null || f.getIsSkipped() == 0)
                .filter(f -> f.getValidationRules() != null && f.getValidationRules().contains("\"regex\""))
                .toList();

        int rowNum = 2;
        for (var row : ctx.getRows()) {
            for (TemplateField field : regexFields) {
                String regex = parseRegex(field.getValidationRules());
                if (regex == null || regex.isBlank()) continue;
                String val = row.get(field.getSourceColumn());
                if (val == null || val.isBlank()) continue;
                if (!Pattern.matches(regex, val.trim())) {
                    result.addError(rowNum, field.getSourceColumn(), "regex_mismatch",
                            "第" + rowNum + "行「" + field.getSourceColumn() + "」格式不符，值：" + val);
                }
            }
            rowNum++;
        }
        return result;
    }

    private String parseRegex(String rulesJson) {
        try {
            return JSONUtil.parseObj(rulesJson).getStr("regex");
        } catch (Exception e) {
            return null;
        }
    }

    @Override public int getOrder() { return 50; }
}
```

- [ ] **Step 6: 创建 DirtyThresholdRule（order=100，整批型，最后执行）**

```java
// src/main/java/com/example/upload/validation/rules/DirtyThresholdRule.java
package com.example.upload.validation.rules;

import com.example.upload.validation.*;

public class DirtyThresholdRule implements ValidationRule {

    @Override
    public ValidationResult validate(ValidationContext ctx) {
        // 此规则在 chain 最后执行，检查已累积错误行数占比
        // 注：此规则由 AsyncValidationServiceImpl 在 chain 执行后单独判断
        // 这里直接返回空结果，占位供 chain 调用
        return new ValidationResult();
    }

    /** 外部调用：判断已有错误是否超阈值 */
    public static boolean exceeds(int errorRows, int totalRows, int thresholdPercent) {
        if (totalRows == 0) return false;
        return (errorRows * 100.0 / totalRows) > thresholdPercent;
    }

    @Override public int getOrder() { return 100; }
}
```

- [ ] **Step 7: 编写 HeaderMatchRuleTest**

```java
// src/test/java/com/example/upload/validation/rules/HeaderMatchRuleTest.java
package com.example.upload.validation.rules;

import com.example.upload.model.entity.TemplateField;
import com.example.upload.validation.ValidationContext;
import com.example.upload.validation.ValidationResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class HeaderMatchRuleTest {

    private final HeaderMatchRule rule = new HeaderMatchRule();

    private TemplateField field(String source, boolean required) {
        TemplateField f = new TemplateField();
        f.setSourceColumn(source);
        f.setIsRequired(required ? 1 : 0);
        f.setIsSkipped(0);
        f.setFieldOrder(0);
        return f;
    }

    @Test
    void passes_when_all_required_columns_present() {
        ValidationContext ctx = ValidationContext.builder()
                .headers(List.of("渠道", "费用", "月份"))
                .rows(List.of())
                .fields(List.of(field("渠道", true), field("费用", true)))
                .dirtyThreshold(10)
                .build();
        ValidationResult result = rule.validate(ctx);
        assertFalse(result.hasErrors());
        assertFalse(result.isAborted());
    }

    @Test
    void aborts_when_required_column_missing() {
        ValidationContext ctx = ValidationContext.builder()
                .headers(List.of("渠道"))
                .rows(List.of())
                .fields(List.of(field("渠道", true), field("费用", true)))
                .dirtyThreshold(10)
                .build();
        ValidationResult result = rule.validate(ctx);
        assertTrue(result.hasErrors());
        assertTrue(result.isAborted());
        assertEquals("header_mismatch", result.getErrors().get(0).getErrorType());
    }
}
```

- [ ] **Step 8: 运行测试**

```bash
mvn test -pl . -Dtest=HeaderMatchRuleTest -q
```
预期：Tests run: 2, Failures: 0, Errors: 0。

- [ ] **Step 9: 编译全量验证**

```bash
mvn clean compile -q
```
预期：BUILD SUCCESS。

- [ ] **Step 10: 提交**

```bash
git add src/main/java/com/example/upload/validation/rules/ \
        src/test/java/com/example/upload/validation/
git commit -m "feat: implement 6 validation rules (header/required/type/enum/regex/threshold)"
```

---

## Task 7: TemplateService + DTOs

**Files:**
- Create: `src/main/java/com/example/upload/model/dto/TemplateDTO.java`
- Create: `src/main/java/com/example/upload/model/dto/TemplateVersionDTO.java`
- Create: `src/main/java/com/example/upload/service/TemplateService.java`
- Create: `src/main/java/com/example/upload/service/impl/TemplateServiceImpl.java`

- [ ] **Step 1: 创建 TemplateDTO**

```java
// src/main/java/com/example/upload/model/dto/TemplateDTO.java
package com.example.upload.model.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TemplateDTO {
    private Long id;
    private String name;
    private Long directoryId;
    private String directoryName;
    private String status;
    private String currentVersion;
    private BigDecimal dirtyThreshold;
    private LocalDateTime createTime;
}
```

- [ ] **Step 2: 创建 TemplateVersionDTO**

```java
// src/main/java/com/example/upload/model/dto/TemplateVersionDTO.java
package com.example.upload.model.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TemplateVersionDTO {
    private Long id;
    private String version;
    private String targetTableName;
    private String changeLog;
    private LocalDateTime createTime;
}
```

- [ ] **Step 3: 创建 TemplateService 接口**

```java
// src/main/java/com/example/upload/service/TemplateService.java
package com.example.upload.service;

import com.example.upload.common.PageResult;
import com.example.upload.model.dto.*;
import com.example.upload.model.entity.TemplateField;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface TemplateService {
    Long createDraft(String name, Long directoryId, java.math.BigDecimal dirtyThreshold, Long userId);
    List<ParsedFieldDTO> uploadSample(Long templateId, MultipartFile file, Long userId);
    DdlPreviewDTO generateDdl(Long templateId);
    void executeTable(Long templateId, Long userId);
    void saveFieldMapping(Long templateId, List<Map<String, Object>> mappings, Long userId);
    void saveValidationRules(Long templateId, List<Map<String, Object>> rules, Long userId);
    void publish(Long templateId, String changeLog, Long userId);
    void disable(Long templateId, Long userId);
    Long copy(Long templateId, Long userId);
    PageResult<TemplateDTO> listTemplates(int page, int size, Long directoryId, String status);
    List<TemplateVersionDTO> listVersions(Long templateId);
    List<TemplateField> getFieldsByVersion(Long versionId);
}
```

- [ ] **Step 4: 创建 TemplateServiceImpl**

```java
// src/main/java/com/example/upload/service/impl/TemplateServiceImpl.java
package com.example.upload.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.upload.common.PageResult;
import com.example.upload.exception.BusinessException;
import com.example.upload.mapper.*;
import com.example.upload.model.dto.*;
import com.example.upload.model.entity.*;
import com.example.upload.service.DdlService;
import com.example.upload.service.FileParseService;
import com.example.upload.service.TemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TemplateServiceImpl implements TemplateService {

    private final TemplateMapper templateMapper;
    private final TemplateVersionMapper templateVersionMapper;
    private final TemplateFieldMapper templateFieldMapper;
    private final DirectoryMapper directoryMapper;
    private final FileParseService fileParseService;
    private final DdlService ddlService;

    @Override
    @Transactional
    public Long createDraft(String name, Long directoryId, BigDecimal dirtyThreshold, Long userId) {
        // 创建模板草稿
        Template tpl = new Template();
        tpl.setName(name);
        tpl.setDirectoryId(directoryId);
        tpl.setStatus("draft");
        tpl.setDirtyThreshold(dirtyThreshold != null ? dirtyThreshold : new BigDecimal("10.00"));
        tpl.setCreatedBy(userId);
        templateMapper.insert(tpl);

        // 预建 V1.0 草稿版本
        TemplateVersion version = new TemplateVersion();
        version.setTemplateId(tpl.getId());
        version.setVersion("V1.0");
        version.setCreatedBy(userId);
        templateVersionMapper.insert(version);

        return tpl.getId();
    }

    @Override
    @Transactional
    public List<ParsedFieldDTO> uploadSample(Long templateId, MultipartFile file, Long userId) {
        Template tpl = getTemplateOrThrow(templateId);
        TemplateVersion version = getDraftVersionOrThrow(templateId);

        // 删除旧草稿字段
        templateFieldMapper.delete(new LambdaQueryWrapper<TemplateField>()
                .eq(TemplateField::getVersionId, version.getId()));

        // 解析样例文件
        List<ParsedFieldDTO> fields = fileParseService.parseSample(file);

        // 存为草稿字段
        for (ParsedFieldDTO f : fields) {
            TemplateField field = new TemplateField();
            field.setVersionId(version.getId());
            field.setFieldName(f.getColumnName());
            field.setSourceColumn(f.getColumnName());
            field.setTargetColumn(toSnakeCase(f.getColumnName()));
            field.setFieldType(f.getInferredType());
            field.setFieldOrder(f.getFieldOrder());
            field.setIsRequired(0);
            field.setIsSkipped(0);
            templateFieldMapper.insert(field);
        }
        return fields;
    }

    @Override
    public DdlPreviewDTO generateDdl(Long templateId) {
        TemplateVersion version = getDraftVersionOrThrow(templateId);
        List<TemplateField> fields = templateFieldMapper.selectList(
                new LambdaQueryWrapper<TemplateField>().eq(TemplateField::getVersionId, version.getId()));
        Template tpl = getTemplateOrThrow(templateId);
        return ddlService.generateDdl(templateId, 1, fields);
    }

    @Override
    @Transactional
    public void executeTable(Long templateId, Long userId) {
        DdlPreviewDTO ddl = generateDdl(templateId);
        String tableName = ddlService.executeCreateTable(ddl);

        TemplateVersion version = getDraftVersionOrThrow(templateId);
        version.setTargetTableName(tableName);
        templateVersionMapper.updateById(version);
    }

    @Override
    @Transactional
    public void saveFieldMapping(Long templateId, List<Map<String, Object>> mappings, Long userId) {
        TemplateVersion version = getDraftVersionOrThrow(templateId);
        for (Map<String, Object> m : mappings) {
            Long fieldId = Long.valueOf(m.get("id").toString());
            TemplateField field = templateFieldMapper.selectById(fieldId);
            if (field == null || !field.getVersionId().equals(version.getId())) continue;
            field.setSourceColumn(str(m, "sourceColumn"));
            field.setTargetColumn(str(m, "targetColumn"));
            field.setIsRequired(intVal(m, "isRequired"));
            field.setIsSkipped(intVal(m, "isSkipped"));
            String constant = str(m, "constantValue");
            field.setConstantValue(constant);
            templateFieldMapper.updateById(field);
        }
    }

    @Override
    @Transactional
    public void saveValidationRules(Long templateId, List<Map<String, Object>> rules, Long userId) {
        TemplateVersion version = getDraftVersionOrThrow(templateId);
        for (Map<String, Object> r : rules) {
            Long fieldId = Long.valueOf(r.get("id").toString());
            TemplateField field = templateFieldMapper.selectById(fieldId);
            if (field == null || !field.getVersionId().equals(version.getId())) continue;
            Object rulesJson = r.get("validationRules");
            field.setValidationRules(rulesJson != null ? rulesJson.toString() : null);
            templateFieldMapper.updateById(field);
        }
    }

    @Override
    @Transactional
    public void publish(Long templateId, String changeLog, Long userId) {
        TemplateVersion version = getDraftVersionOrThrow(templateId);
        if (version.getTargetTableName() == null) {
            throw new BusinessException("请先执行建表（Step2）后再发布");
        }
        version.setChangeLog(changeLog);
        templateVersionMapper.updateById(version);

        Template tpl = getTemplateOrThrow(templateId);
        tpl.setStatus("active");
        tpl.setCurrentVersion(version.getVersion());
        templateMapper.updateById(tpl);

        // 绑定目录的 template_id
        directoryMapper.selectById(tpl.getDirectoryId());
        Directory dir = directoryMapper.selectById(tpl.getDirectoryId());
        if (dir != null) {
            dir.setTemplateId(templateId);
            directoryMapper.updateById(dir);
        }
    }

    @Override
    public void disable(Long templateId, Long userId) {
        Template tpl = getTemplateOrThrow(templateId);
        tpl.setStatus("disabled");
        templateMapper.updateById(tpl);
    }

    @Override
    @Transactional
    public Long copy(Long templateId, Long userId) {
        Template src = getTemplateOrThrow(templateId);
        TemplateVersion srcVersion = templateVersionMapper.selectOne(
                new LambdaQueryWrapper<TemplateVersion>()
                        .eq(TemplateVersion::getTemplateId, templateId)
                        .orderByDesc(TemplateVersion::getCreateTime)
                        .last("LIMIT 1"));

        Long newId = createDraft(src.getName() + "_复制", src.getDirectoryId(), src.getDirtyThreshold(), userId);
        if (srcVersion != null) {
            TemplateVersion newVersion = getDraftVersionOrThrow(newId);
            List<TemplateField> srcFields = templateFieldMapper.selectList(
                    new LambdaQueryWrapper<TemplateField>().eq(TemplateField::getVersionId, srcVersion.getId()));
            for (TemplateField f : srcFields) {
                TemplateField copy = new TemplateField();
                copy.setVersionId(newVersion.getId());
                copy.setFieldName(f.getFieldName());
                copy.setSourceColumn(f.getSourceColumn());
                copy.setTargetColumn(f.getTargetColumn());
                copy.setFieldType(f.getFieldType());
                copy.setFieldOrder(f.getFieldOrder());
                copy.setIsRequired(f.getIsRequired());
                copy.setIsSkipped(f.getIsSkipped());
                copy.setConstantValue(f.getConstantValue());
                copy.setValidationRules(f.getValidationRules());
                templateFieldMapper.insert(copy);
            }
        }
        return newId;
    }

    @Override
    public PageResult<TemplateDTO> listTemplates(int page, int size, Long directoryId, String status) {
        LambdaQueryWrapper<Template> wrapper = new LambdaQueryWrapper<Template>()
                .orderByDesc(Template::getCreateTime);
        if (directoryId != null) wrapper.eq(Template::getDirectoryId, directoryId);
        if (status != null && !status.isBlank()) wrapper.eq(Template::getStatus, status);

        Page<Template> pageResult = templateMapper.selectPage(new Page<>(page, size), wrapper);
        List<TemplateDTO> dtos = pageResult.getRecords().stream().map(this::toDTO).toList();
        return PageResult.of(pageResult.getTotal(), page, size, dtos);
    }

    @Override
    public List<TemplateVersionDTO> listVersions(Long templateId) {
        return templateVersionMapper.selectList(
                new LambdaQueryWrapper<TemplateVersion>()
                        .eq(TemplateVersion::getTemplateId, templateId)
                        .orderByDesc(TemplateVersion::getCreateTime))
                .stream().map(this::toVersionDTO).toList();
    }

    @Override
    public List<TemplateField> getFieldsByVersion(Long versionId) {
        return templateFieldMapper.selectList(
                new LambdaQueryWrapper<TemplateField>()
                        .eq(TemplateField::getVersionId, versionId)
                        .orderByAsc(TemplateField::getFieldOrder));
    }

    // ── 私有方法 ──────────────────────────────────────────────────

    private Template getTemplateOrThrow(Long id) {
        Template tpl = templateMapper.selectById(id);
        if (tpl == null) throw new BusinessException("模板不存在");
        return tpl;
    }

    private TemplateVersion getDraftVersionOrThrow(Long templateId) {
        TemplateVersion v = templateVersionMapper.selectOne(
                new LambdaQueryWrapper<TemplateVersion>()
                        .eq(TemplateVersion::getTemplateId, templateId)
                        .orderByDesc(TemplateVersion::getCreateTime)
                        .last("LIMIT 1"));
        if (v == null) throw new BusinessException("模板版本不存在，请重新创建");
        return v;
    }

    private TemplateDTO toDTO(Template tpl) {
        TemplateDTO dto = new TemplateDTO();
        dto.setId(tpl.getId());
        dto.setName(tpl.getName());
        dto.setDirectoryId(tpl.getDirectoryId());
        dto.setStatus(tpl.getStatus());
        dto.setCurrentVersion(tpl.getCurrentVersion());
        dto.setDirtyThreshold(tpl.getDirtyThreshold());
        dto.setCreateTime(tpl.getCreateTime());
        return dto;
    }

    private TemplateVersionDTO toVersionDTO(TemplateVersion v) {
        TemplateVersionDTO dto = new TemplateVersionDTO();
        dto.setId(v.getId());
        dto.setVersion(v.getVersion());
        dto.setTargetTableName(v.getTargetTableName());
        dto.setChangeLog(v.getChangeLog());
        dto.setCreateTime(v.getCreateTime());
        return dto;
    }

    private String toSnakeCase(String name) {
        return name.replaceAll("[^a-zA-Z0-9_]", "_").toLowerCase();
    }

    private String str(Map<String, Object> m, String key) {
        Object v = m.get(key);
        return v != null ? v.toString() : null;
    }

    private int intVal(Map<String, Object> m, String key) {
        Object v = m.get(key);
        if (v == null) return 0;
        return Integer.parseInt(v.toString());
    }
}
```

- [ ] **Step 5: 编译验证**

```bash
mvn clean compile -q
```
预期：BUILD SUCCESS。

- [ ] **Step 6: 提交**

```bash
git add src/main/java/com/example/upload/model/dto/TemplateDTO.java \
        src/main/java/com/example/upload/model/dto/TemplateVersionDTO.java \
        src/main/java/com/example/upload/service/TemplateService.java \
        src/main/java/com/example/upload/service/impl/TemplateServiceImpl.java
git commit -m "feat: add template service with 5-step wizard CRUD and version management"
```

---

## Task 8: TemplateController + ViewController 路由

**Files:**
- Create: `src/main/java/com/example/upload/controller/TemplateController.java`
- Modify: `src/main/java/com/example/upload/controller/ViewController.java`

- [ ] **Step 1: 创建 TemplateController**

```java
// src/main/java/com/example/upload/controller/TemplateController.java
package com.example.upload.controller;

import com.example.upload.common.PageResult;
import com.example.upload.common.Result;
import com.example.upload.common.constants.UploadConstants;
import com.example.upload.exception.BusinessException;
import com.example.upload.model.dto.*;
import com.example.upload.model.entity.TemplateField;
import com.example.upload.service.TemplateService;
import com.example.upload.model.dto.LoginUserDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/harness/api/v1/templates")
@Tag(name = "模板管理")
@RequiredArgsConstructor
public class TemplateController {

    private final TemplateService templateService;

    @GetMapping
    @Operation(summary = "模板列表")
    public Result<PageResult<TemplateDTO>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long directoryId,
            @RequestParam(required = false) String status,
            HttpSession session) {
        requireAdmin(session);
        return Result.ok(templateService.listTemplates(page, size, directoryId, status));
    }

    @PostMapping
    @Operation(summary = "Step1: 创建模板草稿")
    public Result<Long> create(@RequestBody Map<String, Object> body, HttpSession session) {
        LoginUserDTO user = requireAdmin(session);
        String name = (String) body.get("name");
        Long directoryId = Long.valueOf(body.get("directoryId").toString());
        BigDecimal threshold = body.get("dirtyThreshold") != null
                ? new BigDecimal(body.get("dirtyThreshold").toString()) : new BigDecimal("10");
        return Result.ok(templateService.createDraft(name, directoryId, threshold, user.getUserId()));
    }

    @PutMapping("/{id}/sample")
    @Operation(summary = "Step2a: 上传样例文件，返回解析字段")
    public Result<List<ParsedFieldDTO>> uploadSample(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            HttpSession session) {
        LoginUserDTO user = requireAdmin(session);
        return Result.ok(templateService.uploadSample(id, file, user.getUserId()));
    }

    @PostMapping("/{id}/ddl")
    @Operation(summary = "Step2b: 生成 DDL 预览")
    public Result<DdlPreviewDTO> generateDdl(@PathVariable Long id, HttpSession session) {
        requireAdmin(session);
        return Result.ok(templateService.generateDdl(id));
    }

    @PostMapping("/{id}/table")
    @Operation(summary = "Step2c: 执行建表")
    public Result<Void> executeTable(@PathVariable Long id, HttpSession session) {
        LoginUserDTO user = requireAdmin(session);
        templateService.executeTable(id, user.getUserId());
        return Result.ok();
    }

    @PutMapping("/{id}/field-mapping")
    @Operation(summary = "Step3: 保存字段映射")
    public Result<Void> saveFieldMapping(
            @PathVariable Long id,
            @RequestBody List<Map<String, Object>> mappings,
            HttpSession session) {
        LoginUserDTO user = requireAdmin(session);
        templateService.saveFieldMapping(id, mappings, user.getUserId());
        return Result.ok();
    }

    @PutMapping("/{id}/validation-rules")
    @Operation(summary = "Step4: 保存校验规则")
    public Result<Void> saveValidationRules(
            @PathVariable Long id,
            @RequestBody List<Map<String, Object>> rules,
            HttpSession session) {
        LoginUserDTO user = requireAdmin(session);
        templateService.saveValidationRules(id, rules, user.getUserId());
        return Result.ok();
    }

    @PostMapping("/{id}/publish")
    @Operation(summary = "Step5: 发布模板")
    public Result<Void> publish(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            HttpSession session) {
        LoginUserDTO user = requireAdmin(session);
        templateService.publish(id, body.get("changeLog"), user.getUserId());
        return Result.ok();
    }

    @PostMapping("/{id}/copy")
    @Operation(summary = "复制模板")
    public Result<Long> copy(@PathVariable Long id, HttpSession session) {
        LoginUserDTO user = requireAdmin(session);
        return Result.ok(templateService.copy(id, user.getUserId()));
    }

    @PutMapping("/{id}/disable")
    @Operation(summary = "停用模板")
    public Result<Void> disable(@PathVariable Long id, HttpSession session) {
        LoginUserDTO user = requireAdmin(session);
        templateService.disable(id, user.getUserId());
        return Result.ok();
    }

    @GetMapping("/{id}/versions")
    @Operation(summary = "版本历史")
    public Result<List<TemplateVersionDTO>> versions(@PathVariable Long id, HttpSession session) {
        requireAdmin(session);
        return Result.ok(templateService.listVersions(id));
    }

    @GetMapping("/{id}/fields")
    @Operation(summary = "获取模板字段（按版本）")
    public Result<List<TemplateField>> fields(
            @PathVariable Long id,
            @RequestParam Long versionId,
            HttpSession session) {
        requireAdmin(session);
        return Result.ok(templateService.getFieldsByVersion(versionId));
    }

    private LoginUserDTO requireAdmin(HttpSession session) {
        LoginUserDTO user = (LoginUserDTO) session.getAttribute(UploadConstants.SESSION_LOGIN_USER);
        if (user == null) throw new BusinessException(401, "未登录");
        if (!user.isAdmin()) throw new BusinessException(403, "需要管理员权限");
        return user;
    }
}
```

- [ ] **Step 2: 在 ViewController 中追加两个路由**

在 `adminDirectory` 方法之后追加：

```java
    @GetMapping("/admin/templates")
    public String adminTemplates(HttpSession session, Model model) {
        LoginUserDTO loginUser = getLoginUser(session);
        if (loginUser == null) return "redirect:/login";
        if (!loginUser.isAdmin()) return "redirect:/home";
        model.addAttribute("loginUser", loginUser);
        return "admin/template";
    }

    @GetMapping("/admin/templates/create")
    public String adminTemplateCreate(HttpSession session, Model model) {
        LoginUserDTO loginUser = getLoginUser(session);
        if (loginUser == null) return "redirect:/login";
        if (!loginUser.isAdmin()) return "redirect:/home";
        model.addAttribute("loginUser", loginUser);
        return "admin/template-wizard";
    }
```

- [ ] **Step 3: 更新 admin/template.html 侧边栏链接**

将 `admin/template.html` 中 `href="/admin/template"` 改为 `href="/admin/templates"`，将侧边栏的"目录管理"链接由 `href="/home"` 改为已有的 `href="/admin/directory"`（已在之前版本中改好，确认无误即可）。

- [ ] **Step 4: 编译验证**

```bash
mvn clean compile -q
```
预期：BUILD SUCCESS。

- [ ] **Step 5: 提交**

```bash
git add src/main/java/com/example/upload/controller/TemplateController.java \
        src/main/java/com/example/upload/controller/ViewController.java \
        src/main/resources/templates/admin/template.html
git commit -m "feat: add template controller with 11 endpoints and view routes"
```

---

## Task 9: AsyncValidationService 完整实现

**Files:**
- Modify: `src/main/java/com/example/upload/service/impl/AsyncValidationServiceImpl.java`

- [ ] **Step 1: 完整实现 AsyncValidationServiceImpl**

完全替换文件内容：

```java
// src/main/java/com/example/upload/service/impl/AsyncValidationServiceImpl.java
package com.example.upload.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.upload.common.enums.TaskStatus;
import com.example.upload.mapper.*;
import com.example.upload.model.entity.*;
import com.example.upload.service.AsyncValidationService;
import com.example.upload.service.FileParseService;
import com.example.upload.service.TemplateService;
import com.example.upload.util.FeishuUtil;
import com.example.upload.validation.*;
import com.example.upload.validation.rules.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncValidationServiceImpl implements AsyncValidationService {

    private final UploadTaskMapper uploadTaskMapper;
    private final UploadErrorMapper uploadErrorMapper;
    private final TemplateMapper templateMapper;
    private final TemplateVersionMapper templateVersionMapper;
    private final TemplateFieldMapper templateFieldMapper;
    private final FileParseService fileParseService;
    private final FeishuUtil feishuUtil;
    private final JdbcTemplate jdbcTemplate;
    private final com.example.upload.config.FeishuProperties feishuProperties;

    @Override
    @Async("validationExecutor")
    public void submitAsync(Long taskId) {
        log.info("异步完整校验开始 taskId={}", taskId);
        UploadTask task = uploadTaskMapper.selectById(taskId);
        if (task == null) { log.error("任务不存在 taskId={}", taskId); return; }

        try {
            // 1. 加载模板配置
            Template tpl = templateMapper.selectById(task.getTemplateId());
            if (tpl == null) { failTask(task, "模板不存在"); return; }

            TemplateVersion version = task.getVersionId() != null
                    ? templateVersionMapper.selectById(task.getVersionId())
                    : templateVersionMapper.selectOne(new LambdaQueryWrapper<TemplateVersion>()
                        .eq(TemplateVersion::getTemplateId, tpl.getId())
                        .orderByDesc(TemplateVersion::getCreateTime).last("LIMIT 1"));
            if (version == null) { failTask(task, "模板版本不存在"); return; }

            List<TemplateField> fields = templateFieldMapper.selectList(
                    new LambdaQueryWrapper<TemplateField>().eq(TemplateField::getVersionId, version.getId()));

            // 2. 解析文件
            FileParseService.ParsedResult parsed = fileParseService.parseAll(task.getFilePath());
            int totalRows = parsed.rows().size();

            // 3. 构建校验上下文
            int threshold = tpl.getDirtyThreshold() != null ? tpl.getDirtyThreshold().intValue() : 10;
            ValidationContext ctx = ValidationContext.builder()
                    .headers(parsed.headers())
                    .rows(parsed.rows())
                    .fields(fields)
                    .dirtyThreshold(threshold)
                    .build();

            // 4. 执行校验链
            ValidationChain chain = new ValidationChain()
                    .addRule(new HeaderMatchRule())
                    .addRule(new RequiredFieldRule())
                    .addRule(new TypeCheckRule())
                    .addRule(new EnumValueRule())
                    .addRule(new RegexRule());

            ValidationResult result = chain.execute(ctx);

            // 5. 脏数据阈值判断
            Set<Integer> errorRows = new HashSet<>();
            result.getErrors().forEach(e -> errorRows.add(e.getRowNumber()));
            boolean overThreshold = DirtyThresholdRule.exceeds(errorRows.size(), totalRows, threshold);

            if (result.isAborted() || overThreshold) {
                saveErrors(taskId, result.getErrors());
                task.setStatus(TaskStatus.FAILED.getCode());
                task.setErrorCount(result.getErrors().size());
                uploadTaskMapper.updateById(task);
                String summary = buildErrorSummary(result.getErrors());
                sendNotify(task.getFileName(), false, totalRows, summary);
                log.info("任务 {} 校验失败，错误数={}", taskId, result.getErrors().size());
                return;
            }

            // 6. 批量写入 biz_* 表
            if (version.getTargetTableName() != null && !version.getTargetTableName().isBlank()) {
                batchInsert(version.getTargetTableName(), version.getVersion(), task.getId(), fields, parsed.rows());
            }

            // 7. 成功
            task.setStatus(TaskStatus.SUCCESS.getCode());
            task.setRowCount(totalRows);
            task.setErrorCount(0);
            uploadTaskMapper.updateById(task);
            sendNotify(task.getFileName(), true, totalRows, null);
            log.info("任务 {} 处理成功，写入 {} 行", taskId, totalRows);

        } catch (Exception e) {
            log.error("任务 {} 异步处理异常", taskId, e);
            failTask(task, "系统异常：" + e.getMessage());
        }
    }

    private void batchInsert(String tableName, String version, Long taskId,
                              List<TemplateField> fields, List<Map<String, String>> rows) {
        List<TemplateField> activeFields = fields.stream()
                .filter(f -> f.getIsSkipped() == null || f.getIsSkipped() == 0)
                .toList();
        if (activeFields.isEmpty() || rows.isEmpty()) return;

        StringBuilder colSql = new StringBuilder("(`task_id`,`_version`");
        for (TemplateField f : activeFields) colSql.append(",`").append(f.getTargetColumn()).append("`");
        colSql.append(")");

        StringBuilder placeholders = new StringBuilder("(?,?");
        for (int i = 0; i < activeFields.size(); i++) placeholders.append(",?");
        placeholders.append(")");

        String sql = "INSERT INTO `" + tableName + "` " + colSql + " VALUES " + placeholders;

        List<Object[]> batch = new ArrayList<>();
        for (Map<String, String> row : rows) {
            Object[] args = new Object[2 + activeFields.size()];
            args[0] = taskId;
            args[1] = version;
            for (int i = 0; i < activeFields.size(); i++) {
                TemplateField f = activeFields.get(i);
                args[2 + i] = f.getConstantValue() != null && !f.getConstantValue().isBlank()
                        ? f.getConstantValue()
                        : row.get(f.getSourceColumn());
            }
            batch.add(args);
            if (batch.size() >= 1000) {
                jdbcTemplate.batchUpdate(sql, batch);
                batch.clear();
            }
        }
        if (!batch.isEmpty()) jdbcTemplate.batchUpdate(sql, batch);
    }

    private void failTask(UploadTask task, String reason) {
        task.setStatus(TaskStatus.FAILED.getCode());
        uploadTaskMapper.updateById(task);
        sendNotify(task.getFileName(), false, 0, reason);
    }

    private void saveErrors(Long taskId, List<RowError> errors) {
        for (RowError e : errors) {
            UploadError err = new UploadError();
            err.setTaskId(taskId);
            err.setRowNumber(e.getRowNumber());
            err.setColumnName(e.getColumnName());
            err.setErrorType(e.getErrorType());
            err.setErrorMessage(e.getErrorMessage());
            uploadErrorMapper.insert(err);
        }
    }

    private void sendNotify(String fileName, boolean success, int rows, String errorSummary) {
        String webhookUrl = feishuProperties.getWebhookUrl();
        if (webhookUrl == null || webhookUrl.isBlank()) return;
        String status = success ? "成功" : "失败";
        String msg = "【上传" + status + "】" + fileName + " | " + rows + "行"
                + (errorSummary != null ? " | " + errorSummary : "");
        feishuUtil.sendWebhook(webhookUrl, msg);
    }

    private String buildErrorSummary(List<RowError> errors) {
        return errors.stream().limit(3)
                .map(RowError::getErrorMessage)
                .reduce((a, b) -> a + "；" + b)
                .orElse("");
    }
}
```

- [ ] **Step 2: 编译验证**

```bash
mvn clean compile -q
```
预期：BUILD SUCCESS。

- [ ] **Step 3: 提交**

```bash
git add src/main/java/com/example/upload/service/impl/AsyncValidationServiceImpl.java
git commit -m "feat: complete async validation service with 6-rule chain and biz_* batch insert"
```

---

## Task 10: 前端模板列表页

**Files:**
- Modify: `src/main/resources/templates/admin/template.html`

- [ ] **Step 1: 改造 template.html 为真实列表**

完全替换 `template.html` 内容：

```html
<!DOCTYPE html>
<html lang="zh-CN" xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8"/>
    <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
    <title>模板管理 - 数据上报平台</title>
    <script src="https://cdn.tailwindcss.com"></script>
    <script defer src="https://cdn.jsdelivr.net/npm/alpinejs@3.x.x/dist/cdn.min.js"></script>
    <style>[x-cloak]{display:none!important}</style>
</head>
<body class="bg-gray-50 min-h-screen">
<nav class="bg-white border-b border-gray-200 px-6 flex items-center justify-between fixed top-0 left-0 right-0 z-30 h-14">
    <div class="flex items-center gap-3">
        <a href="/home" class="flex items-center gap-3 hover:opacity-80">
            <div class="w-7 h-7 bg-gradient-to-br from-blue-500 to-indigo-600 rounded-md flex items-center justify-center">
                <svg class="w-4 h-4 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12"/></svg>
            </div>
            <span class="font-semibold text-gray-800 text-sm">数据上报平台</span>
        </a>
    </div>
    <div class="flex items-center gap-3">
        <span class="text-sm text-gray-600" th:text="${loginUser?.userName}"></span>
        <span class="text-xs bg-blue-100 text-blue-600 px-2 py-0.5 rounded-full">管理员</span>
        <a href="/home" class="text-xs text-gray-400 hover:text-gray-600 ml-2">返回首页</a>
    </div>
</nav>
<div class="pt-14 flex min-h-screen">
    <aside class="w-48 bg-white border-r border-gray-200 fixed top-14 left-0 bottom-0 z-20">
        <nav class="p-3 space-y-1">
            <a href="/admin/templates" class="flex items-center gap-2 px-3 py-2 rounded-lg text-sm font-medium bg-blue-50 text-blue-600 border-r-2 border-blue-500">
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 10h16M4 14h16M4 18h16"/></svg>
                数据模板
            </a>
            <a href="/admin/directory" class="flex items-center gap-2 px-3 py-2 rounded-lg text-sm font-medium text-gray-600 hover:bg-gray-50">
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-6l-2-2H5a2 2 0 00-2 2z"/></svg>
                目录管理
            </a>
        </nav>
    </aside>
    <main class="ml-48 flex-1 p-6" x-data="templateList()" x-init="load()">
        <div class="flex items-center justify-between mb-6">
            <div>
                <h1 class="text-xl font-bold text-gray-900">数据模板</h1>
                <p class="text-sm text-gray-500 mt-0.5">管理数据上报字段映射模板</p>
            </div>
            <a href="/admin/templates/create" class="flex items-center gap-2 px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white text-sm font-medium rounded-lg">
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/></svg>
                新建模板
            </a>
        </div>
        <!-- 筛选 -->
        <div class="flex gap-3 mb-4">
            <select x-model="filterStatus" @change="load()" class="text-sm border border-gray-200 rounded-lg px-3 py-1.5 focus:outline-none focus:border-blue-400">
                <option value="">全部状态</option>
                <option value="draft">草稿</option>
                <option value="active">启用</option>
                <option value="disabled">停用</option>
            </select>
        </div>
        <!-- 列表 -->
        <div class="bg-white rounded-xl border border-gray-200 overflow-hidden">
            <template x-if="loading">
                <div class="p-12 text-center text-gray-400">加载中...</div>
            </template>
            <template x-if="!loading && templates.length === 0">
                <div class="p-12 text-center text-gray-400">暂无模板，点击「新建模板」创建</div>
            </template>
            <template x-if="!loading && templates.length > 0">
                <table class="w-full">
                    <thead class="bg-gray-50 border-b border-gray-200">
                    <tr>
                        <th class="text-left text-xs font-medium text-gray-500 uppercase px-6 py-3">模板名称</th>
                        <th class="text-left text-xs font-medium text-gray-500 uppercase px-6 py-3">当前版本</th>
                        <th class="text-left text-xs font-medium text-gray-500 uppercase px-6 py-3">状态</th>
                        <th class="text-left text-xs font-medium text-gray-500 uppercase px-6 py-3">创建时间</th>
                        <th class="text-right text-xs font-medium text-gray-500 uppercase px-6 py-3">操作</th>
                    </tr>
                    </thead>
                    <tbody class="divide-y divide-gray-100">
                    <template x-for="t in templates" :key="t.id">
                        <tr class="hover:bg-gray-50">
                            <td class="px-6 py-3 text-sm font-medium text-gray-800" x-text="t.name"></td>
                            <td class="px-6 py-3 text-sm text-gray-500" x-text="t.currentVersion || '-'"></td>
                            <td class="px-6 py-3">
                                <span class="text-xs px-2 py-0.5 rounded-full font-medium"
                                      :class="t.status==='active'?'bg-green-100 text-green-700':t.status==='draft'?'bg-yellow-100 text-yellow-700':'bg-gray-100 text-gray-500'"
                                      x-text="t.status==='active'?'启用':t.status==='draft'?'草稿':'停用'"></span>
                            </td>
                            <td class="px-6 py-3 text-sm text-gray-500" x-text="t.createTime ? t.createTime.substring(0,10) : '-'"></td>
                            <td class="px-6 py-3 text-right">
                                <div class="flex items-center justify-end gap-2">
                                    <a :href="'/admin/templates/create?id='+t.id" class="text-xs text-blue-600 hover:text-blue-800 font-medium px-2 py-1 rounded hover:bg-blue-50">编辑</a>
                                    <button x-show="t.status==='active'" @click="disable(t)" class="text-xs text-orange-500 hover:text-orange-700 font-medium px-2 py-1 rounded hover:bg-orange-50">停用</button>
                                    <button @click="copyTpl(t)" class="text-xs text-gray-500 hover:text-gray-700 font-medium px-2 py-1 rounded hover:bg-gray-100">复制</button>
                                </div>
                            </td>
                        </tr>
                    </template>
                    </tbody>
                </table>
            </template>
        </div>
        <!-- 分页 -->
        <div x-show="total > size" class="flex justify-end mt-4 gap-2">
            <button @click="page>1&&(page--,load())" class="text-sm px-3 py-1 border border-gray-200 rounded hover:bg-gray-50" :disabled="page===1">上一页</button>
            <span class="text-sm text-gray-500 px-2 py-1" x-text="'第'+page+'页，共'+Math.ceil(total/size)+'页'"></span>
            <button @click="page*size<total&&(page++,load())" class="text-sm px-3 py-1 border border-gray-200 rounded hover:bg-gray-50">下一页</button>
        </div>
    </main>
</div>
<script>
function templateList() {
    return {
        templates: [], loading: true, total: 0, page: 1, size: 20, filterStatus: '',
        async load() {
            this.loading = true;
            try {
                const params = new URLSearchParams({ page: this.page, size: this.size });
                if (this.filterStatus) params.set('status', this.filterStatus);
                const res = await fetch('/harness/api/v1/templates?' + params);
                const json = await res.json();
                if (json.code === 200) { this.templates = json.data.records; this.total = json.data.total; }
            } finally { this.loading = false; }
        },
        async disable(t) {
            if (!confirm(`停用模板「${t.name}」？`)) return;
            const res = await fetch(`/harness/api/v1/templates/${t.id}/disable`, { method: 'PUT' });
            const json = await res.json();
            if (json.code === 200) await this.load();
            else alert(json.msg);
        },
        async copyTpl(t) {
            const res = await fetch(`/harness/api/v1/templates/${t.id}/copy`, { method: 'POST' });
            const json = await res.json();
            if (json.code === 200) { await this.load(); alert('复制成功，新模板为草稿状态'); }
            else alert(json.msg);
        }
    };
}
</script>
</body>
</html>
```

- [ ] **Step 2: 编译验证**

```bash
mvn clean compile -q
```
预期：BUILD SUCCESS。

- [ ] **Step 3: 提交**

```bash
git add src/main/resources/templates/admin/template.html
git commit -m "feat: rebuild template list page with real API integration"
```

---

## Task 11: 前端 5 步向导页

**Files:**
- Create: `src/main/resources/templates/admin/template-wizard.html`

- [ ] **Step 1: 创建 template-wizard.html**

```html
<!DOCTYPE html>
<html lang="zh-CN" xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8"/>
    <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
    <title>创建模板 - 数据上报平台</title>
    <script src="https://cdn.tailwindcss.com"></script>
    <script defer src="https://cdn.jsdelivr.net/npm/alpinejs@3.x.x/dist/cdn.min.js"></script>
    <style>[x-cloak]{display:none!important}</style>
</head>
<body class="bg-gray-50 min-h-screen">
<nav class="bg-white border-b border-gray-200 px-6 flex items-center justify-between fixed top-0 left-0 right-0 z-30 h-14">
    <div class="flex items-center gap-3">
        <a href="/admin/templates" class="flex items-center gap-3 hover:opacity-80">
            <div class="w-7 h-7 bg-gradient-to-br from-blue-500 to-indigo-600 rounded-md flex items-center justify-center">
                <svg class="w-4 h-4 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12"/></svg>
            </div>
            <span class="font-semibold text-gray-800 text-sm">数据上报平台</span>
        </a>
        <svg class="w-4 h-4 text-gray-300" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"/></svg>
        <span class="text-sm text-gray-900 font-medium">创建模板</span>
    </div>
    <div class="flex items-center gap-3">
        <span class="text-sm text-gray-600" th:text="${loginUser?.userName}"></span>
        <a href="/admin/templates" class="text-xs text-gray-400 hover:text-gray-600">返回列表</a>
    </div>
</nav>

<div class="pt-14 max-w-4xl mx-auto p-6" x-data="wizard()" x-init="init()">
    <!-- 步骤指示器 -->
    <div class="flex items-center justify-between mb-8">
        <template x-for="(label, idx) in steps" :key="idx">
            <div class="flex items-center gap-2">
                <div class="w-8 h-8 rounded-full flex items-center justify-center text-sm font-bold border-2 transition-colors"
                     :class="currentStep > idx+1 ? 'bg-green-500 border-green-500 text-white' :
                             currentStep === idx+1 ? 'bg-blue-600 border-blue-600 text-white' :
                             'bg-white border-gray-300 text-gray-400'"
                     x-text="currentStep > idx+1 ? '✓' : idx+1"></div>
                <span class="text-sm" :class="currentStep === idx+1 ? 'font-semibold text-gray-900' : 'text-gray-400'" x-text="label"></span>
                <div x-show="idx < steps.length-1" class="w-12 h-px bg-gray-200 mx-2"></div>
            </div>
        </template>
    </div>

    <!-- 错误提示 -->
    <div x-show="error" x-cloak class="bg-red-50 border border-red-200 text-red-700 text-sm rounded-lg px-4 py-3 mb-4 flex justify-between">
        <span x-text="error"></span>
        <button @click="error=null">✕</button>
    </div>

    <!-- Step 1: 基础信息 -->
    <div x-show="currentStep===1" class="bg-white rounded-xl border border-gray-200 p-6">
        <h2 class="text-lg font-semibold text-gray-900 mb-4">基础信息</h2>
        <div class="space-y-4">
            <div>
                <label class="block text-sm font-medium text-gray-700 mb-1">模板名称 <span class="text-red-500">*</span></label>
                <input type="text" x-model="form.name" placeholder="如：产品营销数据模板"
                       class="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:border-blue-400"/>
            </div>
            <div>
                <label class="block text-sm font-medium text-gray-700 mb-1">绑定目录 <span class="text-red-500">*</span></label>
                <select x-model="form.directoryId" class="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:border-blue-400">
                    <option value="">请选择目录</option>
                    <template x-for="dir in directories" :key="dir.id">
                        <option :value="dir.id" x-text="dir.name"></option>
                    </template>
                </select>
            </div>
            <div>
                <label class="block text-sm font-medium text-gray-700 mb-1">脏数据阈值：<span x-text="form.dirtyThreshold"></span>%</label>
                <input type="range" x-model="form.dirtyThreshold" min="1" max="50" class="w-full"/>
                <p class="text-xs text-gray-400 mt-1">错误行占总行数超过此比例时整批拒绝</p>
            </div>
        </div>
    </div>

    <!-- Step 2: 样例文件 + DDL -->
    <div x-show="currentStep===2" class="bg-white rounded-xl border border-gray-200 p-6">
        <h2 class="text-lg font-semibold text-gray-900 mb-4">上传样例文件 & 建表</h2>
        <div class="space-y-4">
            <div>
                <label class="block text-sm font-medium text-gray-700 mb-1">样例文件（Excel/CSV）<span class="text-red-500">*</span></label>
                <input type="file" accept=".xlsx,.xls,.csv" @change="onFileSelect($event)"
                       class="block w-full text-sm text-gray-600 file:mr-4 file:py-2 file:px-4 file:rounded-lg file:border-0 file:text-sm file:font-medium file:bg-blue-50 file:text-blue-700 hover:file:bg-blue-100"/>
            </div>
            <template x-if="parsedFields.length > 0">
                <div>
                    <p class="text-sm font-medium text-gray-700 mb-2">解析到 <span x-text="parsedFields.length"></span> 个字段：</p>
                    <div class="overflow-x-auto rounded-lg border border-gray-200">
                        <table class="w-full text-sm">
                            <thead class="bg-gray-50"><tr>
                                <th class="text-left px-4 py-2 text-gray-500">列名</th>
                                <th class="text-left px-4 py-2 text-gray-500">推断类型</th>
                            </tr></thead>
                            <tbody class="divide-y divide-gray-100">
                            <template x-for="f in parsedFields" :key="f.fieldOrder">
                                <tr><td class="px-4 py-2 text-gray-800" x-text="f.columnName"></td>
                                    <td class="px-4 py-2"><span class="text-xs bg-blue-100 text-blue-700 px-2 py-0.5 rounded" x-text="f.inferredType"></span></td></tr>
                            </template>
                            </tbody>
                        </table>
                    </div>
                </div>
            </template>
            <template x-if="ddlSql">
                <div>
                    <p class="text-sm font-medium text-gray-700 mb-2">DDL 预览：</p>
                    <pre class="bg-gray-900 text-green-400 text-xs p-4 rounded-lg overflow-x-auto" x-text="ddlSql"></pre>
                    <button @click="executeTable()" :disabled="tableCreated"
                            class="mt-3 px-4 py-2 text-sm font-medium rounded-lg transition-colors"
                            :class="tableCreated ? 'bg-green-100 text-green-700 cursor-default' : 'bg-blue-600 hover:bg-blue-700 text-white'">
                        <span x-text="tableCreated ? '✓ 已建表：' + tableName : '执行建表'"></span>
                    </button>
                </div>
            </template>
        </div>
    </div>

    <!-- Step 3: 字段映射 -->
    <div x-show="currentStep===3" class="bg-white rounded-xl border border-gray-200 p-6">
        <h2 class="text-lg font-semibold text-gray-900 mb-4">字段映射配置</h2>
        <div class="overflow-x-auto">
            <table class="w-full text-sm">
                <thead class="bg-gray-50 border-b border-gray-200">
                <tr>
                    <th class="text-left px-3 py-2 text-gray-500">源列名</th>
                    <th class="text-left px-3 py-2 text-gray-500">目标列名</th>
                    <th class="text-center px-3 py-2 text-gray-500">必填</th>
                    <th class="text-left px-3 py-2 text-gray-500">常量值</th>
                    <th class="text-center px-3 py-2 text-gray-500">跳过</th>
                </tr>
                </thead>
                <tbody class="divide-y divide-gray-100">
                <template x-for="f in fieldMappings" :key="f.id">
                    <tr :class="f.isSkipped ? 'opacity-40' : ''">
                        <td class="px-3 py-2 text-gray-700" x-text="f.sourceColumn"></td>
                        <td class="px-3 py-2">
                            <input type="text" x-model="f.targetColumn" :disabled="f.isSkipped"
                                   class="w-full px-2 py-1 border border-gray-200 rounded text-sm focus:outline-none focus:border-blue-400"/>
                        </td>
                        <td class="px-3 py-2 text-center">
                            <input type="checkbox" :checked="f.isRequired" @change="f.isRequired=f.isRequired?0:1" :disabled="f.isSkipped"/>
                        </td>
                        <td class="px-3 py-2">
                            <input type="text" x-model="f.constantValue" placeholder="常量映射（可选）" :disabled="f.isSkipped"
                                   class="w-full px-2 py-1 border border-gray-200 rounded text-sm focus:outline-none focus:border-blue-400"/>
                        </td>
                        <td class="px-3 py-2 text-center">
                            <input type="checkbox" :checked="f.isSkipped" @change="f.isSkipped=f.isSkipped?0:1"/>
                        </td>
                    </tr>
                </template>
                </tbody>
            </table>
        </div>
    </div>

    <!-- Step 4: 校验规则 -->
    <div x-show="currentStep===4" class="bg-white rounded-xl border border-gray-200 p-6">
        <h2 class="text-lg font-semibold text-gray-900 mb-4">校验规则配置</h2>
        <div class="overflow-x-auto">
            <table class="w-full text-sm">
                <thead class="bg-gray-50 border-b border-gray-200">
                <tr>
                    <th class="text-left px-3 py-2 text-gray-500">字段</th>
                    <th class="text-left px-3 py-2 text-gray-500">枚举值（逗号分隔）</th>
                    <th class="text-left px-3 py-2 text-gray-500">正则表达式</th>
                </tr>
                </thead>
                <tbody class="divide-y divide-gray-100">
                <template x-for="f in fieldMappings.filter(f=>!f.isSkipped)" :key="f.id">
                    <tr>
                        <td class="px-3 py-2 text-gray-700" x-text="f.sourceColumn"></td>
                        <td class="px-3 py-2">
                            <input type="text" x-model="f._enumInput" placeholder="如：北京,上海,广州"
                                   class="w-full px-2 py-1 border border-gray-200 rounded text-sm focus:outline-none focus:border-blue-400"/>
                        </td>
                        <td class="px-3 py-2">
                            <input type="text" x-model="f._regexInput" placeholder="如：^\d{4}-\d{2}$"
                                   class="w-full px-2 py-1 border border-gray-200 rounded text-sm focus:outline-none focus:border-blue-400"/>
                        </td>
                    </tr>
                </template>
                </tbody>
            </table>
        </div>
    </div>

    <!-- Step 5: 发布确认 -->
    <div x-show="currentStep===5" class="bg-white rounded-xl border border-gray-200 p-6">
        <h2 class="text-lg font-semibold text-gray-900 mb-4">发布模板</h2>
        <div class="space-y-4">
            <div class="bg-gray-50 rounded-lg p-4 text-sm space-y-2">
                <div class="flex"><span class="text-gray-500 w-24">模板名称</span><span class="font-medium text-gray-800" x-text="form.name"></span></div>
                <div class="flex"><span class="text-gray-500 w-24">版本号</span><span class="font-medium text-gray-800">V1.0</span></div>
                <div class="flex"><span class="text-gray-500 w-24">字段数</span><span class="font-medium text-gray-800" x-text="fieldMappings.filter(f=>!f.isSkipped).length + ' 个'"></span></div>
            </div>
            <div>
                <label class="block text-sm font-medium text-gray-700 mb-1">变更说明（可选）</label>
                <textarea x-model="changeLog" rows="3" placeholder="描述本版本的主要内容..."
                          class="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:border-blue-400"></textarea>
            </div>
        </div>
    </div>

    <!-- 底部按钮 -->
    <div class="flex justify-between mt-6">
        <button x-show="currentStep > 1" @click="currentStep--"
                class="px-4 py-2 text-sm text-gray-600 border border-gray-200 rounded-lg hover:bg-gray-50">
            上一步
        </button>
        <div class="flex gap-3 ml-auto">
            <button x-show="currentStep < 5" @click="nextStep()" :disabled="loading"
                    class="px-6 py-2 text-sm font-medium bg-blue-600 hover:bg-blue-700 text-white rounded-lg disabled:opacity-50">
                <span x-text="loading ? '处理中...' : '下一步'"></span>
            </button>
            <button x-show="currentStep === 5" @click="publish()" :disabled="loading"
                    class="px-6 py-2 text-sm font-medium bg-green-600 hover:bg-green-700 text-white rounded-lg disabled:opacity-50">
                <span x-text="loading ? '发布中...' : '发布模板'"></span>
            </button>
        </div>
    </div>
</div>

<script>
function wizard() {
    return {
        currentStep: 1,
        steps: ['基础信息', '样例文件', '字段映射', '校验规则', '发布确认'],
        form: { name: '', directoryId: '', dirtyThreshold: 10 },
        templateId: null,
        directories: [],
        parsedFields: [],
        ddlSql: '',
        tableName: '',
        tableCreated: false,
        fieldMappings: [],
        changeLog: '',
        loading: false,
        error: null,

        async init() {
            const res = await fetch('/harness/api/v1/directories/tree');
            const json = await res.json();
            if (json.code === 200) {
                // 展平目录树，只显示叶子节点
                this.directories = this.flatLeaves(json.data || []);
            }
        },

        flatLeaves(nodes) {
            const result = [];
            for (const n of nodes) {
                if (n.isLeaf) result.push(n);
                if (n.children) result.push(...this.flatLeaves(n.children));
            }
            return result;
        },

        async nextStep() {
            this.error = null;
            this.loading = true;
            try {
                if (this.currentStep === 1) await this.saveStep1();
                else if (this.currentStep === 2) this.validateStep2();
                else if (this.currentStep === 3) await this.saveStep3();
                else if (this.currentStep === 4) await this.saveStep4();
                this.currentStep++;
            } catch (e) {
                this.error = e.message;
            } finally {
                this.loading = false;
            }
        },

        async saveStep1() {
            if (!this.form.name.trim()) throw new Error('请输入模板名称');
            if (!this.form.directoryId) throw new Error('请选择绑定目录');
            const res = await fetch('/harness/api/v1/templates', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ name: this.form.name, directoryId: this.form.directoryId, dirtyThreshold: this.form.dirtyThreshold })
            });
            const json = await res.json();
            if (json.code !== 200) throw new Error(json.msg || '创建失败');
            this.templateId = json.data;
        },

        async onFileSelect(event) {
            const file = event.target.files[0];
            if (!file || !this.templateId) return;
            this.loading = true;
            this.error = null;
            try {
                const fd = new FormData();
                fd.append('file', file);
                const res = await fetch(`/harness/api/v1/templates/${this.templateId}/sample`, { method: 'PUT', body: fd });
                const json = await res.json();
                if (json.code !== 200) throw new Error(json.msg || '解析失败');
                this.parsedFields = json.data;
                // 获取 DDL 预览
                const ddlRes = await fetch(`/harness/api/v1/templates/${this.templateId}/ddl`, { method: 'POST' });
                const ddlJson = await ddlRes.json();
                if (ddlJson.code === 200) { this.ddlSql = ddlJson.data.ddlSql; this.tableName = ddlJson.data.tableName; }
            } catch (e) {
                this.error = e.message;
            } finally {
                this.loading = false;
            }
        },

        async executeTable() {
            this.loading = true;
            try {
                const res = await fetch(`/harness/api/v1/templates/${this.templateId}/table`, { method: 'POST' });
                const json = await res.json();
                if (json.code !== 200) throw new Error(json.msg || '建表失败');
                this.tableCreated = true;
                // 加载字段列表，准备 Step3
                await this.loadFields();
            } catch (e) {
                this.error = e.message;
            } finally {
                this.loading = false;
            }
        },

        validateStep2() {
            if (!this.tableCreated) throw new Error('请先执行建表');
        },

        async loadFields() {
            const res = await fetch('/harness/api/v1/templates/' + this.templateId + '/versions');
            const json = await res.json();
            if (json.code === 200 && json.data.length > 0) {
                const versionId = json.data[0].id;
                const fRes = await fetch(`/harness/api/v1/templates/${this.templateId}/fields?versionId=${versionId}`);
                const fJson = await fRes.json();
                if (fJson.code === 200) {
                    this.fieldMappings = fJson.data.map(f => ({ ...f, _enumInput: '', _regexInput: '' }));
                }
            }
        },

        async saveStep3() {
            const res = await fetch(`/harness/api/v1/templates/${this.templateId}/field-mapping`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(this.fieldMappings)
            });
            const json = await res.json();
            if (json.code !== 200) throw new Error(json.msg || '保存字段映射失败');
        },

        async saveStep4() {
            const rules = this.fieldMappings.filter(f => !f.isSkipped).map(f => {
                const ruleObj = {};
                if (f._enumInput && f._enumInput.trim()) {
                    ruleObj.enum = f._enumInput.split(',').map(s => s.trim()).filter(Boolean);
                }
                if (f._regexInput && f._regexInput.trim()) {
                    ruleObj.regex = f._regexInput.trim();
                }
                return { id: f.id, validationRules: Object.keys(ruleObj).length ? JSON.stringify(ruleObj) : null };
            });
            const res = await fetch(`/harness/api/v1/templates/${this.templateId}/validation-rules`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(rules)
            });
            const json = await res.json();
            if (json.code !== 200) throw new Error(json.msg || '保存校验规则失败');
        },

        async publish() {
            this.loading = true;
            this.error = null;
            try {
                const res = await fetch(`/harness/api/v1/templates/${this.templateId}/publish`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ changeLog: this.changeLog })
                });
                const json = await res.json();
                if (json.code !== 200) throw new Error(json.msg || '发布失败');
                alert('模板发布成功！');
                window.location.href = '/admin/templates';
            } catch (e) {
                this.error = e.message;
            } finally {
                this.loading = false;
            }
        }
    };
}
</script>
</body>
</html>
```

- [ ] **Step 2: 编译并启动验证**

```bash
mvn clean compile -q
```
预期：BUILD SUCCESS。

- [ ] **Step 3: 提交**

```bash
git add src/main/resources/templates/admin/template-wizard.html
git commit -m "feat: add 5-step template creation wizard with dropdown field mapping"
```

---

## 自检：Spec 覆盖检查

| Spec 需求 | 对应 Task |
|-----------|-----------|
| AsyncConfig + @Async 线程池 | Task 1 |
| UploadService 接入异步 | Task 2 |
| 样例文件解析 + 类型推断 | Task 3 |
| DDL 生成 + 建表执行 | Task 4 |
| ValidationRule/Context/Chain | Task 5 |
| 6 类校验规则 | Task 6 |
| 模板 CRUD + 版本管理 | Task 7 |
| 模板 Controller 11 个接口 | Task 8 |
| 异步完整校验 + 批量写入 + 通知 | Task 9 |
| 模板列表页 | Task 10 |
| 5步向导页（下拉映射） | Task 11 |
| 飞书通知（复用 FeishuUtil） | Task 9 |
| 版本号递增规则（V1.0→V1.1→V2.0） | Task 7（createDraft + publish） |
| 脏数据阈值整批拒绝 | Task 9（DirtyThresholdRule.exceeds） |
