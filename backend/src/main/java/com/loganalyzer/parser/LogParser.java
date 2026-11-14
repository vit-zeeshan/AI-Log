package com.loganalyzer.parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.function.Consumer;

public interface LogParser {
    boolean canParse(String filename, String sampleLine);
    void parse(BufferedReader reader, Consumer<RawLogEntry> consumer) throws IOException;
}
