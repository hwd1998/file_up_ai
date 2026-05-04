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
