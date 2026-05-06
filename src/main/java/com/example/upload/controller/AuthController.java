package com.example.upload.controller;

import com.example.upload.common.constants.UploadConstants;
import com.example.upload.model.dto.LoginUserDTO;
import com.example.upload.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Controller
@RequestMapping("/auth/feishu")
@Tag(name = "飞书认证")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @GetMapping("/login")
    @Operation(summary = "跳转飞书授权页")
    public void login(HttpServletResponse response) throws IOException {
        String authUrl = authService.buildFeishuAuthUrl();
        response.sendRedirect(authUrl);
    }

    @GetMapping("/callback")
    @Operation(summary = "飞书 OAuth 回调")
    public String callback(@RequestParam(required = false) String code,
                           @RequestParam(required = false) String state,
                           @RequestParam(required = false) String error,
                           HttpSession session) {
        if (error != null || code == null) {
            log.warn("飞书回调异常: error={}, code={}", error, code);
            return "redirect:/login?error=" + URLEncoder.encode("飞书授权失败，请重试", StandardCharsets.UTF_8);
        }
        try {
            LoginUserDTO loginUser = authService.handleFeishuCallback(code);
            session.setAttribute(UploadConstants.SESSION_LOGIN_USER, loginUser);
            log.info("用户 {} 登录成功", loginUser.getUserName());
            return loginUser.isAdmin() ? "redirect:/admin/template" : "redirect:/home";
        } catch (Exception e) {
            log.error("飞书登录处理失败", e);
            return "redirect:/login?error=" + URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8);
        }
    }
}
