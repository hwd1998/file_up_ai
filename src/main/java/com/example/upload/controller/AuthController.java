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
    public String callback(@RequestParam String code,
                           @RequestParam(required = false) String state,
                           HttpSession session,
                           HttpServletResponse response) throws IOException {
        LoginUserDTO loginUser = authService.handleFeishuCallback(code);
        session.setAttribute(UploadConstants.SESSION_LOGIN_USER, loginUser);
        log.info("用户 {} 登录成功", loginUser.getUserName());

        if (loginUser.isAdmin()) {
            return "redirect:/admin/template";
        }
        return "redirect:/home";
    }
}
