package com.example.upload.service;

import com.example.upload.model.dto.LoginUserDTO;

public interface AuthService {
    /** 构建飞书授权跳转 URL */
    String buildFeishuAuthUrl();
    /** 处理飞书回调，返回登录用户信息 */
    LoginUserDTO handleFeishuCallback(String code);
}
