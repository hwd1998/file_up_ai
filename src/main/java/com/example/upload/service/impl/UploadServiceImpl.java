package com.example.upload.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.upload.common.constants.UploadConstants;
import com.example.upload.common.enums.TaskStatus;
import com.example.upload.config.UploadProperties;
import com.example.upload.exception.BusinessException;
import com.example.upload.mapper.*;
import com.example.upload.model.dto.UploadInitDTO;
import com.example.upload.model.entity.*;
import com.example.upload.model.form.UploadInitForm;
import com.example.upload.service.DirectoryService;
import com.example.upload.service.UploadService;
import com.example.upload.util.QuickValidationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UploadServiceImpl implements UploadService {

    private final UploadTaskMapper uploadTaskMapper;
    private final UploadChunkRecordMapper chunkRecordMapper;
    private final UploadErrorMapper uploadErrorMapper;
    private final DirectoryMapper directoryMapper;
    private final DirectoryService directoryService;
    private final UploadProperties uploadProperties;
    private final QuickValidationUtil quickValidationUtil;

    @Override
    @Transactional
    public UploadInitDTO initTask(UploadInitForm form, Long userId) {
        // 权限校验
        directoryService.checkPermission(form.getDirectoryId(), userId, UploadConstants.PERM_UPLOAD);

        // 文件大小校验
        if (form.getFileSize() != null && form.getFileSize() > uploadProperties.getMaxFileSize()) {
            throw new BusinessException("文件大小超过 2GB 限制");
        }

        // 获取目录绑定的模板信息
        Directory dir = directoryMapper.selectById(form.getDirectoryId());
        if (dir == null || dir.getIsDeleted() == 1) {
            throw new BusinessException("目录不存在");
        }

        // 计算分片数
        long fileSize = form.getFileSize() != null ? form.getFileSize() : 0;
        int totalChunks = fileSize > 0
                ? (int) Math.ceil((double) fileSize / uploadProperties.getChunkSize())
                : 1;

        // 创建任务记录
        UploadTask task = new UploadTask();
        task.setDirectoryId(form.getDirectoryId());
        task.setTemplateId(dir.getTemplateId());
        task.setFileName(form.getFileName());
        task.setFileSize(form.getFileSize());
        task.setStatus(TaskStatus.PENDING.getCode());
        task.setRetryCount(0);
        task.setUploadedBy(userId);
        task.setFormData(form.getFormData());
        task.setIsNoData(0);
        task.setExpireAt(LocalDateTime.now().plusDays(uploadProperties.getFileExpireDays()));
        uploadTaskMapper.insert(task);

        UploadInitDTO dto = new UploadInitDTO();
        dto.setTaskId(task.getId());
        dto.setChunkSize(uploadProperties.getChunkSize());
        dto.setTotalChunks(totalChunks);
        return dto;
    }

    @Override
    public void uploadChunk(Long taskId, Integer chunkIndex, Integer totalChunks, MultipartFile file) {
        UploadTask task = getTaskOrThrow(taskId);

        // 保存分片到临时目录
        String chunkPath = getChunkPath(taskId, chunkIndex);
        saveToFile(file, chunkPath);

        // 记录分片
        UploadChunkRecord record = chunkRecordMapper.selectOne(new LambdaQueryWrapper<UploadChunkRecord>()
                .eq(UploadChunkRecord::getTaskId, taskId)
                .eq(UploadChunkRecord::getChunkIndex, chunkIndex));
        if (record == null) {
            record = new UploadChunkRecord();
            record.setTaskId(taskId);
            record.setTotalChunks(totalChunks);
            record.setChunkIndex(chunkIndex);
            record.setChunkPath(chunkPath);
            chunkRecordMapper.insert(record);
        }
    }

    @Override
    public List<Integer> getUploadedChunks(Long taskId) {
        return chunkRecordMapper.selectList(new LambdaQueryWrapper<UploadChunkRecord>()
                        .eq(UploadChunkRecord::getTaskId, taskId)
                        .orderByAsc(UploadChunkRecord::getChunkIndex))
                .stream()
                .map(UploadChunkRecord::getChunkIndex)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void mergeAndValidate(Long taskId, Long userId) {
        UploadTask task = getTaskOrThrow(taskId);

        // 查询所有分片并排序
        List<UploadChunkRecord> chunks = chunkRecordMapper.selectList(
                new LambdaQueryWrapper<UploadChunkRecord>()
                        .eq(UploadChunkRecord::getTaskId, taskId)
                        .orderByAsc(UploadChunkRecord::getChunkIndex));

        // 合并分片
        String mergedPath = getMergedFilePath(taskId, task.getFileName());
        mergeChunks(chunks, mergedPath);

        // 更新文件路径
        task.setFilePath(mergedPath);
        task.setStatus(TaskStatus.VALIDATING.getCode());
        uploadTaskMapper.updateById(task);

        // 同步快速校验（表头 + 必填列，≤2s）
        List<String> errors = quickValidationUtil.quickValidate(mergedPath, task.getTemplateId());
        if (!errors.isEmpty()) {
            // 校验失败：记录错误，更新状态，删除临时文件
            task.setStatus(TaskStatus.FAILED.getCode());
            task.setErrorCount(errors.size());
            uploadTaskMapper.updateById(task);
            saveQuickValidationErrors(taskId, errors);
            deleteFile(mergedPath);
            throw new BusinessException("文件快速校验失败：" + errors.get(0));
        }

        // 快速校验通过，更新状态为处理中（等待异步完整校验，Phase2 实现）
        task.setStatus(TaskStatus.PROCESSING.getCode());
        uploadTaskMapper.updateById(task);
        log.info("任务 {} 快速校验通过，等待异步处理", taskId);
        // TODO Phase2: 投递 RabbitMQ 消息触发异步完整校验
    }

    @Override
    @Transactional
    public void noDataDeclare(Long directoryId, String formData, Long userId) {
        directoryService.checkPermission(directoryId, userId, UploadConstants.PERM_UPLOAD);
        Directory dir = directoryMapper.selectById(directoryId);

        UploadTask task = new UploadTask();
        task.setDirectoryId(directoryId);
        task.setTemplateId(dir.getTemplateId());
        task.setFileName("无数据申报");
        task.setStatus(TaskStatus.SUCCESS.getCode());
        task.setUploadedBy(userId);
        task.setFormData(formData);
        task.setIsNoData(1);
        task.setExpireAt(LocalDateTime.now().plusDays(uploadProperties.getFileExpireDays()));
        uploadTaskMapper.insert(task);
    }

    // ── 私有方法 ───────────────────────────────────────────────

    private UploadTask getTaskOrThrow(Long taskId) {
        UploadTask task = uploadTaskMapper.selectById(taskId);
        if (task == null || task.getIsDeleted() == 1) {
            throw new BusinessException("上传任务不存在");
        }
        return task;
    }

    private String getChunkPath(Long taskId, Integer chunkIndex) {
        String dir = uploadProperties.getTmpDir() + File.separator + taskId;
        new File(dir).mkdirs();
        return dir + File.separator + "chunk_" + chunkIndex;
    }

    private String getMergedFilePath(Long taskId, String fileName) {
        String dir = uploadProperties.getTmpDir() + File.separator + taskId;
        new File(dir).mkdirs();
        return dir + File.separator + fileName;
    }

    private void saveToFile(MultipartFile file, String path) {
        try {
            new File(path).getParentFile().mkdirs();
            try (InputStream in = file.getInputStream();
                 FileOutputStream out = new FileOutputStream(path)) {
                byte[] buf = new byte[8192];
                int len;
                while ((len = in.read(buf)) != -1) {
                    out.write(buf, 0, len);
                }
            }
        } catch (IOException e) {
            throw new BusinessException("分片保存失败：" + e.getMessage());
        }
    }

    private void mergeChunks(List<UploadChunkRecord> chunks, String mergedPath) {
        try (FileOutputStream out = new FileOutputStream(mergedPath, true)) {
            for (UploadChunkRecord chunk : chunks) {
                Path chunkFile = Paths.get(chunk.getChunkPath());
                if (!Files.exists(chunkFile)) {
                    throw new BusinessException("分片文件缺失，请重新上传：chunk_" + chunk.getChunkIndex());
                }
                Files.copy(chunkFile, out);
            }
        } catch (IOException e) {
            throw new BusinessException("文件合并失败：" + e.getMessage());
        }
        // 清理分片文件
        chunks.forEach(c -> deleteFile(c.getChunkPath()));
    }

    private void saveQuickValidationErrors(Long taskId, List<String> errors) {
        for (int i = 0; i < errors.size(); i++) {
            UploadError err = new UploadError();
            err.setTaskId(taskId);
            err.setRowNumber(1);
            err.setErrorType("header_mismatch");
            err.setErrorMessage(errors.get(i));
            uploadErrorMapper.insert(err);
        }
    }

    private void deleteFile(String path) {
        try {
            Files.deleteIfExists(Paths.get(path));
        } catch (IOException e) {
            log.warn("临时文件删除失败: {}", path);
        }
    }
}
