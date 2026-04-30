package com.example.upload.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("templates")
public class Template {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private Long directoryId;
    private String status;
    private String currentVersion;
    private String formFields;
    private BigDecimal dirtyThreshold;
    private Long createdBy;

    @TableLogic
    private Integer isDeleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
