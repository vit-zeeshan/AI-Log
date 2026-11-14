package com.loganalyzer.parser;

import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class LogParserFactory {

    private final List<LogParser> parsers;

    public LogParserFactory(SpringBootLogParser springBootLogParser,
                            JsonLogParser jsonLogParser,
                            GenericLogParser genericLogParser) {
        // Order matters: specific parsers first, generic parser last as fallback
        this.parsers = List.of(jsonLogParser, springBootLogParser, genericLogParser);
    }

    public LogParser getParser(String filename, InputStream sampleStream) {
        String firstLine = null;
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(sampleStream, StandardCharsets.UTF_8));
            firstLine = reader.readLine();
        } catch (IOException e) {
            // Ignore
        }

        for (LogParser parser : parsers) {
            if (parser.canParse(filename, firstLine)) {
                return parser;
            }
        }
        return parsers.get(parsers.size() - 1);
    }
}
