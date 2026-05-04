package com.example.upload.validation.rules;

import com.example.upload.validation.*;

public class DirtyThresholdRule implements ValidationRule {

    @Override
    public ValidationResult validate(ValidationContext ctx) {
        return new ValidationResult();
    }

    /** 外部调用：判断已有错误是否超阈值 */
    public static boolean exceeds(int errorRows, int totalRows, int thresholdPercent) {
        if (totalRows == 0) return false;
        return (errorRows * 100.0 / totalRows) > thresholdPercent;
    }

    @Override public int getOrder() { return 100; }
}
