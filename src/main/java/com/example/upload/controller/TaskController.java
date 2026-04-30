package com.example.upload.controller;

import com.example.upload.common.PageResult;
import com.example.upload.common.Result;
import com.example.upload.common.constants.UploadConstants;
import com.example.upload.exception.BusinessException;
import com.example.upload.model.dto.LoginUserDTO;
import com.example.upload.model.dto.UploadTaskDTO;
import com.example.upload.model.entity.UploadError;
import com.example.upload.model.form.TaskQueryForm;
import com.example.upload.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/harness/api/v1/tasks")
@Tag(name = "任务管理")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @GetMapping
    @Operation(summary = "分页查询上传任务列表")
    public Result<PageResult<UploadTaskDTO>> list(TaskQueryForm form, HttpSession session) {
        LoginUserDTO loginUser = getLoginUser(session);
        return Result.ok(taskService.listTasks(form, loginUser.getUserId(), loginUser.isAdmin()));
    }

    @GetMapping("/{taskId}")
    @Operation(summary = "查询任务详情")
    public Result<UploadTaskDTO> detail(@PathVariable Long taskId, HttpSession session) {
        LoginUserDTO loginUser = getLoginUser(session);
        return Result.ok(taskService.getTask(taskId, loginUser.getUserId(), loginUser.isAdmin()));
    }

    @GetMapping("/{taskId}/errors")
    @Operation(summary = "查询任务校验错误列表")
    public Result<List<UploadError>> errors(@PathVariable Long taskId, HttpSession session) {
        LoginUserDTO loginUser = getLoginUser(session);
        return Result.ok(taskService.listErrors(taskId, loginUser.getUserId(), loginUser.isAdmin()));
    }

    @GetMapping("/{taskId}/download")
    @Operation(summary = "下载原始文件")
    public ResponseEntity<Resource> download(@PathVariable Long taskId, HttpSession session) {
        LoginUserDTO loginUser = getLoginUser(session);
        String filePath = taskService.getDownloadPath(taskId, loginUser.getUserId(), loginUser.isAdmin());
        File file = new File(filePath);
        if (!file.exists()) {
            throw new BusinessException("文件不存在");
        }
        String encodedName = URLEncoder.encode(file.getName(), StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedName)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new FileSystemResource(file));
    }

    @DeleteMapping("/{taskId}")
    @Operation(summary = "删除任务记录（仅限失败任务、本人）")
    public Result<Void> delete(@PathVariable Long taskId, HttpSession session) {
        LoginUserDTO loginUser = getLoginUser(session);
        taskService.deleteTask(taskId, loginUser.getUserId());
        return Result.ok();
    }

    private LoginUserDTO getLoginUser(HttpSession session) {
        LoginUserDTO user = (LoginUserDTO) session.getAttribute(UploadConstants.SESSION_LOGIN_USER);
        if (user == null) throw new BusinessException(401, "未登录");
        return user;
    }
}
