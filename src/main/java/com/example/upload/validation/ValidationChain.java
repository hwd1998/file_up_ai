package com.example.upload.validation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ValidationChain {

    private final List<ValidationRule> rules = new ArrayList<>();

    public ValidationChain addRule(ValidationRule rule) {
        rules.add(rule);
        return this;
    }

    public ValidationResult execute(ValidationContext ctx) {
        ValidationResult merged = new ValidationResult();
        List<ValidationRule> sorted = rules.stream()
                .sorted(Comparator.comparingInt(ValidationRule::getOrder))
                .toList();

        for (ValidationRule rule : sorted) {
            ValidationResult r = rule.validate(ctx);
            merged.getErrors().addAll(r.getErrors());
            if (r.isAborted()) {
                merged.setAborted(true);
                break;
            }
        }
        return merged;
    }
}
