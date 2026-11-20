package com.loganalyzer.service;

import com.loganalyzer.dto.DashboardSummaryDto;
import com.loganalyzer.dto.LogDtos;
import com.loganalyzer.entity.LogAnalysis;
import com.loganalyzer.entity.ParsedError;
import com.loganalyzer.entity.User;
import com.loganalyzer.repository.LogAnalysisRepository;
import com.loganalyzer.repository.LogFileRepository;
import com.loganalyzer.repository.ParsedErrorRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final LogFileRepository logFileRepository;
    private final LogAnalysisRepository logAnalysisRepository;
    private final ParsedErrorRepository parsedErrorRepository;
    private final AnalysisService analysisService;

    public DashboardService(LogFileRepository logFileRepository,
                            LogAnalysisRepository logAnalysisRepository,
                            ParsedErrorRepository parsedErrorRepository,
                            AnalysisService analysisService) {
        this.logFileRepository = logFileRepository;
        this.logAnalysisRepository = logAnalysisRepository;
        this.parsedErrorRepository = parsedErrorRepository;
        this.analysisService = analysisService;
    }

    @Transactional(readOnly = true)
    public DashboardSummaryDto getSummary(User user) {
        boolean isAdmin = "ROLE_ADMIN".equals(user.getRole());
        long totalLogFiles = isAdmin ? logFileRepository.count() : logFileRepository.countByUser(user);
        List<LogAnalysis> analyses = isAdmin
                ? logAnalysisRepository.findAllByOrderByCreatedAtDesc()
                : logAnalysisRepository.findByUserOrderByCreatedAtDesc(user);
        long totalAnalyses = analyses.size();

        long totalLogEntries = 0;
        long totalErrors = 0;
        long totalWarnings = 0;
        long criticalErrors = 0;
        long uniquePatterns = 0;
        long totalDuration = 0;

        Map<String, Long> severityDist = new LinkedHashMap<>();
        severityDist.put("CRITICAL", 0L);
        severityDist.put("HIGH", 0L);
        severityDist.put("MEDIUM", 0L);
        severityDist.put("LOW", 0L);

        Map<String, Long> categoryDist = new LinkedHashMap<>();
        Map<String, Long> serviceDist = new LinkedHashMap<>();
        List<DashboardSummaryDto.TimeSeriesPoint> timeSeries = new ArrayList<>();
        List<LogDtos.ParsedErrorDto> topRecurringErrors = new ArrayList<>();

        if (!analyses.isEmpty()) {
            for (LogAnalysis a : analyses) {
                totalLogEntries += (a.getTotalLines() != null ? a.getTotalLines() : 0);
                totalErrors += (a.getTotalErrors() != null ? a.getTotalErrors() : 0);
                totalWarnings += (a.getTotalWarnings() != null ? a.getTotalWarnings() : 0);
                criticalErrors += (a.getCriticalErrors() != null ? a.getCriticalErrors() : 0);
                uniquePatterns += (a.getUniquePatterns() != null ? a.getUniquePatterns() : 0);
                totalDuration += (a.getDurationMs() != null ? a.getDurationMs() : 0);

                timeSeries.add(new DashboardSummaryDto.TimeSeriesPoint(
                        a.getCreatedAt().toString().substring(0, Math.min(16, a.getCreatedAt().toString().length())),
                        a.getTotalErrors() != null ? a.getTotalErrors() : 0,
                        a.getTotalWarnings() != null ? a.getTotalWarnings() : 0
                ));
            }

            // Top analysis to sample detailed distributions
            LogAnalysis latest = analyses.get(0);
            List<Object[]> sevCounts = parsedErrorRepository.countBySeverity(latest);
            for (Object[] row : sevCounts) {
                severityDist.put(row[0].toString(), ((Number) row[1]).longValue());
            }

            List<Object[]> catCounts = parsedErrorRepository.countByCategory(latest);
            for (Object[] row : catCounts) {
                categoryDist.put(row[0].toString(), ((Number) row[1]).longValue());
            }

            List<Object[]> servCounts = parsedErrorRepository.countByServiceName(latest);
            for (Object[] row : servCounts) {
                serviceDist.put(row[0].toString(), ((Number) row[1]).longValue());
            }

            List<ParsedError> topErrors = parsedErrorRepository.findByAnalysisOrderByOccurrenceCountDesc(latest);
            topRecurringErrors = topErrors.stream()
                    .limit(5)
                    .map(analysisService::mapToErrorDto)
                    .collect(Collectors.toList());
        }

        double avgDuration = totalAnalyses > 0 ? (double) totalDuration / totalAnalyses : 0;

        List<LogDtos.LogAnalysisDto> recentAnalyses = analyses.stream()
                .limit(5)
                .map(a -> LogDtos.LogAnalysisDto.builder()
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
                        .build())
                .collect(Collectors.toList());

        return DashboardSummaryDto.builder()
                .totalLogFiles(totalLogFiles)
                .totalAnalyses(totalAnalyses)
                .totalLogEntries(totalLogEntries)
                .totalErrors(totalErrors)
                .totalWarnings(totalWarnings)
                .criticalErrors(criticalErrors)
                .uniqueErrorPatterns(uniquePatterns)
                .avgAnalysisDurationMs(Math.round(avgDuration * 10.0) / 10.0)
                .errorDistributionBySeverity(severityDist)
                .errorDistributionByCategory(categoryDist)
                .serviceWiseDistribution(serviceDist)
                .topRecurringErrors(topRecurringErrors)
                .errorFrequencyOverTime(timeSeries)
                .recentAnalyses(recentAnalyses)
                .build();
    }
}
