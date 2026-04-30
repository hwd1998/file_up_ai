package com.example.upload.controller;

import com.example.upload.common.constants.UploadConstants;
import com.example.upload.model.dto.LoginUserDTO;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 仅用于本地开发调试，生产环境需通过 Spring Profile 或网关屏蔽此路径。
 */
@Slf4j
@Controller
@RequestMapping("/auth/mock")
public class MockLoginController {

    @GetMapping("/login")
    public String mockLogin(@RequestParam(defaultValue = "开发用户") String username,
                            @RequestParam(defaultValue = "user") String role,
                            HttpSession session) {
        LoginUserDTO mock = new LoginUserDTO();
        mock.setUserId("admin".equals(role) ? -1L : -2L);
        mock.setOpenId("mock_" + role);
        mock.setUserName(username);
        mock.setRole(role);

        session.setAttribute(UploadConstants.SESSION_LOGIN_USER, mock);
        log.warn("[MockLogin] 模拟登录: {} role={}", username, role);

        return "admin".equals(role) ? "redirect:/admin/template" : "redirect:/home";
    }
}
