package com.example.upload.controller;

import com.example.upload.common.PageResult;
import com.example.upload.common.Result;
import com.example.upload.common.constants.UploadConstants;
import com.example.upload.exception.BusinessException;
import com.example.upload.model.dto.DdlPreviewDTO;
import com.example.upload.model.dto.LoginUserDTO;
import com.example.upload.model.dto.ParsedFieldDTO;
import com.example.upload.model.dto.TemplateDTO;
import com.example.upload.model.dto.TemplateVersionDTO;
import com.example.upload.model.entity.TemplateField;
import com.example.upload.service.TemplateService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/harness/api/v1/templates")
@RequiredArgsConstructor
public class TemplateController {

    private final TemplateService templateService;

    @GetMapping
    public Result<PageResult<TemplateDTO>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long directoryId,
            @RequestParam(required = false) String status,
            HttpSession session) {
        requireAdmin(session);
        return Result.ok(templateService.listTemplates(page, size, directoryId, status));
    }

    @PostMapping
    public Result<Long> create(@RequestBody Map<String, Object> body, HttpSession session) {
        LoginUserDTO user = requireAdmin(session);
        String name = (String) body.get("name");
        Long directoryId = Long.valueOf(body.get("directoryId").toString());
        BigDecimal threshold = body.get("dirtyThreshold") != null
                ? new BigDecimal(body.get("dirtyThreshold").toString()) : new BigDecimal("10");
        return Result.ok(templateService.createDraft(name, directoryId, threshold, user.getUserId()));
    }

    @PutMapping("/{id}/sample")
    public Result<List<ParsedFieldDTO>> uploadSample(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            HttpSession session) {
        LoginUserDTO user = requireAdmin(session);
        return Result.ok(templateService.uploadSample(id, file, user.getUserId()));
    }

    @PostMapping("/{id}/ddl")
    public Result<DdlPreviewDTO> generateDdl(@PathVariable Long id, HttpSession session) {
        requireAdmin(session);
        return Result.ok(templateService.generateDdl(id));
    }

    @PostMapping("/{id}/table")
    public Result<Void> executeTable(@PathVariable Long id, HttpSession session) {
        LoginUserDTO user = requireAdmin(session);
        templateService.executeTable(id, user.getUserId());
        return Result.ok();
    }

    @PutMapping("/{id}/field-mapping")
    public Result<Void> saveFieldMapping(
            @PathVariable Long id,
            @RequestBody List<Map<String, Object>> mappings,
            HttpSession session) {
        LoginUserDTO user = requireAdmin(session);
        templateService.saveFieldMapping(id, mappings, user.getUserId());
        return Result.ok();
    }

    @PutMapping("/{id}/validation-rules")
    public Result<Void> saveValidationRules(
            @PathVariable Long id,
            @RequestBody List<Map<String, Object>> rules,
            HttpSession session) {
        LoginUserDTO user = requireAdmin(session);
        templateService.saveValidationRules(id, rules, user.getUserId());
        return Result.ok();
    }

    @PostMapping("/{id}/publish")
    public Result<Void> publish(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            HttpSession session) {
        LoginUserDTO user = requireAdmin(session);
        templateService.publish(id, body.get("changeLog"), user.getUserId());
        return Result.ok();
    }

    @PostMapping("/{id}/copy")
    public Result<Long> copy(@PathVariable Long id, HttpSession session) {
        LoginUserDTO user = requireAdmin(session);
        return Result.ok(templateService.copy(id, user.getUserId()));
    }

    @PutMapping("/{id}/disable")
    public Result<Void> disable(@PathVariable Long id, HttpSession session) {
        LoginUserDTO user = requireAdmin(session);
        templateService.disable(id, user.getUserId());
        return Result.ok();
    }

    @GetMapping("/{id}/versions")
    public Result<List<TemplateVersionDTO>> versions(@PathVariable Long id, HttpSession session) {
        requireAdmin(session);
        return Result.ok(templateService.listVersions(id));
    }

    @GetMapping("/{id}/fields")
    public Result<List<TemplateField>> fields(
            @PathVariable Long id,
            @RequestParam Long versionId,
            HttpSession session) {
        requireAdmin(session);
        return Result.ok(templateService.getFieldsByVersion(versionId));
    }

    private LoginUserDTO requireAdmin(HttpSession session) {
        LoginUserDTO user = (LoginUserDTO) session.getAttribute(UploadConstants.SESSION_LOGIN_USER);
        if (user == null) throw new BusinessException(401, "未登录");
        if (!user.isAdmin()) throw new BusinessException(403, "需要管理员权限");
        return user;
    }
}
