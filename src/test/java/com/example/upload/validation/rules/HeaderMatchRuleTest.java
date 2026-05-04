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
