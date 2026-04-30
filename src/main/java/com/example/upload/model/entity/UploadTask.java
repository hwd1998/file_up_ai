package com.example.upload.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("upload_tasks")
public class UploadTask {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long directoryId;
    private Long templateId;
    private Long versionId;
    private String fileName;
    private Long fileSize;
    private String filePath;
    private String ossPath;
    private String status;
    private Integer rowCount;
    private Integer errorCount;
    private Integer retryCount;
    private Long uploadedBy;
    private String formData;
    private Integer isNoData;
    private LocalDateTime expireAt;

    @TableLogic
    private Integer isDeleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
