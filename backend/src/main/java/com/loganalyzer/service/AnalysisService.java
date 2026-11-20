package com.loganalyzer.service;

import com.loganalyzer.ai.AiOrchestrator;
import com.loganalyzer.analyzer.CorrelationEngine;
import com.loganalyzer.analyzer.ErrorClusterer;
import com.loganalyzer.dto.LogDtos;
import com.loganalyzer.entity.*;
import com.loganalyzer.exception.ResourceNotFoundException;
import com.loganalyzer.parser.LogParser;
import com.loganalyzer.parser.LogParserFactory;
import com.loganalyzer.parser.RawLogEntry;
import com.loganalyzer.repository.LogAnalysisRepository;
import com.loganalyzer.repository.LogFileRepository;
import com.loganalyzer.repository.ParsedErrorRepository;
import com.loganalyzer.repository.RootCauseReportRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
public class AnalysisService {

    private static final Logger log = LoggerFactory.getLogger(AnalysisService.class);

    private final LogFileRepository logFileRepository;
    private final LogAnalysisRepository logAnalysisRepository;
    private final ParsedErrorRepository parsedErrorRepository;
    private final RootCauseReportRepository rootCauseReportRepository;
    private final FileStorageService fileStorageService;
    private final LogParserFactory logParserFactory;
    private final ErrorClusterer errorClusterer;
    private final CorrelationEngine correlationEngine;
    private final AiOrchestrator aiOrchestrator;

    public AnalysisService(LogFileRepository logFileRepository,
                           LogAnalysisRepository logAnalysisRepository,
                           ParsedErrorRepository parsedErrorRepository,
                           RootCauseReportRepository rootCauseReportRepository,
                           FileStorageService fileStorageService,
                           LogParserFactory logParserFactory,
                           ErrorClusterer errorClusterer,
                           CorrelationEngine correlationEngine,
                           AiOrchestrator aiOrchestrator) {
        this.logFileRepository = logFileRepository;
        this.logAnalysisRepository = logAnalysisRepository;
        this.parsedErrorRepository = parsedErrorRepository;
        this.rootCauseReportRepository = rootCauseReportRepository;
        this.fileStorageService = fileStorageService;
        this.logParserFactory = logParserFactory;
        this.errorClusterer = errorClusterer;
        this.correlationEngine = correlationEngine;
        this.aiOrchestrator = aiOrchestrator;
    }

    @Transactional
    public LogDtos.LogAnalysisDto analyzeLogFile(Long logFileId, User user) {
        long startTime = System.currentTimeMillis();

        LogFile logFile = "ROLE_ADMIN".equals(user.getRole())
                ? logFileRepository.findById(logFileId)
                        .orElseThrow(() -> new ResourceNotFoundException("Log file", "id", logFileId))
                : logFileRepository.findByIdAndUser(logFileId, user)
                        .orElseThrow(() -> new ResourceNotFoundException("Log file", "id", logFileId));

        logFile.setStatus(FileStatus.PARSING);
        logFileRepository.save(logFile);

        LogAnalysis analysis = LogAnalysis.builder()
                .logFile(logFile)
                .user(user)
                .status(AnalysisStatus.PARSING)
                .build();
        analysis = logAnalysisRepository.save(analysis);

        List<RawLogEntry> rawEntries = new ArrayList<>();
        AtomicLong totalLinesCount = new AtomicLong(0);
        int totalWarnings = 0;
        int totalRawErrors = 0;

        try {
            // Determine parser
            try (InputStream sampleIs = fileStorageService.getInputStream(logFile.getStoragePath(), logFile.getFileFormat())) {
                LogParser parser = logParserFactory.getParser(logFile.getOriginalFilename(), sampleIs);

                // Stream parse the log file
                try (BufferedReader reader = fileStorageService.getBufferedReader(logFile.getStoragePath(), logFile.getFileFormat())) {
                    parser.parse(reader, entry -> {
                        totalLinesCount.incrementAndGet();
                        if (entry.isErrorOrFatal() || entry.isWarning() || entry.hasStackTrace()) {
                            rawEntries.add(entry);
                        }
                    });
                }
            }

            for (RawLogEntry entry : rawEntries) {
                if (entry.isWarning()) totalWarnings++;
                if (entry.isErrorOrFatal()) totalRawErrors++;
            }

            // Group recurring errors & patterns
            analysis.setStatus(AnalysisStatus.ANALYZING);
            logFile.setStatus(FileStatus.ANALYZING);

            List<ParsedError> clusteredErrors = errorClusterer.cluster(rawEntries);

            // Correlate primary vs secondary errors
            correlationEngine.correlate(clusteredErrors);

            int criticalCount = 0;
            for (ParsedError pe : clusteredErrors) {
                pe.setAnalysis(analysis);
                if (pe.getSeverity() == Severity.CRITICAL) {
                    criticalCount++;
                }
            }

            parsedErrorRepository.saveAll(clusteredErrors);

            // Generate AI Root Cause Reports for the top errors
            for (ParsedError pe : clusteredErrors) {
                RootCauseReport report = aiOrchestrator.generateRootCauseAnalysis(pe);
                report.setAnalysis(analysis);
                report.setParsedError(pe);
                rootCauseReportRepository.save(report);
                pe.setRootCauseReport(report);
            }

            long duration = System.currentTimeMillis() - startTime;

            analysis.setTotalLines(totalLinesCount.get());
            analysis.setTotalErrors(totalRawErrors > 0 ? totalRawErrors : clusteredErrors.size());
            analysis.setTotalWarnings(totalWarnings);
            analysis.setCriticalErrors(criticalCount);
            analysis.setUniquePatterns(clusteredErrors.size());
            analysis.setDurationMs(duration);
            analysis.setStatus(AnalysisStatus.COMPLETED);
            analysis.setSummary(String.format("Log analysis completed in %d ms. Detected %d total errors across %d unique error signatures. %d critical errors require immediate SRE attention.",
                    duration, totalRawErrors, clusteredErrors.size(), criticalCount));

            logFile.setStatus(FileStatus.ANALYZED);
            logFileRepository.save(logFile);
            logAnalysisRepository.save(analysis);

            return mapToDto(analysis, clusteredErrors);

        } catch (Exception e) {
            log.error("Log analysis failed for file id {}: {}", logFileId, e.getMessage(), e);
            analysis.setStatus(AnalysisStatus.FAILED);
            analysis.setSummary("Analysis failed: " + e.getMessage());
            logFile.setStatus(FileStatus.FAILED);
            logFileRepository.save(logFile);
            logAnalysisRepository.save(analysis);
            return mapToDto(analysis, List.of());
        }
    }

