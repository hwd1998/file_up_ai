package com.example.upload.service.impl;

import com.example.upload.exception.BusinessException;
import com.example.upload.model.dto.ParsedFieldDTO;
import com.example.upload.service.FileParseService;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@Slf4j
@Service
public class FileParseServiceImpl implements FileParseService {

    private static final int SAMPLE_ROWS = 20;
    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd"),
            DateTimeFormatter.ofPattern("yyyyMMdd")
    );

    @Override
    public List<ParsedFieldDTO> parseSample(MultipartFile file) {
        String name = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
        try {
            if (name.endsWith(".xlsx") || name.endsWith(".xls")) {
                return parseSampleExcel(file.getInputStream());
            } else if (name.endsWith(".csv")) {
                return parseSampleCsv(file.getInputStream());
            } else {
                throw new BusinessException("仅支持 .xlsx / .xls / .csv 格式");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (IOException e) {
            throw new BusinessException("文件读取失败：" + e.getMessage());
        } catch (Exception e) {
            log.error("解析文件异常", e);
            throw new BusinessException("文件解析失败：" + e.getMessage());
        }
    }

    @Override
    public ParsedResult parseAll(String filePath) {
        File f = new File(filePath);
        if (!f.exists()) throw new BusinessException("文件不存在：" + filePath);
        String name = filePath.toLowerCase();
        try {
            if (name.endsWith(".xlsx") || name.endsWith(".xls")) {
                return parseAllExcel(new FileInputStream(f));
            } else {
                return parseAllCsv(new FileInputStream(f));
            }
        } catch (IOException e) {
            throw new BusinessException("文件解析失败：" + e.getMessage());
        }
    }

    private List<ParsedFieldDTO> parseSampleExcel(InputStream is) throws IOException {
        try (Workbook wb = WorkbookFactory.create(is)) {
            Sheet sheet = wb.getSheetAt(0);
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) throw new BusinessException("文件无表头行");

            int colCount = headerRow.getLastCellNum();
            List<String>[] samples = new ArrayList[colCount];
            for (int i = 0; i < colCount; i++) samples[i] = new ArrayList<>();

            int dataRows = 0;
            for (int r = 1; r <= sheet.getLastRowNum() && dataRows < SAMPLE_ROWS; r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                for (int c = 0; c < colCount; c++) {
                    Cell cell = row.getCell(c);
                    String val = cellToString(cell);
                    if (val != null && !val.isBlank()) samples[c].add(val);
                }
                dataRows++;
            }

            List<ParsedFieldDTO> result = new ArrayList<>();
            for (int c = 0; c < colCount; c++) {
                Cell hCell = headerRow.getCell(c);
                String colName = hCell != null ? hCell.toString().trim() : "column_" + c;
                ParsedFieldDTO dto = new ParsedFieldDTO();
                dto.setColumnName(colName);
                dto.setInferredType(inferType(samples[c]));
                dto.setFieldOrder(c);
                result.add(dto);
            }
            return result;
        }
    }

    private List<ParsedFieldDTO> parseSampleCsv(InputStream is) throws IOException {
        try (CSVReader reader = new CSVReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String[] headers;
            try {
                headers = reader.readNext();
            } catch (CsvValidationException e) {
                throw new BusinessException("CSV 格式错误：" + e.getMessage());
            }
            if (headers == null) throw new BusinessException("文件无表头行");

            List<String>[] samples = new ArrayList[headers.length];
            for (int i = 0; i < headers.length; i++) samples[i] = new ArrayList<>();

            String[] line;
            int count = 0;
            try {
                while ((line = reader.readNext()) != null && count < SAMPLE_ROWS) {
                    for (int c = 0; c < headers.length && c < line.length; c++) {
                        if (line[c] != null && !line[c].isBlank()) samples[c].add(line[c]);
                    }
                    count++;
                }
            } catch (CsvValidationException e) {
                throw new BusinessException("CSV 解析错误：" + e.getMessage());
            }

            List<ParsedFieldDTO> result = new ArrayList<>();
            for (int c = 0; c < headers.length; c++) {
                ParsedFieldDTO dto = new ParsedFieldDTO();
                dto.setColumnName(headers[c].trim());
                dto.setInferredType(inferType(samples[c]));
                dto.setFieldOrder(c);
                result.add(dto);
            }
            return result;
        }
    }

    private ParsedResult parseAllExcel(InputStream is) throws IOException {
        try (Workbook wb = WorkbookFactory.create(is)) {
            Sheet sheet = wb.getSheetAt(0);
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) return new ParsedResult(List.of(), List.of());

            List<String> headers = new ArrayList<>();
            for (int c = 0; c < headerRow.getLastCellNum(); c++) {
                Cell cell = headerRow.getCell(c);
                headers.add(cell != null ? cell.toString().trim() : "");
            }

            List<Map<String, String>> rows = new ArrayList<>();
            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                Map<String, String> map = new LinkedHashMap<>();
                for (int c = 0; c < headers.size(); c++) {
                    Cell cell = row.getCell(c);
                    map.put(headers.get(c), cellToString(cell) != null ? cellToString(cell) : "");
                }
                rows.add(map);
            }
            return new ParsedResult(headers, rows);
        }
    }

    private ParsedResult parseAllCsv(InputStream is) throws IOException {
        try (CSVReader reader = new CSVReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String[] headerArr;
            try {
                headerArr = reader.readNext();
            } catch (CsvValidationException e) {
                throw new BusinessException("CSV 格式错误：" + e.getMessage());
            }
            if (headerArr == null) return new ParsedResult(List.of(), List.of());
            List<String> headers = Arrays.stream(headerArr).map(String::trim).toList();

            List<Map<String, String>> rows = new ArrayList<>();
            String[] line;
            try {
                while ((line = reader.readNext()) != null) {
                    Map<String, String> map = new LinkedHashMap<>();
                    for (int c = 0; c < headers.size(); c++) {
                        map.put(headers.get(c), c < line.length ? line[c] : "");
                    }
                    rows.add(map);
                }
            } catch (CsvValidationException e) {
                throw new BusinessException("CSV 解析错误：" + e.getMessage());
            }
            return new ParsedResult(headers, rows);
        }
    }

    private String inferType(List<String> samples) {
        if (samples.isEmpty()) return "varchar";
        long intCount = samples.stream().filter(this::isInteger).count();
        if (intCount >= samples.size() * 0.8) return "int";
        long decCount = samples.stream().filter(this::isDecimal).count();
        if (decCount >= samples.size() * 0.8) return "decimal";
        long dateCount = samples.stream().filter(this::isDate).count();
        if (dateCount >= samples.size() * 0.8) return "date";
        return "varchar";
    }

    private boolean isInteger(String s) {
        try { Long.parseLong(s.trim()); return true; } catch (NumberFormatException e) { return false; }
    }

    private boolean isDecimal(String s) {
        try { new java.math.BigDecimal(s.trim()); return true; } catch (NumberFormatException e) { return false; }
    }

    private boolean isDate(String s) {
        for (DateTimeFormatter fmt : DATE_FORMATS) {
            try { LocalDate.parse(s.trim(), fmt); return true; } catch (DateTimeParseException ignored) {}
        }
        return false;
    }

    private String cellToString(Cell cell) {
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> DateUtil.isCellDateFormatted(cell)
                    ? cell.getLocalDateTimeCellValue().toLocalDate().toString()
                    : String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCachedFormulaResultType() == CellType.NUMERIC
                    ? String.valueOf((long) cell.getNumericCellValue())
                    : cell.getStringCellValue();
            default -> null;
        };
    }
}
