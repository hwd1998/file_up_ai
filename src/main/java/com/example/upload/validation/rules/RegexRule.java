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
