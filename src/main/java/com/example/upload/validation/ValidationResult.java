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
