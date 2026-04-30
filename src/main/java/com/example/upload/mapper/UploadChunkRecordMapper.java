package com.example.upload.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.upload.model.entity.UploadChunkRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UploadChunkRecordMapper extends BaseMapper<UploadChunkRecord> {
}
