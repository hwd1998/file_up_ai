package com.example.upload.model.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class LoginUserDTO implements Serializable {
    private Long userId;
    private String openId;
    private String userName;
    private String role;
    private String departmentId;
    private String departmentName;

    public boolean isAdmin() {
        return "admin".equals(this.role);
    }
}
