package com.example.upload.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.upload.common.PageResult;
import com.example.upload.exception.BusinessException;
import com.example.upload.mapper.*;
import com.example.upload.model.dto.*;
import com.example.upload.model.entity.*;
import com.example.upload.service.DdlService;
import com.example.upload.service.FileParseService;
import com.example.upload.service.TemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TemplateServiceImpl implements TemplateService {

    private final TemplateMapper templateMapper;
    private final TemplateVersionMapper templateVersionMapper;
    private final TemplateFieldMapper templateFieldMapper;
    private final DirectoryMapper directoryMapper;
    private final FileParseService fileParseService;
    private final DdlService ddlService;

    @Override
    @Transactional
    public Long createDraft(String name, Long directoryId, BigDecimal dirtyThreshold, Long userId) {
        Template tpl = new Template();
        tpl.setName(name);
        tpl.setDirectoryId(directoryId);
        tpl.setStatus("draft");
        tpl.setDirtyThreshold(dirtyThreshold != null ? dirtyThreshold : new BigDecimal("10.00"));
        tpl.setCreatedBy(userId);
        templateMapper.insert(tpl);

        TemplateVersion version = new TemplateVersion();
        version.setTemplateId(tpl.getId());
        version.setVersion("V1.0");
        version.setCreatedBy(userId);
        templateVersionMapper.insert(version);

        return tpl.getId();
    }

    @Override
    @Transactional
    public List<ParsedFieldDTO> uploadSample(Long templateId, MultipartFile file, Long userId) {
        getTemplateOrThrow(templateId);
        TemplateVersion version = getDraftVersionOrThrow(templateId);

        templateFieldMapper.delete(new LambdaQueryWrapper<TemplateField>()
                .eq(TemplateField::getVersionId, version.getId()));

        List<ParsedFieldDTO> fields = fileParseService.parseSample(file);

        for (ParsedFieldDTO f : fields) {
            TemplateField field = new TemplateField();
            field.setVersionId(version.getId());
            field.setFieldName(f.getColumnName());
            field.setSourceColumn(f.getColumnName());
            field.setTargetColumn(toSnakeCase(f.getColumnName()));
            field.setFieldType(f.getInferredType());
            field.setFieldOrder(f.getFieldOrder());
            field.setIsRequired(0);
            field.setIsSkipped(0);
            templateFieldMapper.insert(field);
        }
        return fields;
    }

    @Override
    public DdlPreviewDTO generateDdl(Long templateId) {
        TemplateVersion version = getDraftVersionOrThrow(templateId);
        List<TemplateField> fields = templateFieldMapper.selectList(
                new LambdaQueryWrapper<TemplateField>().eq(TemplateField::getVersionId, version.getId()));
        return ddlService.generateDdl(templateId, 1, fields);
    }

    @Override
    @Transactional
    public void executeTable(Long templateId, Long userId) {
        DdlPreviewDTO ddl = generateDdl(templateId);
        String tableName = ddlService.executeCreateTable(ddl);

        TemplateVersion version = getDraftVersionOrThrow(templateId);
        version.setTargetTableName(tableName);
        templateVersionMapper.updateById(version);
    }

    @Override
    @Transactional
    public void saveFieldMapping(Long templateId, List<Map<String, Object>> mappings, Long userId) {
        TemplateVersion version = getDraftVersionOrThrow(templateId);
        for (Map<String, Object> m : mappings) {
            Long fieldId = Long.valueOf(m.get("id").toString());
            TemplateField field = templateFieldMapper.selectById(fieldId);
            if (field == null || !field.getVersionId().equals(version.getId())) {
                continue;
            }
            field.setSourceColumn(str(m, "sourceColumn"));
            field.setTargetColumn(str(m, "targetColumn"));
            field.setIsRequired(intVal(m, "isRequired"));
            field.setIsSkipped(intVal(m, "isSkipped"));
            field.setConstantValue(str(m, "constantValue"));
            templateFieldMapper.updateById(field);
        }
    }

    @Override
    @Transactional
    public void saveValidationRules(Long templateId, List<Map<String, Object>> rules, Long userId) {
        TemplateVersion version = getDraftVersionOrThrow(templateId);
        for (Map<String, Object> r : rules) {
            Long fieldId = Long.valueOf(r.get("id").toString());
            TemplateField field = templateFieldMapper.selectById(fieldId);
            if (field == null || !field.getVersionId().equals(version.getId())) {
                continue;
            }
            Object rulesJson = r.get("validationRules");
            field.setValidationRules(rulesJson != null ? rulesJson.toString() : null);
            templateFieldMapper.updateById(field);
        }
    }

    @Override
    @Transactional
    public void publish(Long templateId, String changeLog, Long userId) {
        TemplateVersion version = getDraftVersionOrThrow(templateId);
        if (version.getTargetTableName() == null) {
            throw new BusinessException("请先执行建表（Step2）后再发布");
        }
        version.setChangeLog(changeLog);
        templateVersionMapper.updateById(version);

        Template tpl = getTemplateOrThrow(templateId);
        tpl.setStatus("active");
        tpl.setCurrentVersion(version.getVersion());
        templateMapper.updateById(tpl);

        Directory dir = directoryMapper.selectById(tpl.getDirectoryId());
        if (dir != null) {
            dir.setTemplateId(templateId);
            directoryMapper.updateById(dir);
        }
    }

    @Override
    public void disable(Long templateId, Long userId) {
        Template tpl = getTemplateOrThrow(templateId);
        tpl.setStatus("disabled");
        templateMapper.updateById(tpl);
    }

    @Override
    @Transactional
    public Long copy(Long templateId, Long userId) {
        Template src = getTemplateOrThrow(templateId);
        TemplateVersion srcVersion = templateVersionMapper.selectOne(
                new LambdaQueryWrapper<TemplateVersion>()
                        .eq(TemplateVersion::getTemplateId, templateId)
                        .orderByDesc(TemplateVersion::getCreateTime)
                        .last("LIMIT 1"));

        Long newId = createDraft(src.getName() + "_复制", src.getDirectoryId(), src.getDirtyThreshold(), userId);
        if (srcVersion != null) {
            TemplateVersion newVersion = getDraftVersionOrThrow(newId);
            List<TemplateField> srcFields = templateFieldMapper.selectList(
                    new LambdaQueryWrapper<TemplateField>().eq(TemplateField::getVersionId, srcVersion.getId()));
            for (TemplateField f : srcFields) {
                TemplateField copy = new TemplateField();
                copy.setVersionId(newVersion.getId());
                copy.setFieldName(f.getFieldName());
                copy.setSourceColumn(f.getSourceColumn());
                copy.setTargetColumn(f.getTargetColumn());
                copy.setFieldType(f.getFieldType());
                copy.setFieldOrder(f.getFieldOrder());
                copy.setIsRequired(f.getIsRequired());
                copy.setIsSkipped(f.getIsSkipped());
                copy.setConstantValue(f.getConstantValue());
                copy.setValidationRules(f.getValidationRules());
                templateFieldMapper.insert(copy);
            }
        }
        return newId;
    }

    @Override
    public PageResult<TemplateDTO> listTemplates(int page, int size, Long directoryId, String status) {
        LambdaQueryWrapper<Template> wrapper = new LambdaQueryWrapper<Template>()
                .orderByDesc(Template::getCreateTime);
        if (directoryId != null) {
            wrapper.eq(Template::getDirectoryId, directoryId);
        }
        if (status != null && !status.isBlank()) {
            wrapper.eq(Template::getStatus, status);
        }

        Page<Template> pageResult = templateMapper.selectPage(new Page<>(page, size), wrapper);
        List<TemplateDTO> dtos = pageResult.getRecords().stream().map(this::toDTO).toList();
        return PageResult.of(pageResult.getTotal(), page, size, dtos);
    }

    @Override
    public List<TemplateVersionDTO> listVersions(Long templateId) {
        return templateVersionMapper.selectList(
                new LambdaQueryWrapper<TemplateVersion>()
                        .eq(TemplateVersion::getTemplateId, templateId)
                        .orderByDesc(TemplateVersion::getCreateTime))
                .stream().map(this::toVersionDTO).toList();
    }

    @Override
    public List<TemplateField> getFieldsByVersion(Long versionId) {
        return templateFieldMapper.selectList(
                new LambdaQueryWrapper<TemplateField>()
                        .eq(TemplateField::getVersionId, versionId)
                        .orderByAsc(TemplateField::getFieldOrder));
    }

    // ---- private helpers ----

    private Template getTemplateOrThrow(Long id) {
        Template tpl = templateMapper.selectById(id);
        if (tpl == null) {
            throw new BusinessException("模板不存在");
        }
        return tpl;
    }

    private TemplateVersion getDraftVersionOrThrow(Long templateId) {
        TemplateVersion v = templateVersionMapper.selectOne(
                new LambdaQueryWrapper<TemplateVersion>()
                        .eq(TemplateVersion::getTemplateId, templateId)
                        .orderByDesc(TemplateVersion::getCreateTime)
                        .last("LIMIT 1"));
        if (v == null) {
            throw new BusinessException("模板版本不存在，请重新创建");
        }
        return v;
    }

    private TemplateDTO toDTO(Template tpl) {
        TemplateDTO dto = new TemplateDTO();
        dto.setId(tpl.getId());
        dto.setName(tpl.getName());
        dto.setDirectoryId(tpl.getDirectoryId());
        if (tpl.getDirectoryId() != null) {
            Directory dir = directoryMapper.selectById(tpl.getDirectoryId());
            if (dir != null) dto.setDirectoryName(dir.getName());
        }
        dto.setStatus(tpl.getStatus());
        dto.setCurrentVersion(tpl.getCurrentVersion());
        dto.setDirtyThreshold(tpl.getDirtyThreshold());
        dto.setCreateTime(tpl.getCreateTime());
        return dto;
    }

    private TemplateVersionDTO toVersionDTO(TemplateVersion v) {
        TemplateVersionDTO dto = new TemplateVersionDTO();
        dto.setId(v.getId());
        dto.setVersion(v.getVersion());
        dto.setTargetTableName(v.getTargetTableName());
        dto.setChangeLog(v.getChangeLog());
        dto.setCreateTime(v.getCreateTime());
        return dto;
    }

    private String toSnakeCase(String name) {
        return name.replaceAll("[^a-zA-Z0-9_]", "_").toLowerCase();
    }

    private String str(Map<String, Object> m, String key) {
        Object v = m.get(key);
        return v != null ? v.toString() : null;
    }

    private int intVal(Map<String, Object> m, String key) {
        Object v = m.get(key);
        if (v == null) {
            return 0;
        }
        return Integer.parseInt(v.toString());
    }
}
