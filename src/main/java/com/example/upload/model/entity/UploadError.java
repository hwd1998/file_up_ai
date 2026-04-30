package com.example.upload.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("upload_errors")
public class UploadError {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    @TableField("row_num")
    private Integer rowNumber;
    private String columnName;
    private String errorType;
    private String errorMessage;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
