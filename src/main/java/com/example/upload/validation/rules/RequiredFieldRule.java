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
