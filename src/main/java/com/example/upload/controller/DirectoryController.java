package com.example.upload.controller;

import com.example.upload.common.Result;
import com.example.upload.common.constants.UploadConstants;
import com.example.upload.exception.BusinessException;
import com.example.upload.model.dto.DirectoryTreeDTO;
import com.example.upload.model.dto.LoginUserDTO;
import com.example.upload.service.DirectoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/harness/api/v1/directories")
@Tag(name = "目录管理")
@RequiredArgsConstructor
public class DirectoryController {

    private final DirectoryService directoryService;

    @GetMapping("/tree")
    @Operation(summary = "获取目录树（含权限过滤）")
    public Result<List<DirectoryTreeDTO>> getTree(HttpSession session) {
        LoginUserDTO loginUser = getLoginUser(session);
        return Result.ok(directoryService.getTree(loginUser));
    }

    @PostMapping
    @Operation(summary = "创建目录（管理员）")
    public Result<Void> create(@RequestBody Map<String, Object> body, HttpSession session) {
        LoginUserDTO loginUser = getLoginUser(session);
        requireAdmin(loginUser);
        Long parentId = body.get("parentId") != null ? Long.valueOf(body.get("parentId").toString()) : null;
        String name = (String) body.get("name");
        directoryService.create(parentId, name, loginUser.getUserId());
        return Result.ok();
    }

    @PutMapping("/{id}")
    @Operation(summary = "修改目录名（管理员）")
    public Result<Void> rename(@PathVariable Long id,
                               @RequestBody Map<String, String> body,
                               HttpSession session) {
        LoginUserDTO loginUser = getLoginUser(session);
        requireAdmin(loginUser);
        directoryService.rename(id, body.get("name"), loginUser.getUserId());
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除目录（管理员）")
    public Result<Void> delete(@PathVariable Long id, HttpSession session) {
        LoginUserDTO loginUser = getLoginUser(session);
        requireAdmin(loginUser);
        directoryService.delete(id, loginUser.getUserId());
        return Result.ok();
    }

    @PostMapping("/{id}/permissions")
    @Operation(summary = "授权用户访问目录（管理员）")
    public Result<Void> grantPermission(@PathVariable Long id,
                                        @RequestBody Map<String, Object> body,
                                        HttpSession session) {
        LoginUserDTO loginUser = getLoginUser(session);
        requireAdmin(loginUser);
        Long userId = Long.valueOf(body.get("userId").toString());
        String permType = (String) body.get("permType");
        directoryService.grantPermission(id, userId, permType, loginUser.getUserId());
        return Result.ok();
    }

    @DeleteMapping("/{id}/permissions")
    @Operation(summary = "撤销目录权限（管理员）")
    public Result<Void> revokePermission(@PathVariable Long id,
                                         @RequestParam Long userId,
                                         @RequestParam String permType,
                                         HttpSession session) {
        LoginUserDTO loginUser = getLoginUser(session);
        requireAdmin(loginUser);
        directoryService.revokePermission(id, userId, permType);
        return Result.ok();
    }

    private LoginUserDTO getLoginUser(HttpSession session) {
        LoginUserDTO user = (LoginUserDTO) session.getAttribute(UploadConstants.SESSION_LOGIN_USER);
        if (user == null) throw new BusinessException(401, "未登录");
        return user;
    }

    private void requireAdmin(LoginUserDTO user) {
        if (!user.isAdmin()) throw new BusinessException(403, "需要管理员权限");
    }
}
