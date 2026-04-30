package com.example.upload.service;

import com.example.upload.model.dto.UploadInitDTO;
import com.example.upload.model.form.UploadInitForm;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface UploadService {
    /** 初始化上传任务 */
    UploadInitDTO initTask(UploadInitForm form, Long userId);
    /** 上传单个分片 */
    void uploadChunk(Long taskId, Integer chunkIndex, Integer totalChunks, MultipartFile file);
    /** 查询已上传分片索引列表（断点续传） */
    List<Integer> getUploadedChunks(Long taskId);
    /** 合并分片并执行同步快速校验 */
    void mergeAndValidate(Long taskId, Long userId);
    /** 无数据申报 */
    void noDataDeclare(Long directoryId, String formData, Long userId);
}
