package com.loganalyzer.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loganalyzer.entity.ParsedError;
import com.loganalyzer.entity.RootCauseReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class LlmAiAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(LlmAiAnalysisService.class);
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.ai.api-key:}")
    private String apiKey;

    @Value("${app.ai.model:gpt-4o-mini}")
    private String model;

    @Value("${app.ai.api-url:https://api.openai.com/v1/chat/completions}")
    private String apiUrl;

    public boolean isConfigured() {
        return apiKey != null && !apiKey.trim().isEmpty() && !apiKey.equalsIgnoreCase("none");
    }

    public RootCauseReport analyzeWithLlm(ParsedError error) {
        if (!isConfigured()) {
            return null;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            String systemPrompt = "You are a Principal Java Backend Engineer, Site Reliability Engineer (SRE), and DevOps Architect. " +
                    "Analyze the following log error and stack trace. You must return ONLY a valid JSON object with the following fields: " +
                    "whatWentWrong, whyItHappened, affectedComponent, impactAssessment, primaryOrSecondary, supportingEvidence, immediateFix, stepByStepDebugging, suggestedCodeChange, troubleshootingCommands, longTermPrevention. " +
                    "Never invent an exact root cause when evidence is lacking; state confirmed findings vs probable causes. Do not wrap in markdown quotes if possible, return raw json.";

            String userPrompt = String.format("Error Title: %s\nCategory: %s\nSeverity: %s\nService: %s\nTimestamp: %s\nFile: %s:%s\nOccurrences: %d\nMessage: %s\nStack Trace:\n%s",
                    error.getTitle(),
                    error.getCategory().getDisplayName(),
                    error.getSeverity(),
                    error.getServiceName(),
                    error.getFirstOccurrence(),
                    error.getFileName(),
                    error.getLineNumber(),
                    error.getOccurrenceCount(),
                    error.getMessage(),
                    error.getStackTrace() != null ? error.getStackTrace() : "None");

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("messages", List.of(
                    Map.of("role", "system", "content", systemPrompt),
                    Map.of("role", "user", "content", userPrompt)
            ));
            requestBody.put("temperature", 0.2);
            requestBody.put("response_format", Map.of("type", "json_object"));

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                String content = root.path("choices").path(0).path("message").path("content").asText();
                JsonNode parsedContent = objectMapper.readTree(content);

                return RootCauseReport.builder()
                        .parsedError(error)
                        .whatWentWrong(parsedContent.path("whatWentWrong").asText("Analysis completed"))
                        .whyItHappened(parsedContent.path("whyItHappened").asText("See evidence"))
                        .affectedComponent(parsedContent.path("affectedComponent").asText(error.getServiceName()))
                        .impactAssessment(parsedContent.path("impactAssessment").asText("Evaluated"))
                        .primaryOrSecondary(parsedContent.path("primaryOrSecondary").asText("PRIMARY"))
                        .supportingEvidence(parsedContent.path("supportingEvidence").asText("Derived from log trace"))
                        .immediateFix(parsedContent.path("immediateFix").asText("Review code"))
                        .stepByStepDebugging(parsedContent.path("stepByStepDebugging").asText("Inspect logs"))
                        .suggestedCodeChange(parsedContent.path("suggestedCodeChange").asText(""))
                        .troubleshootingCommands(parsedContent.path("troubleshootingCommands").asText(""))
                        .longTermPrevention(parsedContent.path("longTermPrevention").asText("Add monitoring"))
                        .build();
            }
        } catch (Exception e) {
            log.warn("LLM API call failed, falling back to senior heuristic engine: {}", e.getMessage());
        }

        return null;
    }
}
