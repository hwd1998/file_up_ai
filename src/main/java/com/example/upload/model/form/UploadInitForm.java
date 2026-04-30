package com.example.upload.model.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class UploadInitForm {

    @NotNull(message = "目录ID不能为空")
    private Long directoryId;

    @NotBlank(message = "文件名不能为空")
    private String fileName;

    @Positive(message = "文件大小必须大于0")
    private Long fileSize;

    /** 上传时填写的表单数据（JSON 字符串） */
    private String formData;
}
