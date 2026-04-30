package com.example.upload.service.impl;

import cn.hutool.json.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.upload.mapper.SysUserMapper;
import com.example.upload.model.dto.LoginUserDTO;
import com.example.upload.model.entity.SysUser;
import com.example.upload.service.AuthService;
import com.example.upload.util.FeishuUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final FeishuUtil feishuUtil;
    private final SysUserMapper sysUserMapper;

    @Override
    public String buildFeishuAuthUrl() {
        String state = UUID.randomUUID().toString().replace("-", "");
        return feishuUtil.buildAuthUrl(state);
    }

    @Override
    public LoginUserDTO handleFeishuCallback(String code) {
        String userAccessToken = feishuUtil.getUserAccessToken(code);
        JSONObject userInfo = feishuUtil.getUserInfo(userAccessToken);

        String openId = userInfo.getStr("open_id");
        String name = userInfo.getStr("name");
        String email = userInfo.getStr("email");

        // 查找或创建用户
        SysUser user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getOpenId, openId)
        );
        if (user == null) {
            user = new SysUser();
            user.setOpenId(openId);
            user.setUserName(name);
            user.setEmail(email);
            user.setRole("user");
            sysUserMapper.insert(user);
            log.info("新用户首次登录: {} ({})", name, openId);
        } else {
            // 同步最新姓名
            user.setUserName(name);
            user.setEmail(email);
            sysUserMapper.updateById(user);
        }

        LoginUserDTO dto = new LoginUserDTO();
        dto.setUserId(user.getId());
        dto.setOpenId(openId);
        dto.setUserName(name);
        dto.setRole(user.getRole());
        dto.setDepartmentId(user.getDepartmentId());
        dto.setDepartmentName(user.getDepartmentName());
        return dto;
    }
}
