package com.example.upload.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.upload.model.entity.UploadTask;
import com.example.upload.model.form.TaskQueryForm;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UploadTaskMapper extends BaseMapper<UploadTask> {

    IPage<UploadTask> selectTaskPage(Page<UploadTask> page,
                                     @Param("query") TaskQueryForm query,
                                     @Param("userId") Long userId,
                                     @Param("isAdmin") boolean isAdmin);
}
