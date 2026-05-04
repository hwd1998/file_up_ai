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
                if (val == null || val.isBlank()) {
                    continue;
                }
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