    @Transactional(readOnly = true)
    public LogDtos.LogAnalysisDto getAnalysis(Long id, User user) {
        LogAnalysis analysis = "ROLE_ADMIN".equals(user.getRole())
                ? logAnalysisRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Analysis", "id", id))
                : logAnalysisRepository.findByIdAndUser(id, user)
                        .orElseThrow(() -> new ResourceNotFoundException("Analysis", "id", id));
        List<ParsedError> errors = parsedErrorRepository.findByAnalysisOrderByOccurrenceCountDesc(analysis);
        return mapToDto(analysis, errors);
    }

    @Transactional(readOnly = true)
    public Page<LogDtos.ParsedErrorDto> getAnalysisErrors(
            Long analysisId, Severity severity, ErrorCategory category,
            String serviceName, String query, Pageable pageable, User user) {

        LogAnalysis analysis = "ROLE_ADMIN".equals(user.getRole())
                ? logAnalysisRepository.findById(analysisId)
                        .orElseThrow(() -> new ResourceNotFoundException("Analysis", "id", analysisId))
                : logAnalysisRepository.findByIdAndUser(analysisId, user)
                        .orElseThrow(() -> new ResourceNotFoundException("Analysis", "id", analysisId));

        Page<ParsedError> page = parsedErrorRepository.searchErrors(
                analysis, severity, category, serviceName, query, pageable
        );

        return page.map(this::mapToErrorDto);
    }

