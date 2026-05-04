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
