package com.loganalyzer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummaryDto {
    private long totalLogFiles;
    private long totalAnalyses;
    private long totalLogEntries;
    private long totalErrors;
    private long totalWarnings;
    private long criticalErrors;
    private long uniqueErrorPatterns;
    private double avgAnalysisDurationMs;
    private Map<String, Long> errorDistributionBySeverity;
    private Map<String, Long> errorDistributionByCategory;
    private Map<String, Long> serviceWiseDistribution;
    private List<LogDtos.ParsedErrorDto> topRecurringErrors;
    private List<TimeSeriesPoint> errorFrequencyOverTime;
    private List<LogDtos.LogAnalysisDto> recentAnalyses;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TimeSeriesPoint {
        private String timestamp;
        private long errorCount;
        private long warningCount;
    }
}
