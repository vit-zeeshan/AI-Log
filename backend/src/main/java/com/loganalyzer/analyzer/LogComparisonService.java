package com.loganalyzer.analyzer;

import com.loganalyzer.dto.LogComparisonDto;
import com.loganalyzer.entity.LogAnalysis;
import com.loganalyzer.entity.ParsedError;
import com.loganalyzer.entity.User;
import com.loganalyzer.exception.ResourceNotFoundException;
import com.loganalyzer.repository.LogAnalysisRepository;
import com.loganalyzer.repository.ParsedErrorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class LogComparisonService {

    private final LogAnalysisRepository analysisRepository;
    private final ParsedErrorRepository parsedErrorRepository;

    public LogComparisonService(LogAnalysisRepository analysisRepository,
                                ParsedErrorRepository parsedErrorRepository) {
        this.analysisRepository = analysisRepository;
        this.parsedErrorRepository = parsedErrorRepository;
    }

    @Transactional(readOnly = true)
    public LogComparisonDto.CompareResponse compareAnalyses(Long baseId, Long targetId, User user) {
        LogAnalysis baseAnalysis = analysisRepository.findByIdAndUser(baseId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Base analysis", "id", baseId));

        LogAnalysis targetAnalysis = analysisRepository.findByIdAndUser(targetId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Target analysis", "id", targetId));

        List<ParsedError> baseErrors = parsedErrorRepository.findByAnalysisOrderByOccurrenceCountDesc(baseAnalysis);
        List<ParsedError> targetErrors = parsedErrorRepository.findByAnalysisOrderByOccurrenceCountDesc(targetAnalysis);

        Map<String, ParsedError> baseFingerprintMap = new HashMap<>();
        for (ParsedError err : baseErrors) {
            baseFingerprintMap.put(err.getFingerprint(), err);
        }

        Map<String, ParsedError> targetFingerprintMap = new HashMap<>();
        for (ParsedError err : targetErrors) {
            targetFingerprintMap.put(err.getFingerprint(), err);
        }

        List<LogComparisonDto.ErrorDiffItem> newErrors = new ArrayList<>();
        List<LogComparisonDto.ErrorDiffItem> resolvedErrors = new ArrayList<>();
        List<LogComparisonDto.ErrorDiffItem> recurringPatternChanges = new ArrayList<>();

        // Check target errors against base
        for (ParsedError targetErr : targetErrors) {
            String fp = targetErr.getFingerprint();
            if (!baseFingerprintMap.containsKey(fp)) {
                // New error introduced in target version
                newErrors.add(LogComparisonDto.ErrorDiffItem.builder()
                        .fingerprint(fp)
                        .title(targetErr.getTitle())
                        .severity(targetErr.getSeverity().name())
                        .category(targetErr.getCategory().getDisplayName())
                        .baseCount(0)
                        .targetCount(targetErr.getOccurrenceCount())
                        .countDelta(targetErr.getOccurrenceCount())
                        .sampleMessage(targetErr.getMessage())
                        .serviceName(targetErr.getServiceName())
                        .status("NEW")
                        .build());
            } else {
                // Existing error: check if count increased or decreased
                ParsedError baseErr = baseFingerprintMap.get(fp);
                int delta = targetErr.getOccurrenceCount() - baseErr.getOccurrenceCount();
                String status = delta > 0 ? "INCREASED" : (delta < 0 ? "DECREASED" : "UNCHANGED");

                recurringPatternChanges.add(LogComparisonDto.ErrorDiffItem.builder()
                        .fingerprint(fp)
                        .title(targetErr.getTitle())
                        .severity(targetErr.getSeverity().name())
                        .category(targetErr.getCategory().getDisplayName())
                        .baseCount(baseErr.getOccurrenceCount())
                        .targetCount(targetErr.getOccurrenceCount())
                        .countDelta(delta)
                        .sampleMessage(targetErr.getMessage())
                        .serviceName(targetErr.getServiceName())
                        .status(status)
                        .build());
            }
        }

        // Check base errors that are missing in target (RESOLVED)
        for (ParsedError baseErr : baseErrors) {
            String fp = baseErr.getFingerprint();
            if (!targetFingerprintMap.containsKey(fp)) {
                resolvedErrors.add(LogComparisonDto.ErrorDiffItem.builder()
                        .fingerprint(fp)
                        .title(baseErr.getTitle())
                        .severity(baseErr.getSeverity().name())
                        .category(baseErr.getCategory().getDisplayName())
                        .baseCount(baseErr.getOccurrenceCount())
                        .targetCount(0)
                        .countDelta(-baseErr.getOccurrenceCount())
                        .sampleMessage(baseErr.getMessage())
                        .serviceName(baseErr.getServiceName())
                        .status("RESOLVED")
                        .build());
            }
        }

        int baseTotal = baseAnalysis.getTotalErrors();
        int targetTotal = targetAnalysis.getTotalErrors();
        int diff = targetTotal - baseTotal;
        double pctChange = baseTotal > 0 ? ((double) diff / baseTotal) * 100.0 : 0.0;

        String summary = String.format("Comparison between '%s' (Base) and '%s' (Target): %d new errors introduced, %d errors resolved. Total error volume shifted from %d to %d (%.1f%%).",
                baseAnalysis.getLogFile().getOriginalFilename(),
                targetAnalysis.getLogFile().getOriginalFilename(),
                newErrors.size(),
                resolvedErrors.size(),
                baseTotal,
                targetTotal,
                pctChange);

        return LogComparisonDto.CompareResponse.builder()
                .baseAnalysisId(baseId)
                .baseFilename(baseAnalysis.getLogFile().getOriginalFilename())
                .targetAnalysisId(targetId)
                .targetFilename(targetAnalysis.getLogFile().getOriginalFilename())
                .baseTotalErrors(baseTotal)
                .targetTotalErrors(targetTotal)
                .errorCountDifference(diff)
                .percentageChange(Math.round(pctChange * 10.0) / 10.0)
                .newErrors(newErrors)
                .resolvedErrors(resolvedErrors)
                .recurringPatternChanges(recurringPatternChanges)
                .comparisonSummary(summary)
                .build();
    }
}
