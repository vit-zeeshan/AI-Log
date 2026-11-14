package com.loganalyzer.ai;

import com.loganalyzer.entity.ParsedError;
import com.loganalyzer.entity.RootCauseReport;
import org.springframework.stereotype.Service;

@Service
public class AiOrchestrator {

    private final LlmAiAnalysisService llmService;
    private final HeuristicSeniorSreEngine heuristicEngine;

    public AiOrchestrator(LlmAiAnalysisService llmService, HeuristicSeniorSreEngine heuristicEngine) {
        this.llmService = llmService;
        this.heuristicEngine = heuristicEngine;
    }

    public RootCauseReport generateRootCauseAnalysis(ParsedError error) {
        if (llmService.isConfigured()) {
            RootCauseReport llmReport = llmService.analyzeWithLlm(error);
            if (llmReport != null) {
                return llmReport;
            }
        }
        return heuristicEngine.generateReport(error);
    }
}
