package com.loganalyzer.parser;

import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class SpringBootLogParser implements LogParser {

    // Matches standard Spring Boot / Logback / Log4j2 timestamped lines:
    // e.g. 2024-11-14 10:23:45.123 [thread] ERROR com.package.Class - message
    // e.g. 2024-11-14T10:23:45.123Z [service] ERROR ...
    // e.g. 2024-11-14 10:23:45 ERROR [thread] logger - message
    private static final Pattern SPRING_LOG_PATTERN = Pattern.compile(
            "^(\\d{4}-\\d{2}-\\d{2}[T\\s]\\d{2}:\\d{2}:\\d{2}(?:[.,]\\d{3})?(?:Z|[+-]\\d{2}:?\\d{2})?)\\s*(?:\\[(.*?)\\])?\\s*([A-Z]+|ERROR|WARN|INFO|DEBUG|FATAL|TRACE)\\s+(?:(?:\\d+\\s+---\\s+)?(?:\\[(.*?)\\])?\\s*)?(?:([\\w.\\-$]+)\\s*(?:\\(.*?\\))?\\s*[:\\-]\\s*)?(.*)$"
    );

    private static final Pattern SIMPLE_PATTERN = Pattern.compile(
            "^(\\d{4}-\\d{2}-\\d{2}[T\\s]\\d{2}:\\d{2}:\\d{2}(?:[.,]\\d{3})?)\\s+(FATAL|ERROR|WARN|WARNING|INFO|DEBUG|TRACE)\\s+(.*)$"
    );

    private static final Pattern STACK_TRACE_PATTERN = Pattern.compile(
            "^(\\s+at\\s+|Caused by:|org\\.|java\\.|com\\.|io\\.|jakarta\\.|\\.\\.\\.\\s+\\d+\\s+more|\\s*\\t+at\\s+).*"
    );

    private final MaskingService maskingService;

    public SpringBootLogParser(MaskingService maskingService) {
        this.maskingService = maskingService;
    }

    @Override
    public boolean canParse(String filename, String sampleLine) {
        if (sampleLine == null) return false;
        return SPRING_LOG_PATTERN.matcher(sampleLine).matches() || SIMPLE_PATTERN.matcher(sampleLine).matches();
    }

    @Override
    public void parse(BufferedReader reader, Consumer<RawLogEntry> consumer) throws IOException {
        String line;
        long lineNumber = 0;
        RawLogEntry currentEntry = null;

        while ((line = reader.readLine()) != null) {
            lineNumber++;
            String maskedLine = maskingService.mask(line);

            Matcher matcher = SPRING_LOG_PATTERN.matcher(maskedLine);
            if (!matcher.matches()) {
                matcher = SIMPLE_PATTERN.matcher(maskedLine);
            }

            if (matcher.matches()) {
                // New log entry begins, emit previous entry if exists
                if (currentEntry != null) {
                    consumer.accept(currentEntry);
                }

                String timestamp = matcher.group(1);
                String level;
                String service = null;
                String thread = null;
                String logger = null;
                String message;

                if (matcher.groupCount() >= 6) {
                    // SPRING_LOG_PATTERN
                    service = matcher.group(2);
                    level = matcher.group(3);
                    thread = matcher.group(4);
                    logger = matcher.group(5);
                    message = matcher.group(6);
                } else {
                    // SIMPLE_PATTERN
                    level = matcher.group(2);
                    message = matcher.group(3);
                }

                currentEntry = RawLogEntry.builder()
                        .lineNumber(lineNumber)
                        .timestamp(timestamp)
                        .level(level != null ? level.toUpperCase() : "INFO")
                        .service(extractService(service, logger))
                        .thread(thread)
                        .logger(logger)
                        .message(message != null ? message : "")
                        .rawLine(maskedLine)
                        .build();

            } else {
                // Continuation line (e.g. multi-line stack trace or indented message)
                if (currentEntry != null) {
                    currentEntry.getStackTraceLines().add(maskedLine);
                } else if (STACK_TRACE_PATTERN.matcher(maskedLine).matches() || maskedLine.contains("Exception") || maskedLine.contains("Error")) {
                    // Standalone stack trace at top
                    currentEntry = RawLogEntry.builder()
                            .lineNumber(lineNumber)
                            .timestamp("UNKNOWN")
                            .level("ERROR")
                            .message(maskedLine)
                            .rawLine(maskedLine)
                            .build();
                }
            }
        }

        if (currentEntry != null) {
            consumer.accept(currentEntry);
        }
    }

    private String extractService(String explicitService, String logger) {
        if (explicitService != null && !explicitService.trim().isEmpty()) {
            return explicitService.trim();
        }
        if (logger != null && !logger.trim().isEmpty()) {
            // e.g. com.bank.payment.service.PaymentService -> payment-service
            String[] parts = logger.split("\\.");
            if (parts.length >= 2) {
                return parts[parts.length - 2] + "-service";
            }
            return parts[parts.length - 1];
        }
        return "application";
    }
}
