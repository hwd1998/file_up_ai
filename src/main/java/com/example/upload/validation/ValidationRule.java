package com.example.upload.validation;

public interface ValidationRule {
    ValidationResult validate(ValidationContext ctx);
    int getOrder();
}
