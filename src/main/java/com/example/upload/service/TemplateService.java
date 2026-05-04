package com.example.upload.service;

import com.example.upload.common.PageResult;
import com.example.upload.model.dto.*;
import com.example.upload.model.entity.TemplateField;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface TemplateService {
    Long createDraft(String name, Long directoryId, java.math.BigDecimal dirtyThreshold, Long userId);
    List<ParsedFieldDTO> uploadSample(Long templateId, MultipartFile file, Long userId);
    DdlPreviewDTO generateDdl(Long templateId);
    void executeTable(Long templateId, Long userId);
    void saveFieldMapping(Long templateId, List<Map<String, Object>> mappings, Long userId);
    void saveValidationRules(Long templateId, List<Map<String, Object>> rules, Long userId);
    void publish(Long templateId, String changeLog, Long userId);
    void disable(Long templateId, Long userId);
    Long copy(Long templateId, Long userId);
    PageResult<TemplateDTO> listTemplates(int page, int size, Long directoryId, String status);
    List<TemplateVersionDTO> listVersions(Long templateId);
    List<TemplateField> getFieldsByVersion(Long versionId);
}
