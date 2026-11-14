package com.loganalyzer.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

@Component
public class JsonLogParser implements LogParser {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final MaskingService maskingService;

    public JsonLogParser(MaskingService maskingService) {
        this.maskingService = maskingService;
    }

    @Override
    public boolean canParse(String filename, String sampleLine) {
        if (filename != null && filename.toLowerCase().endsWith(".json")) {
            return true;
        }
        if (sampleLine != null) {
            String trimmed = sampleLine.trim();
            return (trimmed.startsWith("{") && trimmed.endsWith("}")) || trimmed.startsWith("[{");
        }
        return false;
    }

    @Override
    public void parse(BufferedReader reader, Consumer<RawLogEntry> consumer) throws IOException {
        String line;
        long lineNumber = 0;

        while ((line = reader.readLine()) != null) {
            lineNumber++;
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.equals("[") || trimmed.equals("]")) {
                continue;
            }
            if (trimmed.endsWith(",")) {
                trimmed = trimmed.substring(0, trimmed.length() - 1);
            }

            try {
                JsonNode root = objectMapper.readTree(trimmed);
                String timestamp = findText(root, "timestamp", "@timestamp", "time", "date", "datetime");
                String level = findText(root, "level", "severity", "logLevel", "status");
                String message = findText(root, "message", "msg", "error", "description");
                String service = findText(root, "service", "serviceName", "app", "application", "logger");
                String thread = findText(root, "thread", "thread_name");
                String stackTrace = findText(root, "stackTrace", "stack_trace", "exception", "trace");

                List<String> stackLines = new ArrayList<>();
                if (stackTrace != null && !stackTrace.isBlank()) {
                    stackLines = Arrays.asList(maskingService.mask(stackTrace).split("\\r?\\n"));
                }

                RawLogEntry entry = RawLogEntry.builder()
                        .lineNumber(lineNumber)
                        .timestamp(timestamp != null ? timestamp : "UNKNOWN")
                        .level(level != null ? level.toUpperCase() : "INFO")
                        .service(service != null ? service : "application")
                        .thread(thread)
                        .message(maskingService.mask(message != null ? message : ""))
                        .stackTraceLines(stackLines)
                        .rawLine(maskingService.mask(trimmed))
                        .build();

                consumer.accept(entry);
            } catch (Exception e) {
                // Not valid json line, ignore or skip
            }
        }
    }

    private String findText(JsonNode root, String... candidates) {
        for (String candidate : candidates) {
            if (root.has(candidate)) {
                return root.get(candidate).asText();
            }
        }
        return null;
    }
}
