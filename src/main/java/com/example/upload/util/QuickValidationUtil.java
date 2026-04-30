package com.example.upload.util;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.upload.mapper.TemplateFieldMapper;
import com.example.upload.mapper.TemplateVersionMapper;
import com.example.upload.mapper.TemplateMapper;
import com.example.upload.model.entity.Template;
import com.example.upload.model.entity.TemplateField;
import com.example.upload.model.entity.TemplateVersion;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class QuickValidationUtil {

    private final TemplateMapper templateMapper;
    private final TemplateVersionMapper templateVersionMapper;
    private final TemplateFieldMapper templateFieldMapper;

    /**
     * 同步快速校验：表头匹配 + 必填列存在性
     * @return 错误列表，空列表代表通过
     */
    public List<String> quickValidate(String filePath, Long templateId) {
        if (templateId == null) {
            return Collections.emptyList();
        }

        Template template = templateMapper.selectById(templateId);
        if (template == null) {
            return Collections.emptyList();
        }

        TemplateVersion version = templateVersionMapper.selectOne(new LambdaQueryWrapper<TemplateVersion>()
                .eq(TemplateVersion::getTemplateId, templateId)
                .eq(TemplateVersion::getVersion, template.getCurrentVersion())
                .last("LIMIT 1"));
        if (version == null) {
            return Collections.emptyList();
        }

        List<TemplateField> fields = templateFieldMapper.selectList(new LambdaQueryWrapper<TemplateField>()
                .eq(TemplateField::getVersionId, version.getId())
                .eq(TemplateField::getIsDeleted, 0)
                .orderByAsc(TemplateField::getFieldOrder));

        try {
            List<String> fileHeaders = readFirstRowHeaders(filePath);
            return validateHeaders(fileHeaders, fields);
        } catch (Exception e) {
            log.error("快速校验读取文件失败: {}", e.getMessage());
            return List.of("文件读取失败，请检查文件格式是否为 .xls/.xlsx/.csv");
        }
    }

    private List<String> readFirstRowHeaders(String filePath) throws IOException {
        String lower = filePath.toLowerCase();
        if (lower.endsWith(".csv")) {
            return readCsvHeaders(filePath);
        } else if (lower.endsWith(".xlsx")) {
            return readExcelHeaders(filePath, false);
        } else if (lower.endsWith(".xls")) {
            return readExcelHeaders(filePath, true);
        }
        throw new BusinessFileFormatException("不支持的文件格式，请上传 .xls/.xlsx/.csv");
    }

    private List<String> readExcelHeaders(String filePath, boolean isXls) throws IOException {
        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook wb = isXls ? new HSSFWorkbook(fis) : new XSSFWorkbook(fis)) {
            Sheet sheet = wb.getSheetAt(0);
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) return Collections.emptyList();
            List<String> headers = new ArrayList<>();
            for (Cell cell : headerRow) {
                headers.add(cell.toString().trim());
            }
            return headers;
        }
    }

    private List<String> readCsvHeaders(String filePath) throws IOException {
        try (java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(new FileInputStream(filePath), "UTF-8"))) {
            String line = reader.readLine();
            if (line == null) return Collections.emptyList();
            return Arrays.stream(line.split(","))
                    .map(s -> s.trim().replace("\"", ""))
                    .collect(Collectors.toList());
        }
    }

    private List<String> validateHeaders(List<String> fileHeaders, List<TemplateField> fields) {
        List<String> errors = new ArrayList<>();
        Set<String> fileHeaderSet = new HashSet<>(fileHeaders);

        for (TemplateField field : fields) {
            if (field.getIsSkipped() == 1 || field.getConstantValue() != null) continue;
            String expected = field.getSourceColumn();
            if (!fileHeaderSet.contains(expected)) {
                errors.add("缺少列：" + expected);
            }
        }

        // 必填列存在性（此处仅校验列存在，值非空校验在异步完整校验中执行）
        for (TemplateField field : fields) {
            if (field.getIsRequired() == 1 && field.getIsSkipped() != 1) {
                if (!fileHeaderSet.contains(field.getSourceColumn())) {
                    String msg = "缺少必填列：" + field.getSourceColumn();
                    if (!errors.contains(msg)) errors.add(msg);
                }
            }
        }
        return errors;
    }

    private static class BusinessFileFormatException extends RuntimeException {
        BusinessFileFormatException(String msg) { super(msg); }
    }
}
