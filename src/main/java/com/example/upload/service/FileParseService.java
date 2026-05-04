package com.example.upload.service;

import com.example.upload.model.dto.ParsedFieldDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface FileParseService {
    /** 解析样例文件，返回字段列表（表头+推断类型） */
    List<ParsedFieldDTO> parseSample(MultipartFile file);

    /** 解析文件全量数据，返回 header + rows（用于异步校验） */
    ParsedResult parseAll(String filePath);

    record ParsedResult(List<String> headers, List<Map<String, String>> rows) {}
}
