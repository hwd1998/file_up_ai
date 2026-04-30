package com.example.upload.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("upload_chunk_records")
public class UploadChunkRecord {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private Integer totalChunks;
    private Integer chunkIndex;
    private String chunkPath;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
