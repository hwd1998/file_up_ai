package com.example.upload.controller;

import com.example.upload.common.Result;
import com.example.upload.common.constants.UploadConstants;
import com.example.upload.exception.BusinessException;
import com.example.upload.model.dto.LoginUserDTO;
import com.example.upload.model.dto.UploadInitDTO;
import com.example.upload.model.form.UploadInitForm;
import com.example.upload.service.UploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/harness/api/v1/upload")
@Tag(name = "文件上传")
@RequiredArgsConstructor
public class UploadController {

    private final UploadService uploadService;

    @PostMapping("/init")
    @Operation(summary = "初始化上传任务，返回 taskId + chunkSize")
    public Result<UploadInitDTO> init(@Valid @RequestBody UploadInitForm form, HttpSession session) {
        LoginUserDTO loginUser = getLoginUser(session);
        return Result.ok(uploadService.initTask(form, loginUser.getUserId()));
    }

    @PostMapping("/chunk")
    @Operation(summary = "上传单个分片")
    public Result<Void> uploadChunk(@RequestParam Long taskId,
                                    @RequestParam Integer chunkIndex,
                                    @RequestParam Integer totalChunks,
                                    @RequestParam MultipartFile file,
                                    HttpSession session) {
        getLoginUser(session);
        uploadService.uploadChunk(taskId, chunkIndex, totalChunks, file);
        return Result.ok();
    }

    @GetMapping("/chunks")
    @Operation(summary = "查询已上传分片索引（断点续传）")
    public Result<List<Integer>> getChunks(@RequestParam Long taskId, HttpSession session) {
        getLoginUser(session);
        return Result.ok(uploadService.getUploadedChunks(taskId));
    }

    @PostMapping("/merge")
    @Operation(summary = "合并分片并执行同步快速校验")
    public Result<Void> merge(@RequestBody Map<String, Object> body, HttpSession session) {
        LoginUserDTO loginUser = getLoginUser(session);
        Long taskId = Long.valueOf(body.get("taskId").toString());
        uploadService.mergeAndValidate(taskId, loginUser.getUserId());
        return Result.ok();
    }

    @PostMapping("/no-data")
    @Operation(summary = "无数据申报")
    public Result<Void> noData(@RequestBody Map<String, Object> body, HttpSession session) {
        LoginUserDTO loginUser = getLoginUser(session);
        Long directoryId = Long.valueOf(body.get("directoryId").toString());
        String formData = body.get("formData") != null ? body.get("formData").toString() : null;
        uploadService.noDataDeclare(directoryId, formData, loginUser.getUserId());
        return Result.ok();
    }

    private LoginUserDTO getLoginUser(HttpSession session) {
        LoginUserDTO user = (LoginUserDTO) session.getAttribute(UploadConstants.SESSION_LOGIN_USER);
        if (user == null) throw new BusinessException(401, "未登录");
        return user;
    }
}
