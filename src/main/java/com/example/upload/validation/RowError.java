package com.example.upload.validation;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RowError {
    private int rowNumber;
    private String columnName;
    private String errorType;
    private String errorMessage;
}
