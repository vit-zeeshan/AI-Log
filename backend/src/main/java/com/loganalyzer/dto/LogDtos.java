package com.loganalyzer.dto;

import com.loganalyzer.entity.AnalysisStatus;
import com.loganalyzer.entity.ErrorCategory;
import com.loganalyzer.entity.FileStatus;
import com.loganalyzer.entity.Severity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

public class LogDtos {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LogFileDto {
        private Long id;
        private String filename;
        private String originalFilename;
        private Long fileSize;
        private String fileFormat;
        private FileStatus status;
        private String checksum;
        private LocalDateTime uploadTimestamp;
        private Long latestAnalysisId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LogAnalysisDto {
        private Long id;
        private Long logFileId;
        private String filename;
        private Long totalLines;
        private Integer totalErrors;
        private Integer totalWarnings;
        private Integer criticalErrors;
        private Integer uniquePatterns;
        private Long durationMs;
        private AnalysisStatus status;
        private String summary;
        private LocalDateTime createdAt;
        private List<ParsedErrorDto> topErrors;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParsedErrorDto {
        private Long id;
        private Long analysisId;
        private String title;
        private String message;
        private Severity severity;
        private ErrorCategory category;
        private String categoryDisplayName;
        private String errorTimestamp;
        private String fileName;
        private Integer lineNumber;
        private String stackTrace;
        private String serviceName;
        private Integer occurrenceCount;
        private String firstOccurrence;
        private String lastOccurrence;
        private String fingerprint;
        private Boolean isPrimary;
        private String supportingEvidence;
        private String rawSnippet;
        private RootCauseReportDto rootCauseReport;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RootCauseReportDto {
        private Long id;
        private Long parsedErrorId;
        private String whatWentWrong;
        private String whyItHappened;
        private String affectedComponent;
        private String impactAssessment;
        private String primaryOrSecondary;
        private String supportingEvidence;
        private String immediateFix;
        private String stepByStepDebugging;
        private String suggestedCodeChange;
        private String troubleshootingCommands;
        private String longTermPrevention;
        private LocalDateTime createdAt;
    }
}
