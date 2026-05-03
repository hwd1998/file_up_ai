package com.example.upload.service.impl;

import com.example.upload.common.enums.TaskStatus;
import com.example.upload.mapper.UploadTaskMapper;
import com.example.upload.model.entity.UploadTask;
import com.example.upload.service.AsyncValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncValidationServiceImpl implements AsyncValidationService {

    private final UploadTaskMapper uploadTaskMapper;

    @Override
    @Async("validationExecutor")
    public void submitAsync(Long taskId) {
        log.info("异步校验开始 taskId={}", taskId);
        UploadTask task = uploadTaskMapper.selectById(taskId);
        if (task == null) {
            log.error("任务不存在 taskId={}", taskId);
            return;
        }
        // Phase 2 Task 9 完整实现
        task.setStatus(TaskStatus.PROCESSING.getCode());
        uploadTaskMapper.updateById(task);
        log.info("异步校验骨架执行完成 taskId={}", taskId);
    }
}
