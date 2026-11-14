package com.loganalyzer.analyzer;

import com.loganalyzer.entity.ErrorCategory;
import com.loganalyzer.entity.ParsedError;
import com.loganalyzer.entity.Severity;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class CorrelationEngine {

    public void correlate(List<ParsedError> errors) {
        if (errors == null || errors.isEmpty()) {
            return;
        }

        // Sort by severity (CRITICAL first, then HIGH), then by occurrence count or first timestamp
        errors.sort(Comparator
                .comparing((ParsedError e) -> severityWeight(e.getSeverity()))
                .reversed()
                .thenComparing(ParsedError::getOccurrenceCount, Comparator.reverseOrder())
        );

        // Find primary root cause candidate:
        // Priority order for root cause triggers:
        // 1. Memory / OutOfMemory
        // 2. Database Connection Leak / Deadlock
        // 3. Infrastructure / Disk Full
        // 4. Configuration Error (missing beans, failed init)
        // 5. Deployment Error (ClassNotFound)
        // 6. Network / Downstream failure
        ParsedError primaryCandidate = null;

        for (ParsedError error : errors) {
            ErrorCategory cat = error.getCategory();
            if (cat == ErrorCategory.MEMORY_ISSUES ||
                cat == ErrorCategory.DATABASE_ERRORS ||
                cat == ErrorCategory.INFRASTRUCTURE_ERRORS ||
                cat == ErrorCategory.CONFIGURATION_ERRORS ||
                cat == ErrorCategory.DEPLOYMENT_ERRORS) {
                primaryCandidate = error;
                break;
            }
        }

        if (primaryCandidate == null) {
            // Pick highest severity or highest occurrence error
            primaryCandidate = errors.get(0);
        }

        primaryCandidate.setIsPrimary(true);
        primaryCandidate.setSupportingEvidence(
                String.format("Identified as PRIMARY root cause based on category [%s] with severity [%s] and %d occurrences. Initiated first around %s.",
                        primaryCandidate.getCategory().getDisplayName(),
                        primaryCandidate.getSeverity(),
                        primaryCandidate.getOccurrenceCount(),
                        primaryCandidate.getFirstOccurrence())
        );

        // Mark secondary errors
        for (ParsedError error : errors) {
            if (error != primaryCandidate) {
                error.setIsPrimary(false);
                error.setSupportingEvidence(
                        String.format("Classified as SECONDARY error cascading from primary root cause [%s] in service [%s].",
                                primaryCandidate.getTitle(),
                                primaryCandidate.getServiceName())
                );
            }
        }
    }

    private int severityWeight(Severity severity) {
        if (severity == null) return 0;
        return switch (severity) {
            case CRITICAL -> 4;
            case HIGH -> 3;
            case MEDIUM -> 2;
            case LOW -> 1;
        };
    }
}
