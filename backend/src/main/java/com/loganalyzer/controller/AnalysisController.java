package com.loganalyzer.controller;

import com.loganalyzer.analyzer.LogComparisonService;
import com.loganalyzer.dto.ApiResponse;
import com.loganalyzer.dto.LogComparisonDto;
import com.loganalyzer.dto.LogDtos;
import com.loganalyzer.entity.ErrorCategory;
import com.loganalyzer.entity.LogAnalysis;
import com.loganalyzer.entity.Severity;
import com.loganalyzer.entity.User;
import com.loganalyzer.repository.LogAnalysisRepository;
import com.loganalyzer.service.AnalysisService;
import com.loganalyzer.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@Tag(name = "Analysis", description = "Endpoints for log analysis, root cause diagnosis, filtering, and comparison")
public class AnalysisController {

    private final AnalysisService analysisService;
    private final LogComparisonService logComparisonService;
    private final LogAnalysisRepository logAnalysisRepository;
    private final AuthService authService;

    public AnalysisController(AnalysisService analysisService,
                              LogComparisonService logComparisonService,
                              LogAnalysisRepository logAnalysisRepository,
                              AuthService authService) {
        this.analysisService = analysisService;
        this.logComparisonService = logComparisonService;
        this.logAnalysisRepository = logAnalysisRepository;
        this.authService = authService;
    }

    @PostMapping("/logs/{id}/analyze")
    @Operation(summary = "Trigger end-to-end log parsing and AI root cause analysis")
    public ResponseEntity<ApiResponse<LogDtos.LogAnalysisDto>> triggerAnalysis(@PathVariable Long id) {
        User user = authService.getCurrentAuthenticatedUser();
        LogDtos.LogAnalysisDto result = analysisService.analyzeLogFile(id, user);
        return ResponseEntity.ok(ApiResponse.ok("Analysis completed", result));
    }

    @GetMapping("/analysis/{id}")
    @Operation(summary = "Get analysis summary and top root cause reports")
    public ResponseEntity<ApiResponse<LogDtos.LogAnalysisDto>> getAnalysis(@PathVariable Long id) {
        User user = authService.getCurrentAuthenticatedUser();
        LogDtos.LogAnalysisDto result = analysisService.getAnalysis(id, user);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/analysis/history")
    @Operation(summary = "Get historical analysis records for current user")
    public ResponseEntity<ApiResponse<List<LogDtos.LogAnalysisDto>>> getHistory() {
        User user = authService.getCurrentAuthenticatedUser();
        List<LogAnalysis> list = "ROLE_ADMIN".equals(user.getRole())
                ? logAnalysisRepository.findAllByOrderByCreatedAtDesc()
                : logAnalysisRepository.findByUserOrderByCreatedAtDesc(user);
        List<LogDtos.LogAnalysisDto> dtos = list.stream().map(a -> LogDtos.LogAnalysisDto.builder()
                .id(a.getId())
                .logFileId(a.getLogFile().getId())
                .filename(a.getLogFile().getOriginalFilename())
                .totalLines(a.getTotalLines())
                .totalErrors(a.getTotalErrors())
                .totalWarnings(a.getTotalWarnings())
                .criticalErrors(a.getCriticalErrors())
                .uniquePatterns(a.getUniquePatterns())
                .durationMs(a.getDurationMs())
                .status(a.getStatus())
                .summary(a.getSummary())
                .createdAt(a.getCreatedAt())
                .build()
        ).collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.ok(dtos));
    }

    @GetMapping("/analysis/{id}/errors")
    @Operation(summary = "Paginated and filtered search of parsed errors for an analysis")
    public ResponseEntity<ApiResponse<Page<LogDtos.ParsedErrorDto>>> getAnalysisErrors(
            @PathVariable Long id,
            @RequestParam(required = false) Severity severity,
            @RequestParam(required = false) ErrorCategory category,
            @RequestParam(required = false) String serviceName,
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size) {
        User user = authService.getCurrentAuthenticatedUser();
        Pageable pageable = PageRequest.of(page, size);
        Page<LogDtos.ParsedErrorDto> result = analysisService.getAnalysisErrors(
                id, severity, category, serviceName, query, pageable, user
        );
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/analysis/{id}/export")
    @Operation(summary = "Export complete Markdown Root Cause Report for download")
    public ResponseEntity<String> exportReport(@PathVariable Long id) {
        User user = authService.getCurrentAuthenticatedUser();
        String report = analysisService.exportAnalysisReport(id, user);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"incident-root-cause-report-" + id + ".md\"")
                .contentType(MediaType.TEXT_MARKDOWN)
                .body(report);
    }

    @PostMapping("/analysis/compare")
    @Operation(summary = "Compare two log analyses to detect introduced errors and frequency deltas")
    public ResponseEntity<ApiResponse<LogComparisonDto.CompareResponse>> compareAnalyses(
            @RequestBody LogComparisonDto.CompareRequest request) {
        User user = authService.getCurrentAuthenticatedUser();
        LogComparisonDto.CompareResponse diff = logComparisonService.compareAnalyses(
                request.getBaseAnalysisId(), request.getTargetAnalysisId(), user
        );
        return ResponseEntity.ok(ApiResponse.ok("Comparison completed", diff));
    }
}
