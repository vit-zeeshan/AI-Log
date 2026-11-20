package com.loganalyzer.controller;

import com.loganalyzer.dto.ApiResponse;
import com.loganalyzer.dto.LogDtos;
import com.loganalyzer.entity.LogFile;
import com.loganalyzer.entity.User;
import com.loganalyzer.service.AuthService;
import com.loganalyzer.service.LogFileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/logs")
@Tag(name = "Log Files", description = "Endpoints for uploading, inspecting, and deleting log files")
public class LogFileController {

    private final LogFileService logFileService;
    private final AuthService authService;

    public LogFileController(LogFileService logFileService, AuthService authService) {
        this.logFileService = logFileService;
        this.authService = authService;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a log file (.log, .txt, .json, .csv, .out, .gz)")
    public ResponseEntity<ApiResponse<LogDtos.LogFileDto>> uploadLogFile(
            @RequestParam("file") MultipartFile file) {
        User user = authService.getCurrentAuthenticatedUser();
        LogDtos.LogFileDto dto = logFileService.uploadLogFile(file, user);
        return ResponseEntity.ok(ApiResponse.ok("File uploaded successfully", dto));
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List **all** uploaded log files (admin view)")
    public ResponseEntity<ApiResponse<List<LogDtos.LogFileDto>>> getAllLogs() {
        List<LogDtos.LogFileDto> files = logFileService.getAllFiles();
        return ResponseEntity.ok(ApiResponse.ok(files));
    }

    @GetMapping
    @Operation(summary = "List all uploaded log files for current user")
    public ResponseEntity<ApiResponse<List<LogDtos.LogFileDto>>> getUserLogs() {
        User user = authService.getCurrentAuthenticatedUser();
        List<LogDtos.LogFileDto> files = logFileService.getUserFiles(user);
        return ResponseEntity.ok(ApiResponse.ok(files));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get specific log file details")
    public ResponseEntity<ApiResponse<LogDtos.LogFileDto>> getLogFile(@PathVariable Long id) {
        User user = authService.getCurrentAuthenticatedUser();
        LogFile file = logFileService.getLogFile(id, user);
        return ResponseEntity.ok(ApiResponse.ok(logFileService.mapToDto(file, null)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an uploaded log file and its analyses")
    public ResponseEntity<ApiResponse<Void>> deleteLogFile(@PathVariable Long id) {
        User user = authService.getCurrentAuthenticatedUser();
        logFileService.deleteFile(id, user);
        return ResponseEntity.ok(ApiResponse.ok("Log file deleted successfully", null));
    }
}
