package com.example.upload.service;

import com.example.upload.model.dto.DirectoryTreeDTO;
import com.example.upload.model.dto.LoginUserDTO;

import java.util.List;

public interface DirectoryService {
    /** 获取当前用户有权限的目录树 */
    List<DirectoryTreeDTO> getTree(LoginUserDTO loginUser);
    /** 创建目录（管理员）*/
    void create(Long parentId, String name, Long operatorId);
    /** 修改目录名（管理员）*/
    void rename(Long id, String name, Long operatorId);
    /** 软删除目录（管理员，无子目录且无绑定模板才可删）*/
    void delete(Long id, Long operatorId);
    /** 授权用户访问目录 */
    void grantPermission(Long directoryId, Long userId, String permType, Long operatorId);
    /** 撤销授权 */
    void revokePermission(Long directoryId, Long userId, String permType);
    /** 校验用户是否有目录指定权限 */
    void checkPermission(Long directoryId, Long userId, String permType);
}
