package com.example.wms_2025.service;

import com.example.wms_2025.domain.entity.OperationLog;
import com.example.wms_2025.domain.entity.User;
import com.example.wms_2025.dto.admin.OperationLogArchiveResponse;
import com.example.wms_2025.dto.admin.OperationLogResponse;
import com.example.wms_2025.repository.OperationLogRepository;
import com.example.wms_2025.repository.OperationLogRepository.OperationLogDailySummaryProjection;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OperationLogService {

    private final OperationLogRepository operationLogRepository;
    private final UserService userService;

    @Transactional
    public void record(String module, String action, String details) {
        User operator = userService.getCurrentUser();
        OperationLog log = new OperationLog();
        log.setOperator(operator);
        log.setModule(module);
        log.setAction(action);
        log.setDetails(sanitize(details));
        operationLogRepository.save(log);
    }

    public boolean hasLogs(Long operatorId) {
        Objects.requireNonNull(operatorId, "operatorId must not be null");
        return operationLogRepository.countByOperatorId(operatorId) > 0;
    }

    public Page<OperationLogResponse> search(String username, String module, String action,
            LocalDateTime startTime, LocalDateTime endTime, Pageable pageable) {
        Objects.requireNonNull(pageable, "pageable must not be null");
        Specification<OperationLog> spec = buildSpecification(username, module, action, startTime, endTime);
        Pageable pageRequest = pageable;
        if (pageable.getSort().isUnsorted()) {
            pageRequest = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                    Sort.by(Sort.Direction.DESC, "createdAt"));
        }
        return operationLogRepository.findAll(spec, pageRequest).map(this::toResponse);
    }

    public byte[] exportLogs(String username, String module, String action,
            LocalDateTime startTime, LocalDateTime endTime) {
        Specification<OperationLog> spec = buildSpecification(username, module, action, startTime, endTime);
        List<OperationLog> logs = operationLogRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "createdAt"));
        return toCsv(logs);
    }

    public List<OperationLogArchiveResponse> listDailyArchives(int limit) {
        int size = Math.max(1, Math.min(limit, 90));
        List<OperationLogDailySummaryProjection> summaries = operationLogRepository
                .findDailySummaries(PageRequest.of(0, size));
        return summaries.stream()
                .map(summary -> new OperationLogArchiveResponse(
                        buildArchiveName(summary.getLogDate()),
                        summary.getLogDate(),
                        summary.getTotal() == null ? 0L : summary.getTotal(),
                        summary.getLastTime()))
                .collect(Collectors.toList());
    }

    public byte[] exportDailyArchive(LocalDate date) {
        Objects.requireNonNull(date, "date must not be null");
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay().minusNanos(1);
        return exportLogs(null, null, null, start, end);
    }

    public List<OperationLogResponse> previewDaily(LocalDate date, int size) {
        Objects.requireNonNull(date, "date must not be null");
        int limit = Math.max(1, Math.min(size, 200));
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay().minusNanos(1);
        Page<OperationLogResponse> page = search(null, null, null, start, end,
                PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt")));
        return page.getContent();
    }

    private Specification<OperationLog> buildSpecification(String username, String module, String action,
            LocalDateTime startTime, LocalDateTime endTime) {
        Specification<OperationLog> spec = Specification.where(null);
        if (StringUtils.hasText(username)) {
            spec = spec.and((root, query, cb) -> cb.equal(cb.lower(root.join("operator").get("username")),
                    username.toLowerCase()));
        }
        if (StringUtils.hasText(module)) {
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("module")),
                    "%" + module.toLowerCase() + "%"));
        }
        if (StringUtils.hasText(action)) {
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("action")),
                    "%" + action.toLowerCase() + "%"));
        }
        if (startTime != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), startTime));
        }
        if (endTime != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), endTime));
        }
        return spec;
    }

    private OperationLogResponse toResponse(OperationLog log) {
        String operator = log.getOperator() != null ? log.getOperator().getUsername() : null;
        return new OperationLogResponse(log.getId(), operator, log.getModule(), log.getAction(),
                log.getDetails(), log.getCreatedAt());
    }

    private byte[] toCsv(List<OperationLog> logs) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        StringBuilder builder = new StringBuilder();
        builder.append("ID,操作人,模块,动作,详情,时间\n");
        logs.forEach(log -> builder
                .append(log.getId() != null ? log.getId() : "")
                .append(',').append(escapeCsv(log.getOperator() != null ? log.getOperator().getUsername() : ""))
                .append(',').append(escapeCsv(log.getModule()))
                .append(',').append(escapeCsv(log.getAction()))
                .append(',').append(escapeCsv(log.getDetails()))
                .append(',').append(log.getCreatedAt() != null ? formatter.format(log.getCreatedAt()) : "")
                .append('\n'));
        return builder.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String escapeCsv(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String sanitized = value.replace("\r", " ").replace("\n", " ");
        String escaped = sanitized.replace("\"", "\"\"");
        return '"' + escaped + '"';
    }

    private String sanitize(String details) {
        if (details == null) {
            return null;
        }
        String trimmed = details.trim();
        if (trimmed.length() <= 1024) {
            return trimmed;
        }
        return trimmed.substring(0, 1021) + "...";
    }

    private String buildArchiveName(LocalDate date) {
        return String.format("operation-log-%s.csv", date.format(DateTimeFormatter.BASIC_ISO_DATE));
    }
}
