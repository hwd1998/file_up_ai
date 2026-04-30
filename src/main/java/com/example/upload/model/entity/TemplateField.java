package com.example.upload.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("template_fields")
public class TemplateField {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long versionId;
    private String fieldName;
    private String sourceColumn;
    private String targetColumn;
    private String fieldType;
    private Integer fieldOrder;
    private Integer isRequired;
    private String constantValue;
    private Integer isSkipped;
    private String validationRules;

    @TableLogic
    private Integer isDeleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
