package com.loganalyzer.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "parsed_errors")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParsedError {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analysis_id", nullable = false)
    private LogAnalysis analysis;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Severity severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ErrorCategory category;

    @Column(name = "error_timestamp", length = 100)
    private String errorTimestamp;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "line_number")
    private Integer lineNumber;

    @Column(name = "stack_trace", columnDefinition = "TEXT")
    private String stackTrace;

    @Column(name = "service_name", length = 100)
    private String serviceName;

    @Column(name = "occurrence_count")
    @Builder.Default
    private Integer occurrenceCount = 1;

    @Column(name = "first_occurrence", length = 100)
    private String firstOccurrence;

    @Column(name = "last_occurrence", length = 100)
    private String lastOccurrence;

    @Column(length = 120)
    private String fingerprint;

    @Column(name = "is_primary")
    @Builder.Default
    private Boolean isPrimary = false;

    @Column(name = "supporting_evidence", columnDefinition = "TEXT")
    private String supportingEvidence;

    @Column(name = "raw_snippet", columnDefinition = "TEXT")
    private String rawSnippet;

    @OneToOne(mappedBy = "parsedError", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private RootCauseReport rootCauseReport;
}
