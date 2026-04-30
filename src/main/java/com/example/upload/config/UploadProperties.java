package com.example.upload.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "upload")
public class UploadProperties {
    private String tmpDir = "./upload-tmp";
    private long chunkSize = 5_242_880L;
    private long maxFileSize = 2_147_483_648L;
    private int maxFilesPerRequest = 40;
    private int fileExpireDays = 90;
}
