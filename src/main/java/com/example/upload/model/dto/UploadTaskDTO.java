package com.example.upload.model.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UploadTaskDTO {
    private Long id;
    private Long directoryId;
    private String fileName;
    private Long fileSize;
    private String status;
    private String statusDesc;
    private Integer rowCount;
    private Integer errorCount;
    private Integer retryCount;
    private String uploaderName;
    private String formData;
    private Integer isNoData;
    private LocalDateTime expireAt;
    private LocalDateTime createTime;
    private boolean downloadable;
}
