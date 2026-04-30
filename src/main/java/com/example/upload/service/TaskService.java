package com.example.upload.service;

import com.example.upload.common.PageResult;
import com.example.upload.model.dto.UploadTaskDTO;
import com.example.upload.model.entity.UploadError;
import com.example.upload.model.form.TaskQueryForm;

import java.util.List;

public interface TaskService {
    PageResult<UploadTaskDTO> listTasks(TaskQueryForm form, Long userId, boolean isAdmin);
    UploadTaskDTO getTask(Long taskId, Long userId, boolean isAdmin);
    List<UploadError> listErrors(Long taskId, Long userId, boolean isAdmin);
    String getDownloadPath(Long taskId, Long userId, boolean isAdmin);
    void deleteTask(Long taskId, Long userId);
}
