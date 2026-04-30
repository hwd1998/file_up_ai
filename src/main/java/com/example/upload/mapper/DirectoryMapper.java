package com.example.upload.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.upload.model.entity.Directory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DirectoryMapper extends BaseMapper<Directory> {

    /** 查询用户有权限的所有目录 ID 列表 */
    List<Long> selectPermittedDirectoryIds(@Param("userId") Long userId,
                                           @Param("permType") String permType);
}
