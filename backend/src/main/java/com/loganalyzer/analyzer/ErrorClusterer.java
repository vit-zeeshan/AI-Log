package com.loganalyzer.analyzer;

import com.loganalyzer.entity.ErrorCategory;
import com.loganalyzer.entity.ParsedError;
import com.loganalyzer.entity.Severity;
import com.loganalyzer.parser.RawLogEntry;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

@Service
public class ErrorClusterer {

    private final ErrorClassifier classifier;

    public ErrorClusterer(ErrorClassifier classifier) {
        this.classifier = classifier;
    }

    public List<ParsedError> cluster(List<RawLogEntry> rawEntries) {
        Map<String, ParsedErrorAccumulator> clusters = new LinkedHashMap<>();

        for (RawLogEntry entry : rawEntries) {
            if (!entry.isErrorOrFatal() && !entry.isWarning() && !entry.hasStackTrace()) {
                continue;
            }

            String title = classifier.extractTitle(entry);
            ErrorCategory category = classifier.classify(entry);
            Severity severity = classifier.determineSeverity(category, entry);
            ErrorClassifier.StackFrameInfo frameInfo = classifier.extractTopStackFrame(entry);

            String fingerprint = generateFingerprint(title, frameInfo, entry.getMessage());

            clusters.compute(fingerprint, (k, acc) -> {
                if (acc == null) {
                    acc = new ParsedErrorAccumulator(
                            title,
                            entry.getMessage(),
                            severity,
                            category,
                            entry.getTimestamp(),
                            frameInfo.fileName(),
                            frameInfo.lineNumber(),
                            entry.getFullStackTrace(),
                            entry.getService(),
                            fingerprint,
                            entry.getRawLine()
                    );
                } else {
                    acc.increment(entry.getTimestamp());
                }
                return acc;
            });
        }

        List<ParsedError> result = new ArrayList<>();
        for (ParsedErrorAccumulator acc : clusters.values()) {
            result.add(acc.toParsedError());
        }

        return result;
    }

    private String generateFingerprint(String title, ErrorClassifier.StackFrameInfo frame, String message) {
        // Normalize: remove numbers, UUIDs, memory addresses, timestamps
        String normalizedMsg = message != null ? message.replaceAll("0x[0-9a-fA-F]+", "[ADDR]")
                .replaceAll("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}", "[UUID]")
                .replaceAll("\\d{4}-\\d{2}-\\d{2}", "[DATE]")
                .replaceAll("\\d+", "[N]") : "";

        String raw = title + ":" + (frame.className() != null ? frame.className() : "") + ":" +
                     (frame.fileName() != null ? frame.fileName() : "") + ":" +
                     (normalizedMsg.length() > 60 ? normalizedMsg.substring(0, 60) : normalizedMsg);

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (int i = 0; i < Math.min(16, hash.length); i++) {
                String hex = Integer.toHexString(0xff & hash[i]);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return String.valueOf(raw.hashCode());
        }
    }

    private static class ParsedErrorAccumulator {
        private final String title;
        private final String message;
        private final Severity severity;
        private final ErrorCategory category;
        private final String firstOccurrence;
        private String lastOccurrence;
        private final String fileName;
        private final Integer lineNumber;
        private final String stackTrace;
        private final String serviceName;
        private final String fingerprint;
        private final String rawSnippet;
        private int count = 1;

        public ParsedErrorAccumulator(String title, String message, Severity severity,
                                      ErrorCategory category, String timestamp,
                                      String fileName, Integer lineNumber, String stackTrace,
                                      String serviceName, String fingerprint, String rawSnippet) {
            this.title = title;
            this.message = message;
            this.severity = severity;
            this.category = category;
            this.firstOccurrence = timestamp;
            this.lastOccurrence = timestamp;
            this.fileName = fileName;
            this.lineNumber = lineNumber;
            this.stackTrace = stackTrace;
            this.serviceName = serviceName;
            this.fingerprint = fingerprint;
            this.rawSnippet = rawSnippet;
        }

        public void increment(String timestamp) {
            this.count++;
            if (timestamp != null && !timestamp.equals("UNKNOWN")) {
                this.lastOccurrence = timestamp;
            }
        }

        public ParsedError toParsedError() {
            return ParsedError.builder()
                    .title(title)
                    .message(message)
                    .severity(severity)
                    .category(category)
                    .errorTimestamp(firstOccurrence)
                    .fileName(fileName)
                    .lineNumber(lineNumber)
                    .stackTrace(stackTrace)
                    .serviceName(serviceName)
                    .occurrenceCount(count)
                    .firstOccurrence(firstOccurrence)
                    .lastOccurrence(lastOccurrence)
                    .fingerprint(fingerprint)
                    .isPrimary(false)
                    .rawSnippet(rawSnippet)
                    .build();
        }
    }
}
