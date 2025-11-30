package com.example.wms_2025.controller;

import com.example.wms_2025.domain.enums.RoleCode;
import com.example.wms_2025.domain.enums.UserStatus;
import com.example.wms_2025.dto.admin.AdminUserCreateRequest;
import com.example.wms_2025.dto.admin.AdminUserResponse;
import com.example.wms_2025.dto.admin.AdminUserUpdateRequest;
import com.example.wms_2025.dto.admin.OperationLogArchiveResponse;
import com.example.wms_2025.dto.admin.OperationLogResponse;
import com.example.wms_2025.dto.admin.UserPasswordResetRequest;
import com.example.wms_2025.dto.admin.UserRoleAssignmentRequest;
import com.example.wms_2025.dto.admin.UserStatusUpdateRequest;
import com.example.wms_2025.dto.common.ApiResponse;
import com.example.wms_2025.dto.common.PageResponse;
import com.example.wms_2025.dto.admin.RoleOptionResponse;
import com.example.wms_2025.service.AdminUserService;
import com.example.wms_2025.service.OperationLogService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminUserService adminUserService;
    private final OperationLogService operationLogService;

    public AdminController(AdminUserService adminUserService, OperationLogService operationLogService) {
        this.adminUserService = adminUserService;
        this.operationLogService = operationLogService;
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<PageResponse<AdminUserResponse>>> listUsers(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(required = false) RoleCode role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int pageNumber = Math.max(page, 0);
        int pageSize = size <= 0 ? 20 : Math.min(size, 100);
        Page<AdminUserResponse> result = adminUserService.searchUsers(username, status, role,
                PageRequest.of(pageNumber, pageSize));
        PageResponse<AdminUserResponse> body = PageResponse.of(result.getTotalElements(), result.getTotalPages(),
                result.getNumber(), result.getSize(), result.getContent());
        return ResponseEntity.ok(ApiResponse.ok("Fetched user list", body));
    }

    @PostMapping("/users")
    public ResponseEntity<ApiResponse<AdminUserResponse>> createUser(
            @Valid @RequestBody AdminUserCreateRequest request) {
        AdminUserResponse response = adminUserService.createUser(request);
        return ResponseEntity.ok(ApiResponse.ok("User created", response));
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<ApiResponse<AdminUserResponse>> updateUser(@PathVariable Long id,
            @Valid @RequestBody AdminUserUpdateRequest request) {
        AdminUserResponse response = adminUserService.updateUser(id, request);
        return ResponseEntity.ok(ApiResponse.ok("User updated", response));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id) {
        adminUserService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.ok("User deleted", null));
    }

    @PutMapping("/users/{id}/status")
    public ResponseEntity<ApiResponse<AdminUserResponse>> updateStatus(@PathVariable Long id,
            @Valid @RequestBody UserStatusUpdateRequest request) {
        AdminUserResponse response = adminUserService.updateStatus(id, request.status());
        return ResponseEntity.ok(ApiResponse.ok("User status updated", response));
    }

    @PutMapping("/users/{id}/password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@PathVariable Long id,
            @Valid @RequestBody UserPasswordResetRequest request) {
        adminUserService.resetPassword(id, request.newPassword());
        return ResponseEntity.ok(ApiResponse.ok("Password reset", null));
    }

    @PostMapping("/users/{id}/roles")
    public ResponseEntity<ApiResponse<AdminUserResponse>> assignRoles(@PathVariable Long id,
            @Valid @RequestBody UserRoleAssignmentRequest request) {
        AdminUserResponse response = adminUserService.assignRoles(id, request.roleCodes());
        return ResponseEntity.ok(ApiResponse.ok("User roles updated", response));
    }

    @GetMapping("/logs")
    public ResponseEntity<ApiResponse<PageResponse<OperationLogResponse>>> listLogs(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int pageNumber = Math.max(page, 0);
        int pageSize = size <= 0 ? 20 : Math.min(size, 100);
        Page<OperationLogResponse> result = operationLogService.search(username, module, action, startTime, endTime,
                PageRequest.of(pageNumber, pageSize));
        PageResponse<OperationLogResponse> body = PageResponse.of(result.getTotalElements(), result.getTotalPages(),
                result.getNumber(), result.getSize(), result.getContent());
        return ResponseEntity.ok(ApiResponse.ok("Fetched operation logs", body));
    }

    @GetMapping("/logs/export")
    public ResponseEntity<byte[]> exportLogs(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        byte[] data = operationLogService.exportLogs(username, module, action, startTime, endTime);
        String timestamp = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now());
        String fileName = String.format("operation-logs-%s.csv", timestamp);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .body(data);
    }

    @GetMapping("/logs/archives")
    public ResponseEntity<ApiResponse<List<OperationLogArchiveResponse>>> listLogArchives(
            @RequestParam(defaultValue = "30") int limit) {
        List<OperationLogArchiveResponse> responses = operationLogService.listDailyArchives(limit);
        return ResponseEntity.ok(ApiResponse.ok("Fetched archive summaries", responses));
    }

    @GetMapping("/logs/archives/{date}/download")
    public ResponseEntity<byte[]> downloadArchive(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        byte[] data = operationLogService.exportDailyArchive(date);
        String fileName = String.format("operation-log-%s.csv", date.format(DateTimeFormatter.BASIC_ISO_DATE));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .body(data);
    }

    @GetMapping("/logs/archives/{date}/preview")
    public ResponseEntity<ApiResponse<List<OperationLogResponse>>> previewArchive(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "50") int size) {
        List<OperationLogResponse> responses = operationLogService.previewDaily(date, size);
        return ResponseEntity.ok(ApiResponse.ok("Fetched archive preview", responses));
    }

    @GetMapping("/roles")
    public ResponseEntity<ApiResponse<List<RoleOptionResponse>>> listRoles() {
        List<RoleOptionResponse> responses = adminUserService.listRoles();
        return ResponseEntity.ok(ApiResponse.ok("Fetched role list", responses));
    }
}
