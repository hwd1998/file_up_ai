package com.example.upload.model.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TemplateDTO {
    private Long id;
    private String name;
    private Long directoryId;
    private String directoryName;
    private String status;
    private String currentVersion;
    private BigDecimal dirtyThreshold;
    private LocalDateTime createTime;
}
