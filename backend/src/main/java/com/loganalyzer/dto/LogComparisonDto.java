package com.loganalyzer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

public class LogComparisonDto {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CompareRequest {
        private Long baseAnalysisId;
        private Long targetAnalysisId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CompareResponse {
        private Long baseAnalysisId;
        private String baseFilename;
        private Long targetAnalysisId;
        private String targetFilename;
        private int baseTotalErrors;
        private int targetTotalErrors;
        private int errorCountDifference;
        private double percentageChange;
        private List<ErrorDiffItem> newErrors;
        private List<ErrorDiffItem> resolvedErrors;
        private List<ErrorDiffItem> recurringPatternChanges;
        private String comparisonSummary;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ErrorDiffItem {
        private String fingerprint;
        private String title;
        private String severity;
        private String category;
        private int baseCount;
        private int targetCount;
        private int countDelta;
        private String sampleMessage;
        private String serviceName;
        private String status; // "NEW", "RESOLVED", "INCREASED", "DECREASED", "UNCHANGED"
    }
}
