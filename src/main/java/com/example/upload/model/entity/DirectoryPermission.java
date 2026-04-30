package com.example.upload.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("directory_permissions")
public class DirectoryPermission {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long directoryId;
    private Long userId;
    private String permissionType;

    @TableLogic
    private Integer isDeleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
