package com.example.upload.controller;

import com.example.upload.common.constants.UploadConstants;
import com.example.upload.model.dto.LoginUserDTO;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
@RequiredArgsConstructor
public class ViewController {

    @GetMapping({"/", "/home"})
    public String home(HttpSession session, Model model) {
        LoginUserDTO loginUser = getLoginUser(session);
        if (loginUser == null) return "redirect:/login";
        model.addAttribute("loginUser", loginUser);
        return "index";
    }

    @GetMapping("/login")
    public String login(HttpSession session) {
        if (getLoginUser(session) != null) return "redirect:/home";
        return "login";
    }

    @GetMapping("/directory/{directoryId}/upload")
    public String uploadPage(@PathVariable Long directoryId, HttpSession session, Model model) {
        LoginUserDTO loginUser = getLoginUser(session);
        if (loginUser == null) return "redirect:/login";
        model.addAttribute("loginUser", loginUser);
        model.addAttribute("directoryId", directoryId);
        return "directory/upload";
    }

    @GetMapping("/directory/{directoryId}/history")
    public String historyPage(@PathVariable Long directoryId, HttpSession session, Model model) {
        LoginUserDTO loginUser = getLoginUser(session);
        if (loginUser == null) return "redirect:/login";
        model.addAttribute("loginUser", loginUser);
        model.addAttribute("directoryId", directoryId);
        return "directory/history";
    }

    @GetMapping("/admin/template")
    public String adminTemplate(HttpSession session, Model model) {
        LoginUserDTO loginUser = getLoginUser(session);
        if (loginUser == null) return "redirect:/login";
        if (!loginUser.isAdmin()) return "redirect:/home";
        model.addAttribute("loginUser", loginUser);
        return "admin/template";
    }

    @GetMapping("/admin/directory")
    public String adminDirectory(HttpSession session, Model model) {
        LoginUserDTO loginUser = getLoginUser(session);
        if (loginUser == null) return "redirect:/login";
        if (!loginUser.isAdmin()) return "redirect:/home";
        model.addAttribute("loginUser", loginUser);
        return "admin/directory";
    }

    @GetMapping("/admin/templates")
    public String adminTemplates(HttpSession session, Model model) {
        LoginUserDTO loginUser = getLoginUser(session);
        if (loginUser == null) return "redirect:/login";
        if (!loginUser.isAdmin()) return "redirect:/home";
        model.addAttribute("loginUser", loginUser);
        return "admin/template";
    }

    @GetMapping("/admin/templates/create")
    public String adminTemplateCreate(HttpSession session, Model model) {
        LoginUserDTO loginUser = getLoginUser(session);
        if (loginUser == null) return "redirect:/login";
        if (!loginUser.isAdmin()) return "redirect:/home";
        model.addAttribute("loginUser", loginUser);
        return "admin/template-wizard";
    }

    private LoginUserDTO getLoginUser(HttpSession session) {
        return (LoginUserDTO) session.getAttribute(UploadConstants.SESSION_LOGIN_USER);
    }
}
