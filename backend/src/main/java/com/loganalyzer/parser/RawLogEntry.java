package com.loganalyzer.parser;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RawLogEntry {
    private long lineNumber;
    private String timestamp;
    private String level; // ERROR, WARN, INFO, FATAL, CRITICAL, DEBUG
    private String thread;
    private String logger;
    private String service;
    private String message;
    @Builder.Default
    private List<String> stackTraceLines = new ArrayList<>();
    private String rawLine;

    public boolean isErrorOrFatal() {
        if (level == null) return false;
        String upper = level.toUpperCase();
        return upper.contains("ERROR") || upper.contains("FATAL") || upper.contains("CRITICAL");
    }

    public boolean isWarning() {
        if (level == null) return false;
        return level.toUpperCase().contains("WARN");
    }

    public boolean hasStackTrace() {
        return stackTraceLines != null && !stackTraceLines.isEmpty();
    }

    public String getFullStackTrace() {
        if (stackTraceLines == null || stackTraceLines.isEmpty()) {
            return "";
        }
        return String.join("\n", stackTraceLines);
    }
}
