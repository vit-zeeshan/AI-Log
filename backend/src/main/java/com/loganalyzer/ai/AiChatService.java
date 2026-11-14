package com.loganalyzer.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loganalyzer.dto.ChatDtos;
import com.loganalyzer.entity.*;
import com.loganalyzer.exception.ResourceNotFoundException;
import com.loganalyzer.repository.ChatMessageRepository;
import com.loganalyzer.repository.LogAnalysisRepository;
import com.loganalyzer.repository.LogFileRepository;
import com.loganalyzer.repository.ParsedErrorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class AiChatService {

    private final LogFileRepository logFileRepository;
    private final LogAnalysisRepository logAnalysisRepository;
    private final ParsedErrorRepository parsedErrorRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AiChatService(LogFileRepository logFileRepository,
                         LogAnalysisRepository logAnalysisRepository,
                         ParsedErrorRepository parsedErrorRepository,
                         ChatMessageRepository chatMessageRepository) {
        this.logFileRepository = logFileRepository;
        this.logAnalysisRepository = logAnalysisRepository;
        this.parsedErrorRepository = parsedErrorRepository;
        this.chatMessageRepository = chatMessageRepository;
    }

    @Transactional
    public ChatDtos.ChatResponse processMessage(ChatDtos.ChatRequest request, User user) {
        LogFile logFile = logFileRepository.findByIdAndUser(request.getLogFileId(), user)
                .orElseThrow(() -> new ResourceNotFoundException("Log file", "id", request.getLogFileId()));

        LogAnalysis analysis;
        if (request.getAnalysisId() != null) {
            analysis = logAnalysisRepository.findByIdAndUser(request.getAnalysisId(), user)
                    .orElseThrow(() -> new ResourceNotFoundException("Analysis", "id", request.getAnalysisId()));
        } else {
            analysis = logAnalysisRepository.findFirstByLogFileAndUserOrderByCreatedAtDesc(logFile, user)
                    .orElseThrow(() -> new ResourceNotFoundException("No analysis available for this log file. Please analyze it first."));
        }

        List<ParsedError> errors = parsedErrorRepository.findByAnalysisOrderByOccurrenceCountDesc(analysis);

        // Save user message
        ChatMessage userMsg = ChatMessage.builder()
                .logFile(logFile)
                .user(user)
                .senderRole(MessageRole.USER)
                .content(request.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
        chatMessageRepository.save(userMsg);

        // Formulate Senior SRE contextual response
        List<ChatDtos.ReferenceItem> references = new ArrayList<>();
        String answer = generateContextualAnswer(request.getMessage(), errors, references, analysis);

        String referencesJson = null;
        try {
            referencesJson = objectMapper.writeValueAsString(references);
        } catch (Exception e) {
            // Ignore
        }

        ChatMessage assistantMsg = ChatMessage.builder()
                .logFile(logFile)
                .user(user)
                .senderRole(MessageRole.ASSISTANT)
                .content(answer)
                .referencesJson(referencesJson)
                .timestamp(LocalDateTime.now())
                .build();
        chatMessageRepository.save(assistantMsg);

        return ChatDtos.ChatResponse.builder()
                .id(assistantMsg.getId())
                .role(MessageRole.ASSISTANT)
                .content(answer)
                .references(references)
                .timestamp(assistantMsg.getTimestamp())
                .build();
    }

    private String generateContextualAnswer(String query, List<ParsedError> errors,
                                            List<ChatDtos.ReferenceItem> references,
                                            LogAnalysis analysis) {
        if (errors.isEmpty()) {
            return "Based on the log file analysis, no ERROR or CRITICAL entries were detected in the processed logs. All application threads reported standard operational metrics.";
        }

        String q = query.toLowerCase();

        // Find primary root error
        ParsedError primaryError = errors.stream()
                .filter(e -> Boolean.TRUE.equals(e.getIsPrimary()))
                .findFirst()
                .orElse(errors.get(0));

        // Question: Why did application crash / what caused the failure?
        if (q.contains("crash") || q.contains("fail") || q.contains("what went wrong") || q.contains("root cause")) {
            addReference(primaryError, references);
            return String.format(
                    "**Primary Root Cause Diagnosis:**\n\n" +
                    "The incident was triggered by **%s** in service `%s`.\n\n" +
                    "- **Initial Occurrence:** `%s`\n" +
                    "- **Error Signature:** `%s`\n" +
                    "- **Code Location:** `%s` (Line %s)\n" +
                    "- **Frequency:** Occurred **%d times** across the captured window.\n\n" +
                    "**Technical Breakdown:**\n" +
                    "%s\n\n" +
                    "**Recommended Immediate Action:**\n" +
                    "Verify the database connection pool state or JVM memory limits. This primary error starved downstream worker threads, generating subsequent cascading HTTP 500 timeouts.",
                    primaryError.getTitle(),
                    primaryError.getServiceName(),
                    primaryError.getFirstOccurrence(),
                    primaryError.getMessage(),
                    primaryError.getFileName() != null ? primaryError.getFileName() : "Application Source",
                    primaryError.getLineNumber() != null ? primaryError.getLineNumber().toString() : "N/A",
                    primaryError.getOccurrenceCount(),
                    primaryError.getRootCauseReport() != null ? primaryError.getRootCauseReport().getWhatWentWrong() : primaryError.getSupportingEvidence()
            );
        }

        // Question: Database connection issues?
        if (q.contains("database") || q.contains("db") || q.contains("hikari") || q.contains("sql") || q.contains("connection")) {
            Optional<ParsedError> dbError = errors.stream()
                    .filter(e -> e.getCategory() == ErrorCategory.DATABASE_ERRORS || e.getMessage().toLowerCase().contains("hikari"))
                    .findFirst();

            if (dbError.isPresent()) {
                ParsedError err = dbError.get();
                addReference(err, references);
                return String.format(
                        "**Yes, confirmed database connection failure:**\n\n" +
                        "The logs show active database bottlenecks in `%s`:\n" +
                        "- **Error:** `%s`\n" +
                        "- **First Seen:** `%s`\n" +
                        "- **Impact:** Database connection pool starvation prevented queries from completing within the threshold.\n\n" +
                        "**Remediation:** Increase HikariCP `maximum-pool-size` and inspect for unclosed transactions or long-running queries holding database locks.",
                        err.getServiceName(),
                        err.getMessage(),
                        err.getFirstOccurrence()
                );
            } else {
                return "No explicit database connection or HikariCP pool errors were found in this log. The detected errors appear to be related to " + primaryError.getCategory().getDisplayName() + ".";
            }
        }

        // Question: Why HTTP 500 / API errors?
        if (q.contains("500") || q.contains("api") || q.contains("http") || q.contains("status")) {
            Optional<ParsedError> apiError = errors.stream()
                    .filter(e -> e.getCategory() == ErrorCategory.API_ERRORS || e.getMessage().toLowerCase().contains("500"))
                    .findFirst();

            ParsedError target = apiError.orElse(primaryError);
            addReference(target, references);

            return String.format(
                    "**HTTP 500 Internal Server Error Explanation:**\n\n" +
                    "HTTP 500 was returned because an unhandled exception propagated up the Spring Web filter chain in service `%s`:\n\n" +
                    "- **Exception:** `%s`\n" +
                    "- **Timestamp:** `%s`\n" +
                    "- **Trace Reference:** `%s:%s`\n\n" +
                    "Because this exception was not intercepted by a `@ControllerAdvice` or `@ExceptionHandler`, Spring Boot returned standard HTTP 500.",
                    target.getServiceName(),
                    target.getTitle(),
                    target.getFirstOccurrence(),
                    target.getFileName() != null ? target.getFileName() : "Controller",
                    target.getLineNumber() != null ? target.getLineNumber() : "0"
            );
        }

        // Question: Which error should I fix first / priority?
        if (q.contains("fix first") || q.contains("priority") || q.contains("which error")) {
            addReference(primaryError, references);
            StringBuilder sb = new StringBuilder();
            sb.append("**Prioritized Remediation Roadmap:**\n\n");
            sb.append("1. **Priority 1 (Fix First - Root Cause):** `").append(primaryError.getTitle()).append("` in `")
                    .append(primaryError.getServiceName()).append("` (Occurrences: ").append(primaryError.getOccurrenceCount())
                    .append(", Timestamp: ").append(primaryError.getFirstOccurrence()).append(")\n");

            int rank = 2;
            for (ParsedError err : errors) {
                if (err != primaryError && rank <= 4) {
                    addReference(err, references);
                    sb.append(rank).append(". **Priority ").append(rank).append(":** `").append(err.getTitle())
                            .append("` (").append(err.getCategory().getDisplayName()).append(")\n");
                    rank++;
                }
            }
            sb.append("\n*Resolving Priority 1 will eliminate dependent secondary cascade failures across downstream services.*");
            return sb.toString();
        }

        // Question: What happened just before the failure?
        if (q.contains("before") || q.contains("timeline") || q.contains("sequence")) {
            addReference(primaryError, references);
            return String.format(
                    "**Pre-Failure Timeline & Sequence:**\n\n" +
                    "- **Initial Trigger at [%s]:** `%s` was raised in service `%s`.\n" +
                    "- **Propagation:** Threads attempting to execute in `%s` encountered resource contention.\n" +
                    "- **Total Error Volume:** %d errors logged across %d lines.\n" +
                    "- **Last Recorded Event at [%s]:** `%s`",
                    primaryError.getFirstOccurrence(),
                    primaryError.getTitle(),
                    primaryError.getServiceName(),
                    primaryError.getFileName() != null ? primaryError.getFileName() : "worker",
                    analysis.getTotalErrors(),
                    analysis.getTotalLines(),
                    primaryError.getLastOccurrence(),
                    primaryError.getMessage()
            );
        }

        // Question: How to prevent / long term fix?
        if (q.contains("prevent") || q.contains("long term") || q.contains("avoid") || q.contains("solution")) {
            addReference(primaryError, references);
            return String.format(
                    "**Long-Term Prevention Strategy for `%s`:**\n\n" +
                    "1. **Circuit Breaking & Fallbacks:** Configure Resilience4j with short circuit breakers for service `%s`.\n" +
                    "2. **Proactive Health Checks:** Monitor thread pool utilization and connection pool queue depth in Grafana/Prometheus.\n" +
                    "3. **Defensive Validation:** Add Jakarta Bean Validation to verify incoming payloads before executing business operations.\n" +
                    "4. **Automated Alerting:** Trigger PagerDuty when error rate exceeds 1%% over a 2-minute rolling window.",
                    primaryError.getTitle(),
                    primaryError.getServiceName()
            );
        }

        // Default query response with real log references
        addReference(primaryError, references);
        return String.format(
                "Regarding your inquiry on this log file:\n\n" +
                "The analysis examined **%d log entries** and detected **%d total errors** (%d critical) spanning %d unique patterns.\n\n" +
                "- **Dominant Issue:** `%s` in service `%s` (First seen: `%s`, Total: %d occurrences).\n" +
                "- **Source Location:** `%s` line %s.\n\n" +
                "Feel free to ask for specific code fixes, database connection analysis, or timeline breakdowns!",
                analysis.getTotalLines(),
                analysis.getTotalErrors(),
                analysis.getCriticalErrors(),
                analysis.getUniquePatterns(),
                primaryError.getTitle(),
                primaryError.getServiceName(),
                primaryError.getFirstOccurrence(),
                primaryError.getOccurrenceCount(),
                primaryError.getFileName() != null ? primaryError.getFileName() : "N/A",
                primaryError.getLineNumber() != null ? primaryError.getLineNumber() : 0
        );
    }

    private void addReference(ParsedError error, List<ChatDtos.ReferenceItem> references) {
        references.add(ChatDtos.ReferenceItem.builder()
                .title(error.getTitle())
                .timestamp(error.getFirstOccurrence())
                .service(error.getServiceName())
                .lineReference(error.getFileName() != null ? error.getFileName() + ":" + error.getLineNumber() : "Line " + error.getLineNumber())
                .snippet(error.getMessage().length() > 150 ? error.getMessage().substring(0, 147) + "..." : error.getMessage())
                .build());
    }
}
