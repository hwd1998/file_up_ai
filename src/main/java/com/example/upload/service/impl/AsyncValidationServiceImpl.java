package com.example.upload.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.upload.common.enums.TaskStatus;
import com.example.upload.mapper.*;
import com.example.upload.model.entity.*;
import com.example.upload.service.AsyncValidationService;
import com.example.upload.service.FileParseService;
import com.example.upload.util.FeishuUtil;
import com.example.upload.validation.*;
import com.example.upload.validation.rules.*;
import com.example.upload.config.FeishuProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncValidationServiceImpl implements AsyncValidationService {

    private final UploadTaskMapper uploadTaskMapper;
    private final UploadErrorMapper uploadErrorMapper;
    private final TemplateMapper templateMapper;
    private final TemplateVersionMapper templateVersionMapper;
    private final TemplateFieldMapper templateFieldMapper;
    private final FileParseService fileParseService;
    private final FeishuUtil feishuUtil;
    private final JdbcTemplate jdbcTemplate;
    private final FeishuProperties feishuProperties;

    @Override
    @Async("validationExecutor")
    public void submitAsync(Long taskId) {
        log.info("异步完整校验开始 taskId={}", taskId);
        UploadTask task = uploadTaskMapper.selectById(taskId);
        if (task == null) { log.error("任务不存在 taskId={}", taskId); return; }

        try {
            // 1. 加载模板配置
            Template tpl = templateMapper.selectById(task.getTemplateId());
            if (tpl == null) { failTask(task, "模板不存在"); return; }

            TemplateVersion version = task.getVersionId() != null
                    ? templateVersionMapper.selectById(task.getVersionId())
                    : templateVersionMapper.selectOne(new LambdaQueryWrapper<TemplateVersion>()
                        .eq(TemplateVersion::getTemplateId, tpl.getId())
                        .orderByDesc(TemplateVersion::getCreateTime).last("LIMIT 1"));
            if (version == null) { failTask(task, "模板版本不存在"); return; }

            List<TemplateField> fields = templateFieldMapper.selectList(
                    new LambdaQueryWrapper<TemplateField>().eq(TemplateField::getVersionId, version.getId()));

            // 2. 解析文件
            FileParseService.ParsedResult parsed = fileParseService.parseAll(task.getFilePath());
            int totalRows = parsed.rows().size();

            // 3. 构建校验上下文
            int threshold = tpl.getDirtyThreshold() != null ? tpl.getDirtyThreshold().intValue() : 10;
            ValidationContext ctx = ValidationContext.builder()
                    .headers(parsed.headers())
                    .rows(parsed.rows())
                    .fields(fields)
                    .dirtyThreshold(threshold)
                    .build();

            // 4. 执行校验链
            ValidationChain chain = new ValidationChain()
                    .addRule(new HeaderMatchRule())
                    .addRule(new RequiredFieldRule())
                    .addRule(new TypeCheckRule())
                    .addRule(new EnumValueRule())
                    .addRule(new RegexRule());

            ValidationResult result = chain.execute(ctx);

            // 5. 脏数据阈值判断
            Set<Integer> errorRows = new HashSet<>();
            result.getErrors().forEach(e -> errorRows.add(e.getRowNumber()));
            boolean overThreshold = DirtyThresholdRule.exceeds(errorRows.size(), totalRows, threshold);

            if (result.isAborted() || overThreshold) {
                saveErrors(taskId, result.getErrors());
                task.setStatus(TaskStatus.FAILED.getCode());
                task.setErrorCount(result.getErrors().size());
                uploadTaskMapper.updateById(task);
                String summary = buildErrorSummary(result.getErrors());
                sendNotify(task.getFileName(), false, totalRows, summary);
                log.info("任务 {} 校验失败，错误数={}", taskId, result.getErrors().size());
                return;
            }

            // 6. 批量写入 biz_* 表
            if (version.getTargetTableName() != null && !version.getTargetTableName().isBlank()) {
                batchInsert(version.getTargetTableName(), version.getVersion(), task.getId(), fields, parsed.rows());
            }

            // 7. 成功
            task.setStatus(TaskStatus.SUCCESS.getCode());
            task.setRowCount(totalRows);
            task.setErrorCount(0);
            uploadTaskMapper.updateById(task);
            sendNotify(task.getFileName(), true, totalRows, null);
            log.info("任务 {} 处理成功，写入 {} 行", taskId, totalRows);

        } catch (Exception e) {
            log.error("任务 {} 异步处理异常", taskId, e);
            failTask(task, "系统异常：" + e.getMessage());
        }
    }

    private void batchInsert(String tableName, String version, Long taskId,
                              List<TemplateField> fields, List<Map<String, String>> rows) {
        List<TemplateField> activeFields = fields.stream()
                .filter(f -> f.getIsSkipped() == null || f.getIsSkipped() == 0)
                .toList();
        if (activeFields.isEmpty() || rows.isEmpty()) return;

        StringBuilder colSql = new StringBuilder("(`task_id`,`_version`");
        for (TemplateField f : activeFields) colSql.append(",`").append(f.getTargetColumn()).append("`");
        colSql.append(")");

        StringBuilder placeholders = new StringBuilder("(?,?");
        for (int i = 0; i < activeFields.size(); i++) placeholders.append(",?");
        placeholders.append(")");

        String sql = "INSERT INTO `" + tableName + "` " + colSql + " VALUES " + placeholders;

        List<Object[]> batch = new ArrayList<>();
        for (Map<String, String> row : rows) {
            Object[] args = new Object[2 + activeFields.size()];
            args[0] = taskId;
            args[1] = version;
            for (int i = 0; i < activeFields.size(); i++) {
                TemplateField f = activeFields.get(i);
                args[2 + i] = f.getConstantValue() != null && !f.getConstantValue().isBlank()
                        ? f.getConstantValue()
                        : row.get(f.getSourceColumn());
            }
            batch.add(args);
            if (batch.size() >= 1000) {
                jdbcTemplate.batchUpdate(sql, batch);
                batch.clear();
            }
        }
        if (!batch.isEmpty()) jdbcTemplate.batchUpdate(sql, batch);
    }

    private void failTask(UploadTask task, String reason) {
        task.setStatus(TaskStatus.FAILED.getCode());
        uploadTaskMapper.updateById(task);
        sendNotify(task.getFileName(), false, 0, reason);
    }

    private void saveErrors(Long taskId, List<RowError> errors) {
        for (RowError e : errors) {
            UploadError err = new UploadError();
            err.setTaskId(taskId);
            err.setRowNumber(e.getRowNumber());
            err.setColumnName(e.getColumnName());
            err.setErrorType(e.getErrorType());
            err.setErrorMessage(e.getErrorMessage());
            uploadErrorMapper.insert(err);
        }
    }

    private void sendNotify(String fileName, boolean success, int rows, String errorSummary) {
        String webhookUrl = feishuProperties.getWebhookUrl();
        if (webhookUrl == null || webhookUrl.isBlank()) return;
        String status = success ? "成功" : "失败";
        String msg = "【上传" + status + "】" + fileName + " | " + rows + "行"
                + (errorSummary != null ? " | " + errorSummary : "");
        try {
            feishuUtil.sendWebhook(webhookUrl, msg);
        } catch (Exception e) {
            log.error("飞书通知发送失败", e);
        }
    }

    private String buildErrorSummary(List<RowError> errors) {
        return errors.stream().limit(3)
                .map(RowError::getErrorMessage)
                .reduce((a, b) -> a + "；" + b)
                .orElse("");
    }
}
