package com.loganalyzer.ai;

import com.loganalyzer.entity.ErrorCategory;
import com.loganalyzer.entity.ParsedError;
import com.loganalyzer.entity.RootCauseReport;
import org.springframework.stereotype.Service;

@Service
public class HeuristicSeniorSreEngine {

    public RootCauseReport generateReport(ParsedError error) {
        String msg = (error.getMessage() + " " + (error.getStackTrace() != null ? error.getStackTrace() : "")).toLowerCase();
        ErrorCategory category = error.getCategory();

        String whatWentWrong;
        String whyItHappened;
        String affectedComponent = error.getServiceName() != null ? error.getServiceName() : "Backend Service";
        String impactAssessment;
        String primaryOrSecondary = Boolean.TRUE.equals(error.getIsPrimary()) ? "PRIMARY" : "SECONDARY";
        String supportingEvidence = error.getSupportingEvidence() != null ? error.getSupportingEvidence() :
                "Identified based on log signature at line " + error.getLineNumber() + " with " + error.getOccurrenceCount() + " occurrences.";
        String immediateFix;
        String stepByStepDebugging;
        String suggestedCodeChange;
        String troubleshootingCommands;
        String longTermPrevention;

        if (msg.contains("hikaricp") || msg.contains("connection is not available") || msg.contains("pool exhausted")) {
            whatWentWrong = "HikariCP database connection pool has been exhausted. Worker threads are waiting past maximum timeout (default 30,000ms) without obtaining an open JDBC connection.";
            whyItHappened = "Active connections are either leaking (open transactions not committed/closed, unclosed ResultSets/Streams) or slow SQL queries are holding connections far longer than pool replenishment rate under load.";
            affectedComponent = error.getServiceName() + " -> Database Connection Pool (HikariCP)";
            impactAssessment = "CRITICAL: Downstream HTTP requests fail with HTTP 500 / 504. Incoming traffic queue stalls, eventually triggering cascade thread exhaustion across worker nodes.";
            immediateFix = "Temporarily increase HikariCP maximum pool size (e.g., maximum-pool-size: 30) and decrease leak-detection-threshold to 2000ms to immediately log leaking stack traces.";
            stepByStepDebugging = "1. Enable HikariCP leak detection in application.yml:\n" +
                    "   spring.datasource.hikari.leak-detection-threshold=2000\n" +
                    "2. Check active PostgreSQL connections:\n" +
                    "   SELECT pid, now() - query_start AS duration, query, state FROM pg_stat_activity WHERE state != 'idle';\n" +
                    "3. Inspect methods annotated with @Transactional to ensure they do not perform remote HTTP calls inside database transactions.";
            suggestedCodeChange = "// 1. Ensure transactions wrap only fast DB operations, NOT remote API calls:\n" +
                    "@Transactional\n" +
                    "public OrderResponse processOrder(OrderRequest req) {\n" +
                    "    // BAD: paymentClient.charge(req); // Keeps DB connection locked!\n" +
                    "    // GOOD: Perform HTTP call outside @Transactional, then save state.\n" +
                    "    return orderRepository.save(order);\n" +
                    "}\n" +
                    "\n" +
                    "// application.yml configuration fix:\n" +
                    "spring:\n" +
                    "  datasource:\n" +
                    "    hikari:\n" +
                    "      maximum-pool-size: 25\n" +
                    "      minimum-idle: 10\n" +
                    "      connection-timeout: 20000\n" +
                    "      leak-detection-threshold: 3000";
            troubleshootingCommands = "# Check active Postgres connections and locks:\n" +
                    "psql -U postgres -d appdb -c \"SELECT count(*), state FROM pg_stat_activity GROUP BY state;\"\n" +
                    "# Inspect thread dump for threads waiting on Hikari pool:\n" +
                    "jstack -l <PID> | grep -A 10 \"HikariPool\"";
            longTermPrevention = "Implement strict connection leak detection in CI integration tests, enforce Open-Session-In-View=false in Spring Boot, and set aggressive query timeouts.";

        } else if (category == ErrorCategory.MEMORY_ISSUES || msg.contains("outofmemoryerror") || msg.contains("heap space")) {
            whatWentWrong = "Java Virtual Machine (JVM) exhausted Java Heap Memory (java.lang.OutOfMemoryError: Java heap space). Garbage Collector could not free sufficient memory for new object allocations.";
            whyItHappened = "Memory leak caused by un-evicted in-memory collections, unbounded cache, large file buffered completely into memory, or excessive pagination page size returning hundreds of thousands of rows.";
            affectedComponent = error.getServiceName() + " -> JVM Heap";
            impactAssessment = "DISASTROUS: Application enters unresponsive state, triggers consecutive Full GC pauses ('stop the world'), fails health check probes, and gets SIGKILLed by container orchestrator.";
            immediateFix = "Restart instance with increased -Xmx limit and trigger Heap Dump on OOM (-XX:+HeapDumpOnOutOfMemoryError).";
            stepByStepDebugging = "1. Verify JVM startup arguments: ensure -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/dumps/oom.hprof are present.\n" +
                    "2. Load generated .hprof file into Eclipse Memory Analyzer (MAT) or VisualVM.\n" +
                    "3. Run 'Dominator Tree' and 'Leak Suspects' report to identify classes consuming >70% retained heap.";
            suggestedCodeChange = "// Switch from loading entire collections into memory to streaming:\n" +
                    "// BAD: List<LogEntry> all = repo.findAll(); // Loads 1M records into heap!\n" +
                    "// GOOD: Stream or slice with paginated queries\n" +
                    "@QueryHints(@QueryHint(name = org.hibernate.jpa.HibernateHints.HINT_FETCH_SIZE, value = \"1000\"))\n" +
                    "Stream<LogEntry> streamAllByCreatedAtBefore(Instant cutoff);";
            troubleshootingCommands = "# Trigger live heap dump without restarting JVM:\n" +
                    "jcmd <PID> GC.heap_dump /tmp/heap_dump.hprof\n" +
                    "# Check real-time GC statistics:\n" +
                    "jstat -gcutil <PID> 1000 10";
            longTermPrevention = "Configure JVM memory limits based on container cgroups, use streaming APIs for large data processing, and implement TTLs for all local in-memory caches.";

        } else if (msg.contains("nullpointerexception")) {
            whatWentWrong = "NullPointerException thrown during dereference of an uninitialized reference or missing entity property.";
            whyItHappened = "An upstream service, optional database column, or unvalidated client payload returned null which was accessed directly without defensive null checks or Optional unwrapping.";
            affectedComponent = (error.getFileName() != null ? error.getFileName() : "Application Code") +
                    (error.getLineNumber() != null ? ":" + error.getLineNumber() : "");
            impactAssessment = "HIGH: Causes immediate transaction abort and HTTP 500 error response to client, failing the business transaction.";
            immediateFix = "Add null-check guards or use Optional.ofNullable() in " + (error.getFileName() != null ? error.getFileName() : "the calling method") + ".";
            stepByStepDebugging = "1. Check line " + (error.getLineNumber() != null ? error.getLineNumber() : "indicated in stack trace") + " in " + (error.getFileName() != null ? error.getFileName() : "source file") + ".\n" +
                    "2. Identify the variable being dereferenced.\n" +
                    "3. Trace where the value originates (HTTP request parameter, repository findById result, or external JSON deserialization).";
            suggestedCodeChange = "// Replace raw dereference with safe Optional pattern:\n" +
                    "// Before: user.getAddress().getCity().toUpperCase();\n" +
                    "// After:\n" +
                    "String city = Optional.ofNullable(user)\n" +
                    "    .map(User::getAddress)\n" +
                    "    .map(Address::getCity)\n" +
                    "    .orElse(\"UNKNOWN\");";
            troubleshootingCommands = "# Check application logs preceding the NPE for request payload:\n" +
                    "grep -B 5 -A 20 \"NullPointerException\" app.log";
            longTermPrevention = "Enable static analysis tools (SpotBugs / SonarQube), use Java 21 Record types with Jakarta Validation (@NotNull), and enforce Optional return types for lookup methods.";

        } else if (category == ErrorCategory.TIMEOUT_ERRORS || msg.contains("sockettimeout") || msg.contains("connecttimeout")) {
            whatWentWrong = "Network I/O timeout while waiting for external service or database response.";
            whyItHappened = "Upstream partner service or downstream microservice degraded, experiencing latency spikes or dropped packets, exceeding the configured client socket timeout.";
            affectedComponent = error.getServiceName() + " -> External API / HTTP Client";
            impactAssessment = "HIGH: Threads stay blocked waiting on socket read, causing thread pool depletion and cascading latency to end users.";
            immediateFix = "Implement fallback circuit breaker responses and configure sensible connection and read timeouts.";
            stepByStepDebugging = "1. Check latency and DNS resolution of destination endpoint.\n" +
                    "2. Verify network routing and egress firewall rules.\n" +
                    "3. Review client timeout configurations in application.yml.";
            suggestedCodeChange = "// Configure explicit timeouts and Resilience4j Circuit Breaker:\n" +
                    "@Bean\n" +
                    "public RestTemplate restTemplate(RestTemplateBuilder builder) {\n" +
                    "    return builder\n" +
                    "        .setConnectTimeout(Duration.ofSeconds(3))\n" +
                    "        .setReadTimeout(Duration.ofSeconds(5))\n" +
                    "        .build();\n" +
                    "}";
            troubleshootingCommands = "# Test endpoint latency and connectivity:\n" +
                    "curl -w \"@curl-format.txt\" -o /dev/null -s -v \"https://api.partner.com/health\"\n" +
                    "# Inspect active socket connections in CLOSE_WAIT or SYN_SENT:\n" +
                    "netstat -an | grep -E \"(CLOSE_WAIT|SYN_SENT)\"";
            longTermPrevention = "Wrap all cross-service network calls with Resilience4j circuit breakers, bulkhead isolation, and asynchronous fallback handlers.";

        } else if (category == ErrorCategory.AUTHENTICATION_ERRORS || msg.contains("expiredjwtexception") || msg.contains("badcredentials")) {
            whatWentWrong = "Authentication or Authorization rejection: client request failed security validation.";
            whyItHappened = "Supplied JWT token has expired, signature verification failed, or credentials provided do not match security identity provider store.";
            affectedComponent = error.getServiceName() + " -> Spring Security Filter Chain";
            impactAssessment = "MEDIUM: Unauthorized requests are rejected with HTTP 401/403. If widespread, could indicate clock skew between auth servers or broken token refresh flow.";
            immediateFix = "Verify client refresh token flow and ensure authentication server NTP clock synchronization.";
            stepByStepDebugging = "1. Decode JWT payload to verify 'exp' (expiration) and 'iss' (issuer) claims.\n" +
                    "2. Check system time across cluster nodes for clock drift.\n" +
                    "3. Review CORS and header forwarding in reverse proxy (NGINX/Cloudflare).";
            suggestedCodeChange = "// Enhance JwtAuthenticationFilter to gracefully handle token expiration:\n" +
                    "try {\n" +
                    "    jwtUtil.validateToken(jwt, userDetails);\n" +
                    "} catch (ExpiredJwtException ex) {\n" +
                    "    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);\n" +
                    "    response.getWriter().write(\"{\\\"error\\\": \\\"TOKEN_EXPIRED\\\"}\");\n" +
                    "    return;\n" +
                    "}";
            troubleshootingCommands = "# Check local system NTP clock sync:\n" +
                    "w32tm /query /status\n" +
                    "# Inspect token expiration claim:\n" +
                    "jwt decode <TOKEN>";
            longTermPrevention = "Implement automated token refresh before expiration, configure clock skew tolerance (e.g., 30s) in JWT parser, and audit auth failures for brute-force patterns.";

        } else {
            // General Senior SRE Deep Technical Analysis for any other error
            whatWentWrong = "Execution error in " + error.getServiceName() + ": [" + error.getTitle() + "]. " +
                    (error.getMessage() != null && !error.getMessage().isEmpty() ? error.getMessage() : "Unhandled runtime exception encountered.");
            whyItHappened = "Exception raised during request lifecycle. State condition was violated or an unhandled failure propagated through the service layer without graceful error handling.";
            affectedComponent = error.getServiceName() + (error.getFileName() != null ? " (" + error.getFileName() + ")" : "");
            impactAssessment = "Service component experienced failure. Affected requests received an error status. Impact severity is classified as " + error.getSeverity() + ".";
            immediateFix = "Inspect line " + (error.getLineNumber() != null ? error.getLineNumber() : "indicated in logs") + " in " +
                    (error.getFileName() != null ? error.getFileName() : "service") + " and add comprehensive exception boundaries.";
            stepByStepDebugging = "1. Correlate timestamp " + error.getFirstOccurrence() + " with infrastructure metrics.\n" +
                    "2. Isolate incoming HTTP/Kafka request that triggered the stack trace.\n" +
                    "3. Reproduce issue in staging environment with identical input parameters.";
            suggestedCodeChange = "// Wrap failure-prone block with specific error handling:\n" +
                    "try {\n" +
                    "    // Business logic in " + error.getServiceName() + "\n" +
                    "} catch (Exception ex) {\n" +
                    "    log.error(\"Operation failed for component: {}\", ex.getMessage(), ex);\n" +
                    "    throw new BusinessException(\"Failed to process operation\", ex);\n" +
                    "}";
            troubleshootingCommands = "# Search logs for similar error patterns across all nodes:\n" +
                    "grep -rn \"" + error.getTitle() + "\" /var/log/application/";
            longTermPrevention = "Add comprehensive integration test suites, configure alerting on error rate thresholds, and implement standardized global error handling.";
        }

        return RootCauseReport.builder()
                .parsedError(error)
                .whatWentWrong(whatWentWrong)
                .whyItHappened(whyItHappened)
                .affectedComponent(affectedComponent)
                .impactAssessment(impactAssessment)
                .primaryOrSecondary(primaryOrSecondary)
                .supportingEvidence(supportingEvidence)
                .immediateFix(immediateFix)
                .stepByStepDebugging(stepByStepDebugging)
                .suggestedCodeChange(suggestedCodeChange)
                .troubleshootingCommands(troubleshootingCommands)
                .longTermPrevention(longTermPrevention)
                .build();
    }
}
