package com.example.upload.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.upload.common.PageResult;
import com.example.upload.common.enums.TaskStatus;
import com.example.upload.exception.BusinessException;
import com.example.upload.mapper.SysUserMapper;
import com.example.upload.mapper.UploadErrorMapper;
import com.example.upload.mapper.UploadTaskMapper;
import com.example.upload.model.dto.UploadTaskDTO;
import com.example.upload.model.entity.SysUser;
import com.example.upload.model.entity.UploadError;
import com.example.upload.model.entity.UploadTask;
import com.example.upload.model.form.TaskQueryForm;
import com.example.upload.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {

    private final UploadTaskMapper uploadTaskMapper;
    private final UploadErrorMapper uploadErrorMapper;
    private final SysUserMapper sysUserMapper;

    @Override
    public PageResult<UploadTaskDTO> listTasks(TaskQueryForm form, Long userId, boolean isAdmin) {
        Page<UploadTask> page = new Page<>(form.getPage(), form.getSize());
        uploadTaskMapper.selectTaskPage(page, form, userId, isAdmin);

        // 批量查询上传人姓名
        List<Long> userIds = page.getRecords().stream()
                .map(UploadTask::getUploadedBy).distinct().collect(Collectors.toList());
        Map<Long, String> userNameMap = userIds.isEmpty()
                ? Map.of()
                : sysUserMapper.selectBatchIds(userIds).stream()
                        .collect(Collectors.toMap(SysUser::getId, SysUser::getUserName));

        List<UploadTaskDTO> dtos = page.getRecords().stream()
                .map(t -> toDTO(t, userNameMap.getOrDefault(t.getUploadedBy(), "")))
                .collect(Collectors.toList());

        return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), dtos);
    }

    @Override
    public UploadTaskDTO getTask(Long taskId, Long userId, boolean isAdmin) {
        UploadTask task = getTaskOrThrow(taskId);
        if (!isAdmin && !task.getUploadedBy().equals(userId)) {
            throw new BusinessException(403, "无权访问该任务");
        }
        SysUser uploader = sysUserMapper.selectById(task.getUploadedBy());
        return toDTO(task, uploader != null ? uploader.getUserName() : "");
    }

    @Override
    public List<UploadError> listErrors(Long taskId, Long userId, boolean isAdmin) {
        UploadTask task = getTaskOrThrow(taskId);
        if (!isAdmin && !task.getUploadedBy().equals(userId)) {
            throw new BusinessException(403, "无权访问该任务");
        }
        return uploadErrorMapper.selectList(new LambdaQueryWrapper<UploadError>()
                .eq(UploadError::getTaskId, taskId)
                .orderByAsc(UploadError::getRowNumber));
    }

    @Override
    public String getDownloadPath(Long taskId, Long userId, boolean isAdmin) {
        UploadTask task = getTaskOrThrow(taskId);
        if (!isAdmin && !task.getUploadedBy().equals(userId)) {
            throw new BusinessException(403, "无权访问该任务");
        }
        if (task.getExpireAt() != null && task.getExpireAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException("文件已过期（超过3个月），无法下载");
        }
        if (task.getFilePath() == null) {
            throw new BusinessException("文件不存在");
        }
        return task.getFilePath();
    }

    @Override
    @Transactional
    public void deleteTask(Long taskId, Long userId) {
        UploadTask task = getTaskOrThrow(taskId);
        if (!task.getUploadedBy().equals(userId)) {
            throw new BusinessException(403, "只能删除自己的上传记录");
        }
        if (TaskStatus.SUCCESS.getCode().equals(task.getStatus())
                || TaskStatus.PARTIAL_FAILED.getCode().equals(task.getStatus())) {
            throw new BusinessException("成功的任务不可删除");
        }
        task.setIsDeleted(1);
        uploadTaskMapper.updateById(task);
    }

    private UploadTask getTaskOrThrow(Long taskId) {
        UploadTask task = uploadTaskMapper.selectById(taskId);
        if (task == null || task.getIsDeleted() == 1) {
            throw new BusinessException("任务不存在");
        }
        return task;
    }

    private UploadTaskDTO toDTO(UploadTask task, String uploaderName) {
        UploadTaskDTO dto = new UploadTaskDTO();
        dto.setId(task.getId());
        dto.setDirectoryId(task.getDirectoryId());
        dto.setFileName(task.getFileName());
        dto.setFileSize(task.getFileSize());
        dto.setStatus(task.getStatus());
        dto.setStatusDesc(resolveStatusDesc(task.getStatus()));
        dto.setRowCount(task.getRowCount());
        dto.setErrorCount(task.getErrorCount());
        dto.setRetryCount(task.getRetryCount());
        dto.setUploaderName(uploaderName);
        dto.setFormData(task.getFormData());
        dto.setIsNoData(task.getIsNoData());
        dto.setExpireAt(task.getExpireAt());
        dto.setCreateTime(task.getCreateTime());
        dto.setDownloadable(task.getFilePath() != null
                && (task.getExpireAt() == null || task.getExpireAt().isAfter(LocalDateTime.now())));
        return dto;
    }

    private String resolveStatusDesc(String status) {
        for (TaskStatus s : TaskStatus.values()) {
            if (s.getCode().equals(status)) return s.getDesc();
        }
        return status;
    }
}
