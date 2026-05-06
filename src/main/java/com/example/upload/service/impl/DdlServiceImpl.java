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

        java.util.Set<String> seen = new java.util.HashSet<>();
        for (TemplateField field : active) {
            String col = field.getTargetColumn();
            if (col == null || col.isBlank()) continue;
            if (!SAFE_NAME.matcher(col).matches()) {
                throw new BusinessException("列名包含非法字符：" + col + "（仅允许字母、数字、下划线）");
            }
            if (!seen.add(col)) {
                throw new BusinessException("存在重复的目标列名「" + col + "」，请在字段映射中修改");
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
