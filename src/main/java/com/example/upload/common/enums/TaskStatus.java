package com.example.upload.common.enums;

import lombok.Getter;

@Getter
public enum TaskStatus {
    PENDING("pending", "等待校验"),
    VALIDATING("validating", "校验中"),
    PROCESSING("processing", "处理中"),
    SUCCESS("success", "成功"),
    FAILED("failed", "失败"),
    PARTIAL_FAILED("partial_failed", "部分失败");

    private final String code;
    private final String desc;

    TaskStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
