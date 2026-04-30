package com.example.upload.model.dto;

import lombok.Data;

import java.util.List;

@Data
public class DirectoryTreeDTO {
    private Long id;
    private Long parentId;
    private String name;
    private String fullPath;
    private Long templateId;
    private String uploadCycle;
    private boolean isLeaf;
    /** 当前用户对此目录的权限列表 */
    private List<String> permissions;
    private List<DirectoryTreeDTO> children;
}
