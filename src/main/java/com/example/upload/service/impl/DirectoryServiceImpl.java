package com.example.upload.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.upload.common.constants.UploadConstants;
import com.example.upload.exception.BusinessException;
import com.example.upload.mapper.DirectoryMapper;
import com.example.upload.mapper.DirectoryPermissionMapper;
import com.example.upload.model.dto.DirectoryTreeDTO;
import com.example.upload.model.dto.LoginUserDTO;
import com.example.upload.model.entity.Directory;
import com.example.upload.model.entity.DirectoryPermission;
import com.example.upload.service.DirectoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DirectoryServiceImpl implements DirectoryService {

    private final DirectoryMapper directoryMapper;
    private final DirectoryPermissionMapper permissionMapper;

    @Override
    public List<DirectoryTreeDTO> getTree(LoginUserDTO loginUser) {
        // 管理员可见全部目录，业务用户只看有权限的
        List<Directory> allDirs = directoryMapper.selectList(new LambdaQueryWrapper<Directory>()
                .eq(Directory::getIsDeleted, 0)
                .orderByAsc(Directory::getId));

        Set<Long> permittedIds;
        if (loginUser.isAdmin()) {
            permittedIds = allDirs.stream().map(Directory::getId).collect(Collectors.toSet());
        } else {
            List<Long> uploadIds = directoryMapper.selectPermittedDirectoryIds(
                    loginUser.getUserId(), UploadConstants.PERM_UPLOAD);
            List<Long> viewIds = directoryMapper.selectPermittedDirectoryIds(
                    loginUser.getUserId(), UploadConstants.PERM_VIEW_HISTORY);
            permittedIds = new HashSet<>();
            permittedIds.addAll(uploadIds);
            permittedIds.addAll(viewIds);
        }

        // 过滤有权限的及其祖先目录
        Set<Long> visibleIds = buildVisibleIds(allDirs, permittedIds);
        List<Directory> visibleDirs = allDirs.stream()
                .filter(d -> visibleIds.contains(d.getId()))
                .collect(Collectors.toList());

        // 查询权限映射
        Map<Long, List<String>> permMap = buildPermMap(loginUser);

        // 构建树
        return buildTree(visibleDirs, null, permMap, permittedIds);
    }

    @Override
    @Transactional
    public void create(Long parentId, String name, Long operatorId) {
        String fullPath;
        if (parentId == null) {
            fullPath = "/" + name;
        } else {
            Directory parent = directoryMapper.selectById(parentId);
            if (parent == null || parent.getIsDeleted() == 1) {
                throw new BusinessException("父目录不存在");
            }
            fullPath = parent.getFullPath() + "/" + name;
        }

        Directory dir = new Directory();
        dir.setParentId(parentId);
        dir.setName(name);
        dir.setFullPath(fullPath);
        dir.setCreatedBy(operatorId);
        directoryMapper.insert(dir);
    }

    @Override
    @Transactional
    public void rename(Long id, String name, Long operatorId) {
        Directory dir = directoryMapper.selectById(id);
        if (dir == null || dir.getIsDeleted() == 1) {
            throw new BusinessException("目录不存在");
        }
        dir.setName(name);
        directoryMapper.updateById(dir);
    }

    @Override
    @Transactional
    public void delete(Long id, Long operatorId) {
        long childCount = directoryMapper.selectCount(new LambdaQueryWrapper<Directory>()
                .eq(Directory::getParentId, id).eq(Directory::getIsDeleted, 0));
        if (childCount > 0) {
            throw new BusinessException("目录下存在子目录，无法删除");
        }
        Directory dir = directoryMapper.selectById(id);
        if (dir == null) {
            throw new BusinessException("目录不存在");
        }
        if (dir.getTemplateId() != null) {
            throw new BusinessException("目录已绑定模板，请先解绑后删除");
        }
        dir.setIsDeleted(1);
        directoryMapper.updateById(dir);
    }

    @Override
    @Transactional
    public void grantPermission(Long directoryId, Long userId, String permType, Long operatorId) {
        long exists = permissionMapper.selectCount(new LambdaQueryWrapper<DirectoryPermission>()
                .eq(DirectoryPermission::getDirectoryId, directoryId)
                .eq(DirectoryPermission::getUserId, userId)
                .eq(DirectoryPermission::getPermissionType, permType)
                .eq(DirectoryPermission::getIsDeleted, 0));
        if (exists > 0) {
            return;
        }
        DirectoryPermission perm = new DirectoryPermission();
        perm.setDirectoryId(directoryId);
        perm.setUserId(userId);
        perm.setPermissionType(permType);
        permissionMapper.insert(perm);
    }

    @Override
    @Transactional
    public void revokePermission(Long directoryId, Long userId, String permType) {
        permissionMapper.delete(new LambdaQueryWrapper<DirectoryPermission>()
                .eq(DirectoryPermission::getDirectoryId, directoryId)
                .eq(DirectoryPermission::getUserId, userId)
                .eq(DirectoryPermission::getPermissionType, permType));
    }

    @Override
    public void checkPermission(Long directoryId, Long userId, String permType) {
        long count = permissionMapper.selectCount(new LambdaQueryWrapper<DirectoryPermission>()
                .eq(DirectoryPermission::getDirectoryId, directoryId)
                .eq(DirectoryPermission::getUserId, userId)
                .eq(DirectoryPermission::getPermissionType, permType)
                .eq(DirectoryPermission::getIsDeleted, 0));
        if (count == 0) {
            throw new BusinessException(403, "无权限访问该目录");
        }
    }

    // ── 私有辅助方法 ──────────────────────────────────────────

    private Set<Long> buildVisibleIds(List<Directory> all, Set<Long> permittedIds) {
        Map<Long, Directory> idMap = all.stream().collect(Collectors.toMap(Directory::getId, d -> d));
        Set<Long> visible = new HashSet<>(permittedIds);
        for (Long pid : permittedIds) {
            Directory cur = idMap.get(pid);
            while (cur != null && cur.getParentId() != null) {
                visible.add(cur.getParentId());
                cur = idMap.get(cur.getParentId());
            }
        }
        return visible;
    }

    private Map<Long, List<String>> buildPermMap(LoginUserDTO loginUser) {
        if (loginUser.isAdmin()) {
            return Collections.emptyMap();
        }
        List<DirectoryPermission> perms = permissionMapper.selectList(
                new LambdaQueryWrapper<DirectoryPermission>()
                        .eq(DirectoryPermission::getUserId, loginUser.getUserId())
                        .eq(DirectoryPermission::getIsDeleted, 0));
        Map<Long, List<String>> map = new HashMap<>();
        for (DirectoryPermission p : perms) {
            map.computeIfAbsent(p.getDirectoryId(), k -> new ArrayList<>()).add(p.getPermissionType());
        }
        return map;
    }

    private List<DirectoryTreeDTO> buildTree(List<Directory> dirs, Long parentId,
                                              Map<Long, List<String>> permMap,
                                              Set<Long> permittedIds) {
        List<DirectoryTreeDTO> result = new ArrayList<>();
        for (Directory dir : dirs) {
            if (!Objects.equals(dir.getParentId(), parentId)) continue;
            DirectoryTreeDTO node = new DirectoryTreeDTO();
            node.setId(dir.getId());
            node.setParentId(dir.getParentId());
            node.setName(dir.getName());
            node.setFullPath(dir.getFullPath());
            node.setTemplateId(dir.getTemplateId());
            node.setUploadCycle(dir.getUploadCycle());
            node.setPermissions(permMap.getOrDefault(dir.getId(), List.of("upload", "view_history")));

            List<DirectoryTreeDTO> children = buildTree(dirs, dir.getId(), permMap, permittedIds);
            node.setLeaf(children.isEmpty());
            node.setChildren(children);
            result.add(node);
        }
        return result;
    }
}
