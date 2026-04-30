package com.example.upload.model.dto;

import lombok.Data;

@Data
public class UploadInitDTO {
    private Long taskId;
    private Long chunkSize;
    private Integer totalChunks;
}
