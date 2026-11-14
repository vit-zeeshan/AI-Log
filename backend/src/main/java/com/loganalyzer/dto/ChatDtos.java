package com.loganalyzer.dto;

import com.loganalyzer.entity.MessageRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

public class ChatDtos {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatRequest {
        private Long logFileId;
        private Long analysisId;
        private String message;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatResponse {
        private Long id;
        private MessageRole role;
        private String content;
        private List<ReferenceItem> references;
        private LocalDateTime timestamp;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReferenceItem {
        private String title;
        private String timestamp;
        private String lineReference;
        private String service;
        private String snippet;
    }
}
