package com.loganalyzer.analyzer;

import com.loganalyzer.entity.ErrorCategory;
import com.loganalyzer.entity.Severity;
import com.loganalyzer.parser.RawLogEntry;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ErrorClassifier {

    private static final Pattern STACK_FRAME_PATTERN = Pattern.compile(
            "at\\s+([a-zA-Z0-9_.$]+)\\.([a-zA-Z0-9_$<]+)\\((([a-zA-Z0-9_]+\\.java):(\\d+)|Native Method|Unknown Source)\\)"
    );

    private static final Pattern EXCEPTION_NAME_PATTERN = Pattern.compile(
            "\\b([a-zA-Z0-9_.]*(?:Exception|Error|Fault|Failure))\\b"
    );

    public ErrorCategory classify(RawLogEntry entry) {
        String text = (entry.getMessage() + " " + entry.getFullStackTrace()).toLowerCase();

        // 14 Categories as specified in Section 6:
        // 1. Memory Issues
        if (text.contains("outofmemoryerror") || text.contains("heap space") ||
            text.contains("gc overhead limit") || text.contains("metaspace") ||
            text.contains("native memory allocation")) {
            return ErrorCategory.MEMORY_ISSUES;
        }

        // 2. CPU Performance Issues
        if (text.contains("cpu throttled") || text.contains("high cpu") ||
            text.contains("thread starvation") || text.contains("thread pool exhausted")) {
            return ErrorCategory.CPU_PERFORMANCE_ISSUES;
        }

        // 3. Database Errors
        if (text.contains("sqlexception") || text.contains("psqlexception") ||
            text.contains("hikaricp") || text.contains("connection pool") ||
            text.contains("deadlock") || text.contains("hibernateexception") ||
            text.contains("constraintviolationexception") || text.contains("dataexception") ||
            text.contains("jdbc") || text.contains("transactionrolledback")) {
            return ErrorCategory.DATABASE_ERRORS;
        }

        // 4. Authentication / Security
        if (text.contains("accessdeniedexception") || text.contains("badcredentialsexception") ||
            text.contains("expiredjwtexception") || text.contains("unauthorized") ||
            text.contains("securityexception") || text.contains("forbidden") ||
            text.contains("authenticationfailed")) {
            return ErrorCategory.AUTHENTICATION_ERRORS;
        }

        if (text.contains("sql injection") || text.contains("xss") ||
            text.contains("csrf") || text.contains("malformed jwt") ||
            text.contains("tampered token") || text.contains("permission denied")) {
            return ErrorCategory.SECURITY_RELATED_EVENTS;
        }

        // 5. Timeout Errors
        if (text.contains("sockettimeoutexception") || text.contains("connecttimeoutexception") ||
            text.contains("request timeout") || text.contains("gateway timeout") ||
            text.contains("transactiontimedoutexception") || text.contains("timed out")) {
            return ErrorCategory.TIMEOUT_ERRORS;
        }

        // 6. Network Errors
        if (text.contains("connectexception") || text.contains("connection refused") ||
            text.contains("unknownhostexception") || text.contains("network unreachable") ||
            text.contains("connection reset by peer") || text.contains("sslhandshakeexception")) {
            return ErrorCategory.NETWORK_ERRORS;
        }

        // 7. API Errors
        if (text.contains("feignexception") || text.contains("restclientexception") ||
            text.contains("webclientresponseexception") || text.contains("http 500") ||
            text.contains("502 bad gateway") || text.contains("503 service unavailable") ||
            text.contains("httpstatuscodeexception")) {
            return ErrorCategory.API_ERRORS;
        }

        // 8. Configuration Errors
        if (text.contains("nosuchbeandefinitionexception") || text.contains("beancreationexception") ||
            text.contains("configurationexception") || text.contains("propertynotfoundexception") ||
            text.contains("missing required property") || text.contains("invalid configuration")) {
            return ErrorCategory.CONFIGURATION_ERRORS;
        }

        // 9. Deployment Errors
        if (text.contains("classnotfoundexception") || text.contains("noclassdeffounderror") ||
            text.contains("incompatibleclasschangeerror") || text.contains("unsupportedclassversionerror") ||
            text.contains("nosuchmethoderror")) {
            return ErrorCategory.DEPLOYMENT_ERRORS;
        }

        // 10. Dependency Failures
        if (text.contains("circuitbreakerexception") || text.contains("callnotpermittedexception") ||
            text.contains("serviceunavailable") || text.contains("resilience4j") ||
            text.contains("hystrix") || text.contains("upstream service failed")) {
            return ErrorCategory.DEPENDENCY_FAILURES;
        }

        // 11. Infrastructure Errors
        if (text.contains("oomkilled") || text.contains("disk full") ||
            text.contains("no space left on device") || text.contains("kubernetes") ||
            text.contains("docker") || text.contains("sigterm") || text.contains("container killed")) {
            return ErrorCategory.INFRASTRUCTURE_ERRORS;
        }

        // 12. Java Exceptions
        if (text.contains("nullpointerexception") || text.contains("illegalargumentexception") ||
            text.contains("arrayindexoutofboundsexception") || text.contains("classcastexception") ||
            text.contains("unsupportedoperationexception") || text.contains("runtimeexception") ||
            text.contains("illegalstateexception")) {
            return ErrorCategory.JAVA_EXCEPTIONS;
        }

        return ErrorCategory.APPLICATION_ERRORS;
    }

    public Severity determineSeverity(ErrorCategory category, RawLogEntry entry) {
        String level = entry.getLevel() != null ? entry.getLevel().toUpperCase() : "INFO";
        String text = (entry.getMessage() + " " + entry.getFullStackTrace()).toLowerCase();

        if (level.contains("FATAL") || level.contains("CRITICAL") ||
            category == ErrorCategory.MEMORY_ISSUES ||
            category == ErrorCategory.INFRASTRUCTURE_ERRORS ||
            text.contains("outofmemory") || text.contains("fatal") || text.contains("deadlock")) {
            return Severity.CRITICAL;
        }

        if (category == ErrorCategory.DATABASE_ERRORS ||
            category == ErrorCategory.SECURITY_RELATED_EVENTS ||
            category == ErrorCategory.DEPLOYMENT_ERRORS ||
            category == ErrorCategory.TIMEOUT_ERRORS ||
            level.contains("ERROR")) {
            return Severity.HIGH;
        }

        if (level.contains("WARN")) {
            return Severity.MEDIUM;
        }

        return Severity.LOW;
    }

    public String extractTitle(RawLogEntry entry) {
        // Extract exception name if present
        Matcher matcher = EXCEPTION_NAME_PATTERN.matcher(entry.getMessage());
        if (matcher.find()) {
            String exc = matcher.group(1);
            return exc;
        }

        // Check in stack trace
        for (String line : entry.getStackTraceLines()) {
            Matcher m = EXCEPTION_NAME_PATTERN.matcher(line);
            if (m.find()) {
                return m.group(1);
            }
        }

        // Shorten message as title
        String msg = entry.getMessage().trim();
        if (msg.isEmpty()) {
            return "Application Error in " + entry.getService();
        }
        int newlineIdx = msg.indexOf('\n');
        if (newlineIdx > 0) {
            msg = msg.substring(0, newlineIdx);
        }
        return msg.length() > 80 ? msg.substring(0, 77) + "..." : msg;
    }

    public StackFrameInfo extractTopStackFrame(RawLogEntry entry) {
        for (String line : entry.getStackTraceLines()) {
            Matcher matcher = STACK_FRAME_PATTERN.matcher(line);
            if (matcher.find()) {
                String className = matcher.group(1);
                String fileName = matcher.group(4);
                int lineNumber = matcher.group(5) != null ? Integer.parseInt(matcher.group(5)) : 0;
                return new StackFrameInfo(className, fileName, lineNumber);
            }
        }
        return new StackFrameInfo(null, null, null);
    }

    public record StackFrameInfo(String className, String fileName, Integer lineNumber) {}
}
