package com.example.upload.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("template_versions")
public class TemplateVersion {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long templateId;
    private String version;
    private String targetTableName;
    private String changeLog;
    private Long createdBy;

    @TableLogic
    private Integer isDeleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
