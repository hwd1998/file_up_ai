package com.example.upload.model.dto;

import lombok.Data;

@Data
public class ParsedFieldDTO {
    private String columnName;
    private String inferredType; // int / decimal / date / varchar
    private int fieldOrder;
}