    @Transactional(readOnly = true)
    public String exportAnalysisReport(Long id, User user) {
        LogAnalysis analysis = "ROLE_ADMIN".equals(user.getRole())
                ? logAnalysisRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Analysis", "id", id))
                : logAnalysisRepository.findByIdAndUser(id, user)
                        .orElseThrow(() -> new ResourceNotFoundException("Analysis", "id", id));
        List<ParsedError> errors = parsedErrorRepository.findByAnalysisOrderByOccurrenceCountDesc(analysis);

        StringBuilder sb = new StringBuilder();
        sb.append("# AI-Powered Incident Root Cause Report\n\n");
        sb.append("**Target File:** `").append(analysis.getLogFile().getOriginalFilename()).append("`\n");
        sb.append("**Analysis Timestamp:** ").append(analysis.getCreatedAt()).append("\n");
        sb.append("**Processing Duration:** ").append(analysis.getDurationMs()).append(" ms\n");
        sb.append("**Total Log Lines:** ").append(analysis.getTotalLines()).append("\n");
        sb.append("**Total Errors Detected:** ").append(analysis.getTotalErrors()).append("\n");
        sb.append("**Critical Errors:** ").append(analysis.getCriticalErrors()).append("\n");
        sb.append("**Unique Error Signatures:** ").append(analysis.getUniquePatterns()).append("\n\n");
        sb.append("---\n\n");
        sb.append("## Executive Summary\n\n");
        sb.append(analysis.getSummary()).append("\n\n");
        sb.append("---\n\n");
        sb.append("## Detailed Error Diagnoses & Solutions\n\n");

        int index = 1;
        for (ParsedError err : errors) {
            sb.append("### ").append(index++).append(". [").append(err.getSeverity()).append("] ")
                    .append(err.getTitle()).append("\n\n");
            sb.append("- **Category:** ").append(err.getCategory().getDisplayName()).append("\n");
            sb.append("- **Service:** `").append(err.getServiceName()).append("`\n");
            sb.append("- **Occurrences:** ").append(err.getOccurrenceCount()).append(" (First: `")
                    .append(err.getFirstOccurrence()).append("`, Last: `").append(err.getLastOccurrence()).append("`)\n");
            sb.append("- **Code Location:** `").append(err.getFileName()).append(":").append(err.getLineNumber()).append("`\n");
            sb.append("- **Primary Cause:** ").append(Boolean.TRUE.equals(err.getIsPrimary()) ? "YES (Root Cause)" : "NO (Cascading / Secondary)").append("\n\n");

            if (err.getRootCauseReport() != null) {
                RootCauseReport r = err.getRootCauseReport();
                sb.append("#### What Went Wrong?\n").append(r.getWhatWentWrong()).append("\n\n");
                sb.append("#### Why Did It Happen?\n").append(r.getWhyItHappened()).append("\n\n");
                sb.append("#### Impact Assessment\n").append(r.getImpactAssessment()).append("\n\n");
                sb.append("#### Supporting Evidence\n").append(r.getSupportingEvidence()).append("\n\n");
                sb.append("#### Immediate Fix\n").append(r.getImmediateFix()).append("\n\n");
                sb.append("#### Step-by-Step Debugging\n```text\n").append(r.getStepByStepDebugging()).append("\n```\n\n");
                if (r.getSuggestedCodeChange() != null && !r.getSuggestedCodeChange().isEmpty()) {
                    sb.append("#### Suggested Code or Configuration Change\n```java\n")
                            .append(r.getSuggestedCodeChange()).append("\n```\n\n");
                }
                if (r.getTroubleshootingCommands() != null && !r.getTroubleshootingCommands().isEmpty()) {
                    sb.append("#### Troubleshooting Commands\n```bash\n")
                            .append(r.getTroubleshootingCommands()).append("\n```\n\n");
                }
                sb.append("#### Long-Term Prevention\n").append(r.getLongTermPrevention()).append("\n\n");
            }

            if (err.getStackTrace() != null && !err.getStackTrace().isEmpty()) {
                sb.append("#### Stack Trace Snippet\n```text\n");
                String[] traceLines = err.getStackTrace().split("\n");
                int limit = Math.min(traceLines.length, 12);
                for (int i = 0; i < limit; i++) {
                    sb.append(traceLines[i]).append("\n");
                }
                if (traceLines.length > 12) {
                    sb.append("... (truncated ").append(traceLines.length - 12).append(" additional lines)\n");
                }
                sb.append("```\n\n");
            }
            sb.append("---\n\n");
        }

        return sb.toString();
    }

    private LogDtos.LogAnalysisDto mapToDto(LogAnalysis a, List<ParsedError> errors) {
        List<LogDtos.ParsedErrorDto> errorDtos = errors.stream()
                .limit(15)
                .map(this::mapToErrorDto)
                .collect(Collectors.toList());

        return LogDtos.LogAnalysisDto.builder()
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
                .topErrors(errorDtos)
                .build();
    }

    public LogDtos.ParsedErrorDto mapToErrorDto(ParsedError pe) {
        LogDtos.RootCauseReportDto reportDto = null;
        if (pe.getRootCauseReport() != null) {
            RootCauseReport r = pe.getRootCauseReport();
            reportDto = LogDtos.RootCauseReportDto.builder()
                    .id(r.getId())
                    .parsedErrorId(pe.getId())
                    .whatWentWrong(r.getWhatWentWrong())
                    .whyItHappened(r.getWhyItHappened())
                    .affectedComponent(r.getAffectedComponent())
                    .impactAssessment(r.getImpactAssessment())
                    .primaryOrSecondary(r.getPrimaryOrSecondary())
                    .supportingEvidence(r.getSupportingEvidence())
                    .immediateFix(r.getImmediateFix())
                    .stepByStepDebugging(r.getStepByStepDebugging())
                    .suggestedCodeChange(r.getSuggestedCodeChange())
                    .troubleshootingCommands(r.getTroubleshootingCommands())
                    .longTermPrevention(r.getLongTermPrevention())
                    .createdAt(r.getCreatedAt())
                    .build();
        }

        return LogDtos.ParsedErrorDto.builder()
                .id(pe.getId())
                .analysisId(pe.getAnalysis().getId())
                .title(pe.getTitle())
                .message(pe.getMessage())
                .severity(pe.getSeverity())
                .category(pe.getCategory())
                .categoryDisplayName(pe.getCategory().getDisplayName())
                .errorTimestamp(pe.getErrorTimestamp())
                .fileName(pe.getFileName())
                .lineNumber(pe.getLineNumber())
                .stackTrace(pe.getStackTrace())
                .serviceName(pe.getServiceName())
                .occurrenceCount(pe.getOccurrenceCount())
                .firstOccurrence(pe.getFirstOccurrence())
                .lastOccurrence(pe.getLastOccurrence())
                .fingerprint(pe.getFingerprint())
                .isPrimary(pe.getIsPrimary())
                .supportingEvidence(pe.getSupportingEvidence())
                .rawSnippet(pe.getRawSnippet())
                .rootCauseReport(reportDto)
                .build();
    }
}
