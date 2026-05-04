package com.example.upload.service;

import com.example.upload.model.dto.DdlPreviewDTO;
import com.example.upload.model.entity.TemplateField;

import java.util.List;

public interface DdlService {
    /** 根据模板字段列表生成 DDL 预览（不执行） */
    DdlPreviewDTO generateDdl(Long templateId, int majorVersion, List<TemplateField> fields);

    /** 执行建表 DDL，返回实际表名 */
    String executeCreateTable(DdlPreviewDTO ddl);
}
