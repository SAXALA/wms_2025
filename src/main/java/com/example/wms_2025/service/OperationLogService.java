package com.example.wms_2025.service;

import com.example.wms_2025.domain.entity.OperationLog;
import com.example.wms_2025.domain.entity.User;
import com.example.wms_2025.dto.admin.OperationLogResponse;
import com.example.wms_2025.repository.OperationLogRepository;
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
        log.setDetails(details);
        operationLogRepository.save(log);
    }

    public boolean hasLogs(Long operatorId) {
        Objects.requireNonNull(operatorId, "operatorId must not be null");
        return operationLogRepository.countByOperatorId(operatorId) > 0;
    }

    public Page<OperationLogResponse> search(String username, String module, String action,
            LocalDateTime startTime, LocalDateTime endTime, Pageable pageable) {
        Objects.requireNonNull(pageable, "pageable must not be null");
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
        return operationLogRepository.findAll(spec, pageable).map(this::toResponse);
    }

    private OperationLogResponse toResponse(OperationLog log) {
        String operator = log.getOperator() != null ? log.getOperator().getUsername() : null;
        return new OperationLogResponse(log.getId(), operator, log.getModule(), log.getAction(),
                log.getDetails(), log.getCreatedAt());
    }
}
