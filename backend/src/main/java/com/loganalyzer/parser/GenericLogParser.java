package com.loganalyzer.parser;

import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class GenericLogParser implements LogParser {

    private static final Pattern LOG_LEVEL_PATTERN = Pattern.compile("(?i)\\b(FATAL|CRITICAL|SEVERE|ERROR|ERR|WARN|WARNING|WARNUNG|INFO|DEBUG|TRACE)\\b");
    private static final Pattern TIMESTAMP_PATTERN = Pattern.compile("\\b(\\d{4}[-/]\\d{2}[-/]\\d{2}[T\\s]\\d{2}:\\d{2}:\\d{2}(?:[.,]\\d{3})?|[A-Za-z]{3}\\s+\\d{1,2}\\s+\\d{2}:\\d{2}:\\d{2})\\b");
    private static final Pattern STACK_TRACE_LINE = Pattern.compile("^(\\s*at\\s+|Caused by:|\\s*\\t).*");

    private final MaskingService maskingService;

    public GenericLogParser(MaskingService maskingService) {
        this.maskingService = maskingService;
    }

    @Override
    public boolean canParse(String filename, String sampleLine) {
        // Fallback parser that accepts any file
        return true;
    }

    @Override
    public void parse(BufferedReader reader, Consumer<RawLogEntry> consumer) throws IOException {
        String line;
        long lineNumber = 0;
        RawLogEntry currentEntry = null;

        while ((line = reader.readLine()) != null) {
            lineNumber++;
            String masked = maskingService.mask(line);

            Matcher levelMatcher = LOG_LEVEL_PATTERN.matcher(masked);
            Matcher timeMatcher = TIMESTAMP_PATTERN.matcher(masked);

            boolean hasLevel = levelMatcher.find();
            boolean isStackLine = STACK_TRACE_LINE.matcher(masked).matches();

            if (hasLevel && !isStackLine) {
                if (currentEntry != null) {
                    consumer.accept(currentEntry);
                }

                String level = levelMatcher.group(1).toUpperCase();
                if (level.equals("ERR") || level.equals("SEVERE")) level = "ERROR";
                if (level.equals("WARNING")) level = "WARN";

                String timestamp = timeMatcher.find() ? timeMatcher.group(1) : "UNKNOWN";

                currentEntry = RawLogEntry.builder()
                        .lineNumber(lineNumber)
                        .timestamp(timestamp)
                        .level(level)
                        .service("system")
                        .message(masked)
                        .rawLine(masked)
                        .build();

            } else if (isStackLine || masked.contains("Exception") || masked.contains("Error")) {
                if (currentEntry != null) {
                    currentEntry.getStackTraceLines().add(masked);
                } else {
                    currentEntry = RawLogEntry.builder()
                            .lineNumber(lineNumber)
                            .timestamp("UNKNOWN")
                            .level("ERROR")
                            .service("system")
                            .message(masked)
                            .rawLine(masked)
                            .build();
                }
            } else {
                if (currentEntry != null && (currentEntry.isErrorOrFatal() || currentEntry.isWarning())) {
                    currentEntry.getStackTraceLines().add(masked);
                }
            }
        }

        if (currentEntry != null) {
            consumer.accept(currentEntry);
        }
    }
}
