package com.example.upload.model.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TemplateVersionDTO {
    private Long id;
    private String version;
    private String targetTableName;
    private String changeLog;
    private LocalDateTime createTime;
}
