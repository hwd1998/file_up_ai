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
